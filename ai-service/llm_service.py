import re
import threading
import time

import httpx
import openai
from openai import OpenAI

from config import (
    LLM_MAX_MODEL_ATTEMPTS,
    LLM_TIMEOUT_SECONDS,
    OPENROUTER_API_KEY,
    OPENROUTER_MODELS,
    PREFERRED_FREE_MODELS
)
from errors import ServiceError


OPENROUTER_URL = "https://openrouter.ai/api/v1"


# ==================================================
# OpenRouter client (created on first use, so a missing
# key only breaks LLM features, not PDF ingest)
# ==================================================

_client = None


def get_client() -> OpenAI:

    global _client

    if not OPENROUTER_API_KEY:
        raise ServiceError(
            503,
            "OPENROUTER_API_KEY is not set in ai-service/.env."
        )

    if _client is None:
        _client = OpenAI(
            base_url=OPENROUTER_URL,
            api_key=OPENROUTER_API_KEY,
            timeout=LLM_TIMEOUT_SECONDS,
            # Retries are handled below by switching models.
            max_retries=0
        )

    return _client


# ==================================================
# Model list
# ==================================================

FREE_MODEL_LIST_TTL = 60 * 60

# Free models that are not general chat models.
EXCLUDED_MODEL_WORDS = ("safety", "guard", "embed", "-vl-", "omni")

_free_models: list[str] = []
_free_models_fetched_at = 0.0

# model id -> time until which it is skipped
_cooldown: dict[str, float] = {}

_lock = threading.Lock()


def fetch_free_models() -> list[str]:
    """
    Asks OpenRouter which ':free' chat models exist right now,
    largest context first.
    """

    response = httpx.get(f"{OPENROUTER_URL}/models", timeout=15)
    response.raise_for_status()

    models = []

    for model in response.json().get("data", []):

        model_id = model.get("id", "")
        pricing = model.get("pricing") or {}

        if not model_id.endswith(":free"):
            continue

        if str(pricing.get("prompt", "0")) not in ("0", "0.0"):
            continue

        if any(word in model_id for word in EXCLUDED_MODEL_WORDS):
            continue

        if (model.get("context_length") or 0) < 32000:
            continue

        models.append((model.get("context_length") or 0, model_id))

    models.sort(reverse=True)

    return [model_id for _, model_id in models]


def candidate_models() -> list[str]:
    """
    The models to try, in order.

    OPENROUTER_MODEL set   -> exactly those models.
    OPENROUTER_MODEL unset -> every free model OpenRouter offers,
                              preferred ones first.
    """

    global _free_models, _free_models_fetched_at

    if OPENROUTER_MODELS:
        return list(OPENROUTER_MODELS)

    with _lock:

        stale = time.time() - _free_models_fetched_at > FREE_MODEL_LIST_TTL

        if stale or not _free_models:
            try:
                available = fetch_free_models()
                preferred = [m for m in PREFERRED_FREE_MODELS if m in available]
                _free_models = preferred + [
                    m for m in available if m not in preferred
                ]
                print(f"    Free models available: {len(_free_models)}")

            except Exception as e:
                print(f"    Could not load the free model list: {e}")

                if not _free_models:
                    _free_models = list(PREFERRED_FREE_MODELS)

            _free_models_fetched_at = time.time()

        return list(_free_models)


def put_on_cooldown(model: str, seconds: float) -> None:
    with _lock:
        _cooldown[model] = time.time() + seconds


def models_to_try() -> list[str]:
    """
    Candidate models without the ones cooling down. If every
    model is cooling down, the one that recovers first is tried.
    """

    models = candidate_models()
    now = time.time()

    with _lock:
        ready = [m for m in models if _cooldown.get(m, 0) <= now]

        if not ready and models:
            ready = [min(models, key=lambda m: _cooldown.get(m, 0))]

    return ready


# ==================================================
# Helpers
# ==================================================

THINK_BLOCK = re.compile(
    r"<think>.*?</think>",
    flags=re.DOTALL | re.IGNORECASE
)


def clean_answer(text: str | None) -> str:
    """
    Removes reasoning that some models put inline in the
    answer, e.g. <think>...</think>.
    """

    if not text:
        return ""

    text = THINK_BLOCK.sub("", text)

    # An unclosed <think> means the answer was cut off
    # while still reasoning; nothing usable follows it.
    if "<think>" in text.lower():
        text = text[:text.lower().index("<think>")]

    return text.strip()


def is_daily_free_limit(detail: str) -> bool:
    """
    The free daily request limit counts for the whole account,
    so switching to another free model does not help.
    """

    return "per-day" in detail.lower()


class ModelFailed(Exception):
    """One model failed; the next model may still work."""

    def __init__(self, reason: str, cooldown_seconds: float):
        super().__init__(reason)
        self.reason = reason
        self.cooldown_seconds = cooldown_seconds


def call_model(client, model: str, prompt: str, max_tokens: int) -> str:

    try:
        response = client.chat.completions.create(
            model=model,
            messages=[
                {
                    "role": "user",
                    "content": prompt
                }
            ],
            temperature=0.2,
            max_tokens=max_tokens,
            extra_body={
                # Keep reasoning short and out of the answer.
                "reasoning": {
                    "effort": "low",
                    "exclude": True
                }
            }
        )

    except openai.APITimeoutError:
        raise ModelFailed("timed out", 5 * 60)

    except openai.APIConnectionError as e:
        raise ServiceError(503, f"Cannot reach OpenRouter: {e}")

    except openai.APIStatusError as e:

        detail = str(e)
        status = e.status_code

        if status == 401:
            raise ServiceError(
                502,
                "OpenRouter rejected the API key. Check OPENROUTER_API_KEY."
            )

        if status == 429 and is_daily_free_limit(detail):
            raise ServiceError(
                503,
                "The daily limit of free AI requests for this OpenRouter "
                "account has been reached. It resets at midnight UTC, or "
                "add credits to OpenRouter to raise the limit."
            )

        if status == 429:
            raise ModelFailed("rate-limited", 2 * 60)

        if status == 402:
            raise ModelFailed("needs credits", 60 * 60)

        if status in (400, 404):
            # Model removed, renamed or not available for free.
            raise ModelFailed(f"unavailable ({status})", 60 * 60)

        raise ModelFailed(f"error {status}", 5 * 60)

    if not response.choices:
        raise ModelFailed("no choices returned", 5 * 60)

    choice = response.choices[0]
    answer = clean_answer(choice.message.content)

    if answer:
        return answer

    if choice.finish_reason == "length":
        raise ModelFailed("used its token limit before answering", 0)

    raise ModelFailed("empty answer", 5 * 60)


# ==================================================
# LLM call
# ==================================================

def ask_llm(
        prompt: str,
        max_tokens: int = 2000
) -> str:
    """
    Tries the available models one after another until one
    answers. A model that fails is skipped for a while, so
    later requests go straight to a working model.
    """

    client = get_client()

    models = models_to_try()[:LLM_MAX_MODEL_ATTEMPTS]

    if not models:
        raise ServiceError(503, "No AI models are configured.")

    failures = []

    for model in models:

        print(f"    Sending request to OpenRouter ({model})...")

        try:
            answer = call_model(client, model, prompt, max_tokens)
            print(f"    OpenRouter response received ({model}).")
            return answer

        except ModelFailed as failure:
            print(f"    {model} failed: {failure.reason}. Trying next model.")
            failures.append(f"{model}: {failure.reason}")

            if failure.cooldown_seconds:
                put_on_cooldown(model, failure.cooldown_seconds)

    if all("rate-limited" in failure for failure in failures):
        raise ServiceError(
            503,
            "All free AI models are busy right now. "
            "Please try again in a minute."
        )

    raise ServiceError(
        502,
        "No AI model could answer. Tried: " + "; ".join(failures)
    )
