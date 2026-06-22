package egovframework.com.cmm;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * @Class Name : ComDefaultVO.java
 * @Description : ComDefaultVO class
 * @Modification Information
 * @
 * @  수정일         수정자                   수정내용
 * @ -------    --------    ---------------------------
 * @ 2009.02.01    조재영         최초 생성
 *
 *  @author 공통서비스 개발팀 조재영
 *  @since 2009.02.01
 *  @version 1.0
 *  @see
 *
 */
public class ComDefaultVO implements Serializable {

	private static final long serialVersionUID = -6062858939907510631L;

	/** 검색조건 */
    private String searchCondition = "";

    /** 검색Keyword */
    private String searchKeyword = "";

    /** 검색사용여부 */
    private String searchUseYn = "";

    /** 현재페이지 */
    private int pageIndex = 1;

    /** 페이지갯수 */
    private int pageUnit = 10;

    /** 페이지사이즈 */
    private int pageSize = 10;

    /** firstIndex */
    private int firstIndex = 1;

    /** lastIndex */
    private int lastIndex = 1;

    /** recordCountPerPage */
    private int recordCountPerPage = 10;

    /** 검색KeywordFrom */
    private String searchKeywordFrom = "";

	/** 검색KeywordTo */
    private String searchKeywordTo = "";

    /** (설문) 설문대상 유형 필터: ''=전체, 'MINE'=내 직업유형/전체대상만 */
    private String surveyTrgetFilter = "";

    /** (설문) 참여여부 필터: ''=전체, 'Y'=참여완료, 'N'=미참여 */
    private String surveyPartcptn = "";

    /** (설문) 로그인 사용자 고유아이디(ESNTL_ID) — 참여여부 판정용 */
    private String loginEsntlId = "";

    /** (설문) 로그인 사용자 직업유형(COM034) — 설문대상 필터용 */
    private String loginOccp = "";

    public String getSurveyTrgetFilter() { return surveyTrgetFilter; }
    public void setSurveyTrgetFilter(String surveyTrgetFilter) { this.surveyTrgetFilter = surveyTrgetFilter; }
    public String getSurveyPartcptn() { return surveyPartcptn; }
    public void setSurveyPartcptn(String surveyPartcptn) { this.surveyPartcptn = surveyPartcptn; }
    public String getLoginEsntlId() { return loginEsntlId; }
    public void setLoginEsntlId(String loginEsntlId) { this.loginEsntlId = loginEsntlId; }
    public String getLoginOccp() { return loginOccp; }
    public void setLoginOccp(String loginOccp) { this.loginOccp = loginOccp; }

	public int getFirstIndex() {
		return firstIndex;
	}

	public void setFirstIndex(int firstIndex) {
		this.firstIndex = firstIndex;
	}

	public int getLastIndex() {
		return lastIndex;
	}

	public void setLastIndex(int lastIndex) {
		this.lastIndex = lastIndex;
	}

	public int getRecordCountPerPage() {
		return recordCountPerPage;
	}

	public void setRecordCountPerPage(int recordCountPerPage) {
		this.recordCountPerPage = recordCountPerPage;
	}

	public String getSearchCondition() {
        return searchCondition;
    }

    public void setSearchCondition(String searchCondition) {
        this.searchCondition = searchCondition;
    }

    public String getSearchKeyword() {
        return searchKeyword;
    }

    public void setSearchKeyword(String searchKeyword) {
        this.searchKeyword = searchKeyword;
    }

    public String getSearchUseYn() {
        return searchUseYn;
    }

    public void setSearchUseYn(String searchUseYn) {
        this.searchUseYn = searchUseYn;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }

    public int getPageUnit() {
        return pageUnit;
    }

    public void setPageUnit(int pageUnit) {
        this.pageUnit = pageUnit;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    @Override
	public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }


    /**
	 * searchKeywordFrom attribute를 리턴한다.
	 * @return String
	 */
	public String getSearchKeywordFrom() {
		return searchKeywordFrom;
	}

	/**
	 * searchKeywordFrom attribute 값을 설정한다.
	 * @param searchKeywordFrom String
	 */
	public void setSearchKeywordFrom(String searchKeywordFrom) {
		this.searchKeywordFrom = searchKeywordFrom;
	}

	/**
	 * searchKeywordTo attribute를 리턴한다.
	 * @return String
	 */
	public String getSearchKeywordTo() {
		return searchKeywordTo;
	}

	/**
	 * searchKeywordTo attribute 값을 설정한다.
	 * @param searchKeywordTo String
	 */
	public void setSearchKeywordTo(String searchKeywordTo) {
		this.searchKeywordTo = searchKeywordTo;
	}
}
