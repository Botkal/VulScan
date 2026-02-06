# VulScan – Experimental Vulnerability Management System

VulScan is an **experimental backend system** developed as part of a **BSc thesis project**
in Business Informatics.  
The goal of the project is to explore the **feasibility and architecture** of an
AI-assisted IT security risk assessment system based on **software inventory data**
and **public vulnerability sources**.

The project is **under active development**.  
Not all planned features are implemented yet, but the **existing functionality is stable
and fully operational**.

---

## Project status

**Status:** Experimental / Thesis project  
**Stage:** Working prototype  
**Focus:** Architecture, data processing, and risk analysis logic

What this means:
- the system is **not production-ready**
- configuration and structure may change
- missing features are intentionally left for future work
- existing endpoints and workflows **work as designed**

---

## Core features (implemented)

- Import of software inventory data via CSV
- Persistent storage using PostgreSQL
- Database schema management with Flyway
- Download and refresh of the CISA Known Exploited Vulnerabilities (KEV) catalog
- Correlation-ready data model (inventory ↔ vulnerabilities)
- REST API for dashboard and analysis
- Static HTML-based UI for basic visualization
- Fully containerized setup using Docker

---

## Planned / future work

The following features are **planned but not yet implemented**:

- Advanced AI-based risk scoring models
- Automated inventory–vulnerability correlation logic
- Alerting and notification mechanisms
- Authentication and access control
- Extended dashboard visualizations
- Production hardening and security tuning

These topics are discussed in the thesis as **future research and development directions**.

---

## Architecture overview

- **Backend:** Spring Boot (Java 21)
- **Persistence:** PostgreSQL 16
- **Database migrations:** Flyway
- **API style:** REST
- **UI:** Static HTML (served by Spring Boot)
- **Containerization:** Docker, Docker Compose

The application follows a modular layered architecture:
- controller
- service
- repository
- entity / DTO separation

---

## Prerequisites

To run the project locally you need:

- Docker
- Docker Compose (v2)

No local Java or database installation is required.

---

## Running the application

The recommended way to run the project is using Docker Compose.

From the project root directory:

```bash
docker compose up --build


## Docker Compose configuration

services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: VulScanDb
      POSTGRES_USER: vulscan
      POSTGRES_PASSWORD: vulscan
    ports:
      - "5432:5432"
    volumes:
      - vulscan_pg:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U vulscan -d VulScanDb"]
      interval: 5s
      timeout: 3s
      retries: 20

  app:
    image: vulscanapp:1.0
    depends_on:
      postgres:
        condition: service_healthy
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/VulScanDb
      SPRING_DATASOURCE_USERNAME: vulscan
      SPRING_DATASOURCE_PASSWORD: vulscan
      SPRING_FLYWAY_ENABLED: "true"

volumes:
  vulscan_pg:


##Available URLs

After startup, the following endpoints are available:

Dashboard UI:
http://localhost:8080

API documentation:
http://localhost:8080/api.html


##Database migrations

Database schema and base structures are managed with Flyway.

Migration scripts are located in:

src/main/resources/db/migration


Migrations are executed automatically on application startup.

##Inventory import

Software inventory data can be imported via the REST API using a CSV file.

Example:

curl -F "file=@SoftwareInventory.csv" \
  http://localhost:8080/api/inventory/import


The expected CSV format is documented in the API documentation page.

