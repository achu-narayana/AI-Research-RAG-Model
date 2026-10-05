import chromadb
import uuid

from pdf_service import extract_text_from_pdf
from chunk_service import split_text
from embedding_service import create_embeddings


client = chromadb.PersistentClient(path="./data/chroma")

collection = client.get_or_create_collection(
    name="research_paper_chunks"
)


def ingest_pdf(file_path, document_id, project_id, paper_name):

    text = extract_text_from_pdf(file_path)

    chunks = split_text(text)

    vectors = create_embeddings(chunks)

    ids = [
        f"chunk_{uuid.uuid4()}"
        for _ in chunks
    ]

    metadatas = [
    {
        "document_id": document_id,
        "project_id": project_id,
        "paper_name": paper_name
    }
    for _ in chunks
]

    collection.add(
        ids=ids,
        documents=chunks,
        embeddings=vectors,
        metadatas=metadatas
    )

    return {
        "characters": len(text),
        "chunks": len(chunks)
    }