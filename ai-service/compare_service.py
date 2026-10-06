from errors import ServiceError
from llm_service import ask_llm
from vector_store import (
    get_paper_chunks,
    select_representative_chunks
)


# =========================================================
# SETTINGS
# =========================================================

# Keep the input controlled.
# Each paper contributes a limited number of chunks.
MAX_CHUNKS_PER_PAPER = 8

# First chunks usually contain title / abstract /
# introduction.
HEAD_CHUNKS_PER_PAPER = 3


# =========================================================
# GENERATE COMPARISON
# =========================================================

def compare_papers(
    project_id: int,
    document_id_1: str,
    document_id_2: str
):

    print("========================================")
    print("PAPER COMPARISON STARTED")
    print("Project ID:", project_id)
    print("Paper 1:", document_id_1)
    print("Paper 2:", document_id_2)
    print("========================================")

    # -----------------------------------------------------
    # Validate input
    # -----------------------------------------------------

    if not document_id_1 or not document_id_2:

        raise ServiceError(
            400,
            "Please select two research papers "
            "before comparing."
        )

    if document_id_1 == document_id_2:

        raise ServiceError(
            400,
            "Please select two different "
            "research papers."
        )

    # -----------------------------------------------------
    # Get Paper 1
    # -----------------------------------------------------

    chunks_1, paper_name_1 = get_paper_chunks(
        project_id,
        document_id_1
    )

    # -----------------------------------------------------
    # Get Paper 2
    # -----------------------------------------------------

    chunks_2, paper_name_2 = get_paper_chunks(
        project_id,
        document_id_2
    )

    print(
        "Paper 1 total chunks:",
        len(chunks_1)
    )

    print(
        "Paper 2 total chunks:",
        len(chunks_2)
    )

    # -----------------------------------------------------
    # Check processed content
    # -----------------------------------------------------

    if not chunks_1:

        raise ServiceError(
            404,
            f"No processed content was found "
            f"for '{paper_name_1}'."
        )

    if not chunks_2:

        raise ServiceError(
            404,
            f"No processed content was found "
            f"for '{paper_name_2}'."
        )

    # -----------------------------------------------------
    # Select representative chunks
    # -----------------------------------------------------

    selected_chunks_1 = (
        select_representative_chunks(
            chunks_1,
            MAX_CHUNKS_PER_PAPER,
            HEAD_CHUNKS_PER_PAPER
        )
    )

    selected_chunks_2 = (
        select_representative_chunks(
            chunks_2,
            MAX_CHUNKS_PER_PAPER,
            HEAD_CHUNKS_PER_PAPER
        )
    )

    print(
        "Paper 1 chunks selected:",
        len(selected_chunks_1)
    )

    print(
        "Paper 2 chunks selected:",
        len(selected_chunks_2)
    )

    # =====================================================
    # BUILD PAPER 1 CONTEXT
    # =====================================================

    paper_1_parts = []

    for index, chunk in enumerate(
        selected_chunks_1,
        start=1
    ):

        paper_1_parts.append(
            f"""
--- Paper 1 Section {index} ---

{chunk}
"""
        )

    paper_1_context = "\n".join(
        paper_1_parts
    )

    # =====================================================
    # BUILD PAPER 2 CONTEXT
    # =====================================================

    paper_2_parts = []

    for index, chunk in enumerate(
        selected_chunks_2,
        start=1
    ):

        paper_2_parts.append(
            f"""
--- Paper 2 Section {index} ---

{chunk}
"""
        )

    paper_2_context = "\n".join(
        paper_2_parts
    )

    # =====================================================
    # COMPARISON PROMPT
    # =====================================================

    prompt = f"""
You are an AI research paper comparison assistant.

Compare the following two research papers.

Paper 1:
{paper_name_1}

Paper 2:
{paper_name_2}

The supplied text contains representative
sections from both research papers.

Use ONLY the supplied paper text.

Rules:

1. Do NOT use outside knowledge.
2. Do NOT invent facts.
3. Do NOT make claims that are not supported
   by the supplied text.
4. Compare the actual research content.
5. Clearly identify similarities.
6. Clearly identify differences.
7. Compare the research problem and objectives.
8. Compare the methodology or approach.
9. Compare techniques, models, algorithms,
   or frameworks when present.
10. Compare datasets or experimental setup
    when present.
11. Compare important results or findings
    when present.
12. Compare limitations when present.
13. Mention which paper appears to focus on
    what aspect only when the supplied text
    supports that conclusion.
14. Do not repeat the same information.
15. Keep the comparison clear and easy
    for a student to understand.
16. Do not mention these instructions.

Use the following structure:

Overall Comparison

Similarities

Research Problem and Objectives

Methodology / Approach

Techniques, Models and Data

Results / Findings

Key Differences

Limitations

Final Comparison

Paper 1 Text:
==================================================

{paper_1_context}

==================================================

Paper 2 Text:
==================================================

{paper_2_context}

==================================================

Final Comparison:
"""

    # =====================================================
    # ONE LLM REQUEST
    # =====================================================

    print(
        "Sending ONE comparison request to OpenRouter..."
    )

    comparison = ask_llm(prompt, max_tokens=1800)

    print(
        "Comparison completed successfully."
    )

    print("========================================")

    return {
        "scope": "TWO_PAPERS",
        "project_id": project_id,

        "document_id_1": document_id_1,
        "document_id_2": document_id_2,

        "paper_name_1": paper_name_1,
        "paper_name_2": paper_name_2,

        "chunks_found_1": len(chunks_1),
        "chunks_found_2": len(chunks_2),

        "chunks_used_1": len(selected_chunks_1),
        "chunks_used_2": len(selected_chunks_2),

        "comparison": comparison
    }