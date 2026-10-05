from pdf_service import extract_text_from_pdf
from chunk_service import split_text


pdf_path = r"C:\Users\Hp\Downloads\pdf_rag.pdf"

text = extract_text_from_pdf(pdf_path)

chunks = split_text(text)

print("Total characters:", len(text))
print("Number of chunks:", len(chunks))

for i, chunk in enumerate(chunks):
    print(f"\n--- Chunk {i + 1} ---")
    print("Characters:", len(chunk))
    print(chunk[:300])