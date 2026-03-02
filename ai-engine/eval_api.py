import json
from typing import List
from dotenv import load_dotenv
import numpy as np
from fastapi import APIRouter, HTTPException
from google.genai import types
from numpy.linalg import norm
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer
from google import genai

router = APIRouter(prefix="/evaluate", tags=["Evaluation Pipeline"])
load_dotenv()
print("Loading vector model")
vector_model = SentenceTransformer('all-MiniLM-L6-v2')
print("Model loaded")


class Triplet(BaseModel):
    subject: str
    predicate: str
    object: str


class GraphExtraction(BaseModel):
    triplets: List[Triplet]


class EvaluationRequest(BaseModel):
    student_text: str
    model_vector: List[float]
    model_triplets: List[Triplet]


class EvaluationResponse(BaseModel):
    semantic_score: float
    missing_concepts: List[str]
    feedback: str


graph_prompt = """
Extract the core facts from the provided text into Subject-Predicate-Object triplets.
Normalize entities (e.g., use "Operating System" instead of "OS").
Predicates must be short, lowercase verbs.
"""

feedback_prompt = """
You are a strict but helpful engineering professor grading an exam.
Review the student's answer based STRICTLY on the mathematical evaluation provided below.

Evaluation Metrics:
- Semantic Similarity Score: {score}/1.0
- Concepts Successfully Explained: {found_concepts}
- Critical Concepts Missed: {missing_concepts}

Student's Answer: {student_text}

Task: Write a short, constructive paragraph telling the student what they got right,
and explaining the specific technical concepts they missed. DO NOT output a numerical grade.
"""


@router.post("/grade-answer", response_model=EvaluationResponse)
async def grade_answer(request: EvaluationRequest):
    """
    Grades a student answer against the model vector and knowledge graph.
    Route: POST /evaluate/grade-answer
    """
    try:
        # Compute cosine similarity
        student_vector = vector_model.encode(request.student_text).tolist()
        A = np.array(student_vector)
        B = np.array(request.model_vector)
        cosine_score = float(np.dot(A, B) / (norm(A) * norm(B)))

        client = genai.Client()

        # Extract student's knowledge graph
        graph_input = f"{graph_prompt}\n\nText:\n{request.student_text}"
        graph_response = client.models.generate_content(
            model="gemini-3-flash-preview",
            contents=graph_input,
            config=types.GenerateContentConfig(
                response_mime_type="application/json",
                response_schema=GraphExtraction,
            ),
        )

        student_graph_data = json.loads(graph_response.text)
        student_triplets = student_graph_data.get("triplets", [])
        student_subjects = {t["subject"].lower() for t in student_triplets}

        # Compare against model triplets to find missing concepts
        found_names = []
        missing_names = []

        for target in request.model_triplets:
            if target.subject.lower() in student_subjects:
                found_names.append(target.subject)
            else:
                missing_names.append(target.subject)

        # Deduplicate
        found_names = list(set(found_names))
        missing_names = list(set(missing_names))

        # Generate AI feedback
        final_prompt = feedback_prompt.format(
            score=round(cosine_score, 3),
            found_concepts=", ".join(found_names) if found_names else "None",
            missing_concepts=", ".join(missing_names) if missing_names else "None",
            student_text=request.student_text,
        )

        feedback_response = client.models.generate_content(
            model="gemini-3-flash-preview",
            contents=final_prompt,
        )

        return EvaluationResponse(
            semantic_score=round(cosine_score, 3),
            missing_concepts=missing_names,
            feedback=feedback_response.text.strip(),
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Evaluation Failed: {str(e)}")