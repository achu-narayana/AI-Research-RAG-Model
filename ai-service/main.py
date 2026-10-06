import hmac
import os
import shutil
import uuid

from fastapi import FastAPI, File, Form, Request, UploadFile
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field

from compare_service import compare_papers
from config import AI_SERVICE_TOKEN, MAX_UPLOAD_BYTES, UPLOAD_DIR
from errors import ServiceError
from ingest_service import ingest_pdf
from rag_service import generate_answer
from summary_service import generate_summary
from vector_store import delete_document, delete_project

app = FastAPI()


# ==================================================
# Errors -> {"detail": "..."}
# ==================================================

@app.exception_handler(ServiceError)
def handle_service_error(request: Request, error: ServiceError):

    print(f"    {request.url.path} failed ({error.status_code}): {error.message}")

    return JSONResponse(
        status_code=error.status_code,
        content={"detail": error.message}
    )


# ==================================================
# Optional shared secret with the Spring Boot API
# ==================================================

@app.middleware("http")
async def check_internal_token(request: Request, call_next):

    if AI_SERVICE_TOKEN and request.url.path != "/":

        token = request.headers.get("X-Internal-Token", "")

        if not hmac.compare_digest(token, AI_SERVICE_TOKEN):
            return JSONResponse(
                status_code=401,
                content={"detail": "Invalid internal token."}
            )

    return await call_next(request)


# ==================================================
# Requests
# ==================================================

class QuestionRequest(BaseModel):
    question: str = Field(min_length=1, max_length=4000)
    project_id: int
    document_id: str | None = None


class SummaryRequest(BaseModel):
    project_id: int
    document_id: str | None = None


class CompareRequest(BaseModel):
    project_id: int
    document_id_1: str | None = None
    document_id_2: str | None = None


# ==================================================
# Endpoints
# ==================================================

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


@app.post("/api/ai/summary")
def summarize_papers(request: SummaryRequest):

    print("Summary request received:")
    print(request.model_dump())

    return generate_summary(
        project_id=request.project_id,
        document_id=request.document_id
    )


@app.post("/api/ai/compare")
def compare_two_papers(request: CompareRequest):

    print("========================================")
    print("COMPARE REQUEST RECEIVED")
    print("Project ID:", request.project_id)
    print("Paper 1:", request.document_id_1)
    print("Paper 2:", request.document_id_2)
    print("========================================")

    return compare_papers(
        request.project_id,
        request.document_id_1,
        request.document_id_2
    )


# Plain "def" (not async): PDF parsing and embedding are
# blocking, so FastAPI runs this in a worker thread and
# other requests keep being served meanwhile.
@app.post("/api/ai/ingest")
def ingest_paper(
    file: UploadFile = File(...),
    document_id: str = Form(...),
    project_id: int = Form(...)
):

    # Only the base name is kept for display; the file on
    # disk always gets a generated name.
    paper_name = os.path.basename(
        (file.filename or "paper.pdf").replace("\\", "/")
    )

    if file.size is not None and file.size > MAX_UPLOAD_BYTES:
        raise ServiceError(413, "The PDF is larger than 20MB.")

    if file.file.read(5) != b"%PDF-":
        raise ServiceError(400, "The uploaded file is not a PDF.")

    file.file.seek(0)

    UPLOAD_DIR.mkdir(parents=True, exist_ok=True)

    file_path = UPLOAD_DIR / f"{uuid.uuid4()}.pdf"

    try:
        with open(file_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)

        result = ingest_pdf(
            str(file_path),
            document_id,
            project_id,
            paper_name
        )

    finally:
        # The text is stored in ChromaDB; the original PDF
        # is kept by the Spring Boot API.
        file_path.unlink(missing_ok=True)

    return {
        "message": "PDF processed successfully",
        "filename": paper_name,
        "document_id": document_id,
        "project_id": project_id,
        "characters": result["characters"],
        "chunks": result["chunks"]
    }


@app.delete("/api/ai/documents/{document_id}")
def delete_paper(document_id: str, project_id: int):

    return {
        "document_id": document_id,
        "deleted_chunks": delete_document(project_id, document_id)
    }


@app.delete("/api/ai/projects/{project_id}")
def delete_project_vectors(project_id: int):

    return {
        "project_id": project_id,
        "deleted_chunks": delete_project(project_id)
    }
