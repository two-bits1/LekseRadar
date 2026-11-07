"""LekseRadar: сбор домашних заданий с Canvas, Teams, itslearning и Visma InSchool в один формат.

    from homework import collect
    result = collect({"canvas": {"base_url": "...", "token": "..."}})
    result.model_dump(mode="json")   # → JSON для бэкенда
"""
from .collect import collect
from .models import Assignment, CollectResult, Credentials

__all__ = ["collect", "Assignment", "CollectResult", "Credentials"]
