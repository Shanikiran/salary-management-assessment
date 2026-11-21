# syntax=docker/dockerfile:1
# Salary Management — one jar (Angular UI + Spring Boot API + SQLite).
# Multi-stage: build the frontend, copy it into the backend resources,
# then build the jar. No credentials or secrets are baked in.
#
# Build:  docker build -t salary-management .
# Run:    docker run -p 8080:8080 salary-management
#         (ships the seeded demo DB; see DEPLOYMENT.md for persistence)

# --- Stage 1: Angular frontend -------------------------------------------
FROM node:24-alpine AS frontend
WORKDIR /build/frontend/salary-app
COPY frontend/salary-app/package*.json ./
RUN npm ci
COPY frontend/salary-app/ ./
RUN npx ng build
# Angular 20 output layout: dist/salary-app/browser/
RUN mkdir -p /build/backend/src/main/resources/static \
 && cp -r dist/salary-app/browser/. /build/backend/src/main/resources/static/

# --- Stage 2: Spring Boot backend ----------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS backend
WORKDIR /build
COPY backend/ ./backend/
COPY --from=frontend /build/backend/src/main/resources/static/ \
     ./backend/src/main/resources/static/
# Seeded demo database (10,000 employees). Rebuild it any time with:
#   bash scripts/seed.sh --reset   (writes backend/data/salary.db)
COPY backend/data/salary.db ./backend/data/salary.db
WORKDIR /build/backend
RUN mvn -q package -DskipTests

# --- Stage 3: runtime -----------------------------------------------------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=backend /build/backend/target/salary-management-1.0.0.jar app.jar
# Seeded demo DB: the jar does NOT contain it (it lives in backend/data/,
# not in src/main/resources), and the app reads ./data/salary.db by default.
# Without this COPY the deployed app would start with an EMPTY database
# (schema only, 0 employees). The DB is tracked in git via the
# !backend/data/salary.db exception in .gitignore, so it is present in the
# Render build context.
COPY --from=backend /build/backend/data/salary.db /app/data/salary.db
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
