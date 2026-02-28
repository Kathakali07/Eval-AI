
from fastapi import FastAPI, HTTPException, UploadFile, File
import os
import uvicorn
import google.generativeai as genai
import fitz

app = FastAPI(title="Backend")
genai.configure(api_key=os.getenv("Gemini_API_Key"))

vision_model = genai.GenerativeModel("gemini-3-flash-preview")

prompt = """
You are an expert transcription assistant. Read the attached handwritten student exam paper. 
Transcribe the text exactly as the student intended it to be read. 
CRITICAL RULE: If the student uses asterisks (*), arrows, or margin notes to insert words into a sentence, 
resolve those spatial relationships and output the final, coherent sentence. 
Ignore crossed-out text. Output ONLY the final plain text, nothing else.
"""

@app.post("/extract-text")
async def extract_text_from_file(file : UploadFile = File(...)):

    filename = file.filename
    filetype = file.content_type
    filebytes = file.read()

    try:
        if filetype.startswith("image/"):
            print("Routing to ai")

            image_part = {
                "mime_type" : filetype,
                "data" : filebytes
            }

            response = vision_model.generate_content([prompt, image_part])

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
                    full_text += page.get_text()+"\n"

                return{
                    "status":"success",
                    "data" : full_text
                }
            else:

                page_data = list()
                for page in pdf_doc:
                    pixmap = page.get_pixmap()
                    page_data.append(pixmap.tobytes('png'))

                image_part = {
                    "mime-type" : "image/png",
                    "data" : page_data
                }

                response = vision_model.generate_content([prompt, image_part])

                return {
                    "status":"success",
                    "data": response.text.strip()
                }
        
        else:
            raise HTTPException(status_code=400, detail="Invalid File Format")

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

    


if __name__ == "__main__":
    uvicorn.run(app, host="localhost", port=8000)