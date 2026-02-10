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

Project has custom extensions for handling `ApiResult<T>` (or `Result<T>`) after a call. Choose based on whether you need to **produce one value** from the result or only **side effects**.

### Imports (project)

| API | Where | Import |
|-----|--------|--------|
| **fold** | Member on `ApiResult` | `import com.thinh.snaplet.utils.network.ApiResult` — then call `result.fold(onSuccess = { ... }, onFailure = { ... })` |
| **onSuccess** | Extension in `NetworkExtensions.kt` | `import com.thinh.snaplet.utils.network.onSuccess` |
| **onFailure** | Extension in `NetworkExtensions.kt` | `import com.thinh.snaplet.utils.network.onFailure` |

- **ApiResult** and **fold**: `app/src/main/java/com/thinh/snaplet/utils/network/ApiResult.kt`
- **onSuccess** / **onFailure**: `app/src/main/java/com/thinh/snaplet/utils/network/NetworkExtensions.kt`

### Rules

- **Use `fold`** only when you need to **produce a single value** from the result and **both success and failure** contribute that same type with different transformations (e.g. success → `UiState.Success(data)`, failure → `UiState.Error(message)`; the outcome is one `UiState`). Use when you assign or return that value (e.g. `_uiState.value = result.fold(...)`).
- **Use `onSuccess` / `onFailure`** when you only need **side effects** (update state differently per branch, show snackbar, set status, emit event). You do **not** use the API result to compute one returned/assigned value; each branch just does its own side effects. Prefer these for "on success do A, on failure do B" with no single derived value.

**Example – fold (produce one value; both branches return same type):**

```kotlin
// You need one UiState from the result; success and failure each transform into that type
_uiState.value = result.fold(
    onSuccess = { data -> UiState.Success(data) },
    onFailure = { error -> UiState.Error(error.message) }
)
```

**Example – onSuccess / onFailure (side effects only; no single derived value):**

```kotlin
// Only side effects: update status, show snackbar. No "result" is assigned from the call.
result
    .onSuccess {
        setDownloadStatus(postId, DownloadStatus.Success)
        emitEvent(HomeUiEvent.ShowSuccess(...))
    }
    .onFailure { e ->
        setDownloadStatus(postId, DownloadStatus.Failed(e.message))
        emitEvent(HomeUiEvent.ShowError(...))
    }
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
