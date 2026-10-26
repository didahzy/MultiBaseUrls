# MultiBaseUrls

[简体中文](README.md) | [English](README.en.md) | [日本語](README.ja.md)

Retrofit のベース URL 切替用ライトウェイトライブラリ。OkHttp インターセプターがインターフェースメソッドのアノテーションを読み取り、**単一の Retrofit インスタンス**のままでメソッドごとに異なる baseUrl へ振り分けます。アノテーションを付けないメソッドには一切影響しません。

```kotlin
// 環境を一度だけ登録
MultiBaseUrls.setup {
    Production with "https://api.example.com/v2/"
    Testing with "https://test.example.com/"
}

// 必要なメソッドにアノテーション
interface ApiService {
    @GET("posts")
    suspend fun getPosts(): List<Post>          // アノテーションなし → 元の baseUrl

    @BaseUrl(Production::class)
    @GET("user")
    suspend fun getUser(): User                 // → https://api.example.com/v2/user
}
```

## 特徴

- **アノテーション駆動**：`@BaseUrl` はメソッドまたはインターフェースクラスに付与可能。メソッドレベルがクラスレベルより優先
- **単一の Retrofit インスタンス**：メソッドごとに自然に別環境へ到達。インスタンスの複製もグローバルな切替状態も不要
- **最小限の設定**：DSL の 1 ブロックで登録完了。`Retrofit.Builder.multiBaseUrls()` が `build()` の代わりになり、元の baseUrl を自動取得するため設定漏れが起きない
- **Retrofit と一致するパスセマンティクス**：元の baseUrl のパスプレフィックスを剥がし、目標 URL 自身のプレフィックスを保持
- **カスタマイズ可能なフォールバック**：アノテーション先の環境が未登録の場合、例外スロー（デフォルト）・元の baseUrl へのフォールバック・独自ロジックから選択可能
- **純粋な Kotlin/JVM**：Android に依存しないため、あらゆる JVM プロジェクトで利用可能

## 要件

- Retrofit ≥ 2.5（`Invocation` 仕組み経由でアノテーションを読み取ります）
- OkHttp 4.x / 5.x

## 導入

```kotlin
dependencies {
    implementation("io.github.didahzy:multibaseurls:0.1.0")
}
```

## クイックスタート

### 1. 環境マーカーの定義と URL 登録

`Host` インターフェースを実装した `object` がそのまま環境マーカーになります：

```kotlin
object Production : Host
object Testing : Host
```

`Application.onCreate` で DSL の 1 ブロックにまとめて設定するのがおすすめです：

```kotlin
MultiBaseUrls.setup {
    Production with "https://api.example.com/v2/"
    Testing with "https://test.example.com/"

    // 任意：未登録環境のフォールバック。未設定の場合は IllegalStateException をスロー
    onUnregistered { marker -> null }
}
```

1 行での登録も可能です（等価。後から登録したものが上書きします）：

```kotlin
Production.baseUrl("https://api.example.com/v2/")
```

### 2. インターセプターの取り付けと Retrofit の構築

```kotlin
val retrofit = Retrofit.Builder()
    .baseUrl("https://default.example.com/")          // デフォルト環境。記述はここ一度だけ
    .client(
        OkHttpClient.Builder()
            .multiBaseUrls()                          // 切替インターセプターを取り付け
            .build()
    )
    .addConverterFactory(GsonConverterFactory.create())
    .multiBaseUrls()                                  // build() の代わり：上の baseUrl を自動取得
    .create(ApiService::class.java)
```

### 3. インターフェースへのアノテーション

```kotlin
@BaseUrl(Testing::class)          // クラスレベル：このインターフェースのデフォルトは Testing
interface ApiService {
    @GET("posts")
    fun getPosts(): Call<List<Post>>              // アノテーションなし → 元の baseUrl

    @BaseUrl(Production::class)
    @GET("user")
    fun getUser(): Call<User>                     // メソッドレベルがクラスレベルを上書き → Production

    @Ignore
    @GET("raw")
    fun getRaw(): Call<User>                      // クラスレベルに @BaseUrl があっても元の baseUrl を強制
}
```

## 切替ルール

| ケース | 動作 |
|--------|------|
| メソッドに `@BaseUrl` あり | メソッドレベルの登録 URL へ切替 |
| メソッドになく、インターフェースクラスに `@BaseUrl` あり | クラスレベルの登録 URL へ切替 |
| メソッドのパラメータに `@Url` あり | 切替なし（絶対 URL のセマンティクスを保護） |
| メソッドに `@Ignore` あり | 切替なし（最優先） |
| 上記以外 | 元の baseUrl を維持 |

## 未登録時のフォールバック

アノテーション先のマーカーに URL が登録されていない場合、デフォルトでは `IllegalStateException` をスローします（設定ミスを早期に検出）。`onUnregistered` でカスタマイズできます：

```kotlin
MultiBaseUrls.setup {
    onUnregistered { marker ->
        when (marker) {
            Production::class -> "https://fallback.example.com/"  // URL を返す：そこへ切替
            else -> null                                          // null を返す：元の baseUrl にフォールバック
        }
    }
}
```

## パスの扱い

切替で変わるのはホストのみで、パスは Retrofit のセマンティクスに従って結合されます：

| シナリオ | 結果 |
|----------|------|
| 元の `baseUrl` にパスプレフィックスなし（例：`https://a.com/`） | リクエストパスはそのまま新しいホストへ：`/posts` → `https://b.com/posts` |
| 元の `baseUrl` が `/api/`、目標が `/v2/` | 旧プレフィックスを剥がし、新プレフィックスを保持：`/api/posts` → `https://b.com/v2/posts` |

2 つ目のシナリオでは、ライブラリが元の baseUrl を知っている必要があります。最も簡単なのは `Retrofit.Builder.multiBaseUrls()` で構築する方法（自動取得）です。Retrofit インスタンスを自分で構築しない場合は、DSL で設定できます：

```kotlin
MultiBaseUrls.setup {
    originalBaseUrl = "https://default.example.com/api/"
}
```

## API 一覧

```kotlin
MultiBaseUrls.setup { /* DSL 設定 */ }           // 一括設定のエントリ
MultiBaseUrls.register(Prod::class, url)         // 登録（DSL の実体）
MultiBaseUrls.unregister(Prod::class)            // 登録解除
MultiBaseUrls.clear()                            // 全消去
MultiBaseUrls.urlOf(Prod::class)                 // 登録済み URL の照会
MultiBaseUrls.originalBaseUrl                    // 元の baseUrl（null = ルートとして扱う）
MultiBaseUrls.onUnregistered                     // 未登録フォールバックポリシー
MultiBaseUrls.interceptor                        // 切替インターセプター（拡張関数の実体）

Production.baseUrl(url)                          // 1 行登録（Host 実装が必要）
OkHttpClient.Builder().multiBaseUrls()           // インターセプターを取り付け
Retrofit.Builder().multiBaseUrls()               // build() + 元の baseUrl を自動取得
```

> 登録された URL は即座に検証され（不正な値は `IllegalArgumentException` をスロー）、末尾の `/` は自動補完されます。後からの登録が前のものを上書きするため、実行時の環境ホットスワップにも対応します。

## R8 / ProGuard

本ライブラリは実行時アノテーションとリフレクションで動作するため、R8 では以下の keep ルールを推奨します：

```
-keepattributes *Annotation*
-keep class ziyao.huang.multibaseurls.** { *; }
```

## サンプル

動作する Android サンプルは [`sample`](sample/) モジュールにあります。3 つのボタンで、アノテーションなし・`@BaseUrl` による切替（パスプレフィックスの保持を含む）・`@Ignore` をデモし、実際のリクエスト URL を UI に表示します。

## ライセンス

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
