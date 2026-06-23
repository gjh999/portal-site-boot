# 전자정부 표준프레임워크 포털 사이트 (Spring Boot + Thymeleaf)

![java](https://img.shields.io/badge/Java-17-007396?style=for-the-badge&logo=openjdk&logoColor=white)
![springboot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![thymeleaf](https://img.shields.io/badge/Thymeleaf-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white)
![krds](https://img.shields.io/badge/KRDS-디지털정부%20표준%20디자인시스템-1A4F8B?style=for-the-badge)
![egovframe](https://img.shields.io/badge/eGovFrame-5.0-1A4F8B?style=for-the-badge)
![maven](https://img.shields.io/badge/Maven-3.9.9-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)
![hsqldb](https://img.shields.io/badge/DB-HSQL%20%7C%20PostgreSQL%20%2B5-CC2927?style=for-the-badge&logo=postgresql&logoColor=white)

> 전자정부 표준프레임워크 5.0 포털사이트(`portal-site-jsp`, Spring + JSP + WAR)를 **Spring Boot + Thymeleaf(JAR)** 로 전환한 프로젝트입니다.
> JSP 원본의 전 기능을 Thymeleaf 화면으로 구현하고, **KRDS(디지털정부 표준 디자인시스템)를 전면 적용**했습니다(Bootstrap 프레임워크 제거).

---

## 프로젝트 소개

### 개요

포털 사이트의 커뮤니케이션 기능(게시판, FAQ, Q&A, 설문, 배너, 회원·권한 관리 등)을 전자정부 표준프레임워크 5.0 기반 **Spring Boot + Thymeleaf** 로 제공합니다. 전 화면을 **KRDS(디지털정부 표준 디자인시스템) 네이티브** 마크업으로 전환(190+ 템플릿)하고, Bootstrap 프레임워크는 제거했습니다. 개발용 DB는 내장 HSQL을 사용하며, 운영용으로 PostgreSQL 등 6종 DBMS의 DDL/DATA·매퍼를 제공합니다.

### 기술 스택

| 항목 | 내용 |
| :--- | :--- |
| 프레임워크 | eGovFrame 5.0 + Spring Boot 3.5 + Thymeleaf |
| 언어 / 빌드 | Java 17 / Maven 3.9.9 (eGovCI-5.0.0 내장) |
| 화면 | Thymeleaf + **공식 KRDS**(디지털정부 표준 디자인시스템, `krds.min.css`) + 호환 레이어(`krds-compat.css`) + **Pretendard GOV** 서체 (전부 로컬, CDN 미사용 / Bootstrap 프레임워크 미사용·아이콘만 유지) |
| 보안 | 세션 기반 Spring Security (`HttpSessionSecurityContextRepository`) |
| 데이터 | MyBatis / 개발=내장 HSQL(파일 영속 `.localdb`) / 운영=PostgreSQL·MySQL·Oracle·Tibero·CUBRID·Altibase |
| 패키징 | 실행 가능 JAR (`java -jar`), 포트 18080 |

---

## 빠른 시작 (Quickstart)

```bash
# 1) 빌드 (eGovCI 내장 Maven/JDK 사용)
export JAVA_HOME="/c/eGovFrame/eGovCI-5.0.0-Windows-64bit/bin/jdk-17.0.17+10"
MVN="/c/eGovFrame/eGovCI-5.0.0-Windows-64bit/bin/apache-maven-3.9.9/bin/mvn"
cd portal-site-boot
"$MVN" clean package -DskipTests

# 2) 실행 (18080 포트)
java -Dfile.encoding=UTF-8 -jar target/egovframe-boot-portal-site-5.0.0.jar --server.port=18080

# 또는 개발 모드
"$MVN" spring-boot:run -Dspring-boot.run.jvmArguments="-Dfile.encoding=UTF-8" -Dspring-boot.run.arguments=--server.port=18080
```

> Windows PowerShell에서 JAR 실행 시 인자는 배열로 전달하세요.
> `& $java @('-Dfile.encoding=UTF-8','-jar',$jar,'--server.port=18080')`

접속: **http://localhost:18080**

### 테스트 계정

| 구분 | 아이디 | 비밀번호 | 권한 |
| :--- | :--- | :--- | :--- |
| 관리자 | `admin` | `1` | ROLE_ADMIN |
| 사용자 | `user` | `1` | ROLE_USER |
| 일반회원 | `user1` | `1` | ROLE_USER |

> 비밀번호 저장 형식 = `Base64(SHA-256(id + 평문))` (`EgovFileScrty.encryptPassword`)

---

## 화면 구성

### 로그인

![login](Docs/images/login.png)

세션 기반 로그인. 로그인 후 권한(관리자/사용자)에 따라 메뉴가 노출됩니다.

### 메인 화면

![main](Docs/images/main.png)

KRDS 다크 테마 랜딩. 공지사항·자유게시판·FAQ·설문참여 요약을 한 화면에 제공합니다.
상단 네비게이션: **홈 · 소개 · 공지사항 · 갤러리 · FAQ · Q&A · 설문참여 · 관리자(관리자 전용)**

### 공지사항 / 갤러리 (게시판)

![notice](Docs/images/notice.png)

![gallery](Docs/images/gallery.png)

공통컴포넌트 게시판을 커스터마이징. 목록·상세·등록·수정·답글·삭제(논리삭제)와 첨부파일·이미지 미리보기를 지원합니다.

### FAQ / Q&A

![faq](Docs/images/faq.png)

![qna](Docs/images/qna.png)

FAQ·Q&A 조회 및 등록. 관리자는 답변 관리가 가능합니다.

### 소개 페이지

![intro](Docs/images/intro.png)

사이트 소개·연혁·조직소개·찾아오시는 길·민원 안내 등 샘플 페이지(`/EgovPageLink.do?linkIndex=N`).

### 회원관리 (관리자)

![member](Docs/images/member.png)

회원 목록·검색·등록·수정·삭제. 관리자 전용.

### 권한관리 (관리자)

![author](Docs/images/author.png)

권한·롤·그룹·사용자별 권한 관리. 관리자 전용.

### 설문관리 (관리자)

![survey](Docs/images/survey.png)

설문(템플릿·설문지·문항·항목·응답결과) 관리. 관리자 전용.

### 배너관리 / 약관관리 (관리자)

![banner](Docs/images/banner.png)

![stplat](Docs/images/stplat.png)

배너·이용약관·개인정보처리방침 관리. 관리자 전용.

---

## 기능 모듈

| 영역 | 기능 |
| :--- | :--- |
| 메인 | 공지·갤러리·FAQ·설문참여 요약, **배너 히어로 캐러셀** 편입 |
| 게시판(cop/bbs) | 공지/갤러리/자료실 CRUD, 답글, 논리삭제, 첨부파일, 게시판마스터·사용정보·템플릿·방명록 |
| 온라인 참여(uss/olh) | FAQ, Q&A (사용자·관리자, 답변관리, **Q&A 상태 색상 배지**) |
| 설문(uss/olp) | 템플릿·설문지·문항·항목·응답·참여 6종 — **모달 문항등록(객관식/서술형)·응답 렌더·검색/필터·참여완료 상태** |
| 약관/방침(uss/sam) | 이용약관·개인정보처리방침(**등록 시 유형 구분**) |
| 회원(uss/umt) | 회원관리, 가입, 비밀번호 변경, **직업유형(OCCP_TY) 필수** |
| 보안(sec) | 권한·롤·그룹·사용자별 권한 |
| 시스템(sym) | **배너(메인/팝업/푸터)**, 우편번호, **달력(격자형)·휴일관리** |

### 이번 릴리스 주요 추가/변경

- **KRDS 전면 적용**: 190+ 템플릿을 KRDS 네이티브 마크업으로 전환, Bootstrap 프레임워크 제거(네이티브 + 호환 레이어). 페이지네이션 KRDS 통일(이전/숫자/다음), 게시판 검색폼·전체건수 정렬, Q&A 상태 색상.
- **푸터 심플형 통일**: 공식 홈페이지 배너 + 3컬럼 + 소셜 링크. 소개 페이지 히어로, 전 페이지 '맨 위로' 버튼.
- **배너 기능**: 유형 구분(메인/팝업/푸터), 캐러셀 자동전환(`portal.banner.interval` 기본 5초), 메인 히어로 슬라이드 편입, 팝업(좌표/크기/묶음 · '오늘 하루 보지 않기' 체크 즉시 닫힘 · 로그인 화면 제외), 링크 URL 선택.
- **설문 기능**: 문항(객관식/서술형) 모달 등록 + 아코디언, 응답 페이지 문항 렌더, 설문대상 '전체' 기본, 회원 직업유형 필수, 제목검색 + 회원유형 필터 + 참여완료 상태.
- **달력**: 격자형(일자 밑 휴일 표시·인쇄 시 달력 영역만), 휴일관리 이동 버튼.
- **파일 첨부**: 허용 확장자 화이트리스트 확장(txt/pdf/doc/hwp/zip 등).
- **개발 편의**: HSQL 파일 영속(`.localdb`) + `shtdb.sql` 체크섬 변경 시 자동 재시드(`Globals.localdb.resetEachStart` 토글로 매 기동 강제 초기화 가능).

---

## 데이터베이스

- **개발(기본)**: 내장 HSQL — `src/main/resources/db/shtdb.sql` (DDL+DATA, 40테이블)
- **운영**: `DATABASE/{dbms}/all_pst_ddl_{dbms}.sql`, `all_pst_data_{dbms}.sql`
  - 지원 DBMS: **PostgreSQL · MySQL · Oracle · Tibero · CUBRID · Altibase** (HSQL 포함 7종)
  - 7종 전부 명명규칙 파리티: 각 **DDL 40테이블 · 매퍼 28파일 · 레거시(LETT*/COMVN*) 0건**
- **명명규칙**: 테이블 `TB_` + snake_case 대문자, 모든 테이블 감사컬럼 4종 필수
  (`FRST_REGIST_PNTTM`, `FRST_REGISTER_ID`, `LAST_UPDT_PNTTM`, `LAST_UPDUSR_ID`)
- **DB 전환**: `Globals.DbType`(기본 hsql) + datasource 설정 변경. 매퍼는 `Egov{기능}_SQL_{dbType}.xml`.

표준 규칙 문서: `Docs/단어규칙.md`(표준단어), `Docs/도메인규칙.md`(표준도메인), `Docs/용어규칙.md`(표준용어), `Docs/db-schema-guide.md`.

---

## 프로젝트 구조

```
portal-site-boot/
├─ src/main/java/egovframework/
│  ├─ com/config/      # Java @Configuration (web.xml/context-*.xml 대체)
│  ├─ com/security/    # 세션 기반 SecurityConfig
│  ├─ com/cmm/         # 공통 VO·서비스·유틸·파일
│  └─ let/{모듈}/web   # 컨트롤러 33종 (cop·uss·sec·sym·uat·main)
├─ src/main/resources/
│  ├─ templates/       # Thymeleaf (layouts/default + fragments + 모듈별 화면)
│  ├─ static/          # 공식 KRDS 킷 + 호환 레이어 (로컬 css/js/images, CDN 미사용)
│  ├─ egovframework/mapper/  # MyBatis 매퍼 (DB타입별 _SQL_{db}.xml)
│  └─ db/shtdb.sql     # HSQL 초기 DDL+DATA
├─ DATABASE/{dbms}/    # 운영 DBMS별 DDL/DATA (6종)
├─ Docs/               # 공통표준 규칙 문서
├─ CLAUDE.md           # 코드베이스 컨텍스트/컨벤션
└─ SKILL.md            # 컨텍스트·하네스 엔지니어링 가이드
```

---

## 참고

- 상세 컨벤션·전환 핵심·함정: [CLAUDE.md](CLAUDE.md)
- 재현 가능한 작업 절차(빌드·검증·DBMS dialect): [SKILL.md](SKILL.md)
- 본 프로젝트는 표준프레임워크 공통컴포넌트 기능 일부를 선정해 구성한 참조용 소스입니다.
- 단독(standalone) 프로젝트입니다. 빌드는 공개 Maven 저장소(`maven.egovframe.go.kr`)의
  `egovframe-boot-starter-parent:5.0.0` 만 참조하며, 다른 로컬 프로젝트에 의존하지 않습니다.

---

## 기여 / 라이선스

- **라이선스**: [Apache License 2.0](LICENSE) — 자유롭게 사용·수정·재배포할 수 있습니다(eGovFramework 기반).
- **오픈소스 고지**: 번들·의존된 서드파티의 라이선스 고지는 [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md) 참조.
  주요: eGovFramework RTE·Spring·Thymeleaf·MyBatis(Apache 2.0), **KRDS**(디지털정부 표준 디자인시스템 — KRDS 이용약관),
  **Pretendard GOV**(SIL OFL 1.1), **Bootstrap Icons**·**Swiper**·**Lombok**(MIT),
  **Andrej Karpathy Guidelines** 스킬(`.claude/skills/karpathy-guidelines/` — MIT, [출처](https://github.com/multica-ai/andrej-karpathy-skills)).
- **표준프레임워크 호환**: eGovFrame 5.x (Spring Boot 3.5)
- **기여 방법**: [Docs/CONTRIBUTING.md](Docs/CONTRIBUTING.md) 및
  [Docs/CONTRIBUTE_README.md](Docs/CONTRIBUTE_README.md) 참고.
  이슈·Pull Request를 환영합니다.
- **보안 취약점 제보**: 공개 이슈가 아닌 비공개 채널로 제보해 주세요
  ([.github/security.md](.github/security.md)).

---

## 문의 (Contact)

- 이메일: **admin@jennysoft.co.kr**
- 이슈·기능 제안: GitHub Issues / Pull Request
