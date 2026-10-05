from ingest_service import ingest_pdf


pdf_path = r"C:\Users\Hp\Downloads\pdf_rag.pdf"

result = ingest_pdf(pdf_path)

print("PDF ingestion completed!")
print("Total characters:", result["characters"])
print("Total chunks:", result["chunks"])