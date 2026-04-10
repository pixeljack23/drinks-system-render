# Drinks System

Spring Boot backend and Thymeleaf frontend for branch ordering and stock management.

## Database modes

The project now supports two Spring profiles:

- `local`: in-memory H2 database for development
- `supabase`: PostgreSQL connection for a hosted Supabase database

`local` is the default profile.

## Run locally

```powershell
./mvnw.cmd spring-boot:run
```

The app starts on `http://localhost:8081`.

## Run with Supabase

1. Copy `.env.example` values into your shell or hosting provider environment.
2. Set `SPRING_PROFILES_ACTIVE=supabase`.
3. Start the app:

```powershell
./mvnw.cmd spring-boot:run
```

Required environment variables:

- `SUPABASE_DB_URL`
- `SUPABASE_DB_USER`
- `SUPABASE_DB_PASSWORD`

Optional project API variables:

- `SUPABASE_PROJECT_URL`
- `SUPABASE_ANON_KEY`

Optional pool settings:

- `SUPABASE_DB_MAX_POOL_SIZE`
- `SUPABASE_DB_MIN_IDLE`
- `SUPABASE_DB_CONNECTION_TIMEOUT_MS`

## Notes

- Local sample data seeding is enabled only in the `local` profile.
- Supabase seeding is disabled by default to avoid polluting a hosted database.
- The Supabase profile uses Hibernate `ddl-auto=update` for first-pass schema creation. For stricter production migrations, add Flyway later.
- `.env.supabase` can hold your Supabase project URL and anon key, but the backend still needs the database connection values above to connect to Postgres.
- Your current Session Pooler host is `aws-1-eu-central-1.pooler.supabase.com:5432` with database `postgres` and user `postgres.tmiwahcybsklinnymfpr`.
- Use the Session Pooler on IPv4-only networks. The direct Supabase database host may require IPv6.

## Deploy on Render

This repo includes [render.yaml](c:\Users\jacks\OneDrive\Desktop\New folder (2)\drink_systems\render.yaml) for a Java web service deployment.

Render service settings:

- Runtime: `docker`
- Image build: [Dockerfile](c:\Users\jacks\OneDrive\Desktop\New folder (2)\drink_systems\Dockerfile)
- Health check path: `/order`

Required sensitive env vars on Render:

- `SUPABASE_ANON_KEY`
- `SUPABASE_DB_PASSWORD`

Non-sensitive env vars are already declared in `render.yaml`.
