import os
from pathlib import Path

from dotenv import load_dotenv


# ==================================================
# Paths
# ==================================================

# Resolve everything relative to this folder so the
# service works no matter where uvicorn is started from.

BASE_DIR = Path(__file__).resolve().parent

DATA_DIR = Path(
    os.getenv("AI_DATA_DIR", str(BASE_DIR / "data"))
)

CHROMA_DIR = DATA_DIR / "chroma"

UPLOAD_DIR = DATA_DIR / "uploads"

COLLECTION_NAME = "research_paper_chunks"


# ==================================================
# Environment
# ==================================================

load_dotenv(BASE_DIR / ".env")

OPENROUTER_API_KEY = os.getenv("OPENROUTER_API_KEY", "").strip()

# Optional comma-separated list of models to use, in order.
# When empty, every free model OpenRouter currently offers
# is used, starting with PREFERRED_FREE_MODELS.
OPENROUTER_MODELS = [
    model.strip()
    for model in os.getenv("OPENROUTER_MODEL", "").split(",")
    if model.strip()
]

PREFERRED_FREE_MODELS = [
    "nvidia/nemotron-3-super-120b-a12b:free",
    "google/gemma-4-31b-it:free",
    "nvidia/nemotron-3-ultra-550b-a55b:free",
    "nvidia/nemotron-3.5-lightning:free"
]

# How many models one request may try before giving up.
# Every attempt can count against the free daily limit.
LLM_MAX_MODEL_ATTEMPTS = int(
    os.getenv("LLM_MAX_MODEL_ATTEMPTS", "4")
)

LLM_TIMEOUT_SECONDS = float(
    os.getenv("LLM_TIMEOUT_SECONDS", "120")
)

# Optional shared secret between Spring Boot and this
# service. When set, every request must send it in the
# X-Internal-Token header.
AI_SERVICE_TOKEN = os.getenv("AI_SERVICE_TOKEN", "").strip()

MAX_UPLOAD_BYTES = 20 * 1024 * 1024
