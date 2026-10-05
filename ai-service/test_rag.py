from rag_service import generate_answer


question = "What is Retrieval Augmented Generation?"

answer = generate_answer(question)

print("\nQuestion:")
print(question)

print("\nRAG Answer:")
print(answer)