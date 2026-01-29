---
name: android-data-layer
description: Overview for Android Data Layer. Use when asked about repository pattern, data layer, RepositoryImpl, remote API, Retrofit, Room, DAO, local database, or data architecture. For implementation use repository-remote (API + safeApiCall) or repository-room (Room DAO).
---

# Android Data Layer – Overview

The Data Layer is split into **two types**; each has its own skill and rules.

---

## 1. Repository with Remote API

- **Skill**: `repository-remote`
- **When**: Writing **Repository** / **RepositoryImpl** that **calls API** (Retrofit, network, HTTP).
- **Rules**: Interface + Impl, **every API call** wrapped in **safeApiCall**, return `ApiResult<T>`, bind in Hilt.
- **Do not** use safeApiCall for Room/DAO.

→ Details: see skill **repository-remote** (full safeApiCall description, transform/onSuccess examples).

---

## 2. Repository with Room DAO (local)

- **Skill**: `repository-room`
- **When**: Writing **Repository** / **RepositoryImpl** that **reads/writes Room** (DAO, local database).
- **Rules**: Interface + Impl, expose **Flow** for observable data, **suspend** for one-shot, **do not** use safeApiCall.
- **Do not** use safeApiCall for DAO.

→ Details: see skill **repository-room**.

---

## When a repository has both API and Room (offline-first)

- **API**: follow skill **repository-remote** (safeApiCall, ApiResult).
- **Room**: follow skill **repository-room** (Flow, suspend, no safeApiCall).
- One Impl can call both `safeApiCall(apiCall = { ... })` and `dao.xxx()`.

---

## Dependency Injection

- All repositories: bind interface ← impl in **RepositoryModule** with `@Binds`, `@Singleton`, `@InstallIn(SingletonComponent::class)`.
