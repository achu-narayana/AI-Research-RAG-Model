"""
Sends one real request to OpenRouter with the models from .env.
Uses one of the free daily requests.

    python scripts/check_openrouter.py
"""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from config import OPENROUTER_MODELS  # noqa: E402
from llm_service import ask_llm  # noqa: E402

print("Models:", ", ".join(OPENROUTER_MODELS))

print(ask_llm("Explain what a research paper is in 3 simple sentences."))
