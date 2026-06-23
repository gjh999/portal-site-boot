# 서드파티 오픈소스 고지 (Third-Party Notices)

본 프로젝트(`portal-site-boot`)는 다음의 서드파티 오픈소스/디자인시스템/폰트/도구를 **번들 또는 의존**하여 사용합니다.
각 구성요소의 저작권과 라이선스는 원저작자/원배포처에 있으며, 아래에 라이선스명과 출처를 명시합니다.
프로젝트 자체 라이선스는 [Apache License 2.0](LICENSE)입니다(eGovFramework 기반).

> 표기 원칙: 실제 번들 파일의 헤더/약관/매니페스트를 확인해 라이선스명을 적었습니다.
> 불확실한 항목은 단정하지 않고 "원배포 약관 참조"로 보수적으로 표기합니다.

---

## 1. 정적 자원 — 번들된 프런트엔드 자산 (`src/main/resources/static/`)

| 구성요소 | 버전 | 라이선스 | 출처 / 비고 |
| :--- | :--- | :--- | :--- |
| **KRDS HTML Component Kit** (디지털정부 표준 디자인시스템, `static/krds/`) | krds-uiux 1.1.0 | **KRDS 이용약관**(디지털정부 디자인시스템 약관). 동봉 `package.json`은 `"license": "ISC"`로 표기되나, 동봉 `README.md`는 "대한민국 디지털 정부 디자인 시스템(KRDS) 이용약관을 따름"으로 안내 → **공식 KRDS 약관을 우선 참조** | 행정안전부 / https://www.krds.go.kr · KRDS 저작권: https://www.krds.go.kr/html/site/utility/utility_06.html · `static/krds/README.md`·`static/krds/package.json` |
| **Pretendard GOV** 서브셋 폰트 (`static/krds/resources/fonts/PretendardGOV-*.subset.woff2`) | — | **SIL Open Font License 1.1 (OFL-1.1)** (Pretendard/Pretendard GOV 배포 라이선스). KRDS 킷에 동봉된 정부 표준 서브셋 | 원작: Pretendard (https://github.com/orioncactus/pretendard) · Pretendard GOV는 KRDS 배포본에 포함 |
| **Bootstrap Icons** (`static/css/bootstrap-icons.min.css` + 폰트, 아이콘 전용) | v1.11.3 | **MIT License** (파일 헤더에 명시) | The Bootstrap Authors · https://icons.getbootstrap.com · (※ Bootstrap CSS 프레임워크 본체는 미사용, 아이콘 폰트만 사용) |
| **Swiper** (KRDS 킷 동봉 캐러셀 — 메인/푸터 배너 슬라이드, `static/krds/resources/.../swiper-bundle.min.*`) | 11.0.6 | **MIT License** (파일 헤더에 명시) | Vladimir Kharlampidi · https://swiperjs.com |

---

## 2. 빌드 의존성 — 주요 Java/백엔드 라이브러리 (`pom.xml`)

대부분 **Apache License 2.0**입니다. 정확한 버전은 `pom.xml` 및 eGovFrame 부트 스타터(BOM)에서 관리됩니다.

| 구성요소 | 라이선스 | 비고 |
| :--- | :--- | :--- |
| **전자정부표준프레임워크 (eGovFramework RTE)** `egovframe-rte-*`, `egovframe-boot-starter-*` | **Apache License 2.0** | 행정안전부 / https://www.egovframe.go.kr |
| **Spring Boot / Spring Framework** (`spring-boot-starter-*`) | Apache License 2.0 | VMware/Spring (세션 기반 Spring Security 포함) |
| **Thymeleaf** + **Thymeleaf Layout Dialect** | Apache License 2.0 | SSR 템플릿 엔진 |
| **MyBatis** / mybatis-spring | Apache License 2.0 | 데이터 접근 |
| **Apache Commons** (lang3, codec, validator, beanutils, dbcp2, logging) | Apache License 2.0 | Apache Software Foundation |
| **Apache Log4j 2** (`log4j-core` 등) | Apache License 2.0 | 로깅 |
| **jjwt** (JSON Web Token) | Apache License 2.0 | 토큰 유틸 |
| **springdoc-openapi** (Swagger UI) | Apache License 2.0 | API 문서 |
| **Hibernate Validator** | Apache License 2.0 | Bean Validation |
| **ICU4J** | **Unicode License (ICU License)** | 국제화 |
| **Project Lombok** | **MIT License** | 코드 생성(컴파일 타임) |
| **HSQLDB** | **HSQLDB License**(BSD 계열) | 내장 개발 DB |
| **PostgreSQL JDBC Driver** | **BSD 2-Clause** | 운영 DB 드라이버 |
| **MySQL Connector/J** | **GPLv2 + Universal FOSS Exception** | 선택적 DB 드라이버(사용 시 라이선스 조건 확인) |

> 위 목록은 주요 항목 요약입니다. 전이 의존성을 포함한 전체 목록은 `mvn license:add-third-party` 또는
> `mvn dependency:tree` 로 생성·확인할 수 있습니다. 각 라이브러리의 정확한 라이선스는 해당 배포처를 따릅니다.

---

## 3. 개발 도구 — 저장소에 동봉된 작업 스킬 (`.claude/skills/`)

| 구성요소 | 라이선스 | 출처 |
| :--- | :--- | :--- |
| **Andrej Karpathy Guidelines** 스킬 (`.claude/skills/karpathy-guidelines/SKILL.md`) | **MIT License** (SKILL.md 프런트매터 `license: MIT`) | https://github.com/multica-ai/andrej-karpathy-skills · Andrej Karpathy의 LLM 코딩 가이드라인 기반 |

> `egov-project`·`egov-component`·`krds-conversion` 스킬은 본 프로젝트 자체 산출물(프로젝트 라이선스 적용)입니다.

---

## 면책 및 정정

- 본 고지는 작성 시점에 번들/의존된 항목을 기준으로 합니다. 의존성 추가·갱신 시 본 문서를 함께 갱신하세요.
- 라이선스 표기가 실제와 다르다고 판단되면 해당 구성요소의 **원배포처 라이선스/약관이 우선**합니다.
- KRDS 디자인시스템 및 정부 표준 폰트의 이용은 **KRDS 공식 이용약관**을 따릅니다.
