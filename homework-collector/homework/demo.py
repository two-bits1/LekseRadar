"""Демо-данные: «сырые» ответы каждой платформы в их настоящем формате.

Они проходят через те же normalize(), что и живые данные, поэтому демо показывает
и сам парсер. Даты считаются от текущего момента, чтобы дедлайны всегда были свежими.
"""
from __future__ import annotations

from datetime import datetime, timedelta, timezone

from .models import Assignment
from .sources import canvas, inschool, itslearning, teams


def _at(days: int, hour: int = 21) -> datetime:
    d = datetime.now(timezone.utc) + timedelta(days=days)
    return d.replace(hour=hour, minute=59, second=0, microsecond=0)


def _z(days: int, hour: int = 21) -> str:
    return _at(days, hour).isoformat().replace("+00:00", "Z")


def canvas_raw() -> list[dict]:
    def item(pid, typ, title, course, days, sub):
        return {"plannable_id": pid, "plannable_type": typ, "context_name": course,
                "plannable": {"title": title, "due_at": _z(days)}, "plannable_date": _z(days),
                "html_url": f"/courses/101/{typ}s/{pid}", "submissions": sub}
    return [
        item(5001, "assignment", "Lab 3: Python-løkker og lister", "Informasjonsteknologi 1", 2, {"submitted": False}),
        item(5002, "quiz", "Quiz: HTML og CSS", "Informasjonsteknologi 1", 5, {"submitted": False}),
        item(5003, "discussion_topic", "Diskusjon: Personvern i sosiale medier", "Samfunnskunnskap", 1, {"submitted": True}),
        item(5004, "assignment", "Essay: Demokrati i Norge", "Samfunnskunnskap", -2, {"submitted": False, "missing": True}),
        item(5005, "assignment", "Prosjekt: Lag en nettside om deg selv", "Informasjonsteknologi 1", -6,
             {"submitted": True, "graded": True}),
    ]


def teams_raw() -> tuple[list[dict], dict[str, str]]:
    classes = {"c-eng": "Engelsk 1FKB", "c-mat": "Matematikk 1P"}

    def a(aid, cls, title, days, sub, text):
        return {"id": aid, "classId": cls, "displayName": title, "dueDateTime": _z(days, 22),
                "webUrl": f"https://teams.microsoft.com/l/entity/66aeee93-507d-479a-a3ef-8f494af43945/classroom?a={aid}",
                "instructions": {"content": text, "contentType": "text"}, "submissions": [{"status": sub}]}
    return [
        a("t-1", "c-eng", "Write a short story (300 words)", 3, "working", "Theme: a day without internet."),
        a("t-2", "c-mat", "Oppgaver 2.10–2.25: Lineære funksjoner", 1, "working", "Vis utregning."),
        a("t-3", "c-eng", "Vocabulary test chapter 4", 7, "working", "Learn the words on page 112."),
        a("t-4", "c-mat", "Innlevering: Prosentregning", -3, "returned", "Ferdig vurdert."),
    ], classes


def itslearning_raw() -> list[dict]:
    def t(tid, title, course, days, done, typ="Assignment"):
        return {"TaskId": tid, "Title": title, "LocationTitle": course, "Deadline": _z(days, 22),
                "Status": "Completed" if done else "NotStarted", "ElementType": typ, "Homework": True,
                "Url": f"https://sms.itslearning.com/LearningToolElement/ViewLearningToolElement.aspx?LearningToolElementId={tid}",
                "Description": "", "_completed": done}
    return [
        t(9101, "Norsk: Skriv en kronikk om skjermbruk", "Norsk skriftlig 1FKB", 4, False),
        t(9102, "Naturfag: Labrapport fotosyntese", "Naturfag 1FKB", 2, False),
        t(9103, "Les kapittel 3 i Gyldendal Naturfag", "Naturfag 1FKB", 0, False, "Page"),
        t(9104, "Norsk: Analyse av novelle", "Norsk skriftlig 1FKB", -5, True),
    ]


def inschool_raw() -> list[dict]:
    def i(eid, label, subject, days, typ):
        return {"entityId": eid, "label": label, "subject": subject, "type": typ, "startTime": "08:30",
                "date": _at(days).strftime("%d/%m/%Y"), "extraInfo": None}
    return [
        i(7001, "Heldagsprøve matematikk", "Matematikk, forb min.spr", 9, "EXAM"),
        i(7002, "Muntlig vurdering engelsk", "Engelsk skriftlig,forb min.spr", 6, "ASSESSMENT"),
        i(7003, "Prøve: Celler og arv", "Naturfag, forb min.spr", 12, "EXAM"),
    ]


def demo(source: str) -> list[Assignment]:
    if source == "canvas":
        items = canvas.normalize(canvas_raw(), "https://canvas.instructure.com")
    elif source == "teams":
        items = teams.normalize(*teams_raw())
    elif source == "itslearning":
        items = itslearning.normalize(itslearning_raw())
    elif source == "inschool":
        items = inschool.normalize(inschool_raw(), "https://demo.inschool.visma.no")
    else:
        raise ValueError(source)
    return [a.model_copy(update={"demo": True}) for a in items]
