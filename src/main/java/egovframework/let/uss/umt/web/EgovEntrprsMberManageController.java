package egovframework.let.uss.umt.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import egovframework.let.uss.umt.service.EgovEntrprsMberManageService;
import egovframework.let.uss.umt.service.EntrprsMberManageVO;
import jakarta.annotation.Resource;

/**
 * 기업회원 가입 관련 요청을 처리하는 Controller.
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   수정일      수정자           수정내용
 *  -------    --------    ---------------------------
 *  2026.06.23  Claude        기업회원 가입 기능 신규
 * </pre>
 */
@Controller
public class EgovEntrprsMberManageController {

	@Resource(name = "entrprsMberManageService")
	private EgovEntrprsMberManageService entrprsMberManageService;

	/**
	 * 회원가입 유형 선택 화면(일반회원 / 기업회원).
	 */
	@RequestMapping("/uss/umt/cmm/EgovMberSbscrbSelect.do")
	public String sbscrbSelect() {
		return "cmm/uss/umt/EgovMberSbscrbSelect";
	}

	/**
	 * 기업회원 약관 확인 화면(가입 진입점).
	 */
	@RequestMapping("/uss/umt/cmm/EgovStplatCnfirmEntrprs.do")
	public String stplatCnfirmEntrprs(Model model) throws Exception {
		// 기업회원용 약관(일반회원과 동일 약관 시드 사용)
		String stplatId = "STPLAT_0000000000001";
		model.addAttribute("stplatList", entrprsMberManageService.selectStplat(stplatId));
		model.addAttribute("sbscrbTy", "USR02"); // 기업회원
		return "cmm/uss/umt/EgovStplatCnfirmEntrprs";
	}

	/**
	 * 기업회원 가입 폼 화면으로 이동한다.
	 */
	@RequestMapping("/uss/umt/cmm/EgovEntrprsMberSbscrbView.do")
	public String entrprsMberSbscrbView(@ModelAttribute("entrprsMberManageVO") EntrprsMberManageVO vo, Model model) throws Exception {
		// @Valid 통과용 기본값(컨트롤러가 제출 시 재설정)
		vo.setGroupId("GROUP_00000000000001");
		vo.setEntrprsMberSttus("P");
		return "cmm/uss/umt/EgovEntrprsMberSbscrb";
	}

	/**
	 * 기업회원 가입처리 후 로그인 화면으로 이동한다.
	 */
	@RequestMapping("/uss/umt/cmm/EgovEntrprsMberSbscrb.do")
	public String entrprsMberSbscrb(@jakarta.validation.Valid @ModelAttribute("entrprsMberManageVO") EntrprsMberManageVO vo,
			BindingResult bindingResult, Model model) throws Exception {

		if (bindingResult.hasErrors()) {
			vo.setGroupId("GROUP_00000000000001");
			vo.setEntrprsMberSttus("P");
			return "cmm/uss/umt/EgovEntrprsMberSbscrb";
		}

		// 가입 후 즉시 로그인 가능하도록 상태 'P'(승인), 일반사용자 그룹 고정
		vo.setEntrprsMberSttus("P");
		vo.setGroupId("GROUP_00000000000001");
		entrprsMberManageService.insertEntrprsMber(vo);
		return "redirect:/uat/uia/egovLoginUsr.do";
	}
}
