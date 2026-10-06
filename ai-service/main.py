from fastapi import FastAPI, UploadFile, File, Form
from pydantic import BaseModel
from compare_service import compare_papers
from rag_service import generate_answer
from ingest_service import ingest_pdf
from summary_service import generate_summary
import shutil
import os

app = FastAPI()


class QuestionRequest(BaseModel):
    question: str
    project_id: int
    document_id: str | None = None

@app.get("/")
def home():
    return {
        "message": "AI Research Paper Assistant is running!"
    }


@app.post("/api/ai/ask")
def ask_question(request: QuestionRequest):

    answer = generate_answer(
        question=request.question,
        project_id=request.project_id,
        document_id=request.document_id
    )

    return {
        "question": request.question,
        "project_id": request.project_id,
        "document_id": request.document_id,
        "answer": answer
    }

class SummaryRequest(BaseModel):
    project_id: int
    document_id: str | None = None


@app.post("/api/ai/summary")
def summarize_papers(request: SummaryRequest):

    print("Summary request received:")
    print(request.model_dump())

    result = generate_summary(
        project_id=request.project_id,
        document_id=request.document_id
    )

    return result
@app.post("/api/ai/compare")
def compare_two_papers(request: dict):

    project_id = request.get("project_id")
    document_id_1 = request.get("document_id_1")
    document_id_2 = request.get("document_id_2")

    print("========================================")
    print("COMPARE REQUEST RECEIVED")
    print("Project ID:", project_id)
    print("Paper 1:", document_id_1)
    print("Paper 2:", document_id_2)
    print("========================================")

    if project_id is None:
        return {
            "scope": "TWO_PAPERS",
            "comparison": "Project ID is required."
        }

    if not document_id_1 or not document_id_2:
        return {
            "scope": "TWO_PAPERS",
            "project_id": project_id,
            "comparison": (
                "Two research papers are required."
            )
        }

    return compare_papers(
        int(project_id),
        document_id_1,
        document_id_2
    )


@app.post("/api/ai/ingest")
async def ingest_paper(
    file: UploadFile = File(...),
    document_id: str = Form(...),
    project_id: int = Form(...)
):

    os.makedirs("./data/uploads", exist_ok=True)

    file_path = f"./data/uploads/{file.filename}"

    with open(file_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    result = ingest_pdf(
        file_path,
        document_id,
        project_id,
        file.filename
    )

    return {
        "message": "PDF processed successfully",
        "filename": file.filename,
        "document_id": document_id,
        "project_id": project_id,
        "characters": result["characters"],
        "chunks": result["chunks"]
    }