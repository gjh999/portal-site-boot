package egovframework.let.uss.umt.service;

/**
 * 마이페이지(본인) 회원정보 조회/수정 비지니스 인터페이스.
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   수정일      수정자           수정내용
 *  -------    --------    ---------------------------
 *  2026.06.23  Claude        마이페이지(본인) 기능 신규
 * </pre>
 */
public interface EgovMypageService {

	/**
	 * 로그인 본인의 회원정보를 조회한다.
	 * @param vo uniqId(ESNTL_ID), userSe 보유
	 * @return 본인 회원정보
	 */
	MypageVO selectMyInfo(MypageVO vo);

	/**
	 * 로그인 본인의 회원정보를 수정한다.
	 * @param vo 수정정보(uniqId, userSe 기준)
	 */
	void updateMyInfo(MypageVO vo);

	/**
	 * 로그인 본인의 현재 암호화된 비밀번호를 조회한다.
	 * @param vo uniqId, userSe 보유
	 * @return 암호화된 비밀번호를 담은 VO
	 */
	MypageVO selectMyPassword(MypageVO vo);

	/**
	 * 로그인 본인의 비밀번호를 변경한다.
	 * @param vo password(암호화 완료), uniqId, userSe 보유
	 */
	void updateMyPassword(MypageVO vo);
}
