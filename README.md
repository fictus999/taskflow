# 🚀 TaskFlow — Full Stack Task Management Platform

![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-green?style=for-the-badge&logo=springboot)
![React](https://img.shields.io/badge/React-18-blue?style=for-the-badge&logo=react)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?style=for-the-badge&logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Ready-blue?style=for-the-badge&logo=docker)
![JWT](https://img.shields.io/badge/JWT-Auth-purple?style=for-the-badge)

> A production-grade, full-stack task management platform built with Spring Boot, React, and PostgreSQL. Features real-time updates, JWT authentication, role-based access control, and a polished responsive UI.

## Features
- ✅ JWT Authentication — Secure login/register with refresh tokens
- ✅ Role-Based Access Control — ADMIN, MANAGER, USER roles
- ✅ Real-time Updates — WebSocket integration for live task updates
- ✅ Full CRUD — Tasks, Projects, Comments, File Attachments
- ✅ Pagination & Filtering — Server-side sorting, searching, filtering
- ✅ Docker Compose — One-command local deployment
- ✅ CI/CD Pipeline — GitHub Actions for automated testing & deployment

## Tech Stack
| Layer | Technology |
|-------|-----------|
| **Backend** | Java 17, Spring Boot 3.2, Spring Security 6 |
| **ORM** | Spring Data JPA, Hibernate, Flyway |
| **Frontend** | React 18, Vite, TanStack Query |
| **Styling** | Tailwind CSS, Framer Motion |
| **Database** | PostgreSQL 15 |
| **Cache** | Redis 7 |
| **Auth** | JWT (JJWT library) |
| **Testing** | JUnit 5, Mockito, Testcontainers |
| **CI/CD** | GitHub Actions |
| **Containers** | Docker, Docker Compose |

## Project Structure
```
taskflow/
├── backend/
│   ├── src/main/java/com/taskflow/
│   │   ├── controller/
│   │   ├── model/
│   │   ├── repository/
│   │   ├── security/
│   │   └── service/
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/
├── frontend/
└── docker-compose.yml
```

## Quick Start
```bash
git clone https://github.com/YOUR_USERNAME/taskflow.git
cd taskflow
docker-compose up --build
```
App runs at: http://localhost:3000

API docs at: http://localhost:8080/swagger-ui.html
