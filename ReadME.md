# NoteTask Backend

Прокси-сервер для клиент-серверного проекта **NoteTask**, его бекенд составляющая. Скрывает учётные данные
GigaChat от клиента, кэширует токены, обеспечивает аутентификацию по API-ключу
и предоставляет единый REST-эндпоинт для генерации задач из распознанного текста.

### Зачем этот сервис? 

Android-приложение использует GigaChat для преобразования распознанного
голосом текста в структурированные задачи. Чтобы **не хранить `client_id`
и `client_secret` в APK** (их можно извлечь декомпиляцией), все вызовы идут
через этот прокси-сервер:
Android ──[X-API-Key]──► NoteTask Backend ──[Bearer + client_secret]──► GigaChat API

---

## 🧱 Архитектура

// граф архитектуры

**Ключевые решения:**

- **Чистая авторизация клиентов** до подключения к контроллеру за счёт spring boot security
- **Один горизонтальный слой** — аутентификация по `X-API-Key`, проверяется
  на каждом запросе. JWT не используется: клиент один, различать некого.
- **Три вертикальных слоя** — Controller → Service → Client. HTTP-контракт,
  бизнес-логика и транспорт изолированы.
- **Single-flight кэш токена** — `TokenProvider` хранит `AtomicReference<Mono>`
  и `CachedToken`. Даже при 100 одновременных запросах с пустым кэшем
  GigaChat вызовется **один раз**.
- **Событийная инвалидация** — при 401 от GigaChat токен сбрасывается,
  запрос повторяется один раз с новым токеном.

---

## 🛠 Стек

| Компонент | Технология                                            |
|---|-------------------------------------------------------|
| Язык | Java 21                                               |
| Фреймворк | Spring Boot 3.3.0                                     |
| Реактивный стек | Spring WebFlux (Netty + Reactor)                      |
| Сборка | Gradle 8.10.2 (Kotlin DSL)                            |
| HTTP-клиент | WebClient (Reactor Netty)                             |
| Сериализация | Jackson                                               |
| Безопасность | Spring Security (кастомный `AuthenticationWebFilter`) |
| Метрики | Micrometer + Actuator                                 |
| Логирование | SLF4J + Logback                                       |
| Тесты | JUnit 5, Mockito, Reactor Test, MockWebServer         |
| Контейнеризация | Docker + Docker Compose                               |

---

## 🚀 Запуск

### Требования

- JDK 21 (Temurin рекомендуется)
- Docker (для контейнерного запуска)
- Аккаунт GigaChat с `client_id` и `client_secret`

### Получение учётных данных GigaChat

1. Зарегистрируйтесь на [Sber ID](https://id.sber.ru/).
2. Перейдите в личный кабинет GigaChat Developers.
3. Создайте приложение — получите `client_id` и `client_secret`.
4. Сохраните их — понадобятся для `.env`.

### Локальный запуск

**1. Клонировать репозиторий:**

```bash
git clone https://github.com/FeodorH/NoteTaskBackend
cd NoteTaskBackend
```
2. Создать .env в корне проекта:

```env
APP_API_KEY=dev-key-local-only
GIGACHAT_CLIENT_ID=ваш-client-id
GIGACHAT_CLIENT_SECRET=ваш-client-secret
```
3. Положить сертификаты Минцифры (если их там нет или они не актуальны) в src/main/resources/certs/:

4. Запустить(можно убрать secret профиль если нет):

```bash
./gradlew bootRun --args='--spring.profiles.active=dev,secret'
```
Или через IDEA Run Configuration с профилями dev,secret.

Приложение запустится на http://localhost:8080.

### Запуск через Docker
//TODO

## 📡 API
POST /v1/generate
Генерирует текст задачи из распознанного текста.

#### Заголовки:

| Заголовок | Обязательный | Описание |
|---|---|---|
| `X-API-Key` | Да | API-ключ клиента |
| `Content-Type` | Да | `application/json` |
#### Тело запроса:
```json
{
  "prompt": "инструкция гигачату"
}
```
| Поле | Тип | Ограничения |
|---|---|---|
| `prompt` | string | `@NotBlank`, `@Size(max=4000)` |

#### Успешный ответ (200):
```json
{
  "response": "Купить хлеб завтра.",
  "status": "200"
}
```
#### Ошибки:
| Статус | Когда | Тело ответа |
|---|---|---|
| `400` | Невалидный `prompt` | `{"status":"validation_error","message":"prompt: ..."}` |
| `401` | Отсутствует / неверный `X-API-Key` | (пусто) |
| `502` | Ошибка авторизации в GigaChat | `{"status":"gigachat_auth_error","message":"..."}` |
| `503` | GigaChat недоступен | `{"status":"gigachat_unavailable","message":"..."}` |

## 🔐 Безопасность
### API-ключ клиента

Клиент идентифицируется заголовком `X-API-Key`, который проверяется на каждом
запросе до попадания в контроллер.

**Как работает:**
```text
1. `ApiKeyAuthenticationConverter` читает `X-API-Key` из заголовка.
2. `ApiKeyAuthenticationManager` сверяет с `app.api-key` из конфигурации.
3. При совпадении кладёт `Authentication` с ролью `ROLE_CLIENT` в `SecurityContext`.
4. При несовпадении — `401 Unauthorized` через `AuthenticationEntryPoint`.
```
**Ограничения подхода:**
- API ключ клиента хранится в APK и может быть извлечён декомпиляцией.
- Утечка ключа → потребуется смена + новая версия приложения.
- **Митигация:** Play Integrity API / аттестация APK (в плане развития).

### Учётные данные GigaChat

`client_id` и `client_secret` **никогда** не покидают сервер:

| Параметр | Где хранится | Кто использует |
|---|---|---|
| `GIGACHAT_CLIENT_ID` | env / `.env` | `GigaChatAuthClient` |
| `GIGACHAT_CLIENT_SECRET` | env / `.env` | `GigaChatAuthClient` |
| `access_token` | Только в памяти (`TokenProvider`) | `GigaChatCompletionClient` |

Access token кэшируется на ~30 минут. Single-flight гарантирует, что даже
при 100 одновременных запросах GigaChat OAuth вызовется **один раз**.

### SSL

GigaChat использует сертификаты **НУЦ Минцифры**, которых нет в стандартном
хранилище Java. `SslConfig` создаёт кастомный `SslContext` с тремя сертификатами:

| Файл | Роль |
|---|---|
| `root_ca.crt` | Корневой сертификат |
| `sub_ca.crt` | Промежуточный |
| `server_cert.crt` | Серверный (`ngw.devices.sberbank.ru`) |

Сертификаты **публичные** — можно коммитить в репозиторий.
Источник: [gosuslugi.ru/crt](https://www.gosuslugi.ru/crt).

## 📊 Метрики и мониторинг
### Actuator

| Эндпоинт | Доступ | Назначение |
|---|---|---|
| `/actuator/health` | Публичный | Живость приложения |
| `/actuator/info` | С ключом | Информация о сборке |
| `/actuator/metrics` | С ключом | Список метрик |

### Ключевые метрики

**Стандартные (Micrometer):**
- `http.server.requests` — HTTP-запросы (count, duration, status)
- `jvm.memory.used` — использование памяти JVM
- `jvm.threads.live` — количество живых потоков
- `process.cpu.usage` — использование CPU

**Кастомные:**
- `gigachat.requests.total` — общее число запросов к GigaChat
- `gigachat.token.refresh.total` — количество обновлений токена

## 🧪 Тесты
Все тесты
```bash
./gradlew test
```

Отдельные классы
```bash
./gradlew test --tests "*TokenProviderTest"
./gradlew test --tests "*GigaChatServiceTest"
./gradlew test --tests "*ChatControllerTest"
./gradlew test --tests "*ApiKeyAuthIntegrationTest"
./gradlew test --tests "*GigaChatAuthClientTest"
./gradlew test --tests "*GigaChatCompletionClientTest"
```

## 🐳 Docker
// TODO - позже
## 🔄 Как это работает: полный цикл запроса
```text
1. Android: POST /v1/generate
   Headers: X-API-Key: <key>
   Body: { "prompt": "купить хлеб завтра" }

2. ApiKeyAuthFilter
   ├── читает X-API-Key
   ├── сверяет с AppProperties.apiKey()
   └── OK → кладёт Authentication в SecurityContext

3. ChatController
   ├── @Valid проверяет ChatRequest
   └── вызывает gigaChatService.generateTask(prompt)

4. GigaChatService
   ├── tokenProvider.getToken()
   │   ├── кэш валиден → Mono.just(token)              // 0 вызовов GigaChat
   │   └── кэш пуст    → single-flight:
   │       ├── inFlight.get() != null → переиспользует
   │       └── inFlight.get() == null → создаёт fresh:
   │           ├── GigaChatAuthClient.requestToken()
   │           ├── POST /api/v2/oauth с Basic Auth
   │           └── парсит access_token + expires_at
   ├── completionClient.complete(prompt, system, token)
   │   └── POST /v1/chat/completions с Bearer token
   ├── 401 → tokenProvider.invalidate() + retry 1 раз
   └── успех → возвращает String

5. ChatController
   └── map(result -> new ChatResponse(result, "200"))

6. Android получает 200 OK с JSON
```

## 🚧 План развития
□ Play Integrity API для защиты API-ключа в APK
□ Rate limiting по клиенту (Resilience4j / Bucket4j)
□ Prometheus + Grafana для мониторинга
□ SSE-стриминг ответа GigaChat (Flux<String>)
□ Кэш ответов на одинаковые промпты (опционально)
□ Поддержка нескольких клиентов (список ClientConfig)
□ Интеграционные тесты с Testcontainers + WireMock

## 📄 Лицензия
Проект предоставлен в ознакомительных целях.
Использование в коммерческих целях без согласия автора запрещено.
