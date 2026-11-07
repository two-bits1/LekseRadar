"""Google Classroom на httpx.MockTransport: курсы → задания → сдачи → наш формат."""
from datetime import timedelta

import httpx

from homework.collect import collect
from homework.models import utcnow

SOON = utcnow() + timedelta(days=3)
OLD = utcnow() - timedelta(days=200)


def handler(request: httpx.Request) -> httpx.Response:
    assert request.headers["Authorization"] == "Bearer ya29.test"
    path = request.url.path
    if path == "/v1/courses":
        assert request.url.params["studentId"] == "me" and request.url.params["courseStates"] == "ACTIVE"
        return httpx.Response(200, json={"courses": [{"id": "c1", "name": "Бази даних"}, {"id": "c2", "name": "Закритий курс"}]})
    if path == "/v1/courses/c1/courseWork":
        if "pageToken" not in request.url.params:
            return httpx.Response(200, json={"nextPageToken": "p2", "courseWork": [
                {"id": "w1", "title": "Лаба 1", "workType": "ASSIGNMENT", "alternateLink": "https://classroom.google.com/w1",
                 "description": "SQL JOIN", "dueDate": {"year": SOON.year, "month": SOON.month, "day": SOON.day},
                 "dueTime": {"hours": 21}},
                {"id": "w2", "title": "Тест", "workType": "MULTIPLE_CHOICE_QUESTION",
                 "dueDate": {"year": SOON.year, "month": SOON.month, "day": SOON.day}}]})
        return httpx.Response(200, json={"courseWork": [
            {"id": "w3", "title": "Старе", "dueDate": {"year": OLD.year, "month": OLD.month, "day": OLD.day}},
            {"id": "w4", "title": "Без строку", "updateTime": utcnow().isoformat()}]})
    if path == "/v1/courses/c1/courseWork/-/studentSubmissions":
        return httpx.Response(200, json={"studentSubmissions": [
            {"courseWorkId": "w1", "state": "TURNED_IN"},
            {"courseWorkId": "w2", "state": "RETURNED", "assignedGrade": 9}]})
    if path.startswith("/v1/courses/c2/"):
        return httpx.Response(403, json={"error": {"status": "PERMISSION_DENIED"}})
    return httpx.Response(404)


def test_classroom_assignments_and_statuses():
    with httpx.Client(transport=httpx.MockTransport(handler)) as client:
        res = collect({"classroom": {"access_token": "ya29.test", "refresh_token": "1//r", "expires_at": 1}}, client=client)
    assert res.sources["classroom"].ok, res.sources["classroom"].error
    by_id = {a.id: a for a in res.assignments}
    assert set(by_id) == {"w1", "w2", "w4"}            # старое (200 дней назад) отброшено, закрытый курс пропущен
    assert by_id["w1"].status == "submitted" and by_id["w1"].due.hour == 21 and by_id["w1"].course == "Бази даних"
    assert by_id["w2"].status == "graded" and by_id["w2"].kind == "quiz" and by_id["w2"].due.hour == 23
    assert by_id["w4"].status == "todo" and by_id["w4"].due is None
    assert all(a.source == "classroom" for a in res.assignments)


def test_expired_token_asks_to_reconnect():
    with httpx.Client(transport=httpx.MockTransport(lambda r: httpx.Response(401))) as client:
        res = collect({"classroom": {"access_token": "old"}}, client=client)
    assert not res.sources["classroom"].ok and res.sources["classroom"].needs_reconnect


def test_non_json_reply_gives_clear_error():
    with httpx.Client(transport=httpx.MockTransport(lambda r: httpx.Response(200, text="<html>"))) as client:
        res = collect({"classroom": {"access_token": "ya29.test"}}, client=client)
    assert not res.sources["classroom"].ok and "Classroom" in res.sources["classroom"].error
