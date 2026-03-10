import json
import os
import re
import requests
from typing import List, Optional
from dotenv import load_dotenv
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
import numpy as np
from numpy.linalg import norm
from sentence_transformers import SentenceTransformer
import sympy
from sympy.parsing.latex import parse_latex

router = APIRouter(prefix="/evaluate", tags=["Evaluation Pipeline"])
load_dotenv()

print("Loading vector model")
vector_model = SentenceTransformer('all-MiniLM-L6-v2')
print("Model loaded")

LMSTUDIO_URL = os.getenv("LMSTUDIO_URL", "http://localhost:1234/v1/chat/completions")


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _clean_llm_response(text: str) -> str:
    """Aggressively strip <think>...</think> tags and markdown code fences."""
    text = re.sub(r'<think>.*?</think>', '', text, flags=re.DOTALL).strip()
    if text.startswith("```json"):
        text = text[7:]
    elif text.startswith("```"):
        text = text[3:]
    if text.endswith("```"):
        text = text[:-3]
    return text.strip()


def _call_lmstudio(payload: dict, step_name: str) -> str:
    """Call LM Studio local server. Returns the raw content string."""
    try:
        resp = requests.post(LMSTUDIO_URL, json=payload, timeout=180)
    except requests.exceptions.ConnectionError:
        raise HTTPException(
            status_code=503,
            detail=f"Could not connect to LM Studio during {step_name}. Please ensure it is running."
        )
    except requests.exceptions.Timeout:
        raise HTTPException(
            status_code=504,
            detail=f"LM Studio timed out during {step_name}. The model may be overloaded."
        )
    except requests.exceptions.RequestException as e:
        raise HTTPException(status_code=503, detail=f"Failed to reach LM Studio during {step_name}: {str(e)}")

    if not resp.ok:
        raise HTTPException(
            status_code=502,
            detail=f"LM Studio returned an error (HTTP {resp.status_code}) during {step_name}."
        )

    try:
        return resp.json()["choices"][0]["message"]["content"].strip()
    except (KeyError, IndexError, TypeError):
        raise HTTPException(status_code=502, detail=f"LM Studio returned an unexpected response during {step_name}.")


def _parse_eval_json(raw_text: str, step_name: str) -> dict:
    """Clean LLM output and parse it as the evaluation JSON schema."""
    cleaned = _clean_llm_response(raw_text)
    print(f"   [{step_name} Cleaned] {cleaned[:500]}")

    try:
        data = json.loads(cleaned)
    except json.JSONDecodeError:
        raise HTTPException(
            status_code=502,
            detail=f"LM Studio returned a malformed response during {step_name}. Cleaned: {cleaned[:300]}"
        )

    return {
        "semantic_score": max(0.0, min(1.0, float(data.get("semantic_score", 0.0)))),
        "missing_concepts": [str(c) for c in data.get("missing_concepts", []) if c],
        "feedback": str(data.get("feedback", "No feedback generated."))
    }


def _evaluate_math_sympy(teacher_latex: str, student_latex: str) -> float:
    """
    Attempt to use SymPy to prove mathematical equivalence between two LaTeX strings.
    Returns 1.0 if strictly equivalent, 0.0 if strictly not equivalent.
    Raises Exception if parsing fails (meaning we should fallback to LLM).
    """
    # Clean up common wrapper characters
    t_clean = teacher_latex.replace('$', '').strip()
    s_clean = student_latex.replace('$', '').strip()

    # Parse LaTeX to SymPy expressions
    t_expr = parse_latex(t_clean)
    s_expr = parse_latex(s_clean)

    # Check equality by simplifying the difference
    diff = sympy.simplify(t_expr - s_expr)
    if diff == 0:
        return 1.0
    return 0.0


def _extract_latex(text: str) -> str:
    """Extract the first LaTeX block or inline math string from text."""
    # Match block math $$...$$
    block_match = re.search(r'\$\$(.*?)\$\$', text, re.DOTALL)
    if block_match:
        return block_match.group(1).strip()
    # Match inline math $...$
    inline_match = re.search(r'\$(.*?)\$', text)
    if inline_match:
        return inline_match.group(1).strip()
    return text.strip()


# ---------------------------------------------------------------------------
# Pydantic models
# ---------------------------------------------------------------------------

class HolisticEvalRequest(BaseModel):
    question_number: str
    student_text: str
    teacher_facts: str  # Graph triplets OR raw LaTeX
    teacher_diagram_rubric: Optional[str] = None
    contains_math: bool = False
    has_diagram: bool = False
    diagram_snippet: Optional[str] = None  # Now essentially 'diagram_description' from Gemini


class HolisticEvalResponse(BaseModel):
    semantic_score: float
    missing_concepts: List[str]
    feedback: str


# ---------------------------------------------------------------------------
# Prompts
# ---------------------------------------------------------------------------

UNIFIED_EVAL_SYSTEM = (
    "You are a strict holistic grading AI. You evaluate exam answers by comparing the student's output "
    "(Written Text, Mathematical Equations, and Diagram Descriptions) to the Teacher's Fact Rubric. Output ONLY valid JSON."
)

UNIFIED_EVAL_USER = """You are grading Question {q_id}.

Here is the Teacher Reference Material:
- Facts/Conceptual Rubric: {text_stream}
- Math Equation Rules (if any): {math_stream}
- Diagram Requirements (if any): {diagram_stream}

Here is the Student's Output:
- Transcribed Answer: {student_text}
- Transcribed Diagram Description (if any): {student_diagram}

Compare the student's output to the teacher's reference material and give a combined, holistic score.

Return ONLY this structured JSON format:
{{"semantic_score": <float 0.0 to 1.0>, "missing_concepts": [<list of strings>], "feedback": "<detailed constructive feedback>"}}

Rules:
- semantic_score: 1.0 if all concepts, equations, and diagrams are correct. 0.0 if completely wrong.
- missing_concepts: list specific facts, numerical facts, or visual parts of the diagram the student failed to include.
- feedback: explain exactly what the student got right and wrong across all present modalities."""


# ---------------------------------------------------------------------------
# Endpoint
# ---------------------------------------------------------------------------

@router.post("/eval", response_model=HolisticEvalResponse)
async def holistic_evaluate(request: HolisticEvalRequest):
    """
    Unified grading endpoint that collapses multimodal grading into a single cohesive text prompt,
    preventing context-window token flooding.
    """
    try:
        final_score = 0.0
        missing_concepts = []
        feedback_parts = []
        
        # ---------------------------------------------------------
        # Branch 1 (Optional): Fast-pass Math Exact Match (SymPy)
        # ---------------------------------------------------------
        sympy_score = None
        if request.contains_math:
            print(f"   [Math Pre-Check] Triggered for Q{request.question_number}.")
            t_latex = _extract_latex(request.teacher_facts)
            s_latex = _extract_latex(request.student_text)
            try:
                sympy_score = _evaluate_math_sympy(t_latex, s_latex)
                if sympy_score == 1.0:
                    feedback_parts.append("Mathematical equation is exactly correct and logically equivalent.")
                else:
                    feedback_parts.append("Mathematical equation is incorrect or not logically equivalent.")
            except Exception as e:
                print(f"   [SymPy] Failed to parse: {e}. Relying completely on Unified LLM evaluator.")

        # ---------------------------------------------------------
        # Unified Text, Math, and Diagram Evaluation Router
        # ---------------------------------------------------------
        print(f"   [Unified Evaluator] Triggered for Q{request.question_number}.")
        
        t_text = request.teacher_facts if not request.contains_math else "None"
        t_math = request.teacher_facts if request.contains_math else "None"
        t_diag = request.teacher_diagram_rubric if request.has_diagram else "None"
        s_diag = request.diagram_snippet if request.has_diagram else "None"
        
        user_content_formatted = UNIFIED_EVAL_USER.format(
            q_id=request.question_number,
            text_stream=t_text,
            math_stream=t_math,
            diagram_stream=t_diag,
            student_text=request.student_text,
            student_diagram=s_diag
        )
        
        # Route logic based on diagram presence
        if request.has_diagram:
            print(f"   [Router] Diagram Detected -> Routing to Gemini 2.5 Flash for Multimodal Grading.")
            from google import genai
            GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
            if not GEMINI_API_KEY:
                raise HTTPException(status_code=500, detail="Gemini API Key missing for Diagram Evaluation.")
            client = genai.Client(api_key=GEMINI_API_KEY)
            
            gemini_prompt = f"{UNIFIED_EVAL_SYSTEM}\n\n{user_content_formatted}"
            response = client.models.generate_content(
                model="gemini-2.5-flash",
                contents=gemini_prompt,
            )
            raw_result = response.text
            eval_data = _parse_eval_json(raw_result, f"Q{request.question_number} Gemini Eval")
        else:
            print(f"   [Router] No Diagram -> Routing to Local LLM (LM Studio).")
            payload = {
                "model": "local-model",
                "messages": [
                    {"role": "system", "content": UNIFIED_EVAL_SYSTEM},
                    {"role": "user", "content": user_content_formatted}
                ],
                "temperature": 0.1
            }
            raw_result = _call_lmstudio(payload, f"Q{request.question_number} Unified Eval")
            eval_data = _parse_eval_json(raw_result, f"Q{request.question_number} Local Eval")
        
        # ---------------------------------------------------------
        # Final Score Consolidation & Fallback Logic
        # ---------------------------------------------------------
        
        llm_score = eval_data["semantic_score"]
        
        if request.contains_math:
            # Mathematical answers are strict logic; ignore textual missing concepts
            if sympy_score == 1.0:
                final_score = 1.0
            else:
                final_score = llm_score
            missing_concepts = [] # Omit entirely as requested by user
        else:
            # Textual answers: Calculate text similarity bounds (as a stabilizing grounding weight)
            s_vec = vector_model.encode(request.student_text).tolist()
            t_vec_text = request.teacher_facts if not request.contains_math else request.teacher_facts
            t_vec = vector_model.encode(t_vec_text).tolist()
            cosine_sim = float(np.dot(s_vec, t_vec) / (norm(s_vec) * norm(t_vec)))
            cosine_sim = max(0.0, cosine_sim)  # Ensure non-negative bounds
            
            # Use a blend of LLM Reasoning (70%) and Vector Similarity (30%)
            final_score = (0.7 * llm_score) + (0.3 * cosine_sim)
            missing_concepts.extend(eval_data.get("missing_concepts", []))
        
        fb_text = eval_data.get("feedback", "")
        if not fb_text.strip():
            fb_text = "The LLM did not provide specific holistic feedback."
        feedback_parts.append(f"AI Holistic Analysis: {fb_text}")

        # Deduplicate missing concepts
        missing_concepts = list(set(missing_concepts))
        final_score_clamped = max(0.0, min(1.0, float(final_score)))

        return HolisticEvalResponse(
            **{
                "semantic_score": round(final_score_clamped, 3),
                "missing_concepts": missing_concepts,
                "feedback": "\n\n".join(feedback_parts)
            }
        )

    except HTTPException:
        raise
    except Exception as e:
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"An unexpected error occurred: {str(e)}")