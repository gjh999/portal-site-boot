# eGovFrame 5.0 포털사이트 (portal-site-boot) — Claude 컨텍스트

## 프로젝트 개요

- **프레임워크**: 전자정부표준프레임워크(eGovFrame) 5.0 + **Spring Boot 3.5.6**(Spring 6.2.11) + **Thymeleaf**
- **유래**: `portal-site-jsp`(Spring + JSP + WAR) → Spring Boot + Thymeleaf(JAR) 전환본
  (`portal-site-jsp`·`simple-homepage`는 별도 저장소의 외부 참조이며, 본 저장소는 이에 의존하지 않는 **단독 프로젝트**다.)
- **Java**: 17 / **빌드**: Maven 3.9.9 (eGovCI-5.0.0-Windows-64bit 내장)
- **DB**: 개발=내장 HSQL(`db/shtdb.sql`) / 운영=PostgreSQL(`DATABASE/postgresql/`)
- **포트**: 18080(`server.port`), context-path: `/`, 패키지 루트: `egovframework`

## 빌드·실행

```bash
export JAVA_HOME="/c/eGovFrame/eGovCI-5.0.0-Windows-64bit/bin/jdk-17.0.17+10"
MVN="/c/eGovFrame/eGovCI-5.0.0-Windows-64bit/bin/apache-maven-3.9.9/bin/mvn"
cd "/c/eGovFrame/workspace-egov/portal-site-boot"
"$MVN" clean compile                 # 컴파일 검증
"$MVN" spring-boot:run -Dspring-boot.run.jvmArguments="-Dfile.encoding=UTF-8"
```
포트 충돌 시(PowerShell): `Get-NetTCPConnection -LocalPort 18080 | %{ Stop-Process -Id $_.OwningProcess -Force }`

> 주의: `spring-boot:run`은 `target/classes`의 리소스를 사용한다. 서버 가동 중 템플릿을
> 수정하면 `cp -r src/main/resources/templates/. target/classes/templates/` 로 동기화하거나 재기동한다.

## 테스트 계정

| 계정 | 구분 | ID | 비밀번호 | 권한 |
|------|------|-----|---------|------|
| 관리자 | 업무사용자(USR) | `admin` | `1` | ROLE_ADMIN |
| 사용자 | 업무사용자(USR) | `user` | `1` | ROLE_USER |
| 일반회원 | 일반회원(GNR) | `user1` | `1` | ROLE_USER |

> 비밀번호 = `Base64(SHA-256(id + 평문비밀번호))` (`EgovFileScrty.encryptPassword(pw, id)`)
> 위 3계정은 모두 상태 `P`(승인) 시드라 즉시 로그인. **셀프 가입 신규 계정은 `A`(승인대기)** 라 관리자 승인 전 로그인 불가(아래 사용자·회원 모델).

## 사용자·회원 모델 (구분·가입·승인·권한) — 신규/변경 시 **반드시 준수** (2026-06-24 통일)

### 사용자 구분 (USER_SE) — 3유형, ESNTL_ID 통합
| 구분 | USER_SE | 테이블 | 상태컬럼 | 가입경로 |
|------|---------|--------|----------|----------|
| 업무사용자 | `USR` | `TB_EMPLYR_INFO` | `EMPLYR_STTUS_CODE` | 관리자 직접 등록 |
| 일반회원 | `GNR` | `TB_GNRL_MBER` | `MBER_STTUS` | 셀프 가입 |
| 기업회원 | `ENT` | `TB_ENTRPRS_MBER` | `ENTRPRS_MBER_STTUS` | 셀프 가입 |

- **통합 뷰**: `VW_USER_MASTER`가 위 3테이블을 `UNION ALL`(컬럼: ESNTL_ID·USER_ID·PASSWORD·USER_NM·USER_ZIP·USER_ADRES·USER_EMAIL·GROUP_ID·USER_SE·ORGNZT_ID·USER_OCCP). GNR/ENT는 GROUP_ID·ORGNZT_ID를 공백(`' '`)으로, USER_SE는 각각 리터럴(`'GNR'`/`'USR'`/`'ENT'`)로 둔다. 고유식별은 `ESNTL_ID`로 통일.
- ⚠️ **두 가지 코드 체계 구분**: 뷰·로그인·마이페이지는 **USER_SE(GNR/USR/ENT)**, 회원관리 화면 내부(목록 컬럼/검색 필터/승인 액션 키)는 **userTy(USR01=일반·USR02=기업·USR03=업무사용자)**. 혼동 금지.

### 가입 경로
- 일반·기업 = 셀프 가입. 진입점 `EgovMberSbscrbSelect`(`cmm/uss/umt`)에서 유형 선택 → 일반(`EgovMberSbscrb`)/기업(`EgovEntrprsMberSbscrb`) 폼.
- 업무사용자 = 셀프 경로 없음, 관리자가 회원관리에서 등록.

### 승인 정책 (가입→승인→로그인) — 이번 세션 통일
- **셀프 가입(일반·기업)은 상태 `A`(가입신청=승인대기)로 저장.** GNR: `EgovMberManageController` 가입 핸들러 `setMberSttus("A")`. ENT: `EgovEntrprsMberManageController` `setEntrprsMberSttus("A")`(과거 `P` 즉시로그인 → `A`로 통일).
- **로그인은 상태 `P`(승인)만 허용.** 로그인 매퍼 `EgovLoginUsr_SQL_{db}.xml`가 3유형 모두 상태컬럼 `= 'P'` 조건 강제 → `A`는 자동 차단(별도 분기 불필요).
- **관리자 승인(A→P)**: 회원관리 목록에서 신청중(`A`) 선택 → 승인. `MberManageDAO.approveMber(uniqId, userTy)`가 userTy(USR01/02/03)별로 해당 테이블 상태컬럼을 `'P'`로 갱신. 승인 대상 파라미터는 `"userTy:uniqId"` 콤마 구분 문자열.
- 업무사용자는 관리자 등록 시 `P`.
- 상태 라벨: `A`='신청중', `P`='승인', `D`='삭제'.
- ⚠️ test 계정이 "가입됐는데 로그인 안 됨"은 승인대기(`A`) **정상 동작**.

### 권한그룹(groupId) — 권한상승 방지
- `GROUP_00000000000000`=ROLE_ADMIN, `GROUP_00000000000001`=ROLE_USER.
- **일반회원 셀프가입은 ROLE_USER 고정**: 가입 핸들러가 `setGroupId("GROUP_00000000000001")`·`setUserTy("USR01")`. (과거 ROLE_ADMIN 부여 버그 제거.)
- **업무사용자(USR)만 ROLE_ADMIN 지정 가능**: 등록/수정 폼(`EgovMberInsert`/`EgovMberSelectUpdt`)의 소속그룹 select가 `userTyForGroup`(`USR`이면 노출) 모델값으로 ROLE_ADMIN 옵션(`GROUP_00000000000000`) 노출 제어. 일반회원관리 모듈은 ROLE_ADMIN 비노출.

### 회원구분 확인 위치
1. 마이페이지(`EgovMberMypage`) '회원구분'(USER_SE).
2. `/cmm/user-types`(`EgovUserTypesController` → `cmm/EgovUserTypes`): 현재 로그인 사용자 구분 배지 + 3유형 안내(공개 URL, SecurityConfig permitAll).
3. 회원관리 목록(`EgovMberManage`) '회원구분' 컬럼: 3테이블 UNION 통합조회로 일반/기업/업무사용자 전 유형 표시(`userTy` 유형 필터·승인 액션).
4. DB `VW_USER_MASTER.USER_SE`.

### 롤 타입 코드 (COM029)
- 롤관리(sec/rmt) 등록/수정의 '롤 타입' 드롭다운 = 공통코드 **COM029**(`url`/`method`/`pointcut`). 컨트롤러 채번 규약: url→web, method→mtd, pointcut→pct. hsql(shtdb.sql)·postgresql 정본 시드, 5종 DBMS 보강(파리티).

### DBMS 파리티 (이 모델 관련)
- 회원관리 통합조회·승인, 로그인 상태 'P' 필터, COM029 시드: 7종 매퍼/DATA 반영. hsql·postgresql은 런타임+파일 검증, mysql/oracle/tibero/cubrid/altibase는 파일 정합·XML well-formed 검증.

## 아키텍처 (JSP → Boot 전환 핵심)

- **설정**: `web.xml`/`context-*.xml`(스프링 XML) → `egovframework.com.config.*`(Java @Configuration).
  레거시 자산은 `_legacy_jsp/`로 분리 보관(빌드 제외).
- **컴포넌트 스캔**: `EgovConfigAppCommon`(Service/Repository), `EgovConfigWebDispatcherServlet`(Controller),
  `@SpringBootApplication`(egovframework 전체).
- **보안**: 세션 기반 Spring Security(`com.security.SecurityConfig`).
  로그인(`EgovLoginController`)이 자격증명 확인 후 `EgovUserDetails` 프린시플을 SecurityContext에
  담아 `HttpSessionSecurityContextRepository`로 세션 저장 → 컨트롤러는 RTE
  `EgovUserDetailsHelper`(SecurityContext 조회)로 인증/권한 판단. **24개 컨트롤러는 무수정 동작**.
- **뷰**: 컨트롤러 반환 뷰명(예: `cop/bbs/EgovNoticeList`)을 그대로 사용 →
  `templates/cop/bbs/EgovNoticeList.html` 매핑. JSP는 작성하지 않는다.
- **레이아웃**: `templates/layouts/default.html`(Thymeleaf Layout Dialect) +
  `templates/fragments/{header,nav,footer}.html`. 정적자원: **공식 KRDS(`static/krds/`) + 호환 레이어(`static/css/krds-compat.css`)**, 로컬. (Bootstrap 프레임워크 미사용)

## 다국어(i18n) 룰 — 신규 기능·화면 추가/변경 시 **반드시 확인** (스킬 `egov-component` §8)
- ⚠️ **현황**: 본 프로젝트 템플릿은 **대부분 하드코딩 한글**(레거시 JSP 포팅분, `#{}` 미사용). **신규/변경·KRDS 전환 시 i18n 키로 전환**한다.
- 사용자 노출 텍스트(라벨·버튼·placeholder·title·alt·검증 메시지)는 **하드코딩 금지** → Thymeleaf `th:text="#{key}"`.
- 메시지는 `egovframework/message/message-ui_{ko,en}.properties`에 **ko·en 동일 키**로 추가(프레임워크 공통은 `message/com/message-common_{ko,en}`). 언어전환 `/cmm/lang(lang='ko'|'en')`.
- **변경/추가 작업마다**: ① 노출 문구 전부 메시지 키 처리 ② ko/en 양쪽 값 존재(키 집합 일치) ③ 누락/한글 fallback 0 — 확인.
- 검증: `comm -3`로 ko/en 키 diff, 변경 html에서 `#{` 없는 한글 스캔.

## DB 명명 규칙

- 테이블 접두어 `TB_` + **SNAKE_CASE 대문자** (구 `LETTN*/LETTC*/COMVN*` 폐기).
- 모든 테이블 감사컬럼 필수: `FRST_REGIST_PNTTM`, `FRST_REGISTER_ID`, `LAST_UPDT_PNTTM`, `LAST_UPDUSR_ID`.
- 단어/도메인/용어 규칙: `Docs/단어규칙.md`, `Docs/도메인규칙.md`, `Docs/db-schema-guide.md`
  (대용량 `용어규칙.md`는 본 저장소 `Docs/` 에 포함 — `용어규칙.md` 및 분할본 `용어규칙_1~4.md`).

### 주요 테이블 매핑 (구 → 신)
| 구 | 신 | | 구 | 신 |
|---|---|---|---|---|
| LETTNBBS | TB_BBS | | LETTNFAQINFO | TB_FAQ_INFO |
| LETTNBBSMASTER | TB_BBS_MASTER | | LETTNQAINFO | TB_QA_INFO |
| LETTNBBSUSE | TB_BBS_USE | | LETTNGNRLMBER | TB_GNRL_MBER |
| LETTNEMPLYRINFO | TB_EMPLYR_INFO | | LETTNTMPLATINFO | TB_TMPLAT_INFO |
| LETTCCMMN*CODE | TB_CMMN_*_CODE | | COMVNUSERMASTER(VIEW) | VW_USER_MASTER |

### MyBatis 매퍼
- `egovframework/mapper/let/{모듈}/Egov{기능}_SQL_{dbType}.xml`, `dbType`=`Globals.DbType`(기본 hsql).
- 전환 시 MySQL→HSQL 변환: `DATE_FORMAT(c,'%Y-%m-%d')`→`TO_CHAR(c,'YYYY-MM-DD')`, `IFNULL`→`COALESCE`,
  테이블명 `LETT*`→`TB_*`. `LIMIT n OFFSET m`은 HSQL 호환.

### 전 DBMS 명명규칙 현행화 완료 (2026-06-16)
- 7개 DB(hsql·postgresql·mysql·oracle·tibero·cubrid·altibase) **완전 파리티**: 각 DDL 40테이블·
  매퍼 28파일·레거시(LETT*/COMVN*) 참조 0건·매퍼 XML 197개 well-formed.
- 과거: hsql만 준수, mysql/oracle/tibero/cubrid/altibase는 레거시 LETT* 원본, postgresql은 매퍼 없음·DDL 23테이블.
- 조치: 비-HSQL은 HSQL 정본 기준 테이블명 치환(레거시→TB_)+형상 정렬+누락(테이블4·매퍼2) 보강.
  **각 DB native dialect는 그 DB 기존 매퍼를 모방해 유지**(상세 변환표는 `SKILL.md` §7-1).
  PostgreSQL은 신규 생성(DDL 40+뷰, DATA 135, 매퍼 28).
- 검증 범위: HSQL/PG 경로만 런타임 검증, 그 외 5종은 파일 정합성·XML well-formed 검증(실제 DB서버 부재).

## 전환 완료 모듈 (화면+기능 검증, HTTP 200)

데이터 계층 전면 구축: TB_ 스키마 39개 테이블(`db/shtdb.sql`) + 전 모듈 `_hsql` 매퍼 26개 + PostgreSQL DDL/DML.

| 기능 | URL | 뷰(Thymeleaf) |
|------|-----|---------------|
| 메인(공지/갤러리/FAQ 요약) | `GET /` | `main/EgovMainView` |
| 로그인/로그아웃 | `/uat/uia/egovLoginUsr.do` 외 | `uat/uia/EgovLoginUsr` |
| 공지/갤러리 목록 | `/cop/bbs/selectBoardList.do?bbsId=` | `cop/bbs/EgovNoticeList` |
| 게시물 상세 | `/cop/bbs/selectBoardArticle.do` | `cop/bbs/EgovNoticeInqire` |
| 게시물 작성 폼 | `/cop/bbs/addBoardArticle.do?bbsId=` | `cop/bbs/EgovNoticeRegist` |
| FAQ 목록/상세/등록(생성 검증완료) | `/uss/olh/faq/FaqListInqire.do` 외 | `uss/olh/faq/EgovFaq*` |
| Q&A 목록/상세/등록 | `/uss/olh/qna/QnaListInqire.do` 외 | `uss/olh/qna/EgovQna*` |
| 이용약관 | `/uss/sam/stp/StplatListInqire.do` | `uss/sam/stp/EgovStplatListInqire` |
| 개인정보처리방침 | `/uss/sam/ipm/listIndvdlInfoPolicy.do` | `uss/sam/ipm/EgovIndvdlInfoPolicyList` |
| 우편번호 검색 | `/sym/cmm/EgovCcmZipSearchList.do` | `cmm/sym/zip/EgovCcmZipSearchList` |
| 회원관리 (ADMIN) | `/uss/umt/mber/EgovMberManage.do` | `cmm/uss/umt/EgovMberManage` |
| 권한관리 (ADMIN) | `/sec/ram/EgovAuthorList.do` | `sec/ram/EgovAuthorManage` |
| 롤관리 (ADMIN) | `/sec/rmt/EgovRoleList.do` | `sec/rmt/EgovRoleManage` |
| 그룹관리 (ADMIN) | `/sec/gmt/EgovGroupList.do` | `sec/gmt/EgovGroupManage` |
| 배너관리 (ADMIN) | `/uss/ion/bnr/selectBannerList.do` | `uss/ion/bnr/EgovBannerList` |

> 검증: 위 20개 화면 HTTP 200, FAQ 등록 생성 플로우(채번→insert→목록반영) 검증 완료. 로그인/로그아웃/권한가드 동작.

## 회원 폼 통일·전화 단일저장 / 첨부 툴팁 / 배너 게시기간 (2026-06-23)
- **회원 등록/수정/가입 폼 통일**: 라벨을 항상 입력칸 위로(KRDS `form-group`), `krds-form-section`(fieldset+legend)로
  섹션 구분(계정 정보·기본 정보·연락처·주소). 클래스명은 KRDS 충돌 회피 위해 `krds-form-section*` 네임스페이스(krds.css).
- **전화번호 단일 키인 저장(비파괴)**: 기존 3분할(`AREA_NO`/`MIDDLE_TELNO`/`END_TELNO`, 각 VARCHAR(4)) 대신
  단일 `TELNO VARCHAR(20)` ADD COLUMN(`TB_GNRL_MBER`, hsql+postgresql). 폼은 단일 입력 1개(`MberManageVO.telno`).
  분할 컬럼은 유지(회귀 안전, 폼에서만 제거). 매퍼 insert/update/select에 `TELNO` 추가.
- **자동 하이픈(순수 JS)**: `common.js`에 `data-phone-format="mobile|tel"` 바인더. mobile=010-1234-5678(3-4-4),
  tel=02-123-4567(서울 02 2자리)/031-123-4567(3자리). 숫자만 허용·백스페이스 정상. 휴대폰(`MBTLNUM`)도 단일+자동하이픈.
- **첨부파일 안내 → 정보 툴팁**: `cmm/fms/EgovFileUploader` 프래그먼트에 `bi-info-circle`+CSS 툴팁(`.egov-file-help[data-tip]`).
  장문 안내(최대 N개·10MB·허용확장자)를 마우스오버/포커스 말풍선으로. 폰트 13px(과대 방지, native title 미사용). max는 게시판 `posblAtchFileNumber`.
- **배너 개선**: ① 셀렉트 라벨 '반영여부 Y/N'→'노출여부 노출/비노출'(Regist/Updt/List 일관) ② 수정일시(`LAST_UPDT_PNTTM`) 표시(Updt)
  ③ **게시기간**: `TB_BANNER`에 `EXPSR_BGNDE/EXPSR_ENDDE DATE nullable` ADD COLUMN(hsql+postgresql). 폼 date 입력 2개,
  `selectBannerListByType`(메인·푸터·팝업 공통 조회)에 `(BGNDE IS NULL OR <=CURRENT_DATE) AND (ENDDE IS NULL OR >=CURRENT_DATE)` 필터
  (기간 미설정=상시). pg는 `CAST(#{x} AS DATE)`, 빈문자→NULL `<choose>`. POPUP_* 컬럼도 pg DDL에 보강(파리티).
- **검증(admin/1, HTTP)**: 회원 등록(telno=02-123-4567·mobile=010-1234-5678) insert→edit 라운드트립 DB 영속 확인,
  배너 게시기간 과거설정 시 메인에서 즉시 비노출→해제 시 복귀, 수정일시 표시(`2026-06-23 15:15:39`), 첨부 툴팁 렌더,
  회원/배너/게시판 폼 9종 200·예외 0. 헤드리스 스크린샷으로 섹션 레이아웃·툴팁 폰트 적정 확인. `.localdb` 재시드.
- **DBMS 파리티**: hsql(런타임 검증)·postgresql(DDL/매퍼) 반영. mysql/oracle/tibero/cubrid/altibase는 미반영(기존 popup 컬럼도 미반영 상태 동일).

## 설문(survey) 기능 개선 — end-to-end 구현·검증 완료 (2026-06-23)
- **문항 모달 등록(아코디언)**: 설문 등록(`EgovQustnrManageRegist`)에서 템플릿유형(객관식/서술형) 선택 후
  순수 JS 모달(외부 라이브러리 없음, KRDS 클래스)로 질문+보기 등록 → 아코디언 표시. hidden `questionsJson`(JSON 배열)으로
  제출하면 `EgovQustnrManageController.saveQuestions()`가 Jackson 파싱 후 qqm/qim 서비스로 문항/보기 저장.
  **문항ID는 IdGen prefix `QQESTN_`** 라 응답 컨트롤러 저장 루프(`sKey.substring(0,6)=="QQESTN"`)와 자동 정합.
  서술형은 `MXMM_CHOISE_CO=null`(숫자컬럼 빈문자 금지) 주의.
- **응답 렌더/응답**: 응답 페이지가 `Comtnqustnrqesitm`/`Comtnqustnriem`로 문항 렌더(객관식=라디오/체크박스, 서술형=textarea).
  시드 설문 `QESTNR_000000000001`에 데모 문항 3개(객관식2+서술형1)·보기7개 시드 → "등록된 문항이 없습니다" 해소.
- **설문대상 '전체'(코드 '00')**: COM034에 '00'=전체 추가, 등록폼 기본선택. '전체'면 모든 회원유형 응답대상.
- **회원 직업유형 필수**: `TB_GNRL_MBER`·`TB_EMPLYR_INFO`에 `OCCP_TY VARCHAR(10)` ADD COLUMN(비파괴),
  `VW_USER_MASTER`에 `USER_OCCP` 컬럼 추가. `MberManageVO.occpTy`(@EgovNullCheck) + 회원 등록/수정/가입 폼에
  직업유형 드롭다운(COM034, '00' 제외). 시드: user=학생(01), 신규 user2=회사원(02), admin=공무원(03), user1=학생(01).
- **검색/필터(참여목록)**: ① 제목검색(QESTNR_SJ) ② 회원유형 필터(MINE=설문대상이 '00'/빈값 또는 로그인 직업유형 일치)
  ③ 참여여부 필터(Y/N). 로그인 직업유형은 `selectLoginUserOccp`(VW_USER_MASTER.USER_OCCP)로 조회.
- **참여완료 상태**: 목록에 `PARTCPTN_AT` 서브쿼리(응답테이블에 로그인 사용자 응답 존재 여부) → '참여완료'/'미참여' 배지.
- **⚠️ MyBatis OGNL 함정**: `<if test="x == 'Y'">`처럼 **단일문자 작은따옴표는 char 리터럴로 처리돼 DataAccessException**
  (dataAccessFailure 화면)을 유발. 반드시 `== &quot;Y&quot;`(문자열)로 작성. (MINE 등 멀티문자는 정상)
- **egovMap 키 케이싱**: 언더스코어 없는 별칭은 소문자(`qestnrSj`→`qestnrsj`), 언더스코어 포함은 카멜(`QESTNR_ID`→`qestnrId`).
  목록 링크가 빈값이던 버그는 `A.QESTNR_ID`에 별칭 `qestnrId` 부여(키 `qestnrid`)로 해결. char(20) 패딩은 `TRIM()`.
- **검증**: 시드/모달 문항 렌더·응답 제출→참여완료 전환·제목검색·MINE/참여여부 필터·회원 직업유형 저장/수정 반영
  모두 HTTP 200·예외 0 확인. 회귀(메인·게시판·설문 6종·회원·권한) 200 유지.
- **DBMS 파리티**: hsql(런타임 검증)·postgresql(DDL/DATA/매퍼) 반영. mysql/oracle/tibero/cubrid/altibase는 미반영(시간상 생략).
- **버그수정 4건(2026-06-23)**:
  ① **라디오/체크박스 비가시(근본원인)**: `krds.min.css`의 `input[type=checkbox],input[type=radio]{position:absolute;width:1px;height:1px;clip:rect(0,0,0,0)…!important}`
     (KRDS는 label::before로 라디오를 그리는 sr-only 패턴)이 호환 `.form-check-input` 라디오/체크박스까지 1px로 클리핑해 완전히 숨김.
     `krds.css`에 `input.form-check-input[type=radio|checkbox]{position:static;width/height:1.05em;clip:auto;appearance:auto …!important}` 추가로 네이티브 컨트롤 복원
     (KRDS 네이티브 `.krds-form-check`는 영향 없음). → 템플릿유형(객관식/서술형)·복수선택·기타 체크박스가 보이고 선택 가능. 헤드리스 스크린샷으로 가시성 확인.
  ② **복수선택 저장/렌더**: 모달 `복수선택` 체크 → `MXMM_CHOISE_CO='9'` 저장 → 응답폼이 `mxmmChoiseCo>1`이면 checkbox(복수)로 렌더(확인됨).
  ③ **응답폼 문항 렌더**: 시드/모달/서술형 등록 모두 정상 렌더 확인(end-to-end). 키 케이싱(`qestnTyCode`/`qestnCn`/`mxmmChoiseCo`/`qestnrqesitmid`) 정합.
  ④ **수정폼 문항 표시/재저장**: Modify 컨트롤러가 qri 서비스로 기존 문항/보기를 `existingQuestionsJson`으로 모델에 담고, 수정폼이 등록폼과 동일한 모달+아코디언으로 초기 로드.
     저장 시 `deleteQustnrQestnManageByQestnr`(설문지+템플릿 단위 문항/보기/응답 일괄삭제, qqm hsql+postgresql 매퍼/DAO/서비스 추가) 후 `saveQuestions` 재등록 → 수정/추가/삭제 반영(round-trip 검증).

## KRDS 전면 적용 — UI 표준화 (2026-06)
- **공식 KRDS(디지털정부 표준 디자인시스템) 전환**: Bootstrap 프레임워크 제거, 190+ 템플릿을 KRDS 네이티브 마크업으로 전환(Wave1·2 + 잔존 정리, 레거시 부트스트랩 0건). 네이티브 컴포넌트 + 호환 레이어(`krds-compat.css`: 그리드/유틸/GNB)·`krds.css`(토큰 정규화) 구성. 정적 자산 로컬(CDN 금지), 폰트 **Pretendard GOV**. 전환·검증 절차는 스킬 `krds-conversion` 참조.
- **페이지네이션 KRDS 통일**(이전/숫자/다음), 게시판 검색폼+전체건수 정렬, **Q&A 상태 색상 배지**, 폼 라디오/체크박스 가시성 보정.
- **푸터 심플형 통일**: 공식 홈페이지 배너 + 3컬럼 + 소셜 링크(i18n). 소개 페이지 히어로 통합, 전 페이지 **'맨 위로'** 스크롤 버튼(우하단 고정, 스크롤 300px↑ 표시, 인쇄 숨김).
- ⚠️ **KRDS `.input-group` 충돌**: KRDS가 `.input-group{flex-direction:column}`을 강제 → 회원 등록/수정 폼의 가로 입력그룹이 세로로 깨짐. `krds.css`에서 해당 폼 input-group을 `flex-direction:row`로 강제 교정. (스킬 `krds-conversion`에 'KRDS 최우선' 원칙·이 사례 기록)

## 배너(uss/ion/bnr) 기능 — 유형 구분·캐러셀·팝업 (2026-06)
- **유형 구분**: 메인/팝업/푸터. 메인 배너는 **히어로 슬라이드 캐러셀**로 편입, 자동전환 주기 `portal.banner.interval`(properties, 기본 5000ms).
- **팝업**: 좌표/크기/묶음 지정, **'오늘 하루 보지 않기'** 체크 즉시 닫힘(쿠키), **로그인 화면 제외**. 링크 URL 선택 노출.
- 캡션 가독성·런타임 영속 등 버그 3건 수정 완료.

## 달력(sym/cal)·휴일 — 격자형 재설계 (2026-06)
- 월간 달력을 **격자형**(균일 셀·테두리·일자 밑 휴일 배지)으로 재설계. **인쇄 시 달력 영역만** 출력.
- 일반/관리자 월간 달력 툴바에 **휴일관리 이동 버튼**(목록 `/sym/cal/EgovRestdeList.do`) 추가(인쇄 버튼 옆).

## 약관·파일·개발DB 기타 (2026-06)
- **약관 등록 유형 구분**: 이용약관/개인정보처리방침을 유형 구분해 등록.
- **파일 첨부 허용 확장자 확장**: `Globals.fileUpload.Extensions`=gif/jpg/jpeg/png/xls/xlsx/pdf/doc/docx/ppt/pptx/hwp/hwpx/txt/zip(화이트리스트). 미허용 확장자는 등록 화면에서 사유 안내.
- **개발용 HSQL 파일 영속**: `jdbc:hsqldb:file:./.localdb/portaldb`로 영속화. `shtdb.sql` SHA-256을 `.localdb/.seedhash`에 기록해 두고 기동 시 비교 → **shtdb.sql 변경 시 자동으로 `.localdb` 삭제 후 재시드**(수동 삭제 불필요). 매 기동 강제 초기화는 `Globals.localdb.resetEachStart=true`. `.localdb`는 `.gitignore` 제외, 운영(jar, postgresql) 무관(hsql 전용 분기). 설정: `EgovConfigAppDatasource`.

## 게시판(cop/bbs) CRUD — 전체 동작 검증 완료 (2026-06-03)
- 등록/수정/답글/삭제 4종 모두 HTTP 302 성공·영속·한글 정상 검증.
- **근본원인(과거 insert 미반영)**: `Board`의 `@EgovNullCheck`(ntcrNm·password·ntceBgnde·ntceEndde·nttSj·nttCn)
  검증 실패 → 컨트롤러가 `bindingResult.hasErrors()` 분기로 폼만 재렌더(200), insert 미도달.
  isAuthenticated/트랜잭션은 정상이었음. **인증 사용자(비익명)는 검증 통과용 hidden 값 필요**.
- **수정 내용**:
  - `EgovNoticeRegist.html`/`EgovNoticeUpdt.html`/`EgovNoticeReply.html`에 hidden `ntcrNm=dummy`,
    `password=dummy` 및 게시기간(`BBSA01`=날짜입력, 그 외=`10000101~99991231` hidden) 추가.
  - `EgovNoticeUpdt`/`EgovNoticeReply`는 스텁→실제 폼으로 신규 구현(`result`/`bdMstr`/`searchVO` 모델 사용).
  - 상세(`EgovNoticeInqire`)에 답글/삭제 버튼 추가(`sec:authorize`). 삭제는 논리삭제(서비스가 nttSj를
    "이 글은 작성자에 의해서 삭제되었습니다."로 치환 후 `USE_AT='N'`) → 목록에 삭제 메시지로 잔류(스레드 보존).
- 적용범위: 공지(BBSA03)/갤러리(BBSA02)/자료실(BBSA03) 모두 동일 템플릿(`cop/bbs/EgovNotice*`).

## 전 모듈 전환 완료 — 런타임 검증 (2026-06-14)
JSP 원본(33개 컨트롤러) 전 기능을 Boot+Thymeleaf로 포팅 완료. 8개 작업분할(WP1~8)을 병렬 구현 후 통합.
- **GET 화면 스모크**: 전 모듈 대표 25개 화면 HTTP 200, 예외 0건(메인·소개8종·게시판·게시판마스터·사용정보·
  템플릿·FAQ(+admin)·QnA(+admin·답변)·약관·개인정보·회원·권한·롤·그룹·배너·우편번호·달력·설문6종).
- **CRUD 쓰기 검증(영속 확인)**: 게시판·FAQ·QnA·배너·우편번호·약관·개인정보처리방침·**설문(qmc)**·**회원** 9종
  등록 후 목록 반영 확인. 멀티파트(파일첨부) 컨트롤러(FAQ·BBS·배너)는 `multipart/form-data`로 제출해야 함
  (urlencoded 제출 시 `IllegalStateException: not MultipartHttpServletRequest`).
- **설문(`uss/olp/**`)**: 과거 `incompatible data type` 이슈 해소 — 목록·등록 정상 동작.

### 배포(패키지 JAR) 핵심 결함 — 뷰 이름 앞 슬래시 (2026-06-15)
- **증상**: `java -jar`로 띄우면 일부 메뉴가 Whitelabel 500(`Error resolving template [/uss/...]`).
  `spring-boot:run`에서는 정상이라 개발 중엔 안 보였음.
- **원인**: 다수 컨트롤러가 뷰 이름을 **앞 슬래시 포함**으로 반환(`return "/uss/..."` 또는
  `String sLocationUrl = "/uss/olp/..."`). Thymeleaf prefix=`classpath:/templates/` + `/uss/..` =
  `classpath:/templates//uss/..`(이중 슬래시). 파일시스템(run)은 OS가 `//`를 정규화 → 통과,
  **JAR 내부 리소스는 정규화 안 함 → 템플릿 미해결 500**.
- **수정**: 전 컨트롤러의 뷰 이름 앞 슬래시 제거(`return "/x/.." → "x/.."`, `= "/x/.." → "x/.."`).
  `redirect:`/`forward:`/`.do` URL은 슬래시 유지(절대경로 필요). 126개 return + 21개 변수할당 교정.
- **규칙**: 뷰 이름은 **앞 슬래시 없이**(`uss/olh/faq/EgovFaqListInqire`). redirect/forward만 `/`로 시작.

### 핵심 통합 수정
- `mapper/config/mapper-config.xml`에 **`callSettersOnNulls=true`** 추가. egovMap 결과에서 **NULL 컬럼이 키 자체로
  누락**되어 Thymeleaf `${item.xxx}`가 `SpelEvaluationException(EL1008E: ... cannot be found)`로 화면 500을
  내던 문제(stp 목록 `lastUpdtPnttm`=시드 NULL, ipm 목록 `frstRegisterNm`=서브쿼리 NULL)를 전역 해소.
  NULL도 키로 보존 → 템플릿은 빈 값 렌더. **egovMap 화면 신규 작성 시 NULL 가능 컬럼은 이 설정 덕에 안전**.
- `db/shtdb.sql`: 약관 시드 `STPLAT_000000000001`(12자리) → `STPLAT_0000000000001`(13자리, IdGen 포맷) 교정
  (회원가입 약관동의 화면 표출 일치).

### 검증 함정(테스트 시 주의 — 앱 버그 아님, 원본 제약 충실 재현)
- **QnA 작성자명 `wrterNm`은 `@Size(max=4)`**(JSP 원본 동일) → 5자 이상이면 등록 검증 실패. 한글 이름 3자 등 OK.
- **회원등록 `groupId`는 `@EgovNullCheck` 필수 + 유효 FK**. 시드 그룹 `GROUP_00000000000000`/`GROUP_00000000000001` 사용.
- VO `@EgovNullCheck` 필드는 폼에 (hidden이라도) 값 제공해야 insert 도달.

## 주의(EgovMap 키 케이싱)
`resultType="egovMap"` 결과는 컬럼 라벨을 카멜케이스로 변환하되, **언더스코어 없는 별칭**은
HSQL이 대문자화 → 전부 소문자 키가 된다(예: `userId`→`userid`, `INQIRECO`→`inqireco`).
언더스코어 포함 별칭은 정상 카멜케이스(`QESTN_SJ`→`qestnSj`). 템플릿에서 키 케이스 주의.
NULL 컬럼도 `callSettersOnNulls=true`로 키는 보존되나 값은 null이므로 `${item.key}`는 빈 문자열 렌더.
