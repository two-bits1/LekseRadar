# collector: HTTP-сервис сбора ДЗ для бэкенда (COLLECTOR-API.md). Наружу не публиковать.
FROM python:3.12-slim
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1 PIP_DISABLE_PIP_VERSION_CHECK=1
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY homework ./homework
RUN useradd --create-home collector
USER collector
EXPOSE 8090
HEALTHCHECK --interval=30s --timeout=3s CMD python -c "import os, urllib.request; urllib.request.urlopen('http://localhost:%s/health' % os.environ.get('PORT', '8090'))"
# PORT задаёт Railway, локально 8090. На Railway задайте HOST=:: : приватная сеть там по IPv6. Access-log выключен: в теле токены, в URL — нет, но лишнего не пишем.
CMD ["sh", "-c", "uvicorn homework.server:app --host ${HOST:-0.0.0.0} --port ${PORT:-8090} --no-access-log"]
