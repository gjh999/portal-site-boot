/*
 * ============================================================
 * KRDS (Korea Design System) 인터랙션/접근성 스크립트
 * 디지털정부 표준 디자인 시스템 (행정안전부) — https://www.krds.go.kr
 * ------------------------------------------------------------
 * KRDS 디자인 레이어(krds.css)와 짝을 이루는 동작 스크립트.
 * 컴포넌트 동작(드롭다운/토글)은 Bootstrap 번들이 담당하고,
 * 이 파일은 KRDS 가 요구하는 웹접근성(KWCAG) 보조 동작을 담당한다.
 * ============================================================
 */
'use strict';
(function () {
  // 본문 바로가기(skip-nav): 이동 후 본문에 실제 포커스를 부여(스크린리더/키보드 대응)
  document.addEventListener('DOMContentLoaded', function () {
    var skip = document.querySelector('.skip-nav');
    var content = document.getElementById('content');
    if (skip && content) {
      skip.addEventListener('click', function () {
        content.setAttribute('tabindex', '-1');
        content.focus();
      });
    }
  });
})();
