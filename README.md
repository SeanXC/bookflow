# BookFlow

BookFlow is a multi-tenant appointment and business management SaaS for small service businesses.

## Tech Stack

- Java 21 and Spring Boot 4.0.8
- PostgreSQL 16, Flyway, and Spring Data JPA
- Spring Security with JWT authentication and role-based access control
- React 18, JavaScript, and Vite
- Docker Compose
- JUnit, Mockito, and Testcontainers

## Project Structure

- `backend/` — Spring Boot REST API
- `frontend/` — React + JavaScript web application

## Current Status

- Backend MVP complete with tenant isolation, scheduling conflict detection,
  management APIs, dashboard analytics, OpenAPI documentation, and automated tests.
- Frontend development in progress.

## Local Development

Start PostgreSQL:

```bash
docker compose up -d postgres
```

Run the backend:

```bash
cd backend
export JWT_SECRET="$(openssl rand -hex 32)"
./mvnw spring-boot:run
```

Run the frontend:

```bash
cd frontend
npm install
npm run dev
```

The frontend runs at `http://localhost:5173`; Swagger UI is available at
`http://localhost:8080/api/docs`.