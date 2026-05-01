# FitMate Deployment Guide

This guide deploys FitMate as a Docker Compose stack:

- Spring Boot application
- PostgreSQL 16
- Elasticsearch 8

## 1. Server Requirements

- Linux server with Docker and Docker Compose
- Recommended memory: 4 GB minimum, 6 GB+ preferred
- Open inbound port `80` / `443` if using a domain and reverse proxy
- Open inbound port `8080` only for quick testing, not long-term production

## 2. Prepare Environment

Copy the environment template:

```bash
cp .env.example .env
```

Edit `.env` and change at least:

```bash
POSTGRES_PASSWORD=your-strong-db-password
DB_PASSWORD=your-strong-db-password
ADMIN_PASSWORD=your-strong-admin-password
SESSION_COOKIE_SECURE=false
```

If the site is behind HTTPS, set:

```bash
SESSION_COOKIE_SECURE=true
```

## 3. Build And Start

```bash
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```

Check status:

```bash
docker compose -f docker-compose.prod.yml --env-file .env ps
docker compose -f docker-compose.prod.yml --env-file .env logs -f app
```

Open:

```text
http://SERVER_IP:8080
```

## 4. Default Accounts

- Admin: `admin` / the value configured in `.env` as `ADMIN_PASSWORD`
- User: `123` / `123`

For public deployment, never keep the development password `password123`.

## 5. Data And Indexing

PostgreSQL data is stored in the `postgres_data` Docker volume.

Elasticsearch data is stored in the `elasticsearch_data` Docker volume.

On application startup:

- Flyway applies database migrations.
- Active workout videos are synchronized into Elasticsearch.
- Knowledge base chunks are synchronized into Elasticsearch.

This means the RAG demo remains usable even if the Elasticsearch index is rebuilt.

## 6. Common Operations

Stop:

```bash
docker compose -f docker-compose.prod.yml --env-file .env down
```

Restart app only:

```bash
docker compose -f docker-compose.prod.yml --env-file .env restart app
```

View logs:

```bash
docker compose -f docker-compose.prod.yml --env-file .env logs -f app
```

Backup PostgreSQL:

```bash
docker compose -f docker-compose.prod.yml --env-file .env exec postgres \
  pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB" > fitmate-backup.sql
```

Restore PostgreSQL:

```bash
cat fitmate-backup.sql | docker compose -f docker-compose.prod.yml --env-file .env exec -T postgres \
  psql -U "$POSTGRES_USER" "$POSTGRES_DB"
```

## 7. Domain And HTTPS

Recommended production shape:

```text
User -> HTTPS domain -> Nginx / Caddy reverse proxy -> FitMate app:8080
```

For a real public deployment, use a reverse proxy with TLS instead of directly exposing port `8080`.

Example Nginx location block:

```nginx
location / {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

## 8. Pre-Launch Checklist

- Change `.env` database password.
- Confirm admin login works.
- Confirm normal user cannot access `/admin/**`.
- Confirm `/recommendations` returns videos and knowledge references.
- Confirm `/admin/knowledge` has seeded knowledge documents.
- Confirm `/admin/imports` can import or review videos if needed.
- Use HTTPS before setting `SESSION_COOKIE_SECURE=true`.
