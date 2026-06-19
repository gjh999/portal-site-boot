# portal-site-boot 전용 스킬 (SKILL.md)

> eGovFrame 5.0 **포털사이트**(JSP→Spring Boot + Thymeleaf 전환본)에서 Claude Code가
> 반복 작업을 재현 가능한 절차로 수행하기 위한 **컨텍스트 엔지니어링 + 하네스 엔지니어링** 가이드.
> 최종 현행화: 2026-06-16. (상세 아키텍처·전환 이력은 같은 폴더 `CLAUDE.md` 참조)
>
> ⚠️ 이 프로젝트는 `simple-homepage`(JWT·`/login`·REST URL·이중해시)와 **인증/URL/비번 방식이 다르다.**
> simple-homepage SKILL.md를 그대로 따르지 말 것. 아래가 portal-site-boot의 정본이다.
>
> 📌 **단독 프로젝트**: 아래에 등장하는 `portal-site-jsp`·`simple-homepage`는 **별도 저장소의 외부 참조**일 뿐
> 본 저장소에 포함·의존되지 않는다. 빌드/실행에는 필요 없다. 표준 규칙 문서(단어/도메인/용어규칙)는 모두 `Docs/` 에 포함되어 있다.

---

## 0. 정체성 한눈에

| 항목 | 값 |
|------|-----|
| 유래 | `portal-site-jsp`(Spring+JSP+WAR) → Boot+Thymeleaf(JAR) |
| 기능 기준(정본) | **`../portal-site-jsp`** — JSP 화면과 동일 기능을 Thymeleaf로 구현 |
| 인증 | **세션 기반 Spring Security**(JWT 아님). `HttpSessionSecurityContextRepository` |
| URL 컨벤션 | eGov 원본 `*.do` 유지 (REST `/bbs/{id}/list` 아님) |
| 뷰 매핑 | 컨트롤러 반환 뷰명 = `templates/<뷰명>.html` (JSP 미작성) |
| 포트 / context | 8080 / `/` , 패키지 루트 `egovframework` |
| DB | 개발=내장 HSQL(`db/shtdb.sql`) / 운영=PostgreSQL(`DATABASE/postgresql/`) |
| Java / 빌드 | 17 / Maven 3.9.9 (eGovCI-5.0.0 내장) |

---

## 1. 표준 참조 순서 (신규 작업 시)

```
① CLAUDE.md            → 전체 구조·전환 핵심·함정
② ../portal-site-jsp   → 동일 기능 JSP 원본(정본). 반드시 대조
③ Docs/단어규칙.md      → 영문약어 결정 (공통표준단어 3284개)
④ Docs/도메인규칙.md    → 타입·길이 결정 (공통표준도메인 129개)
⑤ Docs/용어규칙.md      → 컬럼명·테이블명 (대용량. 분할본 Docs/용어규칙_1~4.md 동봉)
⑥ Docs/db-schema-guide.md → DB 스키마 가이드
⑦ SKILL.md             → 본 파일(절차·함정·검증 체크리스트)
```
규칙 파일에 없는 단어/도메인/용어는 **새로 만들지 말고** 먼저 규칙 파일을 확인하고, 꼭 필요하면 규칙 파일 하단에 추가 후 사용.

---

## 2. 빌드·실행 (검증된 절차)

### 2-1. 경로
```
JAVA_HOME = D:\eGovFrame\eGovCI-5.0.0-Windows-64bit\bin\jdk-17.0.17+10
MVN       = D:\eGovFrame\eGovCI-5.0.0-Windows-64bit\bin\apache-maven-3.9.9\bin\mvn.cmd
PROJECT   = d:\eGovFrame\workspace-egov\portal-site-boot
```

### 2-2. 개발 구동 (spring-boot:run) — Bash 도구
```bash
export JAVA_HOME="/c/eGovFrame/eGovCI-5.0.0-Windows-64bit/bin/jdk-17.0.17+10"
MVN="/c/eGovFrame/eGovCI-5.0.0-Windows-64bit/bin/apache-maven-3.9.9/bin/mvn"
cd "/c/eGovFrame/workspace-egov/portal-site-boot"
"$MVN" clean compile                 # 컴파일 검증
"$MVN" spring-boot:run -Dspring-boot.run.jvmArguments="-Dfile.encoding=UTF-8"
```

### 2-3. 배포 구동 (패키지 JAR) — PowerShell  ⚠️ 인자 파싱 함정 주의
```powershell
# 1) 빌드
& "$MVN" -f "d:\eGovFrame\workspace-egov\portal-site-boot\pom.xml" clean package -DskipTests
# 2) 구동 — 반드시 인자를 '배열'로 전달 (문자열로 -Dfile.encoding=UTF-8 붙이면 PowerShell이
#    '.encoding'을 잘못 잘라 'ClassNotFoundException: /encoding=UTF-8' 발생)
$java = "D:\eGovFrame\eGovCI-5.0.0-Windows-64bit\bin\jdk-17.0.17+10\bin\java.exe"
$jar  = "d:\eGovFrame\workspace-egov\portal-site-boot\target\egovframe-boot-portal-site-5.0.0.jar"
$a = @('-Dfile.encoding=UTF-8','-jar',$jar)
& $java $a > "d:\eGovFrame\workspace-egov\portal-site-boot\target\bootrun.log" 2>&1
```

### 2-4. 포트 충돌 종료 (PowerShell) — TIME_WAIT(Idle PID 0) 회피
```powershell
$c = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
foreach($x in $c){ if($x.OwningProcess -gt 0){ try{ Stop-Process -Id $x.OwningProcess -Force }catch{} } }
```

### 2-5. 기동 확인 / 로그 (UTF-16 BOM 로그 → tr 로 NUL 제거 후 grep)
```bash
log="d:/eGovFrame/workspace-egov/portal-site-boot/target/bootrun.log"
tr -d '\000' < "$log" | grep -aE "Started EgovBootApplication|APPLICATION FAILED"
```

> spring-boot:run 은 `target/classes` 리소스를 사용. 가동 중 템플릿 수정 시
> `cp -r src/main/resources/templates/. target/classes/templates/` 동기화 또는 재기동.

---

## 3. 테스트 계정 / 비밀번호 (⚠️ simple-homepage와 다름 — 단일 해시)

| 계정 | 구분 | ID | 비밀번호 | 권한 |
|------|------|-----|---------|------|
| 관리자 | 업무사용자(USR) | `admin` | `1` | ROLE_ADMIN |
| 사용자 | 업무사용자(USR) | `user` | `1` | ROLE_USER |
| 일반회원 | 일반회원(GNR) | `user1` | `1` | ROLE_USER |

```
저장형식 = Base64(SHA-256(id + 평문))   ← 단일 해시 (simple-homepage의 '이중해시' 아님)
생성:  EgovFileScrty.encryptPassword(pw, id)
```
로그인 처리: `POST /uat/uia/actionSecurityLogin.do` (파라미터 `id`,`password`,`userSe`; userSe 미지정 시 USR).
권한 결정(데모 단순화): `admin`만 ROLE_ADMIN, 그 외 ROLE_USER.

---

## 4. 보안 / 접근 권한 (SecurityConfig.java — 세션 기반)

- `csrf` 비활성화(서버렌더+세션 데모), `formLogin/httpBasic/logout` 비활성(로그인/로그아웃은 EgovLoginController).
- 미인증/권한부족 → `/uat/uia/egovLoginUsr.do` 로 이동.
- **PERMIT_ALL**: `/`, `/cmm/main/**`, `/cmm/forwardPage.do`, `/sym/mms/**`, `/uat/uia/**`,
  `/EgovPageLink.do`, `/validator.do`, 정적자원(`/css,/js,/images,/fonts,/static`), swagger.
- **ADMIN_ONLY (ROLE_ADMIN)**: `/sec/**`, `/cop/com/**`, `/uss/olh/**/admin/**`, `/uss/ion/bnr/**`,
  설문 관리 `/uss/olp/{qtm,qmc,qqm,qim,qrm}/**`, 시스템 `/sym/**`.
- **그 외 anyRequest = permitAll**: 목록/상세 등 조회는 공개. **쓰기 동작은 각 컨트롤러의
  `EgovUserDetailsHelper.isAuthenticated()` 검사가 보호**(JSP 원본 정책 그대로).
- 설문 참여(`/uss/olp/qnn/**`, `/uss/olp/qri/**`)는 관리자 전용 아님(로그인 사용자 참여).

---

## 5. 뷰(Thymeleaf) 규칙 ★ 배포 핵심

### 5-1. 뷰 이름은 **앞 슬래시 없이** (가장 중요)
```java
return "uss/olh/faq/EgovFaqListInqire";   // ✅ 올바름
return "/uss/olh/faq/EgovFaqListInqire";  // ❌ 금지
String sLocationUrl = "uss/olp/qmc/EgovQustnrManageDetail";  // ✅
```
- 이유: prefix `classpath:/templates/` + `/uss/..` = `templates//uss/..`(이중 슬래시).
  `spring-boot:run`(파일시스템)은 OS가 `//` 정규화 → 통과하지만, **패키지 JAR(클래스패스
  리소스)는 정규화 안 함 → 템플릿 미해결 Whitelabel 500**. 개발 중엔 안 보이고 배포 때 터진다.
- `redirect:` / `forward:` / `.do` URL 만 `/`로 시작(절대경로 필요). 뷰 이름은 절대 `/`로 시작 금지.

### 5-2. 위치·레이아웃
```
templates/<컨트롤러 반환 뷰명>.html
templates/layouts/default.html         (Thymeleaf Layout Dialect, layout:decorate)
templates/fragments/{header,nav,footer,pagination}.html
정적자원: static/css|js|images  (KRDS + Bootstrap5, 전부 로컬. CDN 금지)
```

### 5-3. EgovMap 키 케이싱 함정 (egovMap 결과 출력 시)
- 별칭에 **언더스코어 있으면** 카멜케이스: `QESTN_SJ`→`qestnSj`.
- 별칭에 **언더스코어 없으면** HSQL 대문자화 → **전부 소문자**: `indvdlInfoId`(별칭)→`indvdlinfoid`, `INQIRECO`→`inqireco`.
- **NULL 컬럼**은 `mapper-config.xml`의 `callSettersOnNulls=true` 덕에 키는 보존되나 값은 null →
  `${item.key}`는 빈 문자열 렌더(이 설정 없으면 `SpelEvaluationException EL1008E: ... cannot be found` 500).

---

## 6. DB 스키마 표준

### 6-1. 명명 / 감사컬럼
```
테이블 = TB_ + 기능명 (대문자 스네이크케이스). 구 LETT*/LETTC*/COMVN* 폐기.
모든 테이블 감사컬럼 4종 필수:
  FRST_REGIST_PNTTM TIMESTAMP / FRST_REGISTER_ID VARCHAR(20) /
  LAST_UPDT_PNTTM   TIMESTAMP / LAST_UPDUSR_ID    VARCHAR(20)
```

### 6-2. 주요 테이블 매핑 (구 → 신)
| 구 | 신 | 구 | 신 |
|---|---|---|---|
| LETTNBBS | TB_BBS | LETTNFAQINFO | TB_FAQ_INFO |
| LETTNBBSMASTER | TB_BBS_MASTER | LETTNQAINFO | TB_QA_INFO |
| LETTNBBSUSE | TB_BBS_USE | LETTNGNRLMBER | TB_GNRL_MBER |
| LETTNEMPLYRINFO | TB_EMPLYR_INFO | LETTNTMPLATINFO | TB_TMPLAT_INFO |
| LETTCCMMN*CODE | TB_CMMN_*_CODE | COMVNUSERMASTER(VIEW) | VW_USER_MASTER |

### 6-3. 신규 테이블/컬럼 체크리스트
- [ ] TB_ + 스네이크케이스, PK, FK 명명(FK_대상_참조)
- [ ] 감사컬럼 4종 포함
- [ ] `db/shtdb.sql`(HSQL) + `DATABASE/postgresql/`(운영) 갱신
- [ ] `Docs/db-schema-guide.md` 갱신
- [ ] 단어/도메인/용어 규칙 준수

### 6-4. ⚠️ 파괴적 변경 전 백업 (전역 필수 룰)
`DROP TABLE/COLUMN`·`TRUNCATE`·`DELETE` 등 데이터 손실 변경 **전에 반드시** `db-safe-migrate`
스킬로 복구용 SQL 덤프 백업 후 진행(전역 `~/.claude/CLAUDE.md` 룰). PreToolUse 훅이 해당 SQL을
감지해 실행 전 확인을 요구한다. 비파괴(ADD COLUMN/CREATE TABLE)는 백업 불필요.

---

## 7. MyBatis 매퍼 표준
```
위치: src/main/resources/egovframework/mapper/let/{모듈}/Egov{기능}_SQL_{dbType}.xml
dbType = Globals.DbType (기본 hsql)
mapper-config.xml 설정: mapUnderscoreToCamelCase=true, jdbcTypeForNull=VARCHAR, callSettersOnNulls=true
```
MySQL→HSQL 변환: `DATE_FORMAT(c,'%Y-%m-%d')`→`TO_CHAR(c,'YYYY-MM-DD')`, `IFNULL`→`COALESCE`,
테이블명 `LETT*`→`TB_*`. `LIMIT n OFFSET m` 은 HSQL 호환.

### 7-1. 전 DBMS 명명규칙 현행화 완료 (2026-06-16) ★
7개 DB(hsql·postgresql·mysql·oracle·tibero·cubrid·altibase) **완전 파리티**: 각 DDL 40테이블,
매퍼 28파일, 레거시(LETT*/COMVN*) 참조 0건, 매퍼 XML 197개 well-formed.
- **HSQL = 정본**(개발·검증 DB). 신규 매퍼/DDL/시드는 HSQL(`shtdb.sql`,`*_SQL_hsql.xml`)에서 시작.
- 비-HSQL DB는 HSQL 정본 기준으로 **테이블명 치환(레거시→TB_) + Boot포트 형상 + 누락분 보강**으로 정렬.
  단, **각 DB의 native dialect는 그 DB의 기존 매퍼를 모방**해 유지(아래 표). 실제 DB서버가 없어
  HSQL/PG경로만 런타임 검증됨 — mysql/oracle/tibero/cubrid/altibase는 파일 정합성·XML well-formed 검증.
- DBMS별 dialect 변환 규칙(HSQL → 대상):

| 구문 | PostgreSQL | MySQL | Oracle/Tibero | CUBRID | Altibase |
|------|-----------|-------|---------------|--------|----------|
| `sysdate()` | `now()` | `NOW()` | `SYSDATE` | `SYS_DATETIME`(SYSDATETIME) | `SYSDATE` |
| `TO_CHAR(x,'YYYY-MM-DD')` | 유지 | `DATE_FORMAT(x,'%Y-%m-%d')` | 유지 | 유지 | 유지 |
| `a \|\| b` 연결 | 유지 | `CONCAT(a,b)` | 유지(3인자는 `\|\|`) | 유지 | 유지(3인자 `\|\|`) |
| `LIMIT n OFFSET m` | 유지 | 유지 | ROWNUM 서브쿼리 | 유지 | ROWNUM 서브쿼리 |
| `COALESCE`/`SUBSTRING` | 유지 | 유지 | `NVL`/`SUBSTR` | 유지 | `NVL`/`SUBSTR` |
| 타입(DDL) | TEXT(←LONGVARCHAR) | decimal/datetime | VARCHAR2/NUMBER/DATE/CLOB | STRING/DATETIME | varchar2/number/DATE |

- 운영 배포는 `Globals.DbType` + datasource 설정 변경. DDL/DATA는 `DATABASE/{db}/all_pst_{ddl,data}_{db}.sql`.
- 주의: `TB_CMMNTY_USER`/`TB_CLUB_USER`(동호회/커뮤니티 게시판 타게팅)는 전 DB에서 미생성(미사용 레거시
  기능, HSQL 포함). 휴일구분 공통코드(COM017/RES00x)는 시드 미포함 → 휴일 목록의 구분명은 빈 값.

---

## 8. 컨트롤러 검증 함정 (테스트 시 — 앱 버그 아님, JSP 원본 제약 충실 재현)

- **멀티파트 컨트롤러**(FAQ·게시판·배너 insert): 폼은 `multipart/form-data`로 제출해야 함.
  urlencoded 로 보내면 `IllegalStateException: not MultipartHttpServletRequest`.
- **`@EgovNullCheck` 필드**: 폼에 (hidden이라도) 값 제공해야 insert 도달. 누락 시
  `bindingResult.hasErrors()`로 폼만 재렌더(HTTP 200)되고 insert 미실행.
- **QnA 작성자명 `wrterNm` = `@Size(max=4)`**(원본 동일) → 5자↑ 검증 실패. 한글 3자 등 OK.
- **회원등록 `groupId`** = `@EgovNullCheck` 필수 + 유효 FK. 시드 그룹 `GROUP_00000000000000`/`...0001`.
- **회원 수정뷰** 파라미터명 `selectedId`(mberId 아님), **QnA 상세**는 `passwordConfirmAt` 필수.
- 상세/수정 화면을 ID 파라미터 없이 직접 호출하면 400/`EgovBizException` → 정상(목록 링크로 진입해야 함).

---

## 9. 검증·스모크 절차 (재현용)

```bash
# 로그인 쿠키 확보 후 화면 크롤 (curl.exe, CSRF off)
B="http://localhost:8080"; J="target/cj.txt"
curl.exe -s -c "$J" "$B/uat/uia/egovLoginUsr.do" -o /dev/null
curl.exe -s -b "$J" -c "$J" -d 'id=admin&password=1&userSe=USR' "$B/uat/uia/actionSecurityLogin.do" -o /dev/null
# 각 화면 GET → 200/302 면 정상. 멀티파트 등록은 -F 로 multipart 전송.
```
- 전 컨트롤러 화면 엔드포인트(목록/등록폼/상세) HTTP 200/302 + `bootrun.log`에 THYMELEAF/SpelEvaluation
  ERROR 0건이면 통과. 검증 끝나면 `target/*.ps1`,`target/cj.txt` 등 임시파일 정리.
- 한글 값을 PowerShell 스크립트 파일에 쓰면 인코딩 깨짐 → curl `--data-urlencode` 또는 ASCII 스탬프 사용.

---

## 10. 사용 가능한 스킬 / 하네스 (현행)

| 종류 | 항목 | 용도 |
|------|------|------|
| 스킬(전역) | `db-safe-migrate` | 파괴적 DB 변경 전 복구 덤프 백업 (§6-4) |
| 스킬(전역) | `gemini-imagegen` | 이미지 생성 (포털에선 보통 불필요) |
| 플러그인 | `andrej-karpathy-skills` | 코딩 가이드라인 |
| 훅(PreToolUse) | Bash/PowerShell DROP·DELETE·TRUNCATE 감지 | 위험 SQL 실행 전 확인 |
| 설정 | model=opus[1m], effortLevel=high, defaultMode=acceptEdits | `~/.claude/settings.json` |
| 전역 룰 | `~/.claude/CLAUDE.md` | DB 파괴변경 백업 필수 |

---

## 11. 작업 규칙 (자율 진행)
1. 모든 진행상태·설명·요약은 **한글**.
2. 묻지 않고 처음부터 끝까지 **완전 자율** 진행, 막히면 합리적 판단으로 계속.
3. 완료 후 **결과만 요약** 보고.
4. **수정 금지(변경 필요 시 보고)**: `pom.xml`, `SecurityConfig.java`, `application*.properties`,
   `db/shtdb.sql`은 신중히. 파괴적 DB변경은 §6-4 백업 선행.
5. 빌드는 컴파일→(필요시)패키지, 구동 후 스모크까지 확인하고 임시파일 정리.

---

## 12. 변경 이력
| 날짜 | 작업 |
|------|------|
| 2026-06-03 | JSP→Boot 전환, 게시판 CRUD 검증, 세션 기반 Security 정착 |
| 2026-06-14 | 전 모듈(33 컨트롤러) 포팅·검증: 25화면 200, CRUD 9종 영속. `callSettersOnNulls=true` 추가(egovMap NULL키 500 해소), 약관 시드 ID 13자리 교정 |
| 2026-06-15 | 배포 핵심 결함 수정: 뷰 이름 앞 슬래시 제거(return 126 + 변수할당 21건) → 패키지 JAR Whitelabel 500 전수 해소. JAR 구동 인자 배열 전달 확립 |
| 2026-06-16 | SKILL.md 최초 작성(현행화). portal-site-boot 정본 컨텍스트/하네스 가이드 정립 |
