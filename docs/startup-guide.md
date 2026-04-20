# FitMate Startup Guide

This file is a repeatable checklist for starting the project locally.

## Prerequisites

Before starting the project, make sure these tools are installed:

- `Docker Desktop`
- `Docker Compose`
- `Java 21`
- `Maven 3.9+`

Quick checks:

```bash
docker --version
docker compose version
java -version
mvn -version
```

## Project Directory

Enter the project root first:

```bash
cd "/Users/yu/Documents/New project"
```

## Standard Startup Flow

### 1. Start Docker Desktop

If Docker Desktop is not already open:

```bash
open -a Docker
```

Wait until Docker is fully running, then check:

```bash
docker info
```

## 2. Start PostgreSQL

In the project root:

```bash
docker compose up -d
```

Check status:

```bash
docker compose ps
```

Expected result:

- container `fitmate-postgres` is `Up`
- port `5432` is exposed

## 3. Start Spring Boot

In the project root:

```bash
mvn spring-boot:run
```

When startup succeeds, you should see logs similar to:

```text
Tomcat started on port 8080
Started FitmateApplication
```

## 4. Open the App

Open in browser:

[http://localhost:8080](http://localhost:8080)

Demo accounts:

- `student / password123`
- `admin / password123`
- `stu / 123`

## Fast Startup Version

If everything is already installed, this is the shortest repeatable flow:

```bash
cd "/Users/yu/Documents/New project"
open -a Docker
docker compose up -d
mvn spring-boot:run
```

## How To Confirm Everything Is Working

### Database check

```bash
docker compose ps
```

You want to see:

- `fitmate-postgres` is running

### App check

Open:

[http://localhost:8080/login](http://localhost:8080/login)

If the login page opens, the app is working.

## Stop The Project

### Stop Spring Boot

In the terminal where the app is running:

```bash
Ctrl + C
```

### Stop PostgreSQL container

In the project root:

```bash
docker compose down
```

## Common Problems

### 1. `mvn: command not found`

Maven is not installed correctly.

Install with Homebrew:

```bash
brew install maven
```

### 2. `Cannot connect to Docker daemon`

Docker Desktop is not running.

Start it:

```bash
open -a Docker
```

Then re-check:

```bash
docker info
```

### 3. `Connection to localhost:5432 refused`

PostgreSQL container is not running yet.

Run:

```bash
docker compose up -d
docker compose ps
```

### 4. `Port 5432 is already in use`

Another local PostgreSQL instance is already using port `5432`.

You can either:

- stop the other PostgreSQL service
- or change the port mapping in [docker-compose.yml](/Users/yu/Documents/New%20project/docker-compose.yml)

### 5. `Port 8080 is already in use`

Another app is already using port `8080`.

Find the process:

```bash
lsof -i :8080
```

Then stop that process, or change the Spring Boot port in [application.yml](/Users/yu/Documents/New%20project/src/main/resources/application.yml).

### 6. Browser shows old page

Force refresh the browser:

- macOS: `Cmd + Shift + R`

## Recommended Daily Workflow

For normal development:

1. Start Docker Desktop.
2. Run `docker compose up -d`.
3. Run `mvn spring-boot:run`.
4. Open `http://localhost:8080`.
5. When finished, stop Spring Boot with `Ctrl + C`.
6. If you want to fully stop services, run `docker compose down`.

## Optional Git Sync Before Starting

If you want to update the code before starting:

```bash
cd "/Users/yu/Documents/New project"
git pull
docker compose up -d
mvn spring-boot:run
```
