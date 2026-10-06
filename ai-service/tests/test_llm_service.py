from types import SimpleNamespace

import httpx
import openai
import pytest

import llm_service
from errors import ServiceError

MODELS = ["a:free", "b:free", "c:free"]


def completion(content, finish_reason="stop"):
    return SimpleNamespace(
        choices=[
            SimpleNamespace(
                message=SimpleNamespace(content=content),
                finish_reason=finish_reason
            )
        ]
    )


def status_error(status, message="x"):
    request = httpx.Request("POST", "https://openrouter.ai/api/v1/chat/completions")
    response = httpx.Response(status, request=request, json={"error": {"message": message}})
    return openai.APIStatusError(message, response=response, body=None)


class FakeClient:
    """Answers per model: results[model] is a list of results to return in turn."""

    def __init__(self, results):
        self.results = results
        self.calls = []
        self.chat = SimpleNamespace(completions=SimpleNamespace(create=self.create))

    def create(self, **kwargs):
        model = kwargs["model"]
        self.calls.append(model)
        result = self.results[model].pop(0)
        if isinstance(result, Exception):
            raise result
        return result


@pytest.fixture
def use_client(monkeypatch):

    monkeypatch.setattr(llm_service, "OPENROUTER_MODELS", [])
    monkeypatch.setattr(llm_service, "PREFERRED_FREE_MODELS", ["b:free"])
    monkeypatch.setattr(llm_service, "fetch_free_models", lambda: list(MODELS))
    monkeypatch.setattr(llm_service, "_free_models", [])
    monkeypatch.setattr(llm_service, "_free_models_fetched_at", 0.0)
    monkeypatch.setattr(llm_service, "_cooldown", {})

    def install(results):
        client = FakeClient(results)
        monkeypatch.setattr(llm_service, "get_client", lambda: client)
        return client

    return install


def test_clean_answer_strips_reasoning():
    assert llm_service.clean_answer("<think>hmm\nok</think>\n\nOK") == "OK"
    assert llm_service.clean_answer("Answer <think>cut off") == "Answer"
    assert llm_service.clean_answer(None) == ""


def test_preferred_free_model_is_tried_first(use_client):
    client = use_client({"b:free": [completion("\n\nHello")]})

    assert llm_service.ask_llm("hi") == "Hello"
    assert client.calls == ["b:free"]


def test_switches_model_when_one_is_rate_limited(use_client):
    client = use_client({
        "b:free": [status_error(429)],
        "a:free": [completion("from a")],
    })

    assert llm_service.ask_llm("hi") == "from a"
    assert client.calls == ["b:free", "a:free"]


def test_failed_model_is_skipped_on_the_next_request(use_client):
    client = use_client({
        "b:free": [status_error(404)],
        "a:free": [completion("one"), completion("two")],
    })

    llm_service.ask_llm("first")
    client.calls.clear()

    assert llm_service.ask_llm("second") == "two"
    assert client.calls == ["a:free"]


def test_daily_free_limit_stops_immediately(use_client):
    client = use_client({
        "b:free": [status_error(429, "Rate limit exceeded: free-models-per-day")],
    })

    with pytest.raises(ServiceError) as error:
        llm_service.ask_llm("hi")

    assert error.value.status_code == 503
    assert "daily limit" in error.value.message
    assert client.calls == ["b:free"]


def test_all_models_busy_gives_503(use_client):
    use_client({m: [status_error(429)] for m in MODELS})

    with pytest.raises(ServiceError) as error:
        llm_service.ask_llm("hi")

    assert error.value.status_code == 503
    assert "busy" in error.value.message


def test_bad_api_key_does_not_try_other_models(use_client):
    client = use_client({"b:free": [status_error(401)]})

    with pytest.raises(ServiceError) as error:
        llm_service.ask_llm("hi")

    assert "API key" in error.value.message
    assert client.calls == ["b:free"]


def test_configured_models_are_used_as_given(use_client, monkeypatch):
    monkeypatch.setattr(llm_service, "OPENROUTER_MODELS", ["paid/model"])
    client = use_client({"paid/model": [completion("paid")]})

    assert llm_service.ask_llm("hi") == "paid"
    assert client.calls == ["paid/model"]


def test_model_list_falls_back_when_openrouter_is_unreachable(use_client, monkeypatch):

    def offline():
        raise httpx.ConnectError("offline")

    monkeypatch.setattr(llm_service, "fetch_free_models", offline)

    assert llm_service.candidate_models() == ["b:free"]


def test_attempts_are_capped(use_client, monkeypatch):
    monkeypatch.setattr(llm_service, "LLM_MAX_MODEL_ATTEMPTS", 2)
    client = use_client({m: [status_error(500)] for m in MODELS})

    with pytest.raises(ServiceError):
        llm_service.ask_llm("hi")

    assert len(client.calls) == 2
