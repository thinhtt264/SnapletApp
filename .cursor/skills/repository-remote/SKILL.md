---
name: repository-remote
description: Implements Repository with remote API only. Use when the user asks to write RepositoryImpl for API, create repository for network calls, Retrofit, HTTP, backend, safeApiCall, ApiResult, fold, onSuccess, onFailure, or add a new API-backed repository. Enforces interface + RepositoryImpl, safeApiCall for all remote calls only (not Room/DAO), Hilt binding. Use fold for returning/transforming values; use onSuccess/onFailure for side effects only.
---

# Repository with Remote API (safeApiCall)

## When to use this skill

- User asks to write **Repository** / **RepositoryImpl** for **API calls** (Retrofit, network, HTTP, backend).
- User asks to add a new repository for a feature that calls the backend.
- **Do not** use for repository that only reads/writes **Room DAO** (use skill `repository-room`).

---

## Required rules

### 1. Always split Interface + Implementation

- **Interface**: name `XxxRepository`, declare suspend functions returning `ApiResult<T>` for every API call.
- **Implementation**: name `XxxRepositoryImpl`, `@Singleton`, inject API service (and other deps if needed), implement interface.

### 2. Wrap every API call with safeApiCall

- Use **safeApiCall** only for **remote API** (Retrofit). Do not use for Room DAO or local logic.
- Import: `com.thinh.snaplet.utils.network.safeApiCall`
- API service must return `Response<BaseResponse<T>>` (project backend format).

### 3. Hilt binding

- In **RepositoryModule**: use `@Binds` abstract fun bindXxxRepository(impl: XxxRepositoryImpl): XxxRepository.
- `@InstallIn(SingletonComponent::class)`, impl is `@Singleton`.

---

## safeApiCall – detailed description

Project has 2 overloads:

### Overload 1: No transform (return API type as-is)

```kotlin
suspend fun <T> safeApiCall(
    apiCall: suspend () -> Response<BaseResponse<T>>,
    onSuccess: suspend (T) -> Unit = {}
): ApiResult<T>
```

- **apiCall**: lambda that calls API (e.g. `{ apiService.getUser(id) }`). Do not catch exceptions inside.
- **onSuccess**: (optional) runs when API is 2xx and has body; use for saving token, updating cache, etc.
- **Return**: `ApiResult.Success(data)` if 2xx and body has data, else `ApiResult.Failure(ApiError)`.

**Example:**

```kotlin
override suspend fun getUserProfile(userName: String): ApiResult<UserProfile> {
    return safeApiCall(
        apiCall = { apiService.getUserProfile(userName) }
    )
}
```

### Overload 2: With transform (map DTO → domain / expose part of response)

```kotlin
suspend fun <T, R> safeApiCall(
    apiCall: suspend () -> Response<BaseResponse<T>>,
    onSuccess: suspend (T) -> Unit = {},
    transform: (T) -> R
): ApiResult<R>
```

- **apiCall**: same as above.
- **onSuccess**: receives **T** (raw API data), use for side-effects (save token, profile, etc.).
- **transform**: map **T → R**; ViewModel receives **R** (e.g. only `User` instead of full `LoginResponse`).
- **Return**: `ApiResult.Success(transform(data))` on success.

**Example (login: API returns TokenResponse + User, expose only User):**

```kotlin
override suspend fun login(email: String, password: String): ApiResult<UserProfile> {
    return safeApiCall(
        apiCall = { apiService.login(body = LoginRequest(email, password)) },
        onSuccess = { result ->
            dataStoreManager.saveTokens(result.token.accessToken, result.token.refreshToken)
            dataStoreManager.saveUserProfile(result.user)
            _authState.value = AuthState.Authenticated
        },
        transform = { response -> response.user }
    )
}
```

### Notes when using safeApiCall

- **Do not** call API directly and try/catch in repository; always wrap in `safeApiCall`.
- **onSuccess** is for side-effects (save local, update state); **transform** is only for changing return type.
- API error (4xx/5xx) or exception → `ApiResult.Failure(ApiError)` (message from backend or default by http code).
- Repository function return type: **always** `ApiResult<T>` (or `ApiResult<R>` if using transform) for every API call.

---

## ApiResult: fold vs onSuccess / onFailure (consuming results)

Project has custom extensions for handling `ApiResult<T>` after a call. Use the right one depending on whether you need a **value/transformation** or **side effects only**.

### Imports (project)

| API | Where | Import |
|-----|--------|--------|
| **fold** | Member on `ApiResult` | `import com.thinh.snaplet.utils.network.ApiResult` — then call `result.fold(onSuccess = { ... }, onFailure = { ... })` |
| **onSuccess** | Extension in `NetworkExtensions.kt` | `import com.thinh.snaplet.utils.network.onSuccess` |
| **onFailure** | Extension in `NetworkExtensions.kt` | `import com.thinh.snaplet.utils.network.onFailure` |

- **ApiResult** and **fold**: `app/src/main/java/com/thinh/snaplet/utils/network/ApiResult.kt`
- **onSuccess** / **onFailure**: `app/src/main/java/com/thinh/snaplet/utils/network/NetworkExtensions.kt`

### Rules

- **Use `fold`** when the logic must **return or transform a value** and both success and failure must be handled explicitly (decision-making). Example: mapping to UI state, computing a single value from both branches.
- **Use `onSuccess` / `onFailure`** when performing **side effects only** (UI updates, logging, analytics, caching) and **no value is returned or transformed**. They return the same `ApiResult<T>` for chaining.
- **Never use `onSuccess` / `onFailure` to build or return state**; prefer **`fold`** for compiler-safe branching (exhaustive handling of both cases).

**Example – fold (return/transform):**

```kotlin
// ViewModel: build state from result
_uiState.value = result.fold(
    onSuccess = { data -> UiState.Success(data) },
    onFailure = { error -> UiState.Error(error.message) }
)
```

**Example – onSuccess / onFailure (side effects only):**

```kotlin
// Log or show snackbar, then pass result along
result
    .onSuccess { log("Loaded") }
    .onFailure { showSnackbar(it.message) }
// result unchanged; use fold elsewhere if you need to derive state
```

---

## Suggested file structure

```
data/repository/
  XxxRepository.kt          # interface
  XxxRepositoryImpl.kt      # class, @Singleton, safeApiCall per API
di/
  RepositoryModule.kt      # @Binds XxxRepositoryImpl -> XxxRepository
```

---

## Full example (remote only)

**XxxRepository.kt**

```kotlin
interface XxxRepository {
    suspend fun fetchItem(id: String): ApiResult<Item>
    suspend fun submitItem(item: Item): ApiResult<Unit>
}
```

**XxxRepositoryImpl.kt**

```kotlin
@Singleton
class XxxRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : XxxRepository {

    override suspend fun fetchItem(id: String): ApiResult<Item> {
        return safeApiCall(apiCall = { apiService.getItem(id) })
    }

    override suspend fun submitItem(item: Item): ApiResult<Unit> {
        return safeApiCall(
            apiCall = { apiService.postItem(item) },
            onSuccess = { /* optional: cache, analytics */ },
            transform = { }
        )
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
- [ ] Every API call in Impl is wrapped in `safeApiCall` (no direct API call).
- [ ] Repository functions return `ApiResult<T>` for API calls.
- [ ] Impl → Interface bound in `RepositoryModule`.
- [ ] Do not use safeApiCall for Room/DAO (use skill `repository-room` for local DB).
- [ ] When consuming `ApiResult`: use **fold** to return/transform state; use **onSuccess**/**onFailure** only for side effects (no state built from them).
