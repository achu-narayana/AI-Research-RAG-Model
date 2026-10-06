import hashlib
import itertools
import os
import sys
import tempfile
from pathlib import Path

import pytest

# Must happen before any service module is imported:
# use a throwaway ChromaDB and never the real API key.
os.environ["AI_DATA_DIR"] = tempfile.mkdtemp(prefix="ai-service-tests-")
os.environ["OPENROUTER_API_KEY"] = "test-key"
os.environ["AI_SERVICE_TOKEN"] = ""

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))


def fake_embedding(text: str) -> list[float]:
    """Cheap deterministic word-hash vector instead of the real model."""

    vector = [0.0] * 64

    for word in text.lower().split():
        digest = hashlib.md5(word.encode()).digest()
        vector[digest[0] % 64] += 1.0

    return vector


@pytest.fixture(autouse=True)
def no_real_models(monkeypatch):

    import ingest_service
    import llm_service
    import retrieval_service

    monkeypatch.setattr(
        ingest_service,
        "create_embeddings",
        lambda texts: [fake_embedding(t) for t in texts]
    )
    monkeypatch.setattr(
        retrieval_service,
        "create_embedding",
        fake_embedding
    )

    def fail_if_called():
        raise AssertionError("Tests must not call OpenRouter")

    monkeypatch.setattr(llm_service, "get_client", fail_if_called)


_project_ids = itertools.count(1000)


@pytest.fixture
def project_id():
    """A fresh project id per test, so tests don't see each other's data."""
    return next(_project_ids)


def make_pdf(pages: list[str]) -> bytes:

    import pymupdf

    document = pymupdf.open()

    for text in pages:
        page = document.new_page()
        page.insert_textbox(pymupdf.Rect(50, 50, 550, 800), text, fontsize=9)

    data = document.tobytes()
    document.close()

    return data
