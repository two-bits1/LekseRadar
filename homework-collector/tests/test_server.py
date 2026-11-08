"""HTTP-сервис collector: ключ, /collect и вход в Teams/itslearning. Сеть подменена через httpx.MockTransport."""
import httpx
import pytest
from fastapi.testclient import TestClient

from homework import server

KEY = "test-key-0123456789abcdef"
H = {"X-Internal-Key": KEY}


@pytest.fixture
def api(monkeypatch):
    monkeypatch.setenv("COLLECTOR_KEY", KEY)
    monkeypatch.setenv("TEAMS_CLIENT_ID", "cid")
    handlers = {}

    def route(req):
        for part, fn in handlers.items():
            if part in str(req.url):
                return fn(req)
        return httpx.Response(404)

    monkeypatch.setattr(server, "make_client", lambda: httpx.Client(transport=httpx.MockTransport(route)))
    with TestClient(server.app) as c:
        c.handlers = handlers
        yield c


def test_health_needs_no_key(api):
    assert api.get("/health").json() == {"ok": True}


@pytest.mark.parametrize("headers", [{}, {"X-Internal-Key": "wrong"}])
def test_key_required(api, headers):
    assert api.post("/collect", json={}, headers=headers).status_code == 401


def test_short_key_refuses_to_start(monkeypatch):
    monkeypatch.setenv("COLLECTOR_KEY", "short")
    with pytest.raises(RuntimeError):
        with TestClient(server.app):
            pass


def test_public_default_key_refuses_to_start(monkeypatch):
    # старое значение по умолчанию из репозитория собирается из частей, целиком в коде его нет
    monkeypatch.setenv("COLLECTOR_KEY", "-".join(["local", "dev", "collector", "key"]))
    with pytest.raises(RuntimeError):
        with TestClient(server.app):
            pass


def test_collect_canvas(api):
    api.handlers["planner/items"] = lambda req: httpx.Response(200, json=[{
        "plannable_id": 7, "plannable_type": "assignment", "context_name": "MAT1100",
        "plannable": {"title": "Oblig 3", "due_at": "2099-01-01T12:00:00Z"}, "submissions": {}}])
    r = api.post("/collect", json={"canvas": {"base_url": "https://c.test", "token": "t"}}, headers=H)
    assert r.status_code == 200
    body = r.json()
    assert body["sources"]["canvas"] == {"ok": True, "count": 1, "error": None, "needs_reconnect": False}
    assert body["assignments"][0]["id"] == "7" and body["assignments"][0]["course"] == "MAT1100"


def test_collect_platform_error_is_still_200(api):
    api.handlers["planner/items"] = lambda req: httpx.Response(401, json={})
    r = api.post("/collect", json={"canvas": {"base_url": "https://c.test", "token": "bad"}}, headers=H)
    assert r.status_code == 200 and r.json()["sources"]["canvas"]["ok"] is False


def test_collect_rejects_bad_body(api):
    assert api.post("/collect", json={"canvas": {"token": "x"}}, headers=H).status_code == 422


def test_teams_start_and_poll(api):
    api.handlers["/devicecode"] = lambda req: httpx.Response(200, json={
        "user_code": "ABCD", "device_code": "dev", "verification_uri": "https://microsoft.com/devicelogin",
        "interval": 5, "expires_in": 900, "message": "go"})
    start = api.post("/login/teams/start", headers=H).json()
    assert start["user_code"] == "ABCD" and start["device_code"] == "dev"

    api.handlers["/token"] = lambda req: httpx.Response(400, json={"error": "authorization_pending"})
    assert api.post("/login/teams/poll", json={"device_code": "dev"}, headers=H).json() == {"pending": True}

    api.handlers["/token"] = lambda req: httpx.Response(200, json={"access_token": "a", "refresh_token": "r", "expires_in": 3600})
    tok = api.post("/login/teams/poll", json={"device_code": "dev"}, headers=H).json()
    assert tok["access_token"] == "a" and tok["client_id"] == "cid"


def test_teams_denied_is_400(api):
    api.handlers["/token"] = lambda req: httpx.Response(400, json={"error": "expired_token", "error_description": "gone"})
    r = api.post("/login/teams/poll", json={"device_code": "dev"}, headers=H)
    assert r.status_code == 400 and "gone" in r.json()["detail"]


def test_itslearning_url_and_exchange(api):
    url = api.get("/login/itslearning/url", params={"base_url": "https://s.itslearning.com"}, headers=H).json()["url"]
    assert url.startswith("https://s.itslearning.com/oauth2/authorize.aspx?")

    api.handlers["/restapi/oauth2/token"] = lambda req: httpx.Response(200, json={"access_token": "a", "refresh_token": "r"})
    r = api.post("/login/itslearning/exchange", headers=H,
                 json={"base_url": "https://s.itslearning.com", "code_or_url": "itsl-itslearning://login?code=XYZ&state=1"})
    assert r.status_code == 200 and r.json()["refresh_token"] == "r"


def test_itslearning_bad_code_needs_reconnect(api):
    api.handlers["/restapi/oauth2/token"] = lambda req: httpx.Response(400, json={"error": "invalid_grant"})
    r = api.post("/login/itslearning/exchange", headers=H, json={"base_url": "https://s.itslearning.com", "code_or_url": "x"})
    assert r.status_code == 400 and r.json()["needs_reconnect"] is True


def test_network_error_is_502(api):
    def boom(req):
        raise httpx.ConnectError("down")
    api.handlers["/devicecode"] = boom
    assert api.post("/login/teams/start", headers=H).status_code == 502
