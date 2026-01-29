---
name: repository-room
description: Implements Repository with Room DAO only (local database). Use when the user asks to write RepositoryImpl for database, Room, DAO, local persistence, Entity, Flow, or add a new DAO-backed repository. Enforces interface + RepositoryImpl, Flow for observable data, suspend for one-shot, no safeApiCall (safeApiCall is for remote API only).
---

# Repository with Room DAO (local)

## When to use this skill

- User asks to write **Repository** / **RepositoryImpl** for **Room** (DAO, local database, persistence).
- User asks to add a new repository for a feature that reads/writes the database.
- **Do not** use for repository that only calls **remote API** (use skill `repository-remote` with safeApiCall).

---

## Required rules

### 1. Always split Interface + Implementation

- **Interface**: name `XxxRepository`, declare:
  - **Observable data**: `Flow<T>` or `Flow<List<T>>` (from DAO).
  - **One-shot**: `suspend fun` returning plain type (T?, Unit, List<T>, etc.); do not wrap in `ApiResult` for DAO.
- **Implementation**: name `XxxRepositoryImpl`, `@Singleton`, inject DAO (and other deps if needed), implement interface.

### 2. Do not use safeApiCall for Room

- **safeApiCall** is for **remote API** (Retrofit) only. For Room:
  - Call DAO directly in `suspend` or collect from Flow.
  - Room exceptions (if any) can be let crash or wrapped in try/catch per app policy; do not use `safeApiCall`.

### 3. Hilt binding

- In **RepositoryModule**: use `@Binds` abstract fun bindXxxRepository(impl: XxxRepositoryImpl): XxxRepository.
- `@InstallIn(SingletonComponent::class)`, impl is `@Singleton`.

---

## Pattern with Room

### Observable data (from DAO)

- DAO returns `Flow<List<Entity>>` or `Flow<Entity?>`.
- Repository exposes that Flow from DAO (or map to domain model if needed).

```kotlin
// Interface
fun getItemsStream(): Flow<List<Item>>

// Impl
override fun getItemsStream(): Flow<List<Item>> = dao.getAllItems()
```

### One-shot (single read/write)

- Repository calls DAO `suspend` directly; return plain type (T?, Unit, List<T>).

```kotlin
// Interface
suspend fun getItemById(id: String): Item?
suspend fun insertItem(item: Item)
suspend fun deleteItem(id: String)

// Impl
override suspend fun getItemById(id: String): Item? = dao.getById(id)
override suspend fun insertItem(item: Item) { dao.insert(item) }
override suspend fun deleteItem(id: String) { dao.deleteById(id) }
```

### Combining Remote + Room (offline-first)

- If repository has both API and Room:
  - **API**: follow skill **repository-remote** (safeApiCall, ApiResult).
  - **Room**: follow this skill: Flow / suspend, no safeApiCall.
  - One Impl can call both `safeApiCall(apiCall = { ... })` and `dao.xxx()`.

---

## Suggested file structure

```
data/repository/
  XxxRepository.kt          # interface (Flow + suspend)
  XxxRepositoryImpl.kt      # class, @Singleton, inject Dao
data/local/
  dao/
    XxxDao.kt
  entity/
    XxxEntity.kt
di/
  RepositoryModule.kt       # @Binds XxxRepositoryImpl -> XxxRepository
```

---

## Full example (Room only)

**XxxRepository.kt**

```kotlin
interface XxxRepository {
    fun getItemsStream(): Flow<List<Item>>
    suspend fun getItemById(id: String): Item?
    suspend fun insertItem(item: Item)
    suspend fun deleteItem(id: String)
}
```

**XxxRepositoryImpl.kt**

```kotlin
@Singleton
class XxxRepositoryImpl @Inject constructor(
    private val xxxDao: XxxDao
) : XxxRepository {

    override fun getItemsStream(): Flow<List<Item>> = xxxDao.getAllItems()

    override suspend fun getItemById(id: String): Item? = xxxDao.getById(id)

    override suspend fun insertItem(item: Item) {
        xxxDao.insert(item)
    }

    override suspend fun deleteItem(id: String) {
        xxxDao.deleteById(id)
    }
}
```

**RepositoryModule.kt**

```kotlin
@Binds
@Singleton
abstract fun bindXxxRepository(impl: XxxRepositoryImpl): XxxRepository
```

---

## Checklist

- [ ] Interface `XxxRepository` and class `XxxRepositoryImpl` exist.
- [ ] Observable data: expose `Flow<T>` from DAO, do not wrap in ApiResult.
- [ ] One-shot: `suspend fun` calls DAO directly, no safeApiCall.
- [ ] Impl → Interface bound in `RepositoryModule`.
- [ ] If repository has both API and Room: API uses safeApiCall (repository-remote), DAO uses this pattern (repository-room).
