package egovframework.let.uss.umt.web;

import org.egovframe.rte.fdl.security.userdetails.util.EgovUserDetailsHelper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import egovframework.com.cmm.EgovMessageSource;
import egovframework.com.cmm.LoginVO;
import egovframework.let.uss.umt.service.EgovMypageService;
import egovframework.let.uss.umt.service.MypageVO;
import egovframework.let.utl.sim.service.EgovFileScrty;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 마이페이지(본인) — 회원정보 수정 + 비밀번호 변경.
 * 로그인 본인(세션 ESNTL_ID)만 접근/수정한다. userSe 에 따라 대상 테이블이 분기된다.
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   수정일      수정자           수정내용
 *  -------    --------    ---------------------------
 *  2026.06.23  Claude        마이페이지(본인) 기능 신규
 *   2026.07.02  구재호        Spring Boot + Thymeleaf + KRDS + 다국어(i18n) 전환
 * </pre>
 */
@Controller
public class EgovMypageController {

	@Resource(name = "mypageService")
	private EgovMypageService mypageService;

	@Resource(name = "egovMessageSource")
	private EgovMessageSource egovMessageSource;

	/**
	 * 마이페이지 화면(탭: 회원정보 수정 / 비밀번호 변경).
	 */
	@GetMapping("/mypage")
	public String mypage(ModelMap model) {
		if (Boolean.FALSE.equals(EgovUserDetailsHelper.isAuthenticated())) {
			model.addAttribute("message", egovMessageSource.getMessage("fail.common.login"));
			return "uat/uia/EgovLoginUsr";
		}
		LoginVO loginVO = (LoginVO) EgovUserDetailsHelper.getAuthenticatedUser();
		model.addAttribute("myInfo", loadMyInfo(loginVO));
		return "cmm/uss/umt/EgovMberMypage";
	}

	/**
	 * 본인 회원정보 수정 처리.
	 */
	@PostMapping("/mypage/info")
	public String updateInfo(MypageVO form, ModelMap model, HttpServletRequest request) {
		if (Boolean.FALSE.equals(EgovUserDetailsHelper.isAuthenticated())) {
			model.addAttribute("message", egovMessageSource.getMessage("fail.common.login"));
			return "uat/uia/EgovLoginUsr";
		}
		LoginVO loginVO = (LoginVO) EgovUserDetailsHelper.getAuthenticatedUser();

		// 세션 본인 정보로 식별자 고정(폼 위변조 방지)
		form.setUniqId(loginVO.getUniqId());
		form.setUserSe(loginVO.getUserSe());
		form.setUserId(loginVO.getId());
		mypageService.updateMyInfo(form);

		// 세션의 표시 이름 갱신(헤더 등 반영)
		loginVO.setName(form.getUserNm());
		Object sessionLogin = request.getSession().getAttribute("LoginVO");
		if (sessionLogin instanceof LoginVO) {
			((LoginVO) sessionLogin).setName(form.getUserNm());
		}

		model.addAttribute("myInfo", loadMyInfo(loginVO));
		model.addAttribute("activeTab", "info");
		model.addAttribute("resultMsg", "mypage.msg.info.success");
		return "cmm/uss/umt/EgovMberMypage";
	}

	/**
	 * 본인 비밀번호 변경 처리.
	 */
	@PostMapping("/mypage/password")
	public String updatePassword(@RequestParam("oldPassword") String oldPassword,
			@RequestParam("newPassword") String newPassword,
			@RequestParam("newPassword2") String newPassword2,
			ModelMap model) throws Exception {

		if (Boolean.FALSE.equals(EgovUserDetailsHelper.isAuthenticated())) {
			model.addAttribute("message", egovMessageSource.getMessage("fail.common.login"));
			return "uat/uia/EgovLoginUsr";
		}
		LoginVO loginVO = (LoginVO) EgovUserDetailsHelper.getAuthenticatedUser();

		MypageVO key = new MypageVO();
		key.setUniqId(loginVO.getUniqId());
		key.setUserSe(loginVO.getUserSe());

		MypageVO current = mypageService.selectMyPassword(key);
		String encOld = EgovFileScrty.encryptPassword(oldPassword, loginVO.getId());

		String resultMsg;
		if (current == null || !encOld.equals(current.getPassword())) {
			resultMsg = "mypage.msg.pwd.fail.old";
		} else if (!newPassword.equals(newPassword2)) {
			resultMsg = "mypage.msg.pwd.fail.confirm";
		} else {
			key.setPassword(EgovFileScrty.encryptPassword(newPassword, loginVO.getId()));
			mypageService.updateMyPassword(key);
			resultMsg = "mypage.msg.pwd.success";
		}

		model.addAttribute("myInfo", loadMyInfo(loginVO));
		model.addAttribute("activeTab", "password");
		model.addAttribute("resultMsg", resultMsg);
		return "cmm/uss/umt/EgovMberMypage";
	}

	/** 로그인 본인의 회원정보를 조회하되, 조회 실패 시 세션값으로 최소 구성한다. */
	private MypageVO loadMyInfo(LoginVO loginVO) {
		MypageVO key = new MypageVO();
		key.setUniqId(loginVO.getUniqId());
		key.setUserSe(loginVO.getUserSe());
		MypageVO info = mypageService.selectMyInfo(key);
		if (info == null) {
			info = new MypageVO();
			info.setUniqId(loginVO.getUniqId());
			info.setUserSe(loginVO.getUserSe());
			info.setUserId(loginVO.getId());
			info.setUserNm(loginVO.getName());
			info.setEmail(loginVO.getEmail());
		}
		return info;
	}
}
