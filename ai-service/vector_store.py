import chromadb

from config import CHROMA_DIR, COLLECTION_NAME


# =========================================================
# One shared ChromaDB client / collection for all services
# =========================================================

client = chromadb.PersistentClient(path=str(CHROMA_DIR))

collection = client.get_or_create_collection(
    name=COLLECTION_NAME
)


def paper_filter(project_id: int, document_id: str) -> dict:

    return {
        "$and": [
            {"project_id": project_id},
            {"document_id": document_id}
        ]
    }


# =========================================================
# Read
# =========================================================

def get_paper_chunks(
        project_id: int,
        document_id: str,
        default_name: str = "Research Paper"
) -> tuple[list[str], str]:
    """
    Returns all chunks of one paper in their original order,
    plus the paper name.
    """

    results = collection.get(
        where=paper_filter(project_id, document_id),
        include=["documents", "metadatas"]
    )

    documents = results.get("documents") or []
    metadatas = results.get("metadatas") or []

    paper_name = default_name

    if metadatas:
        paper_name = metadatas[0].get("paper_name", default_name)

    # Chroma does not guarantee order, so sort by the index
    # stored at ingest time. Papers ingested before
    # chunk_index existed keep the order Chroma returns.
    pairs = list(zip(documents, metadatas))

    if all("chunk_index" in (meta or {}) for _, meta in pairs):
        pairs.sort(key=lambda pair: pair[1]["chunk_index"])

    return [document for document, _ in pairs], paper_name


# =========================================================
# Delete
# =========================================================

def delete_document(project_id: int, document_id: str) -> int:

    where = paper_filter(project_id, document_id)

    ids = collection.get(where=where, include=[])["ids"]

    if ids:
        collection.delete(ids=ids)

    return len(ids)


def delete_project(project_id: int) -> int:

    where = {"project_id": project_id}

    ids = collection.get(where=where, include=[])["ids"]

    if ids:
        collection.delete(ids=ids)

    return len(ids)


# =========================================================
# Chunk selection for summary / compare
# =========================================================

def select_representative_chunks(
        chunks: list[str],
        max_chunks: int,
        head_count: int
) -> list[str]:
    """
    Picks up to max_chunks chunks that cover the whole paper:
    the first head_count chunks (title / abstract /
    introduction), then chunks spread evenly over the rest,
    always including the last one (conclusion).
    The result keeps the paper's original order.
    """

    if len(chunks) <= max_chunks:
        return list(chunks)

    head_count = min(head_count, max_chunks)

    rest = chunks[head_count:]
    slots = max_chunks - head_count

    if slots <= 0:
        return chunks[:max_chunks]

    if slots == 1:
        picked = [len(rest) - 1]
    else:
        picked = sorted({
            round(i * (len(rest) - 1) / (slots - 1))
            for i in range(slots)
        })

    return chunks[:head_count] + [rest[i] for i in picked]
