import fitz
from fastapi import HTTPException, UploadFile, File, APIRouter
from google import genai
from dotenv import load_dotenv
from google.genai import types

router = APIRouter()
load_dotenv()
client = genai.Client()
prompt = """
You are an expert transcription assistant. Read the attached handwritten student exam paper. 
Transcribe the text exactly as the student intended it to be read. 
CRITICAL RULE: If the student uses asterisks (*), arrows, or margin notes to insert words into a sentence, 
resolve those spatial relationships and output the final, coherent sentence. 
Ignore crossed-out text. Output ONLY the final plain text, nothing else.
"""
@router.post("/read")
async def extract_text_from_file(file : UploadFile = File(...)):

    filename = file.filename
    filetype = file.content_type
    filebytes = await file.read()

    try:
        if filetype.startswith("image/"):
            print("Routing to ai")

            image_part = types.Part.from_bytes(
                data = filebytes,
                mime_type = filetype,
            )

            response = client.models.generate_content(
                model = "gemini-3-flash-preview",
                contents =[prompt, image_part]
            )

            return{
                "status" : "success",
                "extracted_text" : response.text.strip()
            }

        elif filetype == "application/pdf":

            pdf_doc = fitz.open(stream=filebytes, filetype="pdf")

            first_page = pdf_doc[0]
            page_text = first_page.get_text().strip()

            if len(page_text) > 100:
                print("Digital pdf detected")
                full_text = ""

                for page in pdf_doc:
                    full_text += page.get_text( ) +"\n"

                return{
                    "status" :"success",
                    "data" : full_text
                }
            else:

                payload = [prompt]

                for page in pdf_doc:
                    pixmap = page.get_pixmap()
                    page_bytes = pixmap.tobytes('png')

                    image_part = types.Part.from_bytes(
                        data=page_bytes,
                        mime_type="image/png"
                    )

                    payload.append(image_part)

                print(f"Sending {len(pdf_doc)} pages to Vision AI...")
                response = client.models.generate_content(
                    model="gemini-3-flash-preview",
                    contents=payload
                )

                return {
                    "status" :"success",
                    "data": response.text.strip()
                }

        else:
            raise HTTPException(status_code=400, detail="Invalid File Format")

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))