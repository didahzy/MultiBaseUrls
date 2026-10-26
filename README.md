# MultiBaseUrls

[简体中文](README.md) | [English](README.en.md) | [日本語](README.ja.md)

一个轻量的 Retrofit baseUrl 切换库。通过 OkHttp 拦截器读取接口方法上的注解，在**同一个 Retrofit 实例**内按方法分流到不同的 baseUrl——零侵入，不加注解的接口完全不受影响。

```kotlin
// 一次性注册环境
MultiBaseUrls.setup {
    Production with "https://api.example.com/v2/"
    Testing with "https://test.example.com/"
}

// 接口方法按需标注
interface ApiService {
    @GET("posts")
    suspend fun getPosts(): List<Post>          // 无注解 → 原始 baseUrl

    @BaseUrl(Production::class)
    @GET("user")
    suspend fun getUser(): User                 // → https://api.example.com/v2/user
}
```

## 特性

- **注解驱动**：`@BaseUrl` 标在方法或接口类上，方法级优先于类级
- **同一个 Retrofit 实例**：不同方法天然打到不同环境，无需多个实例、无全局切换状态
- **配置极简**：DSL 一次性注册；`Retrofit.Builder.multiBaseUrls()` 替代 `build()`，原始 baseUrl 自动捕获，永不失配
- **路径语义与 Retrofit 一致**：自动剥离旧 baseUrl 的路径前缀，保留目标 URL 自带的前缀
- **可定制的兜底**：注解指向的环境未注册时，可抛异常（默认）、回退原始 baseUrl 或执行自定义逻辑
- **纯 Kotlin/JVM**：不依赖 Android，任何 JVM 项目可用

## 要求

- Retrofit ≥ 2.5（基于 `Invocation` 机制读取注解）
- OkHttp 4.x / 5.x

## 接入

```kotlin
dependencies {
    implementation("io.github.didahzy:multibaseurls:0.1.0")
}
```

## 快速开始

### 1. 定义环境标记并注册 URL

任意 `object` 实现 `Host` 接口即成为环境标记：

```kotlin
object Production : Host
object Testing : Host
```

推荐在 `Application.onCreate` 里用 DSL 一次性配置：

```kotlin
MultiBaseUrls.setup {
    Production with "https://api.example.com/v2/"
    Testing with "https://test.example.com/"

    // 可选：未注册环境的兜底策略，不设置 = 抛 IllegalStateException
    onUnregistered { marker -> null }
}
```

也可以单行注册（两者等价，后注册覆盖先注册）：

```kotlin
Production.baseUrl("https://api.example.com/v2/")
```

### 2. 装拦截器、构建 Retrofit

```kotlin
val retrofit = Retrofit.Builder()
    .baseUrl("https://default.example.com/")          // 默认环境，只写这一处
    .client(
        OkHttpClient.Builder()
            .multiBaseUrls()                          // 安装切换拦截器
            .build()
    )
    .addConverterFactory(GsonConverterFactory.create())
    .multiBaseUrls()                                  // 替代 build()：自动捕获上面的 baseUrl
    .create(ApiService::class.java)
```

### 3. 在接口上标注

```kotlin
@BaseUrl(Testing::class)          // 类级：本接口默认切到 Testing
interface ApiService {
    @GET("posts")
    fun getPosts(): Call<List<Post>>              // 无注解 → 原始 baseUrl

    @BaseUrl(Production::class)
    @GET("user")
    fun getUser(): Call<User>                     // 方法级覆盖类级 → Production

    @Ignore
    @GET("raw")
    fun getRaw(): Call<User>                      // 强制原始 baseUrl，即使类级有 @BaseUrl
}
```

## 切换规则

| 情况 | 行为 |
|------|------|
| 方法上有 `@BaseUrl` | 切到该方法级注册的 URL |
| 方法没有、接口类上有 `@BaseUrl` | 切到类级注册的 URL |
| 方法参数带 `@Url` | 不切换（保护绝对 URL 语义） |
| 方法上有 `@Ignore` | 不切换（优先级最高） |
| 都没有 | 保持原始 baseUrl |

## 未注册兜底

注解指向的标记没有注册过 URL 时，默认抛 `IllegalStateException`（配置错误尽早暴露）。可通过 `onUnregistered` 自定义：

```kotlin
MultiBaseUrls.setup {
    onUnregistered { marker ->
        when (marker) {
            Production::class -> "https://fallback.example.com/"  // 返回 URL：用它切换
            else -> null                                          // 返回 null：回退原始 baseUrl
        }
    }
}
```

## 路径处理

切换只影响 host，路径按 Retrofit 语义拼接：

| 场景 | 结果 |
|------|------|
| 原始 `baseUrl` 无路径前缀（如 `https://a.com/`） | 请求 path 原样挂到新 host：`/posts` → `https://b.com/posts` |
| 原始 `baseUrl` 带 `/api/`，目标带 `/v2/` | 剥掉旧前缀、保留新前缀：`/api/posts` → `https://b.com/v2/posts` |

第二种场景需要库知道原始 baseUrl，推荐直接用 `Retrofit.Builder.multiBaseUrls()` 构建（自动捕获，无需手动配置）；若 Retrofit 实例不是你构建的，也可在 DSL 里手动指定：

```kotlin
MultiBaseUrls.setup {
    originalBaseUrl = "https://default.example.com/api/"
}
```

## API 速查

```kotlin
MultiBaseUrls.setup { /* DSL 配置 */ }        // 批量配置入口
MultiBaseUrls.register(Prod::class, url)      // 注册（DSL 的底层等价）
MultiBaseUrls.unregister(Prod::class)         // 移除注册
MultiBaseUrls.clear()                         // 清空
MultiBaseUrls.urlOf(Prod::class)              // 查询已注册的 URL
MultiBaseUrls.originalBaseUrl                 // 原始 baseUrl（null = 按根路径处理）
MultiBaseUrls.onUnregistered                  // 未注册兜底策略
MultiBaseUrls.interceptor                     // 切换拦截器（扩展函数的底层等价）

Production.baseUrl(url)                       // 单行注册（需实现 Host 接口）
OkHttpClient.Builder().multiBaseUrls()        // 安装拦截器
Retrofit.Builder().multiBaseUrls()            // build() + 自动捕获原始 baseUrl
```

> 注册的 URL 会即时校验（非法值抛 `IllegalArgumentException`）并自动补末尾 `/`；重复注册覆盖旧值，因此也支持运行时热切换环境。

## 混淆

库通过运行时注解 + 反射工作，R8 下建议保留：

```
-keepattributes *Annotation*
-keep class ziyao.huang.multibaseurls.** { *; }
```

## 示例

完整可运行的 Android 示例见 [`sample`](sample/) 模块：三个按钮分别演示无注解、`@BaseUrl` 切换（含路径前缀保留）和 `@Ignore`，UI 直接展示实际请求 URL。

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
