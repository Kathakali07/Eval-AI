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

class RawRequest(BaseModel):
    subject : str
    raw_text: str

class QnAStructure(BaseModel):
    question : str
    answer : str

class DocumentStructure(BaseModel) :
    qna_list : List[QnAStructure]

class Triplet(BaseModel) :
    subject : str
    predicate : str
    object : str

class GraphExtraction(BaseModel):
    triplets : List[Triplet]

chunk_prompt = """
You are an expert data extraction parser.
Read the provided raw text and extract every distinct Question and Answer pair.
Do not summarize or change the academic meaning of the text.
"""

graph_prompt = """
Extract the core facts from the provided text into Subject-Predicate-Object triplets.
Normalize entities (e.g., use "Operating System" instead of "OS").
Predicates must be short, lowercase verbs (e.g., "contains", "manages").
"""

@router.post("/process-qna")
async def process_qna_document(payload : RawRequest):
    try:
        client = genai.Client()

        full_prompt = f"{chunk_prompt}\n\nRaw Text: {payload.raw_text}"

        print("Sending raw text for chunking")
        response = client.models.generate_content(
            model="gemini-3-flash-preview",
            contents=full_prompt,
            config = types.GenerateContentConfig(
                response_mime_type = "application/json",
                response_schema = DocumentStructure,
            ),
        )

        structured_data = json.loads(response.text)

        print(f"Found this many pair : {len(structured_data)}")

        processed_data = []

        for qna in structured_data["qna_list"]:
            question = qna["question"]
            answer = qna["answer"]

            embedding = vector_model.encode(answer).tolist()

            graph_input = f"{graph_prompt}\n\nText:\n{answer}"

            graph_response = client.models.generate_content(
            model="gemini-3-flash-preview",
            contents=graph_input,
            config = types.GenerateContentConfig(
                response_mime_type = "application/json",
                response_schema = GraphExtraction,
            ),
        )

            graph_data = json.loads(graph_response.text)

            processed_data.append({
                "question" : question,
                "answer" : answer,
                "vector_embedding" : embedding,
                "knowledge_graph" : graph_data["triplets"]

            })

            return{
                "status" : "success",
                "subject" : payload.subject,
                "processed_data" : processed_data,
            }

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


