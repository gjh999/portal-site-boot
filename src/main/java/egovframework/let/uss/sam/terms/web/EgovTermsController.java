package egovframework.let.uss.sam.terms.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egovframe.rte.fdl.property.EgovPropertyService;
import org.egovframe.rte.fdl.security.userdetails.util.EgovUserDetailsHelper;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import egovframework.com.cmm.ComDefaultVO;
import egovframework.com.cmm.EgovMessageSource;
import egovframework.com.cmm.LoginVO;
import egovframework.let.uss.sam.ipm.service.EgovIndvdlInfoPolicyService;
import egovframework.let.uss.sam.ipm.service.IndvdlInfoPolicy;
import egovframework.let.uss.sam.stp.service.EgovStplatManageService;
import egovframework.let.uss.sam.stp.service.StplatManageDefaultVO;
import egovframework.let.uss.sam.stp.service.StplatManageVO;
import jakarta.annotation.Resource;

/**
 * 약관(이용약관/개인정보처리방침) 통합 관리 컨트롤러.
 *
 * <pre>
 * - 통합 목록(/uss/sam/terms/list.do): 두 테이블을 유형별 조회 후 병합, 유형필터/검색/페이지네이션(게시판식).
 * - 통합 등록(/uss/sam/terms/registView.do, /regist.do): 유형 선택 후 해당 테이블로 라우팅 저장.
 * - 모달용 조회(/uss/sam/terms/view.do?type=stplat|ipm): 대표(REPRSNT_AT='Y') 버전을 JSON 으로 반환.
 *   푸터는 모든 페이지(비인증 포함)에 노출되므로 view.do 는 공개(SecurityConfig anyRequest permitAll).
 *
 * 개정이력: 2026.06.23  구재호  약관 대표+버전관리+모달 통합 재구성
 * </pre>
 */
@Controller
public class EgovTermsController {

	@Resource(name = "StplatManageService")
	private EgovStplatManageService stplatManageService;

	@Resource(name = "egovIndvdlInfoPolicyService")
	private EgovIndvdlInfoPolicyService egovIndvdlInfoPolicyService;

	@Resource(name = "propertiesService")
	protected EgovPropertyService propertiesService;

	@Resource(name = "egovMessageSource")
	EgovMessageSource egovMessageSource;

	private boolean isAdmin() {
		List<String> authorities = EgovUserDetailsHelper.getAuthorities();
		return authorities != null && authorities.contains("ROLE_ADMIN");
	}

	private boolean isBlank(String s) {
		return s == null || s.trim().isEmpty();
	}

	/**
	 * 모달용 대표(현행) 약관/방침 조회. type=stplat(이용약관) | ipm(개인정보처리방침).
	 * 외부 라이브러리 없는 순수 JS 모달이 fetch 로 호출한다.
	 */
	@ResponseBody
	@RequestMapping(value = "/uss/sam/terms/view.do")
	public Map<String, Object> viewRepresent(
			@RequestParam(value = "type", defaultValue = "stplat") String type) throws Exception {

		Map<String, Object> res = new LinkedHashMap<>();
		res.put("type", type);

		if ("ipm".equals(type)) {
			IndvdlInfoPolicy vo = egovIndvdlInfoPolicyService.selectRepresentIndvdlInfoPolicy();
			if (vo == null) {
				res.put("found", false);
				return res;
			}
			res.put("found", true);
			res.put("nm", nvl(vo.getIndvdlInfoNm()));
			res.put("cn", nvl(vo.getIndvdlInfoDc()));
			res.put("ver", nvl(vo.getVer()));
			res.put("aplcDe", formatDe(vo.getAplcDe()));
		} else {
			StplatManageVO vo = stplatManageService.selectRepresentStplat();
			if (vo == null) {
				res.put("found", false);
				return res;
			}
			res.put("found", true);
			res.put("nm", nvl(vo.getUseStplatNm()));
			res.put("cn", nvl(vo.getUseStplatCn()));
			res.put("ver", nvl(vo.getVer()));
			res.put("aplcDe", formatDe(vo.getAplcDe()));
		}
		return res;
	}

	/**
	 * 통합 약관 목록(이용약관 + 개인정보처리방침). 유형필터/검색/페이지네이션.
	 */
	@RequestMapping(value = "/uss/sam/terms/list.do")
	public String list(
			@RequestParam(value = "termsType", required = false, defaultValue = "") String termsType,
			@RequestParam(value = "searchKeyword", required = false, defaultValue = "") String searchKeyword,
			@RequestParam(value = "pageIndex", required = false, defaultValue = "1") int pageIndex,
			ModelMap model) throws Exception {

		int pageUnit = propertiesService.getInt("pageUnit");
		int pageSize = propertiesService.getInt("pageSize");

		List<Map<String, Object>> merged = new ArrayList<>();

		// 이용약관(stplat)
		if (isBlank(termsType) || "stplat".equals(termsType)) {
			StplatManageDefaultVO svo = new StplatManageDefaultVO();
			svo.setRecordCountPerPage(10000); // 전체 조회 후 병합/페이징은 메모리에서 수행
			svo.setFirstIndex(0);
			if (!isBlank(searchKeyword)) {
				svo.setSearchCondition("useStplatNm");
				svo.setSearchKeyword(searchKeyword);
			}
			List<?> stpList = stplatManageService.selectStplatList(svo);
			for (Object o : stpList) {
				@SuppressWarnings("unchecked")
				Map<String, Object> r = (Map<String, Object>) o;
				merged.add(normalizeStp(r));
			}
			// 검색어가 명칭에 안 걸려도 내용에 걸리는 경우를 위해 내용검색 결과도 병합(중복 제거)
			if (!isBlank(searchKeyword)) {
				StplatManageDefaultVO cvo = new StplatManageDefaultVO();
				cvo.setRecordCountPerPage(10000);
				cvo.setFirstIndex(0);
				cvo.setSearchCondition("useStplatCn");
				cvo.setSearchKeyword(searchKeyword);
				List<?> stpCnList = stplatManageService.selectStplatList(cvo);
				for (Object o : stpCnList) {
					@SuppressWarnings("unchecked")
					Map<String, Object> r = (Map<String, Object>) o;
					Map<String, Object> row = normalizeStp(r);
					if (!containsId(merged, (String) row.get("id"))) {
						merged.add(row);
					}
				}
			}
		}

		// 개인정보처리방침(ipm)
		if (isBlank(termsType) || "ipm".equals(termsType)) {
			ComDefaultVO ivo = new ComDefaultVO();
			ivo.setRecordCountPerPage(10000);
			ivo.setFirstIndex(0);
			if (!isBlank(searchKeyword)) {
				ivo.setSearchCondition("INDVDL_INFO_POLICY_NM");
				ivo.setSearchKeyword(searchKeyword);
			}
			List<?> ipmList = egovIndvdlInfoPolicyService.selectIndvdlInfoPolicyList(ivo);
			for (Object o : ipmList) {
				@SuppressWarnings("unchecked")
				Map<String, Object> r = (Map<String, Object>) o;
				merged.add(normalizeIpm(r));
			}
			if (!isBlank(searchKeyword)) {
				ComDefaultVO icvo = new ComDefaultVO();
				icvo.setRecordCountPerPage(10000);
				icvo.setFirstIndex(0);
				icvo.setSearchCondition("INDVDL_INFO_POLICY_CN");
				icvo.setSearchKeyword(searchKeyword);
				List<?> ipmCnList = egovIndvdlInfoPolicyService.selectIndvdlInfoPolicyList(icvo);
				for (Object o : ipmCnList) {
					@SuppressWarnings("unchecked")
					Map<String, Object> r = (Map<String, Object>) o;
					Map<String, Object> row = normalizeIpm(r);
					if (!containsId(merged, (String) row.get("id"))) {
						merged.add(row);
					}
				}
			}
		}

		// 적용일자(없으면 등록일) 최신순 정렬
		merged.sort(Comparator.comparing(
				(Map<String, Object> r) -> sortKey(r)).reversed());

		int totCnt = merged.size();

		PaginationInfo paginationInfo = new PaginationInfo();
		paginationInfo.setCurrentPageNo(pageIndex);
		paginationInfo.setRecordCountPerPage(pageUnit);
		paginationInfo.setPageSize(pageSize);
		paginationInfo.setTotalRecordCount(totCnt);

		int from = paginationInfo.getFirstRecordIndex();
		int to = Math.min(from + pageUnit, totCnt);
		List<Map<String, Object>> pageList =
				(from <= to && from < totCnt) ? merged.subList(from, to) : Collections.emptyList();

		model.addAttribute("resultList", pageList);
		model.addAttribute("paginationInfo", paginationInfo);
		model.addAttribute("termsType", termsType);
		model.addAttribute("searchKeyword", searchKeyword);
		model.addAttribute("pageIndex", pageIndex);

		return "uss/sam/terms/EgovTermsList";
	}

	/**
	 * 통합 약관 등록 폼.
	 */
	@RequestMapping(value = "/uss/sam/terms/registView.do")
	public String registView(
			@RequestParam(value = "termsType", required = false, defaultValue = "stplat") String termsType,
			ModelMap model) throws Exception {
		if (!isAdmin()) {
			model.addAttribute("message", egovMessageSource.getMessage("fail.common.login"));
			return "uat/uia/EgovLoginUsr";
		}
		model.addAttribute("termsType", termsType);
		return "uss/sam/terms/EgovTermsRegist";
	}

	/**
	 * 통합 약관 등록 처리. 유형에 따라 해당 테이블로 저장.
	 */
	@RequestMapping(value = "/uss/sam/terms/regist.do")
	public String regist(
			@RequestParam(value = "termsType", defaultValue = "stplat") String termsType,
			@RequestParam(value = "nm", required = false) String nm,
			@RequestParam(value = "cn", required = false) String cn,
			@RequestParam(value = "infoProvdAgreCn", required = false) String infoProvdAgreCn,
			@RequestParam(value = "indvdlInfoYn", required = false, defaultValue = "Y") String indvdlInfoYn,
			@RequestParam(value = "ver", required = false) String ver,
			@RequestParam(value = "aplcDe", required = false) String aplcDe,
			@RequestParam(value = "reprsntAt", required = false, defaultValue = "N") String reprsntAt,
			ModelMap model) throws Exception {

		if (!isAdmin()) {
			model.addAttribute("message", egovMessageSource.getMessage("fail.common.login"));
			return "uat/uia/EgovLoginUsr";
		}

		// 필수값 검증(명칭/내용)
		if (isBlank(nm) || isBlank(cn)) {
			model.addAttribute("termsType", termsType);
			model.addAttribute("validationError", true);
			model.addAttribute("nm", nm);
			model.addAttribute("cn", cn);
			model.addAttribute("ver", ver);
			model.addAttribute("aplcDe", aplcDe);
			model.addAttribute("reprsntAt", reprsntAt);
			return "uss/sam/terms/EgovTermsRegist";
		}

		LoginVO loginVO = (LoginVO) EgovUserDetailsHelper.getAuthenticatedUser();
		String userId = loginVO.getUniqId();

		if ("ipm".equals(termsType)) {
			IndvdlInfoPolicy policy = new IndvdlInfoPolicy();
			policy.setIndvdlInfoNm(nm);
			policy.setIndvdlInfoDc(cn);
			policy.setIndvdlInfoYn(isBlank(indvdlInfoYn) ? "Y" : indvdlInfoYn);
			policy.setVer(ver);
			policy.setAplcDe(blankToNull(aplcDe));
			policy.setReprsntAt(isBlank(reprsntAt) ? "N" : reprsntAt);
			policy.setFrstRegisterId(userId);
			policy.setLastUpdusrId(userId);
			egovIndvdlInfoPolicyService.insertIndvdlInfoPolicy(policy);
		} else {
			StplatManageVO vo = new StplatManageVO();
			vo.setUseStplatNm(nm);
			vo.setUseStplatCn(cn);
			vo.setInfoProvdAgreCn(isBlank(infoProvdAgreCn) ? "개인정보 제공에 동의합니다." : infoProvdAgreCn);
			vo.setVer(ver);
			vo.setAplcDe(blankToNull(aplcDe));
			vo.setReprsntAt(isBlank(reprsntAt) ? "N" : reprsntAt);
			vo.setFrstRegisterId(userId);
			vo.setLastUpdusrId(userId);
			stplatManageService.insertStplatCn(vo);
		}

		return "redirect:/uss/sam/terms/list.do?termsType=" + termsType;
	}

	// ===== 내부 유틸 =====

	private Map<String, Object> normalizeStp(Map<String, Object> r) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("termsType", "stplat");
		row.put("termsTypeNm", "이용약관");
		row.put("id", str(r, "useStplatId"));
		row.put("nm", str(r, "useStplatNm"));
		row.put("cn", str(r, "useStplatCn"));
		row.put("ver", str(r, "ver"));
		row.put("aplcDe", str(r, "aplcDe"));
		row.put("reprsntAt", str(r, "reprsntAt"));
		row.put("regDe", str(r, "frstRegistPnttm"));
		return row;
	}

	private Map<String, Object> normalizeIpm(Map<String, Object> r) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("termsType", "ipm");
		row.put("termsTypeNm", "개인정보처리방침");
		row.put("id", str(r, "indvdlInfoId"));
		row.put("nm", str(r, "indvdlInfoNm"));
		row.put("cn", str(r, "indvdlInfoDc"));
		row.put("ver", str(r, "ver"));
		row.put("aplcDe", str(r, "aplcDe"));
		row.put("reprsntAt", str(r, "reprsntAt"));
		row.put("regDe", str(r, "frstRegistPnttm"));
		return row;
	}

	/** egovMap 키 케이싱 대응(소문자/카멜 모두 시도). */
	private String str(Map<String, Object> r, String key) {
		if (r == null) {
			return "";
		}
		Object v = r.get(key);
		if (v == null) {
			v = r.get(key.toLowerCase());
		}
		if (v == null) {
			v = r.get(key.toUpperCase());
		}
		return v == null ? "" : v.toString().trim();
	}

	private boolean containsId(List<Map<String, Object>> list, String id) {
		if (id == null) {
			return false;
		}
		for (Map<String, Object> m : list) {
			if (id.equals(m.get("id"))) {
				return true;
			}
		}
		return false;
	}

	private String sortKey(Map<String, Object> r) {
		String de = (String) r.get("aplcDe");
		if (de == null || de.trim().isEmpty()) {
			String reg = (String) r.get("regDe");
			return reg == null ? "" : reg.replace("-", "");
		}
		return de;
	}

	private String nvl(String s) {
		return s == null ? "" : s;
	}

	private String blankToNull(String s) {
		return (s == null || s.trim().isEmpty()) ? null : s.trim();
	}

	/** YYYYMMDD -> YYYY-MM-DD (그 외 형식은 원본 반환) */
	private String formatDe(String de) {
		if (de == null) {
			return "";
		}
		String d = de.trim();
		if (d.length() == 8 && d.chars().allMatch(Character::isDigit)) {
			return d.substring(0, 4) + "-" + d.substring(4, 6) + "-" + d.substring(6, 8);
		}
		return d;
	}
}
