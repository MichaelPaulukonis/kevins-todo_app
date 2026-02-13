# Docker Setup Guide

This directory contains Docker configuration files to run the Todo application with a PostgreSQL database backend.

## Quick Start

### Prerequisites
- Docker and Docker Compose installed on your system
- For macOS: Docker Desktop should be running

### Running with Docker Compose

1. **Start both services:**
   ```bash
   docker-compose up --build
   ```

   - `--build` rebuilds the application image from your source code
   - Remove `--build` on subsequent runs if you haven't changed the code

2. **Run in the background:**
   ```bash
   docker-compose up -d
   ```

3. **View logs:**
   ```bash
   docker-compose logs -f app
   docker-compose logs -f postgres
   ```

4. **Stop the services:**
   ```bash
   docker-compose down
   ```

## Configuration

### Environment Variables

Edit the following values in `docker-compose.yml` or create a `.env` file based on `.env.example`:

- `DB_NAME`: PostgreSQL database name (default: `todo_db`)
- `DB_USER`: PostgreSQL user (default: `postgres`)
- `DB_PASSWORD`: PostgreSQL password (default: `postgres`)
- `DB_PORT`: PostgreSQL port (default: `5432`)

### Custom Configuration

Create a `.env` file in the project root:
```bash
cp .env.example .env
# Edit .env with your desired values
docker-compose up
```

## Services

### PostgreSQL (postgres)
- **Image**: postgres:16-alpine
- **Port**: 5432 (mapped from container)
- **Data Storage**: Persisted in `postgres_data` volume
- **Connection String**: `jdbc:postgresql://postgres:5432/todo_db`

### Application (app)
- **Built from**: Dockerfile (multi-stage build)
- **Port**: 8080
- **Java Version**: OpenJDK 17
- **Build Tool**: Gradle

## Building Manually

### Build the Docker image only:
```bash
docker build -t todo-app:latest .
```

### Run the image standalone:
```bash
docker run -p 8080:8080 \
  -e DB_HOST=localhost \
  -e DB_PORT=5432 \
  -e DB_NAME=todo_db \
  -e DB_USER=postgres \
  -e DB_PASSWORD=postgres \
  todo-app:latest
```

## Database Initialization

To initialize the database with a custom SQL script:

1. Create an `init.sql` file in the project root
2. Uncomment this line in `docker-compose.yml`:
   ```yaml
   - ./init.sql:/docker-entrypoint-initdb.d/init.sql
   ```
3. Restart Docker Compose

## Connecting to PostgreSQL

From your host machine:
```bash
psql -h localhost -U postgres -d todo_db
```

From inside the app container:
```bash
docker exec -it todo_postgres psql -U postgres -d todo_db
```

## Troubleshooting

### PostgreSQL Connection Refused
- Ensure postgres service is healthy: `docker-compose ps`
- Check logs: `docker-compose logs postgres`
- Wait a few seconds after starting (see `depends_on.condition`)

### Port Already in Use
Change the port mapping in `docker-compose.yml`:
```yaml
ports:
  - "5433:5432"  # Maps container port 5432 to host port 5433
```

### Rebuild Without Cache
```bash
docker-compose build --no-cache
```

### Clean Up Everything
```bash
docker-compose down -v  # -v removes volumes
```

## Next Steps

1. **Update Dockerfile**: Modify the `HEALTHCHECK` endpoint based on your actual application endpoints
2. **Configure Database Connection**: Update your application's Hibernate/database configuration to use environment variables
3. **Add Database Scripts**: Create `init.sql` with schema and initial data
4. **Production Configuration**: Add production-specific settings in environment variables or a separate compose file

## Multi-Stage Build Benefits

The Dockerfile uses a multi-stage build to:
- Keep the final image small (only needs Java runtime, not Gradle)
- Ensure consistent builds across environments
- Reduce build layer caching issues

## Notes for Your Partner

When setting up PostgreSQL integration with Hibernate:
1. Add PostgreSQL JDBC driver to `build.gradle`
2. Configure Hibernate properties in `hibernate.cfg.xml` or application properties
3. Use the `DB_HOST`, `DB_USER`, `DB_PASSWORD` environment variables
4. The database will be automatically created if it doesn't exist (handled by the docker-compose setup)


