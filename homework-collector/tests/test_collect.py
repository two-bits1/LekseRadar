"""Тесты парсера: живые ответы платформ подменяются через httpx.MockTransport."""
import time

import httpx

from homework import collect
from homework.sources import itslearning

FUTURE = "2099-01-01T12:00:00Z"
PAST = "2000-01-01T12:00:00Z"


def client(handler):
    return httpx.Client(transport=httpx.MockTransport(handler))


def test_demo_covers_all_sources_in_one_format():
    res = collect(demo=True)
    assert set(res.sources) == {"canvas", "teams", "itslearning", "inschool"}
    assert all(a.demo for a in res.assignments)
    assert {a.status for a in res.assignments} <= {"todo", "overdue", "submitted", "graded"}
    assert res.assignments[0].status == "overdue"  # просроченные первыми


def test_canvas_parses_planner_and_follows_pages():
    def h(req):
        assert req.headers["authorization"] == "Bearer tok"
        if "page=2" in str(req.url):
            return httpx.Response(200, json=[{"plannable_id": 2, "plannable_type": "quiz", "context_name": "IT",
                                              "plannable": {"title": "Q", "due_at": PAST}, "submissions": {}}])
        return httpx.Response(200, json=[{"plannable_id": 1, "plannable_type": "assignment", "context_name": "IT",
                                          "plannable": {"title": "A", "due_at": FUTURE}, "html_url": "/courses/1/assignments/1",
                                          "submissions": {"submitted": True}}],
                              headers={"Link": '<https://c.test/api/v1/planner/items?page=2>; rel="next"'})
    res = collect({"canvas": {"base_url": "https://c.test", "token": "tok"}}, client=client(h))
    by_id = {a.id: a for a in res.assignments}
    assert by_id["1"].status == "submitted" and by_id["1"].url == "https://c.test/courses/1/assignments/1"
    assert by_id["2"].status == "overdue" and by_id["2"].kind == "quiz"


def test_teams_refreshes_token_and_names_classes():
    def h(req):
        url = str(req.url)
        if url.endswith("/token"):
            return httpx.Response(200, json={"access_token": "new", "refresh_token": "r2", "expires_in": 3600})
        assert req.headers["authorization"] == "Bearer new"
        if "joinedTeams" in url:
            return httpx.Response(200, json={"value": [{"id": "c1", "displayName": "Engelsk"}]})
        return httpx.Response(200, json={"value": [{"id": "a1", "classId": "c1", "displayName": "Essay",
                                                    "dueDateTime": FUTURE, "submissions": [{"status": "returned"}]}]})
    res = collect({"teams": {"refresh_token": "r1", "client_id": "cid"}}, client=client(h))
    a = res.assignments[0]
    assert (a.course, a.status) == ("Engelsk", "graded")
    assert res.updated_credentials["teams"]["refresh_token"] == "r2"


def test_itslearning_tasks_and_code_exchange():
    def h(req):
        if req.url.path.endswith("/oauth2/token"):
            return httpx.Response(200, json={"access_token": "at", "refresh_token": "rt", "expires_in": 3600})
        done = req.url.params["status"] == "Completed"
        return httpx.Response(200, json={"Total": 1, "EntityArray": [
            {"TaskId": 2 if done else 1, "Title": "T", "LocationTitle": "Norsk", "Deadline": FUTURE}]})
    c = client(h)
    tok = itslearning.exchange_code(c, "https://x.itslearning.com", "itsl-itslearning://login?code=abc&state=s")
    assert tok["access_token"] == "at"
    res = collect({"itslearning": {**tok, "expires_at": int(time.time()) + 999}}, client=c)
    assert {a.id: a.status for a in res.assignments} == {"1": "todo", "2": "submitted"}


def test_inschool_expired_session_asks_reconnect_and_demo_fills_rest():
    res = collect({"inschool": {"host": "s.test", "cookies": {"JSESSIONID": "x"}}}, demo=True,
                  client=client(lambda req: httpx.Response(401)))
    assert res.sources["inschool"].ok is False and res.sources["inschool"].needs_reconnect
    assert res.sources["canvas"].demo and not any(a.source == "inschool" for a in res.assignments)


def test_one_broken_source_does_not_break_others():
    def h(req):
        if "canvas" in req.url.host:
            raise httpx.ConnectError("down")
        return httpx.Response(200, json={"value": []})
    res = collect({"canvas": {"base_url": "https://canvas.test", "token": "t"},
                   "teams": {"access_token": "a", "expires_at": int(time.time()) + 999, "client_id": "c"}},
                  client=client(h))
    assert not res.sources["canvas"].ok and res.sources["teams"].ok
