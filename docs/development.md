# ProjectGuard — Development Guide (Phase 1)

## Service Ports

| Service | Port | Description |
|---------|------|-------------|
| React (Vite) | 5173 | Frontend dev server |
| Spring Boot | 8080 | REST API backend |
| FastAPI | 8000 | ML service |
| MySQL | 3306 | Database server |

## Environment Variables

### Backend (required)

| Variable | Description | Example |
|----------|-------------|---------|
| `DB_USERNAME` | MySQL username | `root` |
| `DB_PASSWORD` | MySQL password | *(your local password)* |

**Windows PowerShell (session):**

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_password"
```

Copy `backend/.env.example` as a reference. Do not commit real credentials.

### Frontend (optional)

| Variable | Description | Default |
|----------|-------------|---------|
| `VITE_API_BASE_URL` | Backend API base URL | `http://localhost:8080` |

Defined in `frontend/.env.development`.

## MySQL Configuration

1. Ensure MySQL 8.x is running (service `MySQL80`).
2. Run `database/01-init-schema.sql` to create the `projectguard` database.
3. Set `DB_USERNAME` and `DB_PASSWORD` before starting the backend.
4. Spring Boot connects via JDBC:

```
jdbc:mysql://localhost:3306/projectguard?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
```

JPA/Hibernate settings (development):

- `ddl-auto: none` — no auto schema changes in Phase 1
- `show-sql: true` — log SQL for debugging

## Frontend Configuration

- Dev server: `npm run dev` (port 5173)
- Vite proxy: requests to `/api/*` are forwarded to `http://localhost:8080`

This allows the React app to call the backend using relative paths during development.

## CORS

The backend allows cross-origin requests from `http://localhost:5173` for `/api/**` endpoints (see `CorsConfig.java`).

For production, update allowed origins accordingly.

## Running Each Service

### 1. MySQL

Start MySQL service and run the init script:

```powershell
# Example if mysql.exe is on PATH:
mysql -u root -p < database/01-init-schema.sql
```

### 2. Backend

```powershell
cd backend
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_password"
.\mvnw.cmd spring-boot:run
```

Verify:

- GET http://localhost:8080/api/health
- GET http://localhost:8080/actuator/health

### 3. Frontend

```powershell
cd frontend
npm install
npm run dev
```

Open http://localhost:5173

### 4. ML Service

```powershell
cd ml-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

Verify: GET http://localhost:8000/health

## Technology Versions (Phase 1)

- Java 21 LTS
- Spring Boot 3.5.16
- React + Vite
- Python + FastAPI
- MySQL 8.x
