import fitz
from fastapi import APIRouter, HTTPException, UploadFile, File, Form
from pydantic import BaseModel, Field
from typing import List
from google import genai
from google.genai import types
from dotenv import load_dotenv

load_dotenv()
router = APIRouter()
client = genai.Client()

class ModelAnswerItem(BaseModel):
    question_number: str = Field(description="The number or label of the question (e.g., '1', 'Q2', '3a')")
    question_text: str = Field(description="The actual question being asked")
    model_answer: str = Field(description="The teacher's answer key or expected answer")
    max_marks: float = Field(description="The maximum marks allocated. If not specified, default to 5.0")


class TeacherPaperExtraction(BaseModel):
    qa_pairs: List[ModelAnswerItem]

class StudentAnswerItem(BaseModel):
    question_number: str = Field(description="The question number the student is answering")
    student_text: str = Field(description="The exact transcribed text of the student's answer")


class StudentPaperExtraction(BaseModel):
    answers: List[StudentAnswerItem]




TEACHER_PROMPT = """
You are an expert AI assistant reading a teacher's model question paper and answer key.
Extract every question, its question number, the exact model answer provided, and the maximum marks allocated.
Output this strictly in the requested JSON format.
"""

STUDENT_PROMPT = """
You are an expert transcription assistant. Read the attached handwritten student exam paper.
Extract each answer the student wrote, mapping it to the question number they indicated.
CRITICAL RULE: Transcribe the text exactly as intended. Resolve asterisks and margin notes. Ignore crossed-out text.
Output strictly in the requested JSON format.
"""





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
            target_schema = TeacherPaperExtraction
        elif document_type.lower() == "student":
            system_prompt = STUDENT_PROMPT
            target_schema = StudentPaperExtraction
        else:
            raise HTTPException(status_code=400, detail="document_type must be 'teacher' or 'student'")

        payload = [system_prompt]

        if filetype.startswith("image/"):
            print(f"Processing single image as {document_type}...")
            image_part = types.Part.from_bytes(data=filebytes, mime_type=filetype)
            payload.append(image_part)

        elif filetype == "application/pdf":
            pdf_doc = fitz.open(stream=filebytes, filetype="pdf")


            first_page = pdf_doc[0]
            if len(first_page.get_text().strip()) > 100:
                print(f"Digital PDF detected for {document_type}. Extracting raw text first...")
                full_text = ""
                for page in pdf_doc:
                    full_text += page.get_text() + "\n"

                payload.append(f"RAW DOCUMENT TEXT:\n{full_text}")
            else:
                print(f"Scanned PDF detected. Sending {len(pdf_doc)} pages to Vision AI...")
                for page in pdf_doc:
                    pixmap = page.get_pixmap()
                    image_part = types.Part.from_bytes(
                        data=pixmap.tobytes('png'),
                        mime_type="image/png"
                    )
                    payload.append(image_part)
        else:
            raise HTTPException(status_code=400, detail="Invalid File Format. Must be Image or PDF.")

        print("Executing AI Extraction...")
        response = client.models.generate_content(
            model="gemini-3-flash-preview",
            contents=payload,
            config=types.GenerateContentConfig(
                response_mime_type="application/json",
                response_schema=target_schema,
            )
        )
        return {
            "status": "success",
            "document_type": document_type,
            "structured_data": response.text.strip()
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))