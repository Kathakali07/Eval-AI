
from fastapi import FastAPI
import uvicorn

from data_ingest import router as ingest_router
from eval_api import router as eval_router
from read_api import router as read_router


app = FastAPI(title="Backend")
app.include_router(ingest_router)
app.include_router(eval_router)
app.include_router(read_router)



@app.get("/")
def health_check():
    return {"status": "server is running"}
    


if __name__ == "__main__":
    uvicorn.run("main:app", host="127.0.0.1", port=8000, reload=True)