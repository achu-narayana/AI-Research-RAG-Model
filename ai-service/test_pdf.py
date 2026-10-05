from pdf_service import extract_text_from_pdf


pdf_path = r"C:\Users\Hp\Downloads\pdf_rag.pdf"

text = extract_text_from_pdf(pdf_path)

print("PDF text extracted successfully!")
print("Number of characters:", len(text))

print("\nFirst 1000 characters:\n")
print(text[:1000])