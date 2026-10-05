from embedding_service import create_embedding


text = "Retrieval Augmented Generation helps answer questions from research papers."

vector = create_embedding(text)

print("Embedding created successfully!")
print("Vector length:", len(vector))
print("First 10 values:")
print(vector[:10])