from chunk_service import split_text
from embedding_service import create_embeddings
from errors import ServiceError
from pdf_service import extract_text_from_pdf
from vector_store import collection, delete_document


def ingest_pdf(file_path, document_id, project_id, paper_name):

    try:
        text = extract_text_from_pdf(file_path)
    except Exception as e:
        raise ServiceError(422, f"Could not read the PDF: {e}")

    chunks = split_text(text) if text.strip() else []

    if not chunks:
        raise ServiceError(
            422,
            "No text could be extracted from this PDF. "
            "Scanned PDFs need Tesseract OCR installed."
        )

    vectors = create_embeddings(chunks)

    # Re-ingesting the same paper replaces its old chunks
    # instead of duplicating them.
    delete_document(project_id, document_id)

    ids = [
        f"{project_id}_{document_id}_{index}"
        for index in range(len(chunks))
    ]

    metadatas = [
        {
            "document_id": document_id,
            "project_id": project_id,
            "paper_name": paper_name,
            "chunk_index": index
        }
        for index in range(len(chunks))
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
