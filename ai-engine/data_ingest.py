import json
from typing import List
from dotenv import load_dotenv
from google.genai import types
from pydantic import BaseModel
from google import genai
from fastapi import APIRouter, HTTPException
from sentence_transformers import SentenceTransformer

print("Loading the vector model")
vector_model = SentenceTransformer("all-MiniLM-L6-v2")
print("Model loaded successfully")

router = APIRouter(prefix="/ingest", tags=["Ingestion Pipeline"])
load_dotenv()


class IngestRequest(BaseModel):
    text: str


class Triplet(BaseModel):
    subject: str
    predicate: str
    object: str


class GraphExtraction(BaseModel):
    triplets: List[Triplet]


graph_prompt = """
Extract the core facts from the provided text into Subject-Predicate-Object triplets.
Normalize entities (e.g., use "Operating System" instead of "OS").
Predicates must be short, lowercase verbs (e.g., "contains", "manages").
"""


@router.post("/process-text")
async def process_text(payload: IngestRequest):
    """
    Receives a single model answer text, generates a vector embedding
    and knowledge graph triplets. Returns JSON matching Java's PythonIngestResponse:
    { "vector_embedding": [...], "triplets": [...] }
    """
    try:
        client = genai.Client()

        # Generate vector embedding
        embedding = vector_model.encode(payload.text).tolist()

        # Extract knowledge graph triplets
        graph_input = f"{graph_prompt}\n\nText:\n{payload.text}"
        graph_response = client.models.generate_content(
            model="gemini-3-flash-preview",
            contents=graph_input,
            config=types.GenerateContentConfig(
                response_mime_type="application/json",
                response_schema=GraphExtraction,
            ),
        )

        graph_data = json.loads(graph_response.text)
        triplets = graph_data.get("triplets", [])

        return {
            "vector_embedding": embedding,
            "triplets": triplets,
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
