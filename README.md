# Backend

Java 21 · Spring Boot · `com.team3`

## Requirements

- Java 21
- Docker
- Docker Compose v2
- Git

## Install

```bash
git clone https://github.com/swyp-web15-3team/backend.git
cd backend

cp .env.example .env
./gradlew installGitHooks
```

### Git Hooks

- `pre-commit`: Spotless 포맷 검사
- `pre-push`: 테스트, Checkstyle, Spotless 검사

## How to run

### Development

```bash
docker compose up -d db                     # run PostgreSQL using Docker
./gradlew bootRun                           # run server

curl http://localhost:8080/actuator/health  # healthcheck

# when developing is over, stop db server
docker compose stop db
```

### Docker

```bash
docker compose up -d --build

# check logs
docker compose ps
docker compose logs -f

# stop container. PostgreSQL volume is still available
docker compose down

# only build application image
docker build -t backend .
```

## Test

```bash
./gradlew check
```

---

## 협업 규칙

### 도구

| 도구 | 역할 |
| --- | --- |
| Jira | 칸반·작업 관리 |
| Notion | 문서 (명세, ERD, 회의록) |
| GitHub | 코드·PR·리뷰 |
| Figma | 디자인 |

### 작업 흐름

1. Jira에 이슈를 만든다.
2. 이슈 키로 브랜치를 판다.
3. PR을 올린다. (제목에 이슈 키 포함)
4. 리뷰 후 머지한다.
5. 머지된 브랜치는 삭제한다.

Jira ↔ GitHub 연동이 켜져 있으면, 브랜치·PR의 이슈 키로 칸반 상태가 자동으로 움직인다.

- 브랜치 생성 → In Progress
- PR 오픈 → Review
- 머지 → Done

### 브랜치

형식

```
<type>/<JIRA-KEY>-짧은-설명
```

예시

```
feature/KAN-12-whisky-categories
fix/KAN-34-login-refresh
docs/KAN-50-api-spec
```

종류: `feature` / `fix` / `refactor` / `docs` / `chore` / `test`

- 한 브랜치 = 한 목적
- 기능과 리팩터링을 한 브랜치에 섞지 않는다

### 커밋

형식

```
<type>: 작업 내용
```

예시

```
feat: 위스키 카테고리 목록 API 추가
fix: 품절 상품이 최저가에 포함되던 문제 수정
docs: 위스키 목록 API 명세 정리
```

타입: `feat` / `fix` / `refactor` / `docs` / `style` / `test` / `chore`  
(소문자, `feat:` 형태)

### Pull Request

- 한 PR = 한 목적
- 제목에 Jira 키를 넣는다

```
[KAN-12] feat: 위스키 카테고리 목록 API
```

- 머지 전 리뷰 1명 이상
- 필요하면 본문에 Notion 명세 링크를 추가한다

```
Spec: (Notion URL)
```

---

## 코드 컨벤션

### 용어

| 용어 | 의미 |
| --- | --- |
| Whisky | 위스키 |
| Category | 종류 |
| Origin | 원산지 |
| Region | 생산 지역 |
| Retailer | 판매처 |
| SaleProduct | 판매 상품 |
| Collection | 관심 그룹 |
| Planner | 플래너 |

### 명명

| 대상 | 규칙 | 예 |
| --- | --- | --- |
| Class | PascalCase | `WhiskyController` |
| Method | camelCase | `findById()` |
| Variable | camelCase | `whiskyId` |
| Constant | UPPER_SNAKE_CASE | `MAX_PAGE_SIZE` |
| Boolean | `is` / `has` | `isSoldOut`, `hasPrice` |
| Collection | 복수형 | `whiskies`, `categories` |

메서드 동사

- 조회: `find…` / `get…`
- 생성: `create…`
- 수정: `update…`
- 삭제: `delete…`

### 패키지

도메인 단위로 나눈다. 빈 레이어 패키지는 만들지 않는다.

```
com.team3
├── common
├── security
├── auth
├── user
├── whisky
├── retailer
├── collection
└── planner
```

필요하면 도메인 안에만 하위 패키지를 둔다.

```
auth/
├── dto/
├── jwt/
└── token/
```

### 클래스·의존성

- Controller / Service / Repository 역할은 유지한다.
- 생성자 주입. `@Autowired` 쓰지 않는다.
- `var` 쓰지 않는다. 타입을 명시한다.
- DTO는 `record`를 우선한다.
- 포맷·스타일은 Spotless + Checkstyle을 따른다. (`./gradlew spotlessApply check`)

### Entity

- Lombok을 Entity에 쓰지 않는다.
- `protected` 기본 생성자 + 검증 있는 생성자.
- 접근자는 `get` 없이 (`id()`, `userId()`).
- 상태 변경은 의미 있는 메서드로 (`rotate(...)`). `@Setter` 지양.

스키마 변경은 Flyway 마이그레이션으로 관리한다.  
JPA `ddl-auto`로 테이블을 생성·변경하지 않는다. (현재 설정: `validate`)

### API

공통 프리픽스 `/api/v1`. 리소스는 복수형.

```
POST /api/v1/auth/kakao
POST /api/v1/auth/refresh
POST /api/v1/auth/logout

GET  /api/v1/whiskies
GET  /api/v1/whiskies/{whiskyId}
GET  /api/v1/whisky-categories
```

성공 응답 — `ApiResponse` (`data`만)

```json
{
  "data": {}
}
```

실패 응답 — ProblemDetail (공통 핸들러)

```json
{
  "status": 400,
  "detail": "Request validation failed.",
  "errors": [
    { "field": "code", "message": "..." }
  ]
}
```

`success` 필드 없음. 에러를 `{"success": false, "message": ...}`로 감싸지 않는다.

### 작성 습관

- 메서드 하나 = 책임 하나
- 중괄호 생략 금지
- 매직 넘버 지양 → 상수
- 코드로 드러나는 내용에 주석 달지 않음
- 시크릿·스택트레이스·민감값을 응답/로그에 넣지 않음
```