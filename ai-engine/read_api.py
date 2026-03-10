import base64
import json
import os
import re
import io
import fitz
import requests
from PIL import Image
from fastapi import APIRouter, HTTPException, UploadFile, File, Form
from pydantic import BaseModel, Field
from typing import List, Optional
from dotenv import load_dotenv

from google import genai
from google.genai import types

load_dotenv()
router = APIRouter()

LMSTUDIO_URL = os.getenv("LMSTUDIO_URL", "http://localhost:1234/v1/chat/completions")
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")

try:
    gemini_client = genai.Client(api_key=GEMINI_API_KEY)
    print("Gemini client initialized for math transcription.")
except Exception as e:
    gemini_client = None
    print(f"Warning: Gemini Client failed to initialize: {e}")


CONFIDENCE_THRESHOLD = 0.7

TEACHER_PROMPT = """
You are an expert AI assistant reading a teacher's model question paper and answer key.
I am providing you with the highly accurate text transcription of the document (which already has LaTeX math formatted properly).

Extract every question, its question number, the exact model answer provided, and the maximum marks allocated.

CRITICAL MATH RULE: Preserve all LaTeX math formatting (`$` and `$$`) exactly as provided in the transcript. Set `contains_math` to True if math is present.

CRITICAL DIAGRAM RULE: If the transcript contains the tag [DIAGRAM_DETECTED] for a question, set `has_diagram` to True, and extract the descriptive text immediately following it into `diagram_snippet`. If no diagram, set to null.

You must also provide a "confidence_score" (a float from 0.0 to 1.0) representing how clearly you were able to parse the document into JSON.

You MUST return ONLY valid JSON matching this schema, without any markdown formatting, explanations, or <think> tags:
{
  "confidence_score": 0.95,
  "qa_pairs": [
    {"question_number": "str", "question_text": "str", "model_answer": "str", "max_marks": 5.0, "contains_math": true, "has_diagram": false, "diagram_snippet": null}
  ]
}
"""

STUDENT_PROMPT = """
You are an expert transcription JSON structurer. Read the provided text transcript of the student's exam paper.
Extract each answer the student wrote, mapping it to the question number they indicated.

CRITICAL MATH RULE: Preserve all LaTeX math formatting (`$` and `$$`) exactly as provided in the transcript. Set `contains_math` to True if math is present.

CRITICAL DIAGRAM RULE: If the transcript contains the tag [DIAGRAM_DETECTED] for a question, set `has_diagram` to True, and extract the descriptive text immediately following it into `diagram_snippet`. If no diagram, set to null.

You must also provide a "confidence_score" (a float from 0.0 to 1.0) representing how clearly you were able to parse the document into JSON.

You MUST return ONLY valid JSON matching this schema, without any markdown formatting, explanations, or <think> tags:
{
  "confidence_score": 0.85,
  "answers": [
    {"question_number": "str", "student_text": "str", "contains_math": false, "has_diagram": true, "diagram_snippet": "Description of the student's diagram..."}
  ]
}
"""

GEMINI_TRANSCRIBE_PROMPT = """
You are an expert document transcriber. 
Transcribe all text from this image exactly as written. 

CRITICAL MATH RULE: For ANY mathematical equations, formulas, fractions, variables, or numbers, you MUST use valid LaTeX syntax. Wrap inline math in `$` and block math in `$$`. Do not solve anything; only transcribe exactly what is written.

CRITICAL DIAGRAM RULE: If there are any hand-drawn diagrams, plots, tables, or structural figures in the image, you MUST explicitly write the string: `[DIAGRAM_DETECTED]` and briefly describe the visual elements immediately after.

Transcribe the document now:
"""


def _clean_llm_response(text: str) -> str:
    """Strip <think>...</think> tags and markdown code fences from LLM output."""
    text = re.sub(r'<think>.*?</think>', '', text, flags=re.DOTALL).strip()
    if text.startswith("```json"):
        text = text[7:]
    elif text.startswith("```"):
        text = text[3:]
    if text.endswith("```"):
        text = text[:-3]
    return text.strip()


def _encode_image_to_base64(image_bytes: bytes, mime_type: str) -> str:
    """Encode raw image bytes to a data URI string. (Unused but kept for utility)"""
    b64 = base64.b64encode(image_bytes).decode("utf-8")
    return f"data:{mime_type};base64,{b64}"


def _call_lmstudio(system_prompt: str, user_content: str) -> dict:
    """Sends the purely textual transcript to Local LLM to strictly format as JSON."""
    messages = [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": user_content}
    ]

    data = {
        "model": "local-model",
        "messages": messages,
        "temperature": 0.1
    }

    try:
        response = requests.post(LMSTUDIO_URL, json=data, timeout=120)
    except Exception as e:
        raise HTTPException(
            status_code=503,
            detail=f"Failed to reach Local LLM (LM Studio): {str(e)}"
        )

    if not response.ok:
        raise HTTPException(
            status_code=502,
            detail=f"Local LLM returned an error (HTTP {response.status_code})."
        )

    try:
        result_text = response.json()["choices"][0]["message"]["content"].strip()
    except (KeyError, IndexError, TypeError):
        raise HTTPException(
            status_code=502,
            detail="Local LLM returned an unexpected response format."
        )

    cleaned = _clean_llm_response(result_text)
    print(f"   [Local LLM JSON Check] {cleaned[:500]}...")

    try:
        return json.loads(cleaned)
    except json.JSONDecodeError:
        raise HTTPException(
            status_code=502,
            detail=f"Local LLM failed to format the JSON correctly. Cleaned: {cleaned[:300]}"
        )


@router.post("/read")
async def extract_structured_data(
        file: UploadFile = File(...),
        document_type: str = Form(..., description="Must be 'teacher' or 'student'")
):
    try:
        filebytes = await file.read()
        filetype = file.content_type

        if document_type.lower() == "teacher":
            system_prompt = TEACHER_PROMPT
        elif document_type.lower() == "student":
            system_prompt = STUDENT_PROMPT
        else:
            raise HTTPException(status_code=400, detail="document_type must be 'teacher' or 'student'")

        if not gemini_client:
            raise HTTPException(status_code=500, detail="Gemini API key is not configured.")

        # --- Document Loading ---
        images_to_process = []  # List of PIL Images
        
        if filetype.startswith("image/"):
            img = Image.open(io.BytesIO(filebytes)).convert("RGB")
            images_to_process.append(img)
        elif filetype == "application/pdf":
            pdf_doc = fitz.open(stream=filebytes, filetype="pdf")
            for page in pdf_doc:
                pixmap = page.get_pixmap(dpi=200)
                png_bytes = pixmap.tobytes("png")
                img = Image.open(io.BytesIO(png_bytes)).convert("RGB")
                images_to_process.append(img)
        else:
            raise HTTPException(status_code=400, detail="Invalid File Format. Must be Image or PDF.")

        # --- Step 1: Gemini Transcription ---
        print(f"Sending {len(images_to_process)} page(s) to Gemini 3.1 Flash Lite for LaTeX Math & Text OCR...")
        full_transcript = []
        
        for i, img in enumerate(images_to_process):
            # Pass image to Gemini 3.1 Flash Lite
            response = gemini_client.models.generate_content(
                model="gemini-3.1-flash-lite-preview",
                contents=[img, GEMINI_TRANSCRIBE_PROMPT]
            )
            transcript = response.text
            full_transcript.append(f"--- PAGE {i+1} TRANSCRIPT ---\n{transcript}")

        aggregated_text = "\n\n".join(full_transcript)
        print(f"Gemini OCR completed. Transcribed {len(aggregated_text)} characters.")

        # --- Step 2: Local LLM Structuring ---
        print("Sending transcript to Local LLM for JSON structuring...")
        
        user_content = f"TRANSCRIPT:\n{aggregated_text}\n\n"
        extracted_data = _call_lmstudio(system_prompt, user_content)

        # --- Confidence check ---
        confidence = extracted_data.get("confidence_score", 0.0)
        try:
            confidence = float(confidence)
        except (TypeError, ValueError):
            confidence = 0.0

        if confidence < CONFIDENCE_THRESHOLD:
            raise HTTPException(
                status_code=422,
                detail=(
                    f"The document could not be understood clearly enough "
                    f"(confidence: {confidence:.0%}, minimum required: {CONFIDENCE_THRESHOLD:.0%}). "
                    f"Please rescan the document with better lighting."
                )
            )

        extracted_data.pop("confidence_score", None)

        return {
            "status": "success",
            "document_type": document_type,
            "confidence_score": confidence,
            "structured_data": json.dumps(extracted_data)
        }

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"An unexpected error occurred: {str(e)}")