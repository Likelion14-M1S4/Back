# CLAUDE.md

MeisterBear 백엔드 (Spring Boot, Java 21). 코드 작성·리뷰 시 아래 규칙을 따른다.
**전체 컨벤션은 [docs/CONVENTION.md](docs/CONVENTION.md) 참고** — 아래는 자주 어기는 핵심만 압축한 것.

## 반드시 지킬 것 (위반 잦음)

- **Entity에 `@Setter` 금지.** 상태 변경은 `@Builder` + 도메인 메서드로만. 공통 시간 필드는 `BaseTimeEntity` 상속.
- **모든 응답은 `BaseResponse<T>`로 감싼다.** 성공 `BaseResponse.success(data)`, 실패는 `{Domain}ErrorCode` 기반 `CustomException` throw → `GlobalExceptionHandler`가 처리.
- **에러코드 포맷 = `{도메인 대문자}{HTTP상태코드}`** (예: `PROD404`, `AUTH401`, `CHARM409`). `{Domain}ErrorCode implements BaseErrorCode`.
- **시크릿 평문 커밋 절대 금지.** `jwt.secret` 등은 `${JWT_SECRET}` 형태로만. `application-prod.yml`, `.env`는 `.gitignore` 필수.
- **리스트+페이지네이션 응답**은 `data` 안에 `content` + `pageInfo`로 감싼다.

## 패키지 구조

`com.meisterbear` 하위: `global`(common/config/exception), `security`, `domain/{도메인}`.
도메인 내부: `controller/ service/ repository/ entity/ dto/{request,response}/ exception/{Domain}ErrorCode.java`.

## 네이밍

- `{도메인}Controller` / `{도메인}Service` / `{Entity}Repository`
- Entity: 단수 명사(`User`) · Request DTO: `{동작}{도메인}Request`(`CreateProductRequest`) · Response DTO: `{도메인}Response`
- 조회 `find~`/`get~`, 변경 `create~`/`update~`/`delete~`
- DTO 필드마다 `@Schema(description="", example="")`
- Service 조회 `@Transactional(readOnly = true)`, 쓰기 `@Transactional`
- 로그: `log.info("[UserService] 유저 조회 완료 - userId={}", id)` (서비스명 접두어 + key=value)

## 커밋 / 브랜치

- 커밋: `<이모지> <Type>: <제목>` — ✨ Feat / 🐛 Fix / ♻️ Refactor / 🔧 Settings / 📝 Docs. **1커밋 = 1엔드포인트**, `wip`·`수정` 뭉치 커밋 금지.
- 판정: 동작 새로 생기면 Feat, 틀린 것 고치면 Fix, 동작 안 변하면 Refactor, 코드 아니면 Settings/Docs.
- 브랜치: `{type}/{도메인}` (예: `feat/user`), 최신 `develop`에서 분기 → PR → develop 머지. `main`/`develop` 직접 push 지양.

## 시큐리티

카카오 소셜 로그인 단독. Access 60분 / Refresh 14일(DB `user.refresh_token`). JWT는 `io.jsonwebtoken:jjwt`. 재발급 시 Refresh rotate + DB 갱신, 로그아웃/탈퇴 시 `refresh_token` null.
