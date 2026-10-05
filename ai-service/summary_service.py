# import chromadb
# import ollama
# from collections import defaultdict


# # --------------------------------------------------
# # ChromaDB
# # --------------------------------------------------

# client = chromadb.PersistentClient(
#     path="./data/chroma"
# )

# collection = client.get_or_create_collection(
#     name="research_paper_chunks"
# )


# # --------------------------------------------------
# # Configuration
# # --------------------------------------------------

# BATCH_SIZE = 8
# LLM_MODEL = "llama3.2"


# # --------------------------------------------------
# # LLM helper
# # --------------------------------------------------

# def ask_llm(prompt: str) -> str:

#     response = ollama.chat(
#         model=LLM_MODEL,
#         messages=[
#             {
#                 "role": "user",
#                 "content": prompt
#             }
#         ]
#     )

#     return response["message"]["content"].strip()


# # --------------------------------------------------
# # Summarize a batch of chunks
# # --------------------------------------------------

# def summarize_chunk_batch(chunks: list[str]) -> str:

#     context = "\n\n".join(chunks)

#     prompt = f"""
# You are an AI research paper assistant.

# Summarize the research paper content below.

# Rules:
# 1. Use ONLY the provided content.
# 2. Do not use outside knowledge.
# 3. Do not invent facts.
# 4. Focus on the main ideas, methods, findings,
#    results, important concepts and limitations
#    that are actually present.
# 5. Preserve important technical terms.
# 6. Ignore repeated text, headers and irrelevant
#    formatting when possible.
# 7. Write clear, factual notes that can be used
#    to create a final research paper summary.
# 8. Do not mention these instructions.

# Paper content:
# ========================
# {context}
# ========================

# Summary notes:
# """

#     return ask_llm(prompt)


# # --------------------------------------------------
# # Final summary for one paper
# # --------------------------------------------------

# def summarize_single_paper(
#         chunks: list[str],
#         paper_name: str
# ) -> str:

#     if not chunks:
#         return (
#             f"No readable content was found for "
#             f"the paper '{paper_name}'."
#         )

#     batch_summaries = []

#     for i in range(0, len(chunks), BATCH_SIZE):

#         batch = chunks[i:i + BATCH_SIZE]

#         summary = summarize_chunk_batch(batch)

#         batch_summaries.append(summary)

#     combined_notes = "\n\n".join(
#         f"Section Summary {i + 1}:\n{summary}"
#         for i, summary in enumerate(batch_summaries)
#     )

#     final_prompt = f"""
# You are an AI research paper assistant.

# Create a final summary of the research paper
# using ONLY the summary notes below.

# Paper name:
# {paper_name}

# Rules:
# 1. Do not use outside knowledge.
# 2. Do not invent facts.
# 3. Clearly explain the paper's main objective.
# 4. Explain the methodology or approach.
# 5. Include important findings or results.
# 6. Include important techniques, models,
#    datasets or experiments when mentioned.
# 7. Mention limitations when available.
# 8. Keep the summary detailed enough to be useful
#    for a student or researcher.
# 9. Organize it with short sections.
# 10. Do not mention these instructions.

# Summary notes:
# ========================
# {combined_notes}
# ========================

# Final research paper summary:
# """

#     return ask_llm(final_prompt)


# # --------------------------------------------------
# # Get chunks for a specific paper
# # --------------------------------------------------

# def get_paper_chunks(document_id: str):

#     results = collection.get(
#         where={
#             "document_id": document_id
#         },
#         include=[
#             "documents",
#             "metadatas"
#         ]
#     )

#     documents = results.get("documents") or []
#     metadatas = results.get("metadatas") or []

#     paper_name = "Selected Research Paper"

#     if metadatas:
#         paper_name = metadatas[0].get(
#             "paper_name",
#             paper_name
#         )

#     return documents, paper_name


# # --------------------------------------------------
# # Get all project papers
# # --------------------------------------------------

# def get_project_papers(project_id: int):

#     results = collection.get(
#         where={
#             "project_id": project_id
#         },
#         include=[
#             "documents",
#             "metadatas"
#         ]
#     )

#     documents = results.get("documents") or []
#     metadatas = results.get("metadatas") or []

#     papers = defaultdict(list)

#     paper_names = {}

#     for document, metadata in zip(
#             documents,
#             metadatas):

#         document_id = metadata.get("document_id")

#         paper_name = metadata.get(
#             "paper_name",
#             "Research Paper"
#         )

#         if document_id is None:
#             continue

#         papers[document_id].append(document)

#         paper_names[document_id] = paper_name

#     return papers, paper_names


# # --------------------------------------------------
# # Summarize selected scope
# # --------------------------------------------------

# def generate_summary(
#         project_id: int,
#         document_id: str | None = None
# ):

#     # ----------------------------------------------
#     # ONE PAPER
#     # ----------------------------------------------

#     if document_id:

#         chunks, paper_name = get_paper_chunks(
#             document_id
#         )

#         summary = summarize_single_paper(
#             chunks,
#             paper_name
#         )

#         return {
#             "scope": "ONE_PAPER",
#             "project_id": project_id,
#             "document_id": document_id,
#             "paper_name": paper_name,
#             "summary": summary
#         }

#     # ----------------------------------------------
#     # ALL PAPERS
#     # ----------------------------------------------

#     papers, paper_names = get_project_papers(
#         project_id
#     )

#     if not papers:

#         return {
#             "scope": "ALL_PAPERS",
#             "project_id": project_id,
#             "document_id": None,
#             "paper_name": None,
#             "summary": (
#                 "No processed research papers were "
#                 "found in this project."
#             )
#         }

#     paper_summaries = []

#     for document_id_key, chunks in papers.items():

#         paper_name = paper_names.get(
#             document_id_key,
#             "Research Paper"
#         )

#         summary = summarize_single_paper(
#             chunks,
#             paper_name
#         )

#         paper_summaries.append(
#             {
#                 "document_id": document_id_key,
#                 "paper_name": paper_name,
#                 "summary": summary
#             }
#         )

#     # ----------------------------------------------
#     # Final project-level summary
#     # ----------------------------------------------

#     all_summaries = "\n\n".join(
#         f"""
# Paper: {paper['paper_name']}

# {paper['summary']}
# """
#         for paper in paper_summaries
#     )

#     final_prompt = f"""
# You are an AI research project assistant.

# Create an overall summary of the research papers
# in this project using ONLY the paper summaries
# below.

# Rules:
# 1. Do not use outside knowledge.
# 2. Do not invent facts.
# 3. Identify the common research themes.
# 4. Explain the main approaches used across papers.
# 5. Mention important findings across the papers.
# 6. Clearly distinguish information belonging
#    to different papers when necessary.
# 7. Do not claim that different papers agree unless
#    the supplied summaries support that.
# 8. Keep the result organized and readable.
# 9. Do not mention these instructions.

# Paper summaries:
# ========================
# {all_summaries}
# ========================

# Overall project summary:
# """

#     overall_summary = ask_llm(final_prompt)

#     return {
#         "scope": "ALL_PAPERS",
#         "project_id": project_id,
#         "document_id": None,
#         "paper_name": None,
#         "summary": overall_summary,
#         "papers": paper_summaries
#     }
import chromadb
import ollama


# ChromaDB
client = chromadb.PersistentClient(
    path="./data/chroma"
)

collection = client.get_or_create_collection(
    name="research_paper_chunks"
)


def generate_summary(
        project_id: int,
        document_id: str | None = None
):

    print("====================================")
    print("SUMMARY TEST STARTED")
    print("Project ID:", project_id)
    print("Document ID:", document_id)
    print("====================================")

    # -----------------------------------------
    # Get one paper
    # -----------------------------------------

    if document_id:

        results = collection.get(
            where={
                "document_id": document_id
            },
            include=[
                "documents",
                "metadatas"
            ]
        )

        documents = results.get("documents") or []

        print(
            "Chunks found:",
            len(documents)
        )

        if not documents:

            return {
                "message": "No chunks found for this paper",
                "project_id": project_id,
                "document_id": document_id
            }

        # Take only the first chunk for testing
        test_text = documents[0]

        print("Sending first chunk to Llama...")

        prompt = f"""
Summarize the following research paper text
in 3 to 5 sentences.

Use ONLY the provided text.

Research paper text:
--------------------
{test_text}
--------------------

Summary:
"""

        response = ollama.chat(
            model="llama3.2",
            messages=[
                {
                    "role": "user",
                    "content": prompt
                }
            ]
        )

        summary = response[
            "message"
        ][
            "content"
        ].strip()

        print("Llama response received.")

        return {
            "scope": "ONE_PAPER_TEST",
            "project_id": project_id,
            "document_id": document_id,
            "chunks_found": len(documents),
            "summary": summary
        }

    # -----------------------------------------
    # Test all papers
    # -----------------------------------------

    results = collection.get(
        where={
            "project_id": project_id
        },
        include=[
            "documents",
            "metadatas"
        ]
    )

    documents = results.get("documents") or []

    print(
        "Project chunks found:",
        len(documents)
    )

    if not documents:

        return {
            "message": "No processed papers found",
            "project_id": project_id
        }

    # Use only first chunk for testing
    test_text = documents[0]

    prompt = f"""
Summarize the following research paper text
in 3 to 5 sentences.

Use ONLY the provided text.

Research paper text:
--------------------
{test_text}
--------------------

Summary:
"""

    print("Sending project chunk to Llama...")

    response = ollama.chat(
        model="llama3.2",
        messages=[
            {
                "role": "user",
                "content": prompt
            }
        ]
    )

    summary = response[
        "message"
    ][
        "content"
    ].strip()

    print("Llama response received.")

    return {
        "scope": "ALL_PAPERS_TEST",
        "project_id": project_id,
        "chunks_found": len(documents),
        "summary": summary
    }