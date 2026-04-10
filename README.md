# Enterprise-Grade Authorization System (RBAC + OAuth2 + Audit)

## 1. Executive Summary

**Project Name:** `auth-ms` (Authorization Microservice)

**Objective:** Build a production-ready, reusable authentication and authorization center that supports Role-Based Access Control (RBAC), OAuth2 JWT tokens, comprehensive audit logging, and enterprise security standards.

**Target Environment:** Cloud-native (Docker/K8s), Stateless, High Availability.

**Business Value:** Any business system (CRM, ERP, Admin Console) can plug into this service without rebuilding permission logic.

## 2. Technology Stack

| Layer | Technology | Version | Production Justification |
| :--- | :--- | :--- | :--- |
| **Framework** | Spring Boot / Spring Security | 3.2.x | Industry standard for enterprise Java |
| **Auth Standard** | OAuth 2.0 + JWT | RFC 6749 | Stateless, Scalable, SSO ready |
| **Persistence** | MySQL + MyBatis-Plus | 8.0 | Dynamic SQL, Optimistic Lock |
| **Cache** | Redis (Cluster mode ready) | 7.x | Token blacklist, Permission cache, Rate limiting |
| **Object Storage** | MinIO (Optional) | Latest | Audit log export storage |
| **Observability** | Prometheus + Grafana + Loki | Latest | Metrics, Logs, Traces |
| **Container** | Docker + K8s (Helm) | Latest | Standardized deployment |
| **API Docs** | SpringDoc OpenAPI 3 | 2.x | Auto-generated, Frontend-ready |

## 3. System Architecture

┌─────────────────────────────────────────────────────────────┐
│ Client (Web/Mobile) │
└───────────────────────────────┬─────────────────────────────┘
│ HTTPS
▼
┌─────────────────────────────────────────────────────────────┐
│ Nginx (Rate Limiting / SSL Term) │
└───────────────────────────────┬─────────────────────────────┘
│
▼
┌─────────────────────────────────────────────────────────────┐
│ Spring Boot Cluster (Pods) │
│ ┌──────────┐ ┌──────────┐ ┌──────────┐ │
│ │ Auth-1 │ │ Auth-2 │ │ Auth-3 │ │
│ └──────────┘ └──────────┘ └──────────┘ │
└───────────────────────────────┬─────────────────────────────┘
│
┌─────────────────┼─────────────────┐
▼ ▼ ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│ Redis Cluster │ │ MySQL Master │ │ Kafka/RabbitMQ│
│ (Session/Cache) │ │ (Write) │ │ (Async Audit) │
│ │ │ │ │ │ │
│ │ │ ▼ │ │ │
│ │ │ MySQL Slave │ │ │
│ │ │ (Read) │ │ │
└─────────────────┘ └─────────────────┘ └─────────────────┘

text

## 4. Database Design (ERD)

### 4.1 Core Tables

| Table | Description | Key Fields |
| :--- | :--- | :--- |
| `user` | System users | `id`, `username`(unique), `password`(bcrypt), `status`, `version` |
| `role` | Roles (Admin/Editor/Viewer) | `id`, `role_code`, `role_name`, `deleted` |
| `permission` | Fine-grained permissions | `id`, `permission_code`(e.g., `user:delete`), `parent_id`(tree) |
| `user_role` | User-Role mapping | `user_id`, `role_id` |
| `role_permission` | Role-Permission mapping | `role_id`, `permission_id` |
| `audit_log` | Immutable audit trail | `user_id`, `operation`, `params`(desensitized), `ip`, `duration_ms` |

### 4.2 Index Strategy

```sql
-- Critical for production performance
CREATE INDEX idx_username ON user(username);  -- Login query
CREATE INDEX idx_user_id ON user_role(user_id);  -- Join queries
CREATE INDEX idx_audit_time ON audit_log(create_time);  -- Log queries
CREATE INDEX idx_audit_user ON audit_log(user_id);