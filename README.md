# ProjectGuard

Intelligent Project Planning & Risk Analysis — Final Year Project

## Project Structure

```
ProjectGuard/
├── frontend/      # React + Vite (port 5173)
├── backend/       # Spring Boot REST API (port 8080)
├── ml-service/    # Python FastAPI (port 8000)
├── database/      # MySQL initialization scripts
└── docs/          # Development documentation
```

## Prerequisites

- Java 21 LTS
- Node.js 18+
- Python 3.11+
- MySQL 8.x
- Git

## Quick Start

See [docs/development.md](docs/development.md) for detailed setup, ports, environment variables, and CORS configuration.

### Frontend

```powershell
cd frontend
npm install
npm run dev
```

### Backend

Set database credentials, then:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

### ML Service

```powershell
cd ml-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

### Database

Run `database/01-init-schema.sql` against your MySQL server to create the `projectguard` database.

## Phase 1 Status

Phase 1 establishes the project foundation only. No business features (authentication, risk analysis, dashboards, etc.) are implemented yet.

## Health Endpoints

| Service  | URL |
|----------|-----|
| Frontend | http://localhost:5173 |
| Backend  | http://localhost:8080/api/health |
| Backend Actuator | http://localhost:8080/actuator/health |
| ML Service | http://localhost:8000/health |
