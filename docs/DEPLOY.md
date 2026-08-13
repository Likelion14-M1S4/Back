# 배포 가이드 (Dev 서버)

프론트엔드 언블록용 서버 배포 문서. **EC2에 JDK·MySQL·Nginx를 네이티브로 설치**하고, 앱은 **systemd 서비스**로 돌린다. 컨벤션 §9대로 **`main`에 머지되면** GitHub Actions가 jar를 빌드해 EC2로 전송하고 서비스를 재기동한다.

## 구성 요약

```
개발자 ──PR──▶ develop ──승격 PR/머지──▶ main
                                          │ (GitHub Actions: deploy.yml)
                                          ▼
                              EC2 (Ubuntu, t3.micro)
                              ├─ nginx (:443 HTTPS → localhost:8080)   ※ certbot
                              ├─ meisterbear.service (systemd, java -jar)
                              └─ MySQL (네이티브, DB: meisterbear)
```

- **DB**: EC2에 네이티브 MySQL. 프로필 `dev`(`ddl-auto: update`)라 테이블 자동 생성.
- **프로세스 관리**: systemd (`Restart=always`) → 크래시/재부팅 시 자동 재시작. (nohup 아님)
- **jar 빌드**: GitHub Actions에서 수행, EC2로는 완성된 jar만 전송.

---

## 1. EC2 생성 (처음 1회)

1. **AMI**: Ubuntu Server 22.04 / 24.04 LTS
2. **인스턴스 유형**: `t3.micro` (프리티어)
3. **키 페어**: 새로 생성 → `.pem` 다운로드 (Secret `EC2_SSH_KEY`에 사용)
4. **보안그룹 인바운드**: `22`(내 IP), `80`(0.0.0.0/0), `443`(0.0.0.0/0)
   - (초기 테스트용으로 `8080`을 잠깐 열어 `http://IP:8080`로 확인 후 닫아도 됨)
5. **고급 세부 정보 > 사용자 데이터**: `scripts/ec2-userdata.sh` 내용 전체 붙여넣기
   → JDK21 + MySQL + Nginx + Certbot + swap(2GB) 자동 설치 (1~3분 소요)
6. **Elastic IP 할당 후 이 인스턴스에 연결** ⭐
   → 중지/재시작해도 IP가 안 바뀜(도메인 A레코드 유지). 실행 중엔 무료.

---

## 2. MySQL 초기 설정 (처음 1회, SSH 접속 후)

```bash
ssh -i key.pem ubuntu@<Elastic-IP>
sudo mysql
```
```sql
CREATE DATABASE meisterbear CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'meisterbear'@'localhost' IDENTIFIED WITH caching_sha2_password BY '앱_DB_비밀번호';
GRANT ALL PRIVILEGES ON meisterbear.* TO 'meisterbear'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```
> 여기서 정한 `앱_DB_비밀번호`를 Secret `DB_PASSWORD`, 유저명 `meisterbear`를 `DB_USERNAME`에 넣는다.

---

## 3. GitHub Secrets 등록 (처음 1회)

`Settings → Secrets and variables → Actions → New repository secret`

| Secret | 값 |
| --- | --- |
| `EC2_HOST` | Elastic IP |
| `EC2_USERNAME` | `ubuntu` |
| `EC2_SSH_KEY` | `.pem` 파일 **전체 내용** (`-----BEGIN...` 포함) |
| `DB_USERNAME` | `meisterbear` |
| `DB_PASSWORD` | 2번에서 정한 DB 비밀번호 |
| `JWT_SECRET` | 랜덤 문자열 **32자 이상** |
| `CORS_ALLOWED_ORIGINS` | 프론트 origin들 (예: `http://localhost:5173,https://front.도메인`) |
| `KAKAO_CLIENT_ID` / `KAKAO_REDIRECT_URI` | 카카오 쓰면, 아니면 빈 값 |

> ⚠️ `DB_PASSWORD`·`JWT_SECRET`엔 `$`, `"`, 공백 같은 특수문자를 피한다(영문+숫자+`-_` 권장).

---

## 4. 첫 배포

- **자동**: `develop`→`main` PR 머지 → `deploy.yml` 실행 (빌드 → jar/유닛/.env 전송 → `systemctl restart`)
- **수동**: `Actions → Deploy → Run workflow`

배포 성공 후 앱 상태 확인(SSH):
```bash
sudo systemctl status meisterbear      # active (running) 확인
journalctl -u meisterbear -f           # 앱 로그 실시간
```
초기 테스트로 8080을 열어뒀다면: `http://<Elastic-IP>:8080/swagger-ui/index.html`

---

## 5. 도메인(DNS) + HTTPS (가비아)

프론트가 https로 배포되면 http API 호출이 차단(mixed content)되므로 HTTPS를 붙인다.

**(1) 가비아 DNS A레코드**
- My가비아 → 도메인 → DNS 관리 → 레코드 추가
- **타입: `A`, 호스트: `api`, 값: Elastic IP, TTL: 600**
- 전파(수분) 대기 → `api.내도메인`이 IP로 찍히는지 확인

**(2) Nginx 리버스 프록시 배치** (SSH)
```bash
# 저장소의 deploy/nginx-meisterbear.conf 를 서버로 복사했다고 가정 (또는 vi로 직접 작성)
sudo cp ~/app/nginx-meisterbear.conf /etc/nginx/sites-available/meisterbear   # 없으면 vi로 생성
sudo vi /etc/nginx/sites-available/meisterbear      # server_name 을 api.내도메인 으로 수정
sudo ln -sf /etc/nginx/sites-available/meisterbear /etc/nginx/sites-enabled/meisterbear
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx
```

**(3) Certbot으로 HTTPS 발급 (자동 설정 + 자동 갱신)**
```bash
sudo certbot --nginx -d api.내도메인
# 이메일 입력 → 약관 y → 도메인 확인
```
→ certbot이 nginx 설정에 443/SSL을 자동 추가하고, **자동 갱신 타이머까지 등록**한다.

**(4) 확인**: `https://api.내도메인/swagger-ui/index.html`

> `CORS_ALLOWED_ORIGINS`에 프론트의 실제 origin(로컬 + 배포 URL)을 넣고 재배포해야 프론트에서 호출된다.

---

## 6. 프론트엔드 안내

프론트는 EC2/Nginx/systemd를 몰라도 된다. 아래만 전달:
- **API Base URL**: `https://api.내도메인` (HTTPS 전이면 `http://<Elastic-IP>`)
- **API 문서**: `.../swagger-ui/index.html`
- 프론트 origin을 알려주면 `CORS_ALLOWED_ORIGINS`에 추가 → 재배포

---

## 7. 트러블슈팅

| 증상 | 확인 |
| --- | --- |
| 배포 후 502 | `sudo systemctl status meisterbear`, `journalctl -u meisterbear -e` |
| DB 연결 실패 | 2번 유저/DB 생성 확인, Secret `DB_USERNAME/DB_PASSWORD` 일치 확인 |
| 메모리 부족(OOM) | `free -h`로 swap 확인, `Xmx` 조정 |
| 앱은 뜨는데 도메인 접속 안 됨 | DNS 전파, `sudo nginx -t`, 보안그룹 80/443 |
| CORS 에러 | `CORS_ALLOWED_ORIGINS`에 프론트 origin 정확히(scheme+host+port) 추가 |
