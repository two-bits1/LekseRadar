"""collector: внутренний HTTP-сервис для бэкенда. Контракт: COLLECTOR-API.md.

    COLLECTOR_KEY=… uvicorn homework.server:app --port 8090

Сервис ничего не хранит. Тела запросов (там токены) не логируются.
"""
from __future__ import annotations

import os
import secrets
from contextlib import asynccontextmanager
from typing import Optional

import httpx
from fastapi import Depends, FastAPI, Header, HTTPException, Request
from fastapi.responses import JSONResponse
from pydantic import BaseModel

from .collect import collect
from .models import Credentials, SourceError, TeamsCreds
from .sources import itslearning, teams


def make_client() -> httpx.Client:
    """Отдельная функция, чтобы тесты подменяли сеть на httpx.MockTransport."""
    return httpx.Client(timeout=30, follow_redirects=False)


@asynccontextmanager
async def lifespan(app: FastAPI):
    key = os.environ.get("COLLECTOR_KEY", "")
    if len(key) < 16:
        raise RuntimeError("COLLECTOR_KEY не задан или короче 16 символов (openssl rand -hex 32)")
    app.state.key = key
    yield


app = FastAPI(title="LekseRadar collector", version="1.0", lifespan=lifespan,
              docs_url=None, redoc_url=None, openapi_url=None)


def internal_key(request: Request, x_internal_key: Optional[str] = Header(None)):
    if not x_internal_key or not secrets.compare_digest(x_internal_key, request.app.state.key):
        raise HTTPException(401, "invalid internal key")


@app.exception_handler(SourceError)
def source_error(_: Request, e: SourceError):
    return JSONResponse({"detail": str(e), "needs_reconnect": e.needs_reconnect}, status_code=400)


@app.exception_handler(httpx.HTTPError)
def network_error(_: Request, e: httpx.HTTPError):
    return JSONResponse({"detail": f"платформа недоступна: {type(e).__name__}"}, status_code=502)


@app.get("/health")
def health():
    return {"ok": True}


@app.post("/collect", dependencies=[Depends(internal_key)])
def collect_endpoint(creds: Credentials):
    with make_client() as client:
        return collect(creds, client=client).model_dump(mode="json")


class TeamsStart(BaseModel):
    client_id: Optional[str] = None


class TeamsPoll(BaseModel):
    device_code: str
    client_id: Optional[str] = None


class ItslearningExchange(BaseModel):
    base_url: str
    code_or_url: str


@app.post("/login/teams/start", dependencies=[Depends(internal_key)])
def teams_start(body: Optional[TeamsStart] = None):
    cid = teams.client_id(TeamsCreds(client_id=body.client_id if body else None))
    with make_client() as client:
        d = teams.start_device_login(client, cid)
    return {k: d.get(k) for k in ("user_code", "verification_uri", "device_code", "interval", "expires_in", "message")}


@app.post("/login/teams/poll", dependencies=[Depends(internal_key)])
def teams_poll(body: TeamsPoll):
    cid = teams.client_id(TeamsCreds(client_id=body.client_id))
    with make_client() as client:
        tok = teams.poll_device_login(client, cid, body.device_code)
    return tok if tok is not None else {"pending": True}


@app.get("/login/itslearning/url", dependencies=[Depends(internal_key)])
def itslearning_url(base_url: str):
    return {"url": itslearning.authorize_url(base_url)}


@app.post("/login/itslearning/exchange", dependencies=[Depends(internal_key)])
def itslearning_exchange(body: ItslearningExchange):
    with make_client() as client:
        return itslearning.exchange_code(client, body.base_url, body.code_or_url)
