package egovframework.let.uss.olp.qmc.web;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.egovframe.rte.fdl.property.EgovPropertyService;
import org.egovframe.rte.fdl.security.userdetails.util.EgovUserDetailsHelper;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import egovframework.com.cmm.ComDefaultCodeVO;
import egovframework.com.cmm.ComDefaultVO;
import egovframework.com.cmm.EgovMessageSource;
import egovframework.com.cmm.LoginVO;
import egovframework.com.cmm.service.EgovCmmUseService;
import egovframework.let.uss.olp.qmc.service.EgovQustnrManageService;
import egovframework.let.uss.olp.qmc.service.QustnrManageVO;
import egovframework.let.uss.olp.qqm.service.EgovQustnrQestnManageService;
import egovframework.let.uss.olp.qqm.service.QustnrQestnManageVO;
import egovframework.let.uss.olp.qim.service.EgovQustnrItemManageService;
import egovframework.let.uss.olp.qim.service.QustnrItemManageVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * 설문관리를 처리하는 Controller Class 구현
 * @author 공통서비스 장동한
 * @since 2009.03.20
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      수정자           수정내용
 *  -------    --------    ---------------------------
 *   2009.03.20  장동한          최초 생성
 *   2011.08.31  JJY            경량환경 템플릿 커스터마이징버전 생성
 *  2026.06.17  구재호          Spring Boot + Thymeleaf 전환
 *
 * </pre>
 */
@Controller
public class EgovQustnrManageController {

	private static final Logger LOGGER = LoggerFactory.getLogger(EgovQustnrManageController.class);

	/** EgovMessageSource */
	@Resource(name = "egovMessageSource")
	EgovMessageSource egovMessageSource;

	@Resource(name = "egovQustnrManageService")
	private EgovQustnrManageService egovQustnrManageService;

	@Resource(name = "egovQustnrQestnManageService")
	private EgovQustnrQestnManageService egovQustnrQestnManageService;

	@Resource(name = "egovQustnrItemManageService")
	private EgovQustnrItemManageService egovQustnrItemManageService;

	/** EgovPropertyService */
	@Resource(name = "propertiesService")
	protected EgovPropertyService propertiesService;

	@Resource(name = "EgovCmmUseService")
	private EgovCmmUseService cmmUseService;

	/**
	 * 설문관리 팝업 목록을 조회한다.
	 * @param searchVO
	 * @param commandMap
	 * @param qustnrManageVO
	 * @param model
	 * @return "/uss/olp/qmc/EgovQustnrManageListPopup"
	 * @throws Exception
	 */
	@RequestMapping(value = "/uss/olp/qmc/EgovQustnrManageListPopup.do")
	public String EgovQustnrManageListPopup(@ModelAttribute("searchVO") ComDefaultVO searchVO, @RequestParam Map<String, Object> commandMap, QustnrManageVO qustnrManageVO,
			ModelMap model) throws Exception {

		String sCmd = commandMap.get("cmd") == null ? "" : (String) commandMap.get("cmd");
		if (sCmd.equals("del")) {
			egovQustnrManageService.deleteQustnrManage(qustnrManageVO);
		}

		/** EgovPropertyService.sample */
		searchVO.setPageUnit(propertiesService.getInt("pageUnit"));
		searchVO.setPageSize(propertiesService.getInt("pageSize"));

		/** pageing */
		PaginationInfo paginationInfo = new PaginationInfo();
		paginationInfo.setCurrentPageNo(searchVO.getPageIndex());
		paginationInfo.setRecordCountPerPage(searchVO.getPageUnit());
		paginationInfo.setPageSize(searchVO.getPageSize());

		searchVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
		searchVO.setLastIndex(paginationInfo.getLastRecordIndex());
		searchVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

		model.addAttribute("resultList", egovQustnrManageService.selectQustnrManageList(searchVO));

		model.addAttribute("searchKeyword", commandMap.get("searchKeyword") == null ? "" : (String) commandMap.get("searchKeyword"));
		model.addAttribute("searchCondition", commandMap.get("searchCondition") == null ? "" : (String) commandMap.get("searchCondition"));

		int totCnt = egovQustnrManageService.selectQustnrManageListCnt(searchVO);
		paginationInfo.setTotalRecordCount(totCnt);
		model.addAttribute("paginationInfo", paginationInfo);

		return "uss/olp/qmc/EgovQustnrManageListPopup";
	}

	/**
	 * 설문관리 목록을 조회한다.
	 * @param searchVO
	 * @param commandMap
	 * @param qustnrManageVO
	 * @param model
	 * @return  "/uss/olp/qmc/EgovQustnrManageList"
	 * @throws Exception
	 */
	@RequestMapping(value = "/uss/olp/qmc/EgovQustnrManageList.do")
	public String EgovQustnrManageList(@ModelAttribute("searchVO") ComDefaultVO searchVO, @RequestParam Map<String, Object> commandMap, QustnrManageVO qustnrManageVO,
			ModelMap model, HttpServletRequest request) throws Exception {

		// 메인화면에서 넘어온 경우 메뉴 갱신을 위해 추가
		request.getSession().setAttribute("menuNo", "5000000");
		
		String sCmd = commandMap.get("cmd") == null ? "" : (String) commandMap.get("cmd");
		if (sCmd.equals("del")) {
			egovQustnrManageService.deleteQustnrManage(qustnrManageVO);
		}

		/** EgovPropertyService.sample */
		searchVO.setPageUnit(propertiesService.getInt("pageUnit"));
		searchVO.setPageSize(propertiesService.getInt("pageSize"));

		/** pageing */
		PaginationInfo paginationInfo = new PaginationInfo();
		paginationInfo.setCurrentPageNo(searchVO.getPageIndex());
		paginationInfo.setRecordCountPerPage(searchVO.getPageUnit());
		paginationInfo.setPageSize(searchVO.getPageSize());

		searchVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
		searchVO.setLastIndex(paginationInfo.getLastRecordIndex());
		searchVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

		model.addAttribute("resultList", egovQustnrManageService.selectQustnrManageList(searchVO));

		model.addAttribute("searchKeyword", commandMap.get("searchKeyword") == null ? "" : (String) commandMap.get("searchKeyword"));
		model.addAttribute("searchCondition", commandMap.get("searchCondition") == null ? "" : (String) commandMap.get("searchCondition"));

		int totCnt = egovQustnrManageService.selectQustnrManageListCnt(searchVO);
		paginationInfo.setTotalRecordCount(totCnt);
		model.addAttribute("paginationInfo", paginationInfo);

		return "uss/olp/qmc/EgovQustnrManageList";
	}

	/**
	 * 설문관리 목록을 상세조회 조회한다.
	 * @param searchVO
	 * @param qustnrManageVO
	 * @param commandMap
	 * @param model
	 * @return "/uss/olp/qmc/EgovQustnrManageDetail";
	 * @throws Exception
	 */
	@RequestMapping(value = "/uss/olp/qmc/EgovQustnrManageDetail.do")
	public String EgovQustnrManageDetail(@ModelAttribute("searchVO") ComDefaultVO searchVO, QustnrManageVO qustnrManageVO, @RequestParam Map<String, Object> commandMap,
			ModelMap model) throws Exception {

		String sLocationUrl = "uss/olp/qmc/EgovQustnrManageDetail";

		String sCmd = commandMap.get("cmd") == null ? "" : (String) commandMap.get("cmd");

		if (sCmd.equals("del")) {
			egovQustnrManageService.deleteQustnrManage(qustnrManageVO);
			sLocationUrl = "redirect:/uss/olp/qmc/EgovQustnrManageList.do";
		} else {

			//공통코드  직업유형 조회
			ComDefaultCodeVO voComCode = new ComDefaultCodeVO();
			voComCode.setCodeId("COM034");
			model.addAttribute("comCode034", cmmUseService.selectCmmCodeDetail(voComCode));

			model.addAttribute("resultList", egovQustnrManageService.selectQustnrManageDetail(qustnrManageVO));
		}

		return sLocationUrl;
	}

	/**
	 * 설문관리를 수정한다.
	 * @param searchVO
	 * @param commandMap
	 * @param qustnrManageVO
	 * @param bindingResult
	 * @param model
	 * @return "/uss/olp/qmc/EgovQustnrManageModify"
	 * @throws Exception
	 */
	@RequestMapping(value = "/uss/olp/qmc/EgovQustnrManageModify.do")
	public String QustnrManageModify(@ModelAttribute("searchVO") ComDefaultVO searchVO, @RequestParam Map<String, Object> commandMap,
			@Valid @ModelAttribute("qustnrManageVO") QustnrManageVO qustnrManageVO,
			BindingResult bindingResult, ModelMap model) throws Exception {
		// 0. Spring Security 사용자권한 처리
		Boolean isAuthenticated = EgovUserDetailsHelper.isAuthenticated();
		if (!isAuthenticated) {
			model.addAttribute("message", egovMessageSource.getMessage("fail.common.login"));
			return "uat/uia/EgovLoginUsr";
		}

		//로그인 객체 선언
		LoginVO loginVO = (LoginVO) EgovUserDetailsHelper.getAuthenticatedUser();

		String sLocationUrl = "uss/olp/qmc/EgovQustnrManageModify";

		String sCmd = commandMap.get("cmd") == null ? "" : (String) commandMap.get("cmd");

		// GET 요청 등 초기 진입시에는 검증 에러를 노출하지 않는다.
		if (!"save".equals(sCmd)) {
			bindingResult = new BeanPropertyBindingResult(qustnrManageVO, "qustnrManageVO");
			model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "qustnrManageVO", bindingResult);
		}

		//공통코드  직업유형 조회
		ComDefaultCodeVO voComCode = new ComDefaultCodeVO();
		voComCode.setCodeId("COM034");
		model.addAttribute("comCode034", cmmUseService.selectCmmCodeDetail(voComCode));

		if (sCmd.equals("save")) {

			if (bindingResult.hasErrors()) {

				model.addAttribute("resultList", egovQustnrManageService.selectQustnrManageDetail(qustnrManageVO));

				//설문템플릿 정보 불러오기
				model.addAttribute("listQustnrTmplat", egovQustnrManageService.selectQustnrTmplatManageList(qustnrManageVO));

				return sLocationUrl;
			}

			//아이디 설정
			qustnrManageVO.setFrstRegisterId(loginVO.getUniqId());
			qustnrManageVO.setLastUpdusrId(loginVO.getUniqId());

			egovQustnrManageService.updateQustnrManage(qustnrManageVO);
			sLocationUrl = "redirect:/uss/olp/qmc/EgovQustnrManageList.do";
		} else {
			model.addAttribute("resultList", egovQustnrManageService.selectQustnrManageDetail(qustnrManageVO));

			QustnrManageVO newQustnrManageVO = egovQustnrManageService.selectQustnrManageDetailModel(qustnrManageVO);
			model.addAttribute("qustnrManageVO", newQustnrManageVO);

			//설문템플릿 정보 불러오기
			model.addAttribute("listQustnrTmplat", egovQustnrManageService.selectQustnrTmplatManageList(qustnrManageVO));
		}

		return sLocationUrl;
	}

	/**
	 * 설문관리를 등록한다.
	 * @param searchVO
	 * @param commandMap
	 * @param qustnrManageVO
	 * @param bindingResult
	 * @param model
	 * @return "/uss/olp/qmc/EgovQustnrManageRegist"
	 * @throws Exception
	 */
	@RequestMapping(value = "/uss/olp/qmc/EgovQustnrManageRegist.do")
	public String QustnrManageRegist(@ModelAttribute("searchVO") ComDefaultVO searchVO, @RequestParam Map<String, Object> commandMap,
			@Valid @ModelAttribute("qustnrManageVO") QustnrManageVO qustnrManageVO, BindingResult bindingResult, ModelMap model) throws Exception {
		// 0. Spring Security 사용자권한 처리
		
		Boolean isAuthenticated = EgovUserDetailsHelper.isAuthenticated();
		if (!isAuthenticated) {
			model.addAttribute("message", egovMessageSource.getMessage("fail.common.login"));
			return "uat/uia/EgovLoginUsr";
		}

		//로그인 객체 선언
		LoginVO loginVO = (LoginVO) EgovUserDetailsHelper.getAuthenticatedUser();

		String sLocationUrl = "uss/olp/qmc/EgovQustnrManageRegist";

		String sCmd = commandMap.get("cmd") == null ? "" : (String) commandMap.get("cmd");
		LOGGER.info("cmd => {}", sCmd);

		// 초기 페이지 로드 시에는 검증 에러 무시
		if (!"save".equals(sCmd)) {
			bindingResult = new BeanPropertyBindingResult(qustnrManageVO, "qustnrManageVO");
			model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "qustnrManageVO", bindingResult);

			// 설문 기간 기본값: 시작일=오늘, 종료일=오늘+7일 (yyyy-MM-dd)
			java.time.LocalDate today = java.time.LocalDate.now();
			if (qustnrManageVO.getQestnrBeginDe() == null || qustnrManageVO.getQestnrBeginDe().isEmpty()) {
				qustnrManageVO.setQestnrBeginDe(today.toString());
			}
			if (qustnrManageVO.getQestnrEndDe() == null || qustnrManageVO.getQestnrEndDe().isEmpty()) {
				qustnrManageVO.setQestnrEndDe(today.plusDays(7).toString());
			}
		}

		//공통코드  직업유형 조회
		ComDefaultCodeVO voComCode = new ComDefaultCodeVO();
		voComCode.setCodeId("COM034");
		model.addAttribute("comCode034", cmmUseService.selectCmmCodeDetail(voComCode));

		if (sCmd.equals("save")) {

			if (bindingResult.hasErrors()) {
				//설문템플릿 정보 불러오기
				model.addAttribute("listQustnrTmplat", egovQustnrManageService.selectQustnrTmplatManageList(qustnrManageVO));
				return sLocationUrl;
			}

			//아이디 설정
			qustnrManageVO.setFrstRegisterId(loginVO.getUniqId());
			qustnrManageVO.setLastUpdusrId(loginVO.getUniqId());

			egovQustnrManageService.insertQustnrManage(qustnrManageVO);

			// 모달로 등록한 문항/보기 저장 (questionsJson 파라미터)
			String questionsJson = commandMap.get("questionsJson") == null ? "" : (String) commandMap.get("questionsJson");
			saveQuestions(questionsJson, qustnrManageVO.getQestnrId(), qustnrManageVO.getQestnrTmplatId(), loginVO.getUniqId());

			sLocationUrl = "redirect:/uss/olp/qmc/EgovQustnrManageList.do";
		} else {
			//설문템플릿 정보 불러오기
			model.addAttribute("listQustnrTmplat", egovQustnrManageService.selectQustnrTmplatManageList(qustnrManageVO));
		}

		return sLocationUrl;
	}

	/**
	 * 설문 등록 모달에서 작성한 문항/보기를 저장한다.
	 * questionsJson 형식: [{"type":"1","content":"...","multi":true,"items":[{"content":"...","etc":false}]}, ...]
	 * type "1"=객관식(보기 있음), "2"=서술형(보기 없음).
	 * @param questionsJson 문항 JSON 문자열
	 * @param qestnrId 설문지ID
	 * @param qestnrTmplatId 설문템플릿ID
	 * @param uniqId 등록자 고유아이디
	 */
	private void saveQuestions(String questionsJson, String qestnrId, String qestnrTmplatId, String uniqId) throws Exception {
		if (questionsJson == null || questionsJson.trim().isEmpty() || qestnrTmplatId == null || qestnrTmplatId.isEmpty()) {
			return;
		}
		ObjectMapper om = new ObjectMapper();
		JsonNode root;
		try {
			root = om.readTree(questionsJson);
		} catch (Exception e) {
			LOGGER.warn("questionsJson 파싱 실패: {}", e.getMessage());
			return;
		}
		if (root == null || !root.isArray()) {
			return;
		}

		int qSn = 1;
		for (JsonNode q : root) {
			String type = q.path("type").asText("1");
			String content = q.path("content").asText("");
			if (content.trim().isEmpty()) {
				continue;
			}
			boolean multi = q.path("multi").asBoolean(false);

			QustnrQestnManageVO qVO = new QustnrQestnManageVO();
			qVO.setQestnrId(qestnrId);
			qVO.setQestnrTmplatId(qestnrTmplatId);
			qVO.setQestnSn(String.valueOf(qSn));
			qVO.setQestnTyCode("2".equals(type) ? "2" : "1");
			qVO.setQestnCn(content);
			// 객관식만 최대선택건수 설정, 서술형은 null(숫자 컬럼에 빈문자 금지)
			qVO.setMxmmChoiseCo("1".equals(qVO.getQestnTyCode()) ? (multi ? "9" : "1") : null);
			qVO.setFrstRegisterId(uniqId);
			qVO.setLastUpdusrId(uniqId);
			// insertQustnrQestnManage 가 QQESTN_* ID를 채번하여 qVO 에 설정한다.
			egovQustnrQestnManageService.insertQustnrQestnManage(qVO);

			if ("1".equals(qVO.getQestnTyCode())) {
				JsonNode items = q.path("items");
				int iSn = 1;
				if (items.isArray()) {
					for (JsonNode it : items) {
						String iemCn = it.path("content").asText("");
						if (iemCn.trim().isEmpty()) {
							continue;
						}
						boolean etc = it.path("etc").asBoolean(false);
						QustnrItemManageVO iVO = new QustnrItemManageVO();
						iVO.setQestnrQesitmId(qVO.getQestnrQesitmId());
						iVO.setQestnrId(qestnrId);
						iVO.setQestnrTmplatId(qestnrTmplatId);
						iVO.setIemSn(String.valueOf(iSn));
						iVO.setIemCn(iemCn);
						iVO.setEtcAnswerAt(etc ? "Y" : "N");
						iVO.setFrstRegisterId(uniqId);
						iVO.setLastUpdusrId(uniqId);
						egovQustnrItemManageService.insertQustnrItemManage(iVO);
						iSn++;
					}
				}
			}
			qSn++;
		}
	}
}
