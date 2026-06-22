package egovframework.let.uss.ion.bnr.web;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import egovframework.let.uss.ion.bnr.service.BannerVO;
import egovframework.let.uss.ion.bnr.service.EgovBannerService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * 모든 화면(레이아웃 fragment 포함)에서 사용할 배너 정보를 모델에 주입하는 ControllerAdvice.
 *
 * <pre>
 * - footerBannerList : 푸터 슬라이드 배너(BANNER_TY='FOOTER', REFLCT_AT='Y')
 * - popupBannerList  : 페이지 진입 팝업 배너(BANNER_TY='POPUP', REFLCT_AT='Y')
 * - bannerInterval   : 슬라이드 자동 전환 주기(ms). portal.banner.interval 프로퍼티(기본 5000)
 * 메인 배너(MAIN)는 홈 화면 전용이므로 EgovMainController 에서 별도로 모델에 담는다.
 * 데이터 계층 오류가 나도 화면이 렌더링되도록 방어적으로 빈 목록을 제공한다.
 * </pre>
 *
 * @author Boot 전환
 */
@Slf4j
@ControllerAdvice
public class EgovBannerGlobalAdvice {

	@Resource(name = "egovBannerService")
	private EgovBannerService egovBannerService;

	@Value("${portal.banner.interval:5000}")
	private long bannerInterval;

	/**
	 * 슬라이드 자동 전환 주기(ms)를 모든 모델에 주입한다.
	 */
	@ModelAttribute("bannerInterval")
	public long bannerInterval() {
		return bannerInterval;
	}

	/**
	 * 푸터 배너 목록을 모든 모델에 주입한다.
	 */
	@ModelAttribute("footerBannerList")
	public List<BannerVO> footerBannerList() {
		return selectByType("FOOTER");
	}

	/**
	 * 팝업 배너 목록을 모든 모델에 주입한다.
	 */
	@ModelAttribute("popupBannerList")
	public List<BannerVO> popupBannerList() {
		return selectByType("POPUP");
	}

	private List<BannerVO> selectByType(String bannerTy) {
		try {
			BannerVO bannerVO = new BannerVO();
			bannerVO.setBannerTy(bannerTy);
			List<BannerVO> list = egovBannerService.selectBannerListByType(bannerVO);
			return list == null ? List.of() : list;
		} catch (Exception e) {
			log.warn("배너 조회 실패(type={}): {}", bannerTy, e.getMessage());
			return List.of();
		}
	}
}
