FROM python:3.13-slim
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1 PYTHONPATH=/app
WORKDIR /app
RUN addgroup --system app && adduser --system --ingroup app app
COPY requirements.txt .
RUN pip install --no-cache-dir --requirement requirements.txt
COPY alembic.ini ./
COPY migrations ./migrations
COPY app ./app
RUN chown -R app:app /app
USER app
EXPOSE 8080
CMD ["sh", "-c", "alembic upgrade head && python -m app.seed && uvicorn app.main:app --host 0.0.0.0 --port 8080 --proxy-headers"]
