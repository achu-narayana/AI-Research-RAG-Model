from langchain_huggingface import HuggingFaceEmbeddings


embeddings = HuggingFaceEmbeddings(
    model_name="sentence-transformers/all-MiniLM-L6-v2"
)


def create_embedding(text):
    return embeddings.embed_query(text)


def create_embeddings(texts):
    return embeddings.embed_documents(texts)