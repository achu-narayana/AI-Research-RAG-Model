import chromadb
from collections import Counter

client = chromadb.PersistentClient(path="./data/chroma")

collection = client.get_collection(
    name="research_paper_chunks"
)

results = collection.get(
    include=["metadatas"]
)

print("\nTotal chunks:")
print(len(results["ids"]))

print("\nPapers stored:")
print("----------------")

paper_counts = Counter()

for metadata in results["metadatas"]:
    paper_id = metadata.get("document_id")
    paper_name = metadata.get("paper_name")
    
    paper_counts[(paper_id, paper_name)] += 1

for (paper_id, paper_name), count in sorted(paper_counts.items()):
    print(
        f"Paper ID: {paper_id} | "
        f"Name: {paper_name} | "
        f"Chunks: {count}"
    )