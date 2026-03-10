import json
import os
import re
import requests
from typing import List
from dotenv import load_dotenv
from pydantic import BaseModel
from fastapi import APIRouter, HTTPException
from sentence_transformers import SentenceTransformer

print("Loading the vector model")
vector_model = SentenceTransformer("all-MiniLM-L6-v2")
print("Model loaded successfully")

router = APIRouter(prefix="/ingest", tags=["Ingestion Pipeline"])
load_dotenv()

LMSTUDIO_URL = os.getenv("LMSTUDIO_URL", "http://localhost:1234/v1/chat/completions")


def _clean_llm_response(text: str) -> str:
    """Aggressively strip <think>...</think> tags and markdown code fences from LLM output."""
    text = re.sub(r'<think>.*?</think>', '', text, flags=re.DOTALL).strip()
    if text.startswith("```json"):
        text = text[7:]
    elif text.startswith("```"):
        text = text[3:]
    if text.endswith("```"):
        text = text[:-3]
    return text.strip()


def _call_lmstudio(payload: dict, step_name: str) -> str:
    """Helper to call LM Studio local server. Returns the content string."""
    try:
        resp = requests.post(LMSTUDIO_URL, json=payload, timeout=120)
    except requests.exceptions.ConnectionError:
        raise HTTPException(
            status_code=503,
            detail=f"Could not connect to LM Studio during {step_name}. Please ensure it is running with a model loaded."
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
            detail=f"LM Studio returned an error (HTTP {resp.status_code}) during {step_name}. Please check that a model is loaded."
        )

    try:
        return resp.json()["choices"][0]["message"]["content"].strip()
    except (KeyError, IndexError, TypeError):
        raise HTTPException(status_code=502, detail=f"LM Studio returned an unexpected response during {step_name}.")


class IngestRequest(BaseModel):
    text: str


class Triplet(BaseModel):
    subject: str
    predicate: str
    object: str


graph_prompt = """
Extract the core facts from the provided text into Subject-Predicate-Object triplets.
Normalize entities (e.g., use "Operating System" instead of "OS").
Predicates must be short, lowercase verbs (e.g., "contains", "manages").
You MUST return ONLY valid JSON matching this schema, without any markdown formatting, explanations, or <think> tags:
{
  "triplets": [
    {"subject": "str", "predicate": "str", "object": "str"}
  ]
}
"""


@router.post("/process-text")
async def process_text(payload: IngestRequest):
    """
    Receives a single model answer text, generates a vector embedding.

    MATH BYPASS: If the text contains '$' (LaTeX math), skip LLM triplet
    extraction entirely — store the raw LaTeX as a single "math_fact" triplet
    so the Java backend's PythonIngestResponse contract (vector_embedding + triplets) is preserved.

    For text (no '$'), proceed with the standard LM Studio triplet extraction.
    """
    try:
        # Generate vector embedding (always needed)
        embedding = vector_model.encode(payload.text).tolist()

        is_math = "$" in payload.text

        if is_math:
            # --- MATH BYPASS: No LLM call needed ---
            print(f"   [Math Bypass] LaTeX detected, skipping triplet extraction. Storing raw equation.")
            # Pack the raw LaTeX into a single triplet so the Java DTO contract is preserved.
            # The eval endpoint will check for this sentinel subject to activate math grading.
            triplets = [{
                "subject": "__MATH__",
                "predicate": "equals",
                "object": payload.text
            }]
        else:
            # --- TEXT PATH: Standard LM Studio triplet extraction ---
            print(f"   [Text Path] No LaTeX detected, calling LM Studio for graph extraction.")
            graph_input = f"{graph_prompt}\n\nText:\n{payload.text}"

            data = {
                "model": "local-model",
                "messages": [
                    {"role": "user", "content": graph_input}
                ],
                "temperature": 0.1
            }

            result_text = _call_lmstudio(data, "knowledge graph extraction")
            print(f"   [LM Studio Raw Response] {result_text[:500]}")

            cleaned_text = _clean_llm_response(result_text)
            print(f"   [LM Studio Cleaned] {cleaned_text[:500]}")

            try:
                graph_data = json.loads(cleaned_text)
            except json.JSONDecodeError:
                raise HTTPException(
                    status_code=502,
                    detail=f"LM Studio returned a malformed response. Cleaned: {cleaned_text[:300]}"
                )

            triplets = graph_data.get("triplets", [])
            # Filter out triplets with null/empty fields — Java backend crashes on nulls
            triplets = [
                t for t in triplets
                if t.get("subject") and t.get("predicate") and t.get("object")
            ]

        return {
            "vector_embedding": embedding,
            "triplets": triplets,
        }

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"An unexpected error occurred during text processing: {str(e)}")
