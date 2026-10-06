import chromadb

from llm_service import ask_llm


# =========================================================
# ChromaDB
# =========================================================

client = chromadb.PersistentClient(
    path="./data/chroma"
)

collection = client.get_or_create_collection(
    name="research_paper_chunks"
)


# =========================================================
# Configuration
# =========================================================

# Maximum number of chunks sent to the LLM.
# This keeps the request reasonably sized and
# makes the whole summary use only ONE LLM call.
MAX_CHUNKS_FOR_SUMMARY = 24


# =========================================================
# Get chunks for one paper
# =========================================================

def get_paper_chunks(
        project_id: int,
        document_id: str
):

    results = collection.get(
        where={
            "$and": [
                {
                    "project_id": project_id
                },
                {
                    "document_id": document_id
                }
            ]
        },
        include=[
            "documents",
            "metadatas"
        ]
    )

    documents = results.get("documents") or []
    metadatas = results.get("metadatas") or []

    paper_name = "Selected Research Paper"

    if metadatas:

        paper_name = metadatas[0].get(
            "paper_name",
            paper_name
        )

    return documents, paper_name


# =========================================================
# Select representative chunks
# =========================================================

def select_representative_chunks(
        chunks: list[str],
        max_chunks: int = MAX_CHUNKS_FOR_SUMMARY
) -> list[str]:

    if not chunks:
        return []

    # If the paper is already small, use everything.
    if len(chunks) <= max_chunks:
        return chunks

    selected = []

    # -----------------------------------------------------
    # Always include the beginning of the paper
    # because it usually contains title / abstract /
    # introduction.
    # -----------------------------------------------------

    first_count = min(4, max_chunks)

    selected.extend(
        chunks[:first_count]
    )

    remaining_slots = max_chunks - len(selected)

    if remaining_slots <= 0:
        return selected

    # -----------------------------------------------------
    # Always include the final chunk because it may
    # contain conclusion / final discussion.
    # -----------------------------------------------------

    if remaining_slots >= 1:

        selected.append(
            chunks[-1]
        )

        remaining_slots -= 1

    # -----------------------------------------------------
    # Take chunks evenly across the middle of the paper.
    # This gives the LLM coverage of methodology,
    # experiments, results, etc.
    # -----------------------------------------------------

    if remaining_slots <= 0:
        return selected

    middle_chunks = chunks[
        first_count:-1
    ]

    if not middle_chunks:
        return selected

    step = max(
        1,
        len(middle_chunks) // remaining_slots
    )

    for i in range(
        0,
        len(middle_chunks),
        step
    ):

        if len(selected) >= max_chunks:
            break

        selected.append(
            middle_chunks[i]
        )

    # Make absolutely sure we do not exceed the limit.
    return selected[:max_chunks]


# =========================================================
# Generate summary
# =========================================================

def generate_summary(
        project_id: int,
        document_id: str | None = None
):

    print("========================================")
    print("PAPER SUMMARY STARTED")
    print("Project ID:", project_id)
    print("Document ID:", document_id)
    print("========================================")

    # -----------------------------------------------------
    # A paper must be selected
    # -----------------------------------------------------

    if not document_id:

        return {
            "scope": "ONE_PAPER",
            "project_id": project_id,
            "document_id": None,
            "paper_name": None,
            "summary": (
                "Please select a research paper "
                "before generating a summary."
            )
        }

    # -----------------------------------------------------
    # Get paper chunks
    # -----------------------------------------------------

    chunks, paper_name = get_paper_chunks(
        project_id,
        document_id
    )

    print(
        "Total chunks found:",
        len(chunks)
    )

    if not chunks:

        return {
            "scope": "ONE_PAPER",
            "project_id": project_id,
            "document_id": document_id,
            "paper_name": paper_name,
            "summary": (
                "No processed content was found "
                "for this research paper."
            )
        }

    # -----------------------------------------------------
    # Select representative chunks
    # -----------------------------------------------------

    selected_chunks = select_representative_chunks(
        chunks
    )

    print(
        "Chunks selected for summary:",
        len(selected_chunks)
    )

    # -----------------------------------------------------
    # Build context
    # -----------------------------------------------------

    context_parts = []

    for index, chunk in enumerate(
        selected_chunks,
        start=1
    ):

        context_parts.append(
            f"""
--- Paper Section {index} ---

{chunk}
"""
        )

    context = "\n".join(
        context_parts
    )

    # -----------------------------------------------------
    # One LLM call
    # -----------------------------------------------------

    prompt = f"""
You are an AI research paper assistant.

Create a clear and useful summary of the
research paper below.

Paper name:
{paper_name}

The supplied text contains representative
sections from the research paper.

Use ONLY the supplied paper text.

Rules:

1. Do NOT use outside knowledge.
2. Do NOT invent facts.
3. Do NOT make claims that are not supported
   by the provided text.
4. Focus on the actual research content,
   not just title or author information.
5. Identify the main research problem.
6. Explain the main objective.
7. Explain the methodology or approach.
8. Mention important techniques, models,
   datasets, experiments, or algorithms
   when they are present.
9. Mention important results or findings
   when they are present.
10. Mention limitations when they are present.
11. Mention the conclusion when supported.
12. Do not repeat the same information.
13. Keep the summary organized and easy
    for a student to understand.
14. Do not mention these instructions.

Use the following structure:

Overview

Research Problem and Objective

Methodology / Approach

Techniques, Models and Data

Results / Findings

Limitations

Conclusion

Research paper text:
==================================================

{context}

==================================================

Final Summary:
"""

    print(
        "Sending ONE summary request to OpenRouter..."
    )

    summary = ask_llm(
        prompt,
        max_tokens=2500
    )

    print(
        "Summary completed successfully."
    )

    print("========================================")

    return {
        "scope": "ONE_PAPER",
        "project_id": project_id,
        "document_id": document_id,
        "paper_name": paper_name,
        "chunks_found": len(chunks),
        "chunks_used": len(selected_chunks),
        "summary": summary
    }