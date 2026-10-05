from retrieval_service import search_similar_chunks

question = "What is Retrieval Augmented Generation?"

results = search_similar_chunks(
    question,
    top_k=5,
    paper_id=1
)

print("\nQuestion:")
print(question)

print("\nRetrieved chunks from Paper 1:")
print("--------------------------------")

for i, chunk in enumerate(results, start=1):
    print(f"\nResult {i}:")
    print(chunk[:1000])