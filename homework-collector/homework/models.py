"""Общий формат задания и тела запросов. Все платформы приводятся к Assignment."""
from __future__ import annotations

from datetime import datetime, timezone
from typing import Literal, Optional

from pydantic import BaseModel, Field

Source = Literal["canvas", "teams", "itslearning", "inschool", "classroom"]
Status = Literal["todo", "overdue", "submitted", "graded"]


class Assignment(BaseModel):
    source: Source
    id: str = Field(description="id задания в исходной платформе")
    title: str
    course: str
    due: Optional[datetime] = Field(None, description="дедлайн, UTC")
    status: Status
    url: Optional[str] = None
    description: str = ""
    kind: str = "assignment"  # assignment | quiz | exam | discussion | task


# ---------- учётные данные, которые бэкенд хранит у себя и передаёт в collect() ----------

class CanvasCreds(BaseModel):
    base_url: str = Field(examples=["https://canvas.instructure.com"])
    token: str = Field(description="личный токен: Account → Settings → New Access Token")


class OAuthCreds(BaseModel):
    access_token: Optional[str] = None
    refresh_token: Optional[str] = None
    expires_at: Optional[int] = Field(None, description="unix-время, когда access_token истечёт")


class TeamsCreds(OAuthCreds):
    client_id: Optional[str] = Field(None, description="Entra app id; по умолчанию из TEAMS_CLIENT_ID")


class ItslearningCreds(OAuthCreds):
    base_url: str = Field(examples=["https://sms.itslearning.com"])


class ClassroomCreds(OAuthCreds):
    """Google Classroom: access_token получает и обновляет бэкенд (у него секрет клиента Google)."""


class InschoolCreds(BaseModel):
    host: str = Field(examples=["ohg-vgs.inschool.visma.no"])
    cookies: dict[str, str] = Field(description="cookie сессии InSchool (JSESSIONID, Authorization и др.)")


class Credentials(BaseModel):
    canvas: Optional[CanvasCreds] = None
    teams: Optional[TeamsCreds] = None
    itslearning: Optional[ItslearningCreds] = None
    inschool: Optional[InschoolCreds] = None
    classroom: Optional[ClassroomCreds] = None


class SourceResult(BaseModel):
    ok: bool
    count: int = 0
    error: Optional[str] = None
    needs_reconnect: bool = False


class CollectResult(BaseModel):
    generated_at: datetime
    assignments: list[Assignment]
    sources: dict[str, SourceResult]
    updated_credentials: dict[str, dict] = Field(
        default_factory=dict,
        description="новые токены/cookie после обновления: бэкенд должен сохранить их вместо старых")


class SourceError(Exception):
    """Ошибка источника с понятным текстом. needs_reconnect=True — пользователю нужно войти заново."""

    def __init__(self, message: str, needs_reconnect: bool = False):
        super().__init__(message)
        self.needs_reconnect = needs_reconnect


def utcnow() -> datetime:
    return datetime.now(timezone.utc)


def parse_dt(value) -> Optional[datetime]:
    if not value:
        return None
    try:
        t = datetime.fromisoformat(str(value).replace("Z", "+00:00"))
    except ValueError:
        return None
    return t if t.tzinfo else t.replace(tzinfo=timezone.utc)


def open_status(due: Optional[datetime]) -> Status:
    """Статус несданного задания: просрочено, если дедлайн прошёл."""
    return "overdue" if due and due < utcnow() else "todo"
