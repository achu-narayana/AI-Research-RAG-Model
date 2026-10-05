import chromadb

from embedding_service import create_embedding


client = chromadb.PersistentClient(
    path="./data/chroma"
)

collection = client.get_or_create_collection(
    name="research_paper_chunks"
)


def search_similar_chunks(
        question,
        top_k=6,
        document_id=None,
        project_id=None
):

    question_embedding = create_embedding(question)

    query_args = {
        "query_embeddings": [question_embedding],
        "n_results": top_k
    }

    # Specific PDF selected
    if document_id is not None:
        query_args["where"] = {
            "document_id": document_id
        }

    # "All Papers" selected
    elif project_id is not None:
        query_args["where"] = {
            "project_id": project_id
        }

    else:
        raise ValueError(
            "Either document_id or project_id is required"
        )

    results = collection.query(**query_args)

    documents = results.get("documents", [[]])[0]

    return documents