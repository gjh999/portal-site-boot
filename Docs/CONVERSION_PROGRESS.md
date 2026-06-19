# portal-site-boot 전환 진행상황 (JSP → Thymeleaf 전수 전환)

> 목표: portal-site-jsp의 모든 JSP 화면을 portal-site-boot의 Thymeleaf 템플릿+화면으로 구현하고
> 컨트롤러 전 기능을 동작/테스트한다. simple-homepage(Boot+Thymeleaf 레퍼런스) 컨벤션을 따른다.
> 기록 규칙: 화면 완료 시 상태를 ✅, 진행중 🔶, 미착수 ⬜, 런타임버그 🐞로 표기.

## 핵심 전환 패턴 (재확인)
- 뷰: 컨트롤러 반환 뷰명 위치에 `templates/<경로>.html`(layout:decorate=`~{layouts/default}`).
- 검증 함정: VO의 `@EgovNullCheck` 필드는 **null·빈문자 모두 검증실패**(ObjectUtils.isEmpty). 폼에 hidden dummy 또는 사전채움 필수. POST 성공=302, 실패=200(폼 재렌더).
- egovMap 키 케이싱: 언더스코어 없는 별칭→소문자(`RDCNT`→`rdcnt`), 언더스코어 별칭→카멜(`QESTN_SJ`→`qestnSj`).
- 단일 String 파라미터는 `#{임의이름}`에 그대로 바인딩됨(매퍼 WHERE 컬럼만 정확하면 됨).
- 가동중 템플릿 수정은 `target/classes`에도 동기화(thymeleaf.cache=false). 매퍼/자바 변경은 재기동 필요.

## 모듈별 상태 (총 스텁 97 기준, 2026-06-03 시작)

### 게시판 cop/bbs (사용자) — ✅ 완료(이전 세션, 검증됨)
- 목록/상세/등록/수정/답글/삭제 4종 CRUD 302 검증.

### FAQ/Q&A uss/olh — 🔶 진행중
- ✅ FAQ 수정(EgovFaqCnUpdt) — 라이브 200
- ✅ Q&A 수정(EgovQnaCnUpdt), 비번확인(EgovQnaPasswordConfirm) — 라이브 200
- ✅ Q&A 답변 상세(EgovQnaAnswerDetailInqire)/답변폼(EgovQnaCnAnswerUpdt) — 라이브 200
- 🐞 Q&A 답변목록(EgovQnaAnswerListInqire) — 500(매퍼 런타임), 재기동 후 로그진단 필요
- ⬜ FAQ admin(faq/admin/*), Q&A admin(qna/admin/*), EgovLeft, RealnmChoice

### 회원 uss/umt(cmm/uss/umt) — 🔶 진행중
- ✅ 등록(EgovMberInsert), 비번변경(EgovMberPasswordUpdt) — 라이브 200
- 🐞 수정(EgovMberSelectUpdt) — 매퍼 selectMber_S가 ESNTL_ID로 조회(버그)→MBER_ID로 수정함, 재기동 후 검증
- ⬜ 가입(EgovMberSbscrb), ID중복(EgovIdDplctCnfirm), 약관확인(EgovStplatCnfirm)

### 보안 sec/(ram·rmt·gmt·rgm) — ⬜ 미착수
- 권한 EgovAuthorInsert/Update/RoleManage, 롤 EgovRoleInsert/Update,
  그룹 EgovGroupInsert/Update/Search, EgovAuthorGroupManage, accessDenied

### 배너 uss/ion/bnr — ✅ 화면완료(등록 검증)
- EgovBannerRegist/Updt/View 실제 폼 구현. 목록→상세(getBanner)·등록(addViewBanner) 링크 수정.
- 검증: 등록폼 200, 등록 POST 200·영속(idGen 시드충돌 해소 후), 뷰/상세 200.
- 🐞 updtBanner.do 500 — 매퍼 `<if test="isAtchFile == 'true'">`가 boolean을 문자열 비교 → NumberFormatException(For input string: "true"). 매퍼 버그(미수정, 보고).

### 약관 uss/sam/stp — ✅ 완료(검증)
- EgovStplatCnRegist/CnUpdt/DetailInqire/EgovLeft 실제 폼 구현. 목록→상세/등록 링크 추가(sec:authorize).
- 검증: 전 화면 200, 등록·수정 POST 200·영속(forward→목록 렌더라 302아닌 200 정상), 검증실패시 폼 재렌더.

### 게시판마스터·사용정보·템플릿 cop/bbs·cop/com — ✅ 완료
- 게시판마스터(EgovBoardMstr*, EgovBdMstrByTrget*), 사용정보(EgovBoardUseInf*),
  템플릿(EgovTemplate*), admin 게시판(cop/bbs/admin/*) 전 화면 200. 마스터등록/템플릿등록 POST 영속.
- 단독마스터(EgovBBSLoneMstr*): 매퍼 네임스페이스 `BBSLoneMasterDAO` 전체 누락 → 신규
  `EgovBBSLoneMaster_SQL_hsql.xml` 생성(TB_BBS_MASTER 기준). 재기동 후 목록 200 검증.

### 설문 uss/olp/(qmc·qqm·qim·qtm·qnn·qri·qrm·mgt) — ✅ 완료 (28+화면)
- 전 화면 200. qmc/qri 매퍼 HSQL 타입호환(TO_CHAR(char)→CASE/SUBSTRING) 수정.
- **핵심 함정**: EgovMap은 **값이 null인 키를 맵에서 제거**한다 → SpringEL 점접근(`${item.x}`)이
  EL1008E 예외. 해결: 브래킷 접근 `${item['key']}`(없으면 null 반환). 키 케이싱(언더스코어 규칙)도 준수.

### 우편번호·일정 cmm/sym/(zip·cal) — ✅ 완료
- 우편 EgovCcmZip*(목록/상세/수정/등록/엑셀/팝업) 200.
- 일정 캘린더(normal/admin day·week·month·year, popup) + 휴일(RestdeList/Detail/Modify) 200.
- 휴일 매퍼 `RestdeManageDAO` 전체 누락 → 신규 `EgovRestdeManage_SQL_hsql.xml` 생성 +
  `db/shtdb.sql`에 TB_RESTDE DDL/COM017코드/시드 추가(2026년 휴일 5건). 재기동 후 200·데이터표시 검증.

## 수정·생성한 매퍼/DB (모두 재기동 반영·검증 완료)
- uss/umt `selectMber_S`: WHERE ESNTL_ID→MBER_ID (회원수정 500 해결) ✅
- sec `now()`→`TO_CHAR(sysdate(),'YYYY-MM-DD')`: AuthorManage/RoleManage/GroupManage
  insert·update (CHAR(20) 날짜컬럼 truncation 해결) ✅ — 그룹등록·권한수정 POST 영속 검증.
- bnr `<if test="isAtchFile == 'true'">`→`== true` (배너수정 NumberFormatException 해결) ✅
- olp qmc/qri `TO_CHAR(char)`→`CASE/SUBSTRING` (설문목록/상세 HSQL 타입호환) ✅
- 신규: `cop/bbs/EgovBBSLoneMaster_SQL_hsql.xml`, `sym/cal/EgovRestdeManage_SQL_hsql.xml`,
  `db/shtdb.sql`(TB_RESTDE) ✅

## 템플릿 런타임 함정(EgovMap)
- 답변목록 500 원인: `${item.inqireco}` — 답변쿼리는 `RDCNT`(별칭없음)→키 `rdcnt`. 동작하던
  목록쿼리는 `RDCNT INQIRECO`라 키 `inqireco`. → 쿼리별 실제 별칭 확인 필수.
- EgovMap은 null 값 키 제거 → 점접근 예외. 데이터에 따라 잠재적이므로 list/detail은 `${item['key']}` 권장.

## 최종 검증 결과 (2026-06-04)
- **전 모듈 대표 25개 화면 HTTP 200/302 (실패 0)** — 메인/로그인/게시판/마스터/단독마스터/FAQ/Q&A/
  답변/회원/가입/권한/롤/그룹/약관/배너/설문(관리·문항·템플릿)/우편/휴일/캘린더(주·연).
- 기능(POST) 검증: 게시판 CRUD 4종(302 영속, 이전 세션), 회원수정/그룹등록/권한수정/약관등록·수정/
  배너등록/마스터등록/템플릿등록/FAQ·Q&A 등록·답변 = 성공·영속(forward방식은 200).
- 서버 재기동 2회 모두 정상 기동(약 3.7~3.9초, OOM 없음). `mvn -o spring-boot:run`(clean 없이=증분)으로
  자원부족 회피, 힙 `-Xmx768m -XX:+UseSerialGC`.
- 한글 POST를 curl(Windows)로 테스트 시 바이트 손상은 테스트 도구 한계(앱 정상). 브라우저/정상 인코딩은 정상.

## 2차 보강 (2026-06-04) — 목록화면 정상화 + 랜딩페이지
- **raw 객체 덤프 스텁 31개 정상화**: 제네릭 스텁(`th:text="${item}"`)이 VO를 toString 덤프하던
  목록/메인 화면(게시판마스터·단독마스터·사용정보·템플릿·admin공지·설문 문항/항목/응답/템플릿·
  FAQ/Q&A admin·배너메인·여론조사·각 EgovMain)을 실제 Bootstrap 표/카드로 재작성. raw덤프 0 검증.
  - egovMap 행은 **브래킷 접근 `${item['key']}`**로 통일(null 키 누락시에도 안전).
- **메뉴 보강**: 상단 "관리자" 드롭다운에 11개 화면 전체 노출(회원·권한·롤·그룹·게시판마스터·
  Q&A답변·배너·약관·설문·휴일·우편), 배너 라우트 404 수정.
- **다크테마 대비 수정**: nav 프래그먼트 클래스 `egov-navbar`→`egov-nav` 추가(테마 CSS 미적용 버그),
  `.egov-nav .nav-link` 색 #e7ebff·700. (브라우저 하드 새로고침 필요)
- **랜딩페이지 신규**(`main/EgovMainView`): 히어로(그라데이션·CTA)·통계밴드·핵심서비스 6카드·
  공지/갤러리 패널·FAQ·CTA 섹션. 다크테마 일관, 로그인 상태별 버튼 분기. 200 검증.
- 남은 비템플릿 이슈: `selectBBSUseInfsByTrget.do` 500(매퍼가 없는 테이블 TB_CMMNTY_USER 참조, 스키마 갭).

## 남은 사소 항목(선택)
- sec 고아 statement: authorRoleManageDAO.selectAuthorRole/updateAuthorRole(미호출, 필요시 추가).
- olp `qri/template/template.do` 무인자 호출 시 뷰명 누락 500(정상 호출=templateUrl 지정 시 200, 컨트롤러 기본뷰 처리 필요-자바영역).
- 배너 최초 등록 idGen 시드 충돌(채번 진행 후 정상) — 시드/채번 동기화 개선 여지.
