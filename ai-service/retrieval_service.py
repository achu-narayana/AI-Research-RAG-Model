from embedding_service import create_embedding
from vector_store import collection, paper_filter


def search_similar_chunks(
        question,
        top_k=6,
        document_id=None,
        project_id=None
) -> list[dict]:
    """
    Returns the top_k most similar chunks as
    {"text": ..., "paper_name": ...} dicts.
    """

    if project_id is None:
        raise ValueError("project_id is required")

    # Specific PDF selected (still limited to the project)
    if document_id is not None:
        where = paper_filter(project_id, document_id)

    # "All Papers" selected
    else:
        where = {"project_id": project_id}

    question_embedding = create_embedding(question)

    results = collection.query(
        query_embeddings=[question_embedding],
        n_results=top_k,
        where=where,
        include=["documents", "metadatas"]
    )

    documents = (results.get("documents") or [[]])[0]
    metadatas = (results.get("metadatas") or [[]])[0]

    return [
        {
            "text": document,
            "paper_name": (meta or {}).get("paper_name", "Research Paper")
        }
        for document, meta in zip(documents, metadatas)
    ]
