# Backend Code Convention

---

## 1. 브랜치 전략

```
main      ← 배포 브랜치
develop   ← 통합 브랜치 (PR 머지 대상)
{type}/{도메인명}  ← 작업 브랜치
```

```
feat/user
fix/auth
refactor/product
```

- 브랜치 생성 → 작업 → PR → 코드래빗 리뷰 → develop 머지
- 도메인 1개당 브랜치 1개
- `main`, `develop` 직접 push 지양

**hotfix (긴급 수정)**

- `hotfix/{내용}`: main에서 분기 → main 머지(=자동 배포) → **develop에도 머지 (백머지 필수)**
- 발동 조건은 "깨졌을 때"만 — 서버 장애인데 develop에 승격 못 할 작업이 섞여 있는 경우
- 미완성 기능은 hotfix 금지, 열린 PR로 남긴다

**브랜치 삭제**

- 머지된 브랜치는 삭제 — 저장소 설정 `Automatically delete head branches` ON (PR 페이지에서 복구 가능)
- 다음 작업은 최신 develop에서 같은 이름으로 다시 생성

---

## 2. 커밋 컨벤션

```
<이모지> <Type>: <제목>
```

| 이모지 | 타입 | 설명 |
| --- | --- | --- |
| ✨ | Feat | 새 기능 (엔드포인트·동작 추가) |
| 🐛 | Fix | 버그 수정 (의도와 다른 동작 교정) |
| ♻️ | Refactor | 동작 변화 없는 정리 (파일 이동·삭제·이름 변경 포함) |
| 🔧 | Settings | 설정·의존성·CI (build.gradle, yml, workflow, 초기 세팅) |
| 📝 | Docs | 문서·주석 (README 포함) |

**판정 기준**: 동작이 새로 생기면 Feat, 틀린 게 고쳐지면 Fix, 동작이 안 변하면 Refactor, 코드가 아니면 Settings/Docs

```
✨ Feat: 로그인 API 추가
🐛 Fix: 토큰 만료 시 401 응답 오류 수정
```

**커밋 단위**

- **1커밋 = 1엔드포인트** (또는 1수정) — `wip`, `수정` 같은 뭉치 커밋 금지
- Merge/Revert 커밋은 깃허브 버튼이 자동 생성 — 손으로 쓰지 않음

---

## 3. PR 규칙

제목: `<이모지> <타입>: <작업 내용>`

```
✨ Feat: 회원가입 API 구현
```

- PR 생성 → 코드래빗 리뷰 확인/반영 → 본인 Approve → 머지
- Self-merge 허용

---

## 4. 패키지 구조 (멋사 실습 코드 구조 스타일)

```
com.meisterbear
├── MeisterBearApplication.java
├── global
│   ├── common
│   │   ├── BaseResponse.java
│   │   └── BaseTimeEntity.java
│   ├── config
│   │   ├── SecurityConfig.java
│   │   ├── SwaggerConfig.java
│   │   └── CorsConfig.java
│   └── exception
│       ├── CustomException.java
│       ├── GlobalErrorCode.java
│       ├── GlobalExceptionHandler.java
│       └── model/BaseErrorCode.java
├── security
│   ├── JwtProvider.java
│   ├── JwtAuthenticationFilter.java
│   ├── CustomUserDetails.java
│   └── CustomUserDetailsService.java
└── domain
    ├── auth
    ├── user
    ├── product
    ├── character
    ├── story
    ├── charm
    ├── inspection
    ├── chat
    └── admin
```

**도메인 내부 구조**

```
{domain}/
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
│   ├── request/
│   └── response/
└── exception/{Domain}ErrorCode.java
```

---

## 5. 네이밍

| 대상 | 규칙 | 예시 |
| --- | --- | --- |
| Controller | `{도메인}Controller` | `UserController` |
| Service | `{도메인}Service` | `UserService` |
| Repository | `{Entity}Repository` | `UserRepository` |
| Entity | 단수 명사 | `User`, `Product` |
| Request DTO | `{동작}{도메인}Request` | `CreateProductRequest` |
| Response DTO | `{도메인}Response` | `ProductResponse` |
| ErrorCode | `{도메인}ErrorCode` | `ProductErrorCode` |
| 조회 메서드 | `find~` / `get~` | `findById` |
| 생성/수정/삭제 | `create~` / `update~` / `delete~` | `createProduct` |

**규칙**

- Entity `Setter` 금지 → `@Builder` + 도메인 메서드로 상태 변경
- 공통 시간 필드 필요한 Entity는 `BaseTimeEntity` 상속
- DTO 필드마다 `@Schema(description="", example="")` 작성
- Service 조회: `@Transactional(readOnly = true)`, 쓰기: `@Transactional`
- 로그: `log.info("[UserService] 유저 조회 완료 - userId={}", id)` — 서비스명 접두어 + key=value 포맷

---

## 6. 공통 응답 포맷

```java
@Getter
@AllArgsConstructor
public class BaseResponse<T> {
    private boolean success;
    private Object code;
    private String message;
    private T data;

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(true, 200, "요청이 성공적으로 처리되었습니다.", data);
    }
    public static <T> BaseResponse<T> error(String code, String message) {
        return new BaseResponse<>(false, code, message, null);
    }
}
```

**성공**

```json
{ "success": true, "code": 200, "message": "요청이 성공적으로 처리되었습니다.", "data": {} }
```

**실패**

```json
{ "success": false, "code": "PROD404", "message": "해당 상품을 찾을 수 없습니다.", "data": null }
```

**에러코드 포맷**: `{도메인 대문자}{HTTP 상태코드}` → `PROD404`, `AUTH401`, `CHARM409`

```java
public enum ProductErrorCode implements BaseErrorCode {
    PRODUCT_NOT_FOUND("PROD404", "해당 상품을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
}
```

> `{도메인}{HTTP상태코드}`로 통일한 것

**GlobalExceptionHandler 처리 대상**

- `CustomException` — 도메인별 `{Domain}ErrorCode` 기반
- `MethodArgumentNotValidException` — `@Valid` 검증 실패
- `HttpMessageNotReadableException` — Request Body 형식 오류
- `ConstraintViolationException` — PathVariable/RequestParam 검증 실패
- `Exception` — 예상치 못한 서버 오류 (fallback)

리스트 응답 페이지네이션이 필요한 엔드포인트는 `data` 안에 `content` + `pageInfo`로 감싼다 (API 스펙 문서 기존 결정 유지):

```json
"data": { "content": [], "pageInfo": { "page": 0, "size": 20, "totalElements": 132, "totalPages": 7 } }
```

---

## 7. 시큐리티

| 항목 | 값 |
| --- | --- |
| 소셜 로그인 | 카카오 단독 (자체 회원가입 없음) |
| Access Token 만료 | 60분 |
| Refresh Token 만료 | 14일 |
| Refresh Token 저장소 | DB (`user.refresh_token` 컬럼) |
| 라이브러리 | `io.jsonwebtoken:jjwt` |

- 로그아웃/탈퇴 시 `refresh_token` null 초기화
- 재발급 시 Refresh Token rotate 후 DB 갱신

**필터 체인**

```java
.csrf(csrf -> csrf.disable())
.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
.authorizeHttpRequests(auth -> auth
        .requestMatchers("/error").permitAll()
        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
        .requestMatchers("/api/auth/**").permitAll()
        .requestMatchers("/api/admin/**").hasRole("ADMIN")
        .anyRequest().authenticated())
.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
```

- CORS는 `CorsConfig` 그대로: `cors.allowed-origins` 프로퍼티화, `Authorization` 헤더 노출, `allowCredentials(true)`.

---

## 8. Swagger

- 의존성: `springdoc-openapi-starter-webmvc-ui:2.8.1`
- 접근: `/swagger-ui/index.html`
- `prod` 환경에서 비활성화

```yaml
# application-prod.yml
springdoc:
  swagger-ui.enabled: false
  api-docs.enabled: false
```

- `SwaggerConfig`의 `bearerAuth` SecurityScheme 등록 (Swagger UI `Authorize` 버튼으로 JWT 테스트), `Info.title`만 프로젝트명으로 교체.

---

## 9. CI/CD (GitHub Actions)

| 트리거 | 동작 |
| --- | --- |
| PR → develop | 빌드 체크 (테스트 제외) |
| 머지 → main | 운영 서버 자동 배포 |

```yaml
# .github/workflows/build.yml
name: Build Check
on:
  pull_request:
    branches: [ develop ]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - run: chmod +x gradlew
      - run: ./gradlew build -x test
```

- 빌드 실패 시 머지 불가 (브랜치 보호 규칙 설정)
- 민감 정보는 GitHub Secrets 등록 후 `${ENV_VAR}` 주입

---

## 10. 코드래빗

```yaml
# .coderabbit.yaml
language: "ko-KR"
reviews:
  profile: "chill"
  request_changes_workflow: false
  high_level_summary: true
  auto_review:
    enabled: true
    drafts: false
  path_filters:
    - "!**/*.md"
    - "!.github/**"
chat:
  auto_reply: true
```

- `request_changes_workflow: false`로 코드래빗 리뷰가 머지를 블로킹하지 않게 설정
- (참고용 리뷰, self-merge 허용 정책과 일치).

---

## 11. 환경 변수

```
application.yml         # 공통
application-dev.yml     # 개발 (Swagger ON, 로컬 DB)
application-prod.yml    # 운영 (Swagger OFF, 운영 DB)
```

- 민감 정보(`JWT_SECRET`, `DB_PASSWORD`, `KAKAO_CLIENT_ID` 등)는 GitHub Secrets 등록
- `application-prod.yml`, `.env` 파일은 `.gitignore` 등록 필수
- `jwt.secret` 평문 커밋 금지 → `${JWT_SECRET}` 형태로만
