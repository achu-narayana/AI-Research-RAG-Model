import pymupdf


_ocr_warning_shown = False


def ocr_page(page) -> str:
    """
    OCR needs Tesseract to be installed. Without it, the page
    is skipped instead of failing the whole PDF.
    """

    global _ocr_warning_shown

    try:
        text_page = page.get_textpage_ocr(
            language="eng",
            dpi=150
        )
        return page.get_text(
            "text",
            textpage=text_page
        ).strip()

    except Exception as e:
        if not _ocr_warning_shown:
            print(f"    OCR unavailable, skipping image-only pages: {e}")
            _ocr_warning_shown = True
        return ""


def extract_text_from_pdf(file_path):

    text = ""

    with pymupdf.open(file_path) as document:

        for page in document:
            page_text = page.get_text("text", sort=True).strip()

            # If normal text extraction gives little/no text,
            # use OCR for this page.
            if len(page_text) < 10:
                page_text = ocr_page(page) or page_text

            text += page_text + "\n"

    return text
