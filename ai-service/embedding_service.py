from functools import cache

from langchain_huggingface import HuggingFaceEmbeddings


# Loaded on first use; the model takes a few seconds to load.
@cache
def get_embeddings():
    return HuggingFaceEmbeddings(
        model_name="sentence-transformers/all-MiniLM-L6-v2"
    )


def create_embedding(text):
    return get_embeddings().embed_query(text)


def create_embeddings(texts):
    return get_embeddings().embed_documents(texts)
