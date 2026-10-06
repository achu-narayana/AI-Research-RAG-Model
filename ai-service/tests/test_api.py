import pytest
from fastapi.testclient import TestClient

import main
import rag_service
import summary_service
from conftest import make_pdf

api = TestClient(main.app)

PAPER = [
    "Attention Is All You Need. Abstract: We propose the Transformer, "
    "a model based solely on attention mechanisms. " * 20,
    "Results: the Transformer reaches 28.4 BLEU on English to German "
    "translation, beating recurrent models. " * 20,
    "Conclusion: attention alone is enough for sequence transduction. " * 20,
]


def ingest(project_id, document_id, pages=PAPER, filename="paper.pdf"):
    return api.post(
        "/api/ai/ingest",
        files={"file": (filename, make_pdf(pages), "application/pdf")},
        data={"document_id": document_id, "project_id": str(project_id)},
    )


def test_ingest_stores_chunks_in_order(project_id):
    response = ingest(project_id, "doc-a")

    assert response.status_code == 200
    assert response.json()["chunks"] > 3

    chunks, name = summary_service.get_paper_chunks(project_id, "doc-a")
    assert name == "paper.pdf"
    assert len(chunks) == response.json()["chunks"]
    assert chunks[0].startswith("Attention Is All You Need")
    assert "Conclusion" in chunks[-1]


def test_reingest_replaces_instead_of_duplicating(project_id):
    first = ingest(project_id, "doc-a").json()["chunks"]
    ingest(project_id, "doc-a")

    chunks, _ = summary_service.get_paper_chunks(project_id, "doc-a")
    assert len(chunks) == first


def test_path_in_filename_is_ignored(project_id):
    response = ingest(project_id, "doc-a", filename="..\\..\\evil.pdf")

    assert response.status_code == 200
    assert response.json()["filename"] == "evil.pdf"
    assert not (main.UPLOAD_DIR.parent.parent / "evil.pdf").exists()
    # The temporary upload is removed after ingest.
    assert list(main.UPLOAD_DIR.glob("*.pdf")) == []


def test_non_pdf_is_rejected(project_id):
    response = api.post(
        "/api/ai/ingest",
        files={"file": ("x.pdf", b"hello", "application/pdf")},
        data={"document_id": "doc-x", "project_id": str(project_id)},
    )

    assert response.status_code == 400
    assert response.json() == {"detail": "The uploaded file is not a PDF."}


def test_pdf_without_text_gives_422(project_id):
    response = ingest(project_id, "doc-empty", pages=[""])

    assert response.status_code == 422


def test_ask_only_searches_the_selected_project(project_id, monkeypatch):
    ingest(project_id, "doc-a")
    ingest(project_id + 500, "doc-other", pages=["Unrelated cooking recipes. " * 50])

    prompts = []
    monkeypatch.setattr(rag_service, "ask_llm", lambda p: prompts.append(p) or "answer")

    response = api.post(
        "/api/ai/ask",
        json={"question": "What BLEU score?", "project_id": project_id, "document_id": "doc-other"},
    )

    # doc-other belongs to another project, so nothing is found.
    assert response.status_code == 200
    assert "could not find" in response.json()["answer"]
    assert prompts == []

    response = api.post(
        "/api/ai/ask",
        json={"question": "What BLEU score?", "project_id": project_id},
    )

    assert response.json()["answer"] == "answer"
    assert "[From: paper.pdf]" in prompts[0]
    assert "cooking" not in prompts[0]
    # The context is included once, not twice.
    assert prompts[0].count("RETRIEVED RESEARCH PAPER CONTEXT") == 1


@pytest.mark.parametrize("body, status", [
    ({}, 400),
    ({"document_id": "missing"}, 404),
])
def test_summary_errors_use_status_codes(project_id, body, status):
    response = api.post("/api/ai/summary", json={"project_id": project_id, **body})

    assert response.status_code == status
    assert "detail" in response.json()


def test_compare_same_paper_is_400(project_id):
    response = api.post(
        "/api/ai/compare",
        json={"project_id": project_id, "document_id_1": "a", "document_id_2": "a"},
    )

    assert response.status_code == 400


def test_llm_errors_reach_the_caller(project_id, monkeypatch):
    ingest(project_id, "doc-a")

    def rate_limited(prompt, max_tokens=2000):
        raise main.ServiceError(503, "rate limited")

    monkeypatch.setattr(summary_service, "ask_llm", rate_limited)

    response = api.post("/api/ai/summary", json={"project_id": project_id, "document_id": "doc-a"})

    assert response.status_code == 503
    assert response.json() == {"detail": "rate limited"}


def test_delete_document_and_project(project_id):
    ingest(project_id, "doc-a")
    ingest(project_id, "doc-b")

    response = api.delete(f"/api/ai/documents/doc-a?project_id={project_id}")
    assert response.status_code == 200
    assert response.json()["deleted_chunks"] > 0
    assert summary_service.get_paper_chunks(project_id, "doc-a")[0] == []

    response = api.delete(f"/api/ai/projects/{project_id}")
    assert response.json()["deleted_chunks"] > 0
    assert summary_service.get_paper_chunks(project_id, "doc-b")[0] == []


def test_internal_token_is_enforced_when_configured(monkeypatch):
    monkeypatch.setattr(main, "AI_SERVICE_TOKEN", "secret")

    assert api.post("/api/ai/summary", json={"project_id": 1}).status_code == 401
    assert api.get("/").status_code == 200

    response = api.post(
        "/api/ai/summary",
        json={"project_id": 1},
        headers={"X-Internal-Token": "secret"},
    )
    assert response.status_code == 400
