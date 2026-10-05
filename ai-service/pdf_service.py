import pymupdf


def extract_text_from_pdf(file_path):
    document = pymupdf.open(file_path)

    text = ""

    for page in document:
        page_text = page.get_text("text", sort=True).strip()

        # If normal text extraction gives little/no text,
        # use OCR for this page.
        if len(page_text) < 10:
            text_page = page.get_textpage_ocr(
                language="eng",
                dpi=150
            )
            page_text = page.get_text(
                "text",
                textpage=text_page
            ).strip()

        text += page_text + "\n"

    document.close()

    return text