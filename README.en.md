# MultiBaseUrls

[简体中文](README.md) | [English](README.en.md) | [日本語](README.ja.md)

A lightweight baseUrl-switching library for Retrofit. An OkHttp interceptor reads annotations on interface methods and routes each method of a **single Retrofit instance** to a different baseUrl — zero intrusion, and interfaces without annotations are completely unaffected.

```kotlin
// Register environments once
MultiBaseUrls.setup {
    Production with "https://api.example.com/v2/"
    Testing with "https://test.example.com/"
}

// Annotate interface methods as needed
interface ApiService {
    @GET("posts")
    suspend fun getPosts(): List<Post>          // untagged → original baseUrl

    @BaseUrl(Production::class)
    @GET("user")
    suspend fun getUser(): User                 // → https://api.example.com/v2/user
}
```

## Features

- **Annotation-driven**: `@BaseUrl` on a method or interface class; method-level wins over class-level
- **One Retrofit instance**: different methods naturally hit different environments — no extra instances, no global switching state
- **Minimal configuration**: register everything in one DSL block; `Retrofit.Builder.multiBaseUrls()` replaces `build()` and captures the original baseUrl automatically, so it can never drift
- **Retrofit-consistent path semantics**: the old baseUrl's path prefix is stripped, and the target URL's own prefix is preserved
- **Customizable fallback**: when an annotated environment has no registered URL, you can throw (default), fall back to the original baseUrl, or run custom logic
- **Pure Kotlin/JVM**: no Android dependency — works in any JVM project

## Requirements

- Retrofit ≥ 2.5 (annotations are read via the `Invocation` mechanism)
- OkHttp 4.x / 5.x

## Installation

```kotlin
dependencies {
    implementation("io.github.didahzy:multibaseurls:0.1.0")
}
```

## Quick start

### 1. Define environment markers and register URLs

Any `object` implementing `Host` becomes an environment marker:

```kotlin
object Production : Host
object Testing : Host
```

Configure everything in one DSL block, ideally in `Application.onCreate`:

```kotlin
MultiBaseUrls.setup {
    Production with "https://api.example.com/v2/"
    Testing with "https://test.example.com/"

    // Optional: fallback for unregistered environments; if unset, throws IllegalStateException
    onUnregistered { marker -> null }
}
```

Single-line registration works too (equivalent; later registrations override earlier ones):

```kotlin
Production.baseUrl("https://api.example.com/v2/")
```

### 2. Install the interceptor and build Retrofit

```kotlin
val retrofit = Retrofit.Builder()
    .baseUrl("https://default.example.com/")          // default environment, written once
    .client(
        OkHttpClient.Builder()
            .multiBaseUrls()                          // install the switching interceptor
            .build()
    )
    .addConverterFactory(GsonConverterFactory.create())
    .multiBaseUrls()                                  // instead of build(): captures the baseUrl above
    .create(ApiService::class.java)
```

### 3. Annotate the interface

```kotlin
@BaseUrl(Testing::class)          // class level: this interface defaults to Testing
interface ApiService {
    @GET("posts")
    fun getPosts(): Call<List<Post>>              // untagged → original baseUrl

    @BaseUrl(Production::class)
    @GET("user")
    fun getUser(): Call<User>                     // method level overrides class level → Production

    @Ignore
    @GET("raw")
    fun getRaw(): Call<User>                      // forces the original baseUrl, even with class-level @BaseUrl
}
```

## Switching rules

| Case | Behavior |
|------|----------|
| `@BaseUrl` on the method | Switches to the method-level registered URL |
| No `@BaseUrl` on the method, but on the interface class | Switches to the class-level registered URL |
| Method has a `@Url` parameter | No switching (absolute-URL semantics are protected) |
| `@Ignore` on the method | No switching (highest priority) |
| None of the above | Keeps the original baseUrl |

## Unregistered fallback

When an annotated marker has no registered URL, an `IllegalStateException` is thrown by default (configuration errors surface early). Customize it via `onUnregistered`:

```kotlin
MultiBaseUrls.setup {
    onUnregistered { marker ->
        when (marker) {
            Production::class -> "https://fallback.example.com/"  // return a URL: switch to it
            else -> null                                          // return null: fall back to the original baseUrl
        }
    }
}
```

## Path handling

Switching only changes the host; paths are joined with Retrofit semantics:

| Scenario | Result |
|----------|--------|
| Original `baseUrl` has no path prefix (e.g. `https://a.com/`) | The request path is carried over as-is: `/posts` → `https://b.com/posts` |
| Original `baseUrl` has `/api/`, target has `/v2/` | Old prefix stripped, new prefix kept: `/api/posts` → `https://b.com/v2/posts` |

For the second scenario the library needs to know the original baseUrl. The easiest way is building with `Retrofit.Builder.multiBaseUrls()` (captured automatically); if the Retrofit instance isn't built by you, set it in the DSL:

```kotlin
MultiBaseUrls.setup {
    originalBaseUrl = "https://default.example.com/api/"
}
```

## API cheat sheet

```kotlin
MultiBaseUrls.setup { /* DSL configuration */ }   // batch configuration entry
MultiBaseUrls.register(Prod::class, url)          // register (what the DSL calls)
MultiBaseUrls.unregister(Prod::class)             // remove a registration
MultiBaseUrls.clear()                             // clear everything
MultiBaseUrls.urlOf(Prod::class)                  // query a registered URL
MultiBaseUrls.originalBaseUrl                     // original baseUrl (null = treat as root)
MultiBaseUrls.onUnregistered                      // unregistered fallback policy
MultiBaseUrls.interceptor                         // the switching interceptor (what the extensions install)

Production.baseUrl(url)                           // single-line registration (must implement Host)
OkHttpClient.Builder().multiBaseUrls()            // install the interceptor
Retrofit.Builder().multiBaseUrls()                // build() + capture the original baseUrl
```

> Registered URLs are validated eagerly (invalid values throw `IllegalArgumentException`) and get a trailing `/` appended. Later registrations override earlier ones, so runtime hot-switching of environments works too.

## R8 / ProGuard

The library works through runtime annotations and reflection; keep the following under R8:

```
-keepattributes *Annotation*
-keep class ziyao.huang.multibaseurls.** { *; }
```

## Sample

A runnable Android sample lives in the [`sample`](sample/) module: three buttons demo untagged requests, `@BaseUrl` switching (including path-prefix preservation), and `@Ignore`, with the actual request URL shown in the UI.

## License

```
Copyright 2026 Huang Ziyao (ziyao.huang)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
