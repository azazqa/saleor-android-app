# Saleor App

[Saleor](https://saleor.io/) GraphQL API를 사용하는 Android 스토어프론트입니다. Jetpack Compose, Navigation 3, Hilt, Apollo Kotlin으로 구성되며, 카탈로그 탐색부터 장바구니·배송 정보·Toss 결제까지 이어집니다.

- 패키지: `com.bdf.saleor`
- 앱 이름: Saleor Store
- minSdk 31, compileSdk 37, targetSdk 36, Java/Kotlin 17

## 전체 구조

Gradle 멀티모듈 + 계층형 아키텍처입니다. UI는 feature 모듈, 도메인 모델·네트워크·저장소는 core 모듈에 둡니다.

```
SaleorApp
├── app                  # 진입점, 내비게이션, Hilt Application
├── feature
│   ├── catalog          # 홈, 카테고리, 검색, 상품 목록/상세
│   ├── account          # 로그인, 회원가입, 주문, 주소, 설정
│   └── checkout         # 장바구니, 배송 정보, 결제, Toss
├── core
│   ├── model            # 공유 데이터 클래스 (상품, 주소, 체크아웃 등)
│   ├── network          # Apollo GraphQL 클라이언트, 스키마·쿼리
│   ├── datastore        # 토큰·체크아웃 ID (DataStore)
│   ├── data             # Repository 구현, Hilt 바인딩
│   ├── designsystem     # 테마, 공통 Compose 컴포넌트
│   └── testing          # Fake Repository, 테스트 유틸
└── build-logic          # 공통 Gradle convention 플러그인
```

의존 방향은 `app` → `feature` → `core:data` → `core:network` / `core:datastore` → `core:model` 입니다. `core:designsystem`은 UI 모듈이 공통으로 참조합니다.

### 모듈 역할

| 모듈 | 역할 |
|------|------|
| `:app` | `SaleorApplication`, `MainActivity`, `SaleorNavHost`. 하단 탭(홈 / 카테고리 / 검색 / 내 계정)과 상품·계정·결제 화면을 연결합니다. |
| `:feature:catalog` | 홈(추천 컬렉션), 카테고리, 검색, 상품 목록·상세, 장바구니 담기. |
| `:feature:account` | 로그인·회원가입·비밀번호 찾기, 주문 목록·상세, 주소록, 적립금, 계정 설정. |
| `:feature:checkout` | 장바구니 → 배송 정보(배송지·배송 방법) → 결제. Toss Payments, 카카오 우편번호. |
| `:core:model` | GraphQL/UI가 공유하는 순수 Kotlin 모델. |
| `:core:network` | Apollo 클라이언트, Authorization 인터셉터, `*.graphql` 작업, `schema.graphqls`. 빌드 시 Saleor 설정을 `BuildConfig`에 넣습니다. |
| `:core:datastore` | 액세스/리프레시 토큰, 채널별 체크아웃 ID. |
| `:core:data` | `Catalog` / `Auth` / `Account` / `Order` / `Cart` / `Checkout` Repository 구현. |
| `:core:designsystem` | Material 3 브랜드 테마(네이비/골드/크림), `StorefrontTopBar`, `ScreenSurface`, 스낵바. |
| `:core:testing` | 단위·UI 테스트용 Fake Repository와 `MainDispatcherRule`. |

### 런타임 흐름

1. **UI** — Compose 화면이 ViewModel을 관찰합니다. 화면은 feature 모듈, 공통 크롬은 `core:designsystem`과 `app`의 `SaleorNavHost`에 있습니다.
2. **ViewModel** — Hilt로 Repository를 주입받아 상태를 만듭니다.
3. **Repository (`core:data`)** — Apollo 호출 결과를 `core:model` 타입으로 매핑합니다.
4. **Network (`core:network`)** — `SALEOR_API_URL`로 GraphQL 요청. 로그인 시 Authorization 헤더를 붙입니다. 메모리 + SQLite 정규화 캐시를 사용합니다.
5. **Datastore** — JWT와 게스트 체크아웃 ID를 기기에 유지합니다.

결제 진행은 **장바구니 → 배송 정보 → 결제** 3단계입니다. 실물 상품은 배송 정보 화면에서 배송지와 배송 방법을 함께 고릅니다. 결제는 Toss Payments SDK(`TossPaymentActivity`)를 사용합니다.

### 내비게이션

하단 탭 목적지: `Home`, `Categories`, `Search`, `Account`.

스택으로 열리는 화면: 상품 목록/상세, 회원가입, 비밀번호 찾기, 주문 상세, 장바구니, 체크아웃, 주문 완료.

정의는 `app/.../navigation/NavKeys.kt` 입니다.

## 설정 파일

Saleor 연결 값은 **빌드 타임**에 고정됩니다. 앱 실행 중에는 바꿀 수 없습니다. 우선순위는 아래와 같습니다.

1. `local.properties` (로컬·CI 오버라이드, Git에 올리지 않음)
2. `gradle.properties`의 `saleor.*` 키
3. `core/network/build.gradle.kts`의 기본값

`core:network`가 이 값을 `BuildConfig` 필드로 넣고, Hilt `SaleorCatalogConfig`로 주입합니다. Apollo introspection의 `endpointUrl`도 같은 API URL을 씁니다.

### `gradle.properties`

프로젝트 전역 Gradle 옵션과 Saleor 기본값입니다.

| 키 | 의미 |
|----|------|
| `org.gradle.jvmargs` | Gradle 데몬 JVM 메모리·인코딩 (`-Xmx2048m`, UTF-8). |
| `org.gradle.configuration-cache` | Configuration Cache 사용. |
| `kotlin.code.style` | Kotlin 공식 코드 스타일. |
| `saleor.api.url` | Saleor GraphQL 엔드포인트. |
| `saleor.channel` | 판매 채널 슬러그 (예: `kr`). |
| `saleor.locale` | 스토어프론트 로케일 슬러그 (예: `ko`). GraphQL `LanguageCodeEnum`은 대문자(`KO`)로 변환됩니다. |
| `saleor.checkout.country` | 체크아웃 국가 코드 (예: `KR`). |
| `saleor.featured.collection` | 홈 추천 상품 컬렉션 슬러그. |
| `saleor.storefront.url` | 웹 스토어프론트 베이스 URL. 비밀번호 재설정·계정 확인 리다이렉트에 사용합니다. |

현재 기본 API는 `https://saleor-api.klms.co.kr/graphql/`, 스토어프론트는 `https://saleor.klms.co.kr` 입니다.

### `local.properties`

Android SDK 경로(`sdk.dir`)와 로컬 전용 Saleor 오버라이드를 둡니다. `.gitignore`에 포함되어 있습니다.

다른 백엔드로 붙이려면 같은 키를 이 파일에 적습니다.

```properties
sdk.dir=C:\\Users\\<user>\\AppData\\Local\\Android\\Sdk
saleor.api.url=https://example.com/graphql/
saleor.channel=kr
saleor.locale=ko
saleor.checkout.country=KR
saleor.featured.collection=featured-products
saleor.storefront.url=https://example.com
```

### `settings.gradle.kts`

루트 프로젝트 이름(`Saleor App`), 플러그인/의존성 저장소, 모듈 `include`를 선언합니다. `build-logic`은 `includeBuild`로 연결합니다. 의존성 저장소는 `google()`, `mavenCentral()`, Toss SDK용 `jitpack.io`입니다. 모듈에서 저장소를 추가하지 못하도록 `FAIL_ON_PROJECT_REPOS`를 사용합니다.

### `build.gradle.kts` (루트)

Android Application/Library, Kotlin Compose/Serialization, Apollo, Hilt, KSP 플러그인을 `apply false`로만 선언합니다. 실제 적용은 각 모듈과 convention 플러그인이 합니다.

### `gradle/libs.versions.toml`

버전 카탈로그입니다. AGP, Kotlin, Compose BOM, Apollo, Hilt, Coil, DataStore, Toss Payments 등 라이브러리·플러그인 버전을 한곳에서 관리합니다.

### `gradle/wrapper/gradle-wrapper.properties`

Gradle 배포판(현재 9.5.0)과 체크섬을 고정합니다. `./gradlew` / `gradlew.bat`이 이 버전을 사용합니다.

### `gradle/gradle-daemon-jvm.properties`

Foojay toolchain resolver가 Gradle 데몬 JVM(현재 25)을 고를 때 쓰는 파일입니다. 앱 컴파일 타깃(17)과는 별개입니다.

### `build-logic/`

Convention 플러그인입니다. 모듈 `build.gradle.kts`를 짧게 유지합니다.

| 플러그인 ID | 용도 |
|-------------|------|
| `saleor.android.application` | 앱 모듈: compileSdk 37, minSdk 31, targetSdk 36, Java 17. |
| `saleor.android.application.compose` | 앱 + Compose 컴파일러. |
| `saleor.android.library` | 라이브러리 모듈 SDK·Java 설정. |
| `saleor.android.library.compose` | 라이브러리 + Compose. |
| `saleor.android.hilt` | Hilt + KSP. |

`build-logic/settings.gradle.kts`는 루트의 `libs.versions.toml`을 그대로 참조합니다.

### 모듈 `build.gradle.kts`

- `app/build.gradle.kts` — `applicationId`, 버전, feature/core 의존성, Navigation 3, 계측 테스트(`HiltTestRunner`).
- `core/network/build.gradle.kts` — `saleor.*` → `BuildConfig`, Apollo 패키지 `com.bdf.saleor.graphql`, 스키마 `src/main/graphql/schema.graphqls`.
- feature 모듈 — Compose UI와 해당 Repository만 의존합니다. `:feature:checkout`는 Toss SDK와 WebView(카카오 우편번호)를 추가로 씁니다.

### Android 매니페스트·테마

- `app/src/main/AndroidManifest.xml` — `INTERNET`, `SaleorApplication`, `MainActivity`, HTTPS만 허용(`usesCleartextTraffic=false`).
- `feature/checkout/.../AndroidManifest.xml` — `TossPaymentActivity`.
- `app/src/main/res/values/themes.xml`, `colors.xml` — XML 테마. Compose 브랜드 색은 `core/designsystem/.../theme/Color.kt`와 맞춥니다.

### `.gitignore`

`local.properties`, 빌드 산출물, IDE 캐시, 키스토어(`*.jks`, `keystore.properties`)를 제외합니다.

## 빌드

Android Studio에서 루트 프로젝트를 열거나:

```bash
./gradlew :app:assembleDebug
./gradlew test
```

Windows에서는 `gradlew.bat`을 사용합니다.

스키마를 Saleor API에서 다시 받으려면 `:core:network`의 Apollo download 태스크를 실행합니다. `saleor.api.url`이 가리키는 엔드포인트에 접근할 수 있어야 합니다.
