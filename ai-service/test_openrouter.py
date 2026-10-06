from llm_service import ask_llm


response = ask_llm(
    "Explain what a research paper is in 3 simple sentences."
)

print("\nQwen response:")
print(response)