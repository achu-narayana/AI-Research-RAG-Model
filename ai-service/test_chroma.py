import chromadb

from embedding_service import create_embedding


client = chromadb.PersistentClient(path="./data/chroma")

collection = client.get_or_create_collection(
    name="research_papers"
)

documents = [
    "Retrieval-Augmented Generation retrieves relevant information from documents.",
    "Large language models can generate answers using retrieved context."
]

embeddings = [
    create_embedding(documents[0]),
    create_embedding(documents[1])
]

collection.add(
    ids=["test1", "test2"],
    documents=documents,
    embeddings=embeddings
)

question = "How does RAG retrieve information?"

question_embedding = create_embedding(question)

results = collection.query(
    query_embeddings=[question_embedding],
    n_results=1
)

print("ChromaDB is working!")

print("\nQuestion:")
print(question)

print("\nRetrieved document:")
print(results["documents"][0][0])