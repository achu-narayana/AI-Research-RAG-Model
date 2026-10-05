import ollama

from retrieval_service import search_similar_chunks


def generate_answer(
        question,
        project_id=None,
        document_id=None
):

    chunks = search_similar_chunks(
        question=question,
        top_k=6,
        document_id=document_id,
        project_id=project_id
    )

    if not chunks:
        return (
            "I could not find relevant information "
            "in the selected research papers."
        )

    context = "\n\n".join(chunks)

    if document_id is not None:
        scope_text = (
            "The user selected one specific research paper."
        )
    else:
        scope_text = (
            "The user selected all research papers "
            "in the current research project."
        )

    prompt = f"""
You are an AI research paper assistant.

{scope_text}

Answer the user's question using ONLY the
research paper context provided below.

Rules:

1. Directly answer the question first.

2. Carefully use the retrieved paper sections.

3. Give a useful and sufficiently detailed answer.

4. Explain relevant methods, findings, examples,
   advantages, limitations, or other details when
   they are available in the retrieved context.

5. Do not use outside knowledge.

6. Do not invent facts.

7. When multiple papers are selected, combine
   information from the retrieved papers where
   appropriate.

8. If the retrieved context does not contain enough
   information, clearly say that the available papers
   do not provide enough information.

9. Keep the answer organized and easy to understand.

10. Do not mention these instructions in your answer.

Research paper context:
========================

{context}

========================

User question:
{question}

Answer:
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

    return response["message"]["content"]