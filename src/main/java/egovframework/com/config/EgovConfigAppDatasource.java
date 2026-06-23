package egovframework.com.config;

import java.io.File;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import lombok.extern.slf4j.Slf4j;

/**
 * @ClassName : EgovConfigAppDatasource.java
 * @Description : DataSource 설정
 *
 * @author : 윤주호
 * @since  : 2021. 7. 20
 * @version : 1.0
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일              수정자               수정내용
 *  -------------  ------------   ---------------------
 *   2021. 7. 20    윤주호               최초 생성
 *  2026.06.17  구재호          Spring Boot + Thymeleaf 전환
 * </pre>
 *
 */
@Slf4j
@Configuration
public class EgovConfigAppDatasource {

    /**
     * 개발용 파일 기반 HSQL DB 경로(프로젝트 루트 하위 .localdb/portaldb).
     * 이 디렉터리는 .gitignore 로 제외(커밋 금지)되며, 스키마(DDL)가 바뀌면 디렉터리를 지우고
     * 재기동하면 db/shtdb.sql 로 재초기화된다.
     */
    private static final String HSQL_DB_DIR = "./.localdb";
    private static final String HSQL_DB_NAME = "portaldb";

    private final Environment env;

    public EgovConfigAppDatasource(Environment env) {
        this.env = env;
    }

	private String dbType;

	private String className;

	private String url;

	private String userName;

	private String password;

	@PostConstruct
	void init() {
		dbType = env.getProperty("Globals.DbType");
		//Exception 처리 필요
		className = env.getProperty("Globals." + dbType + ".DriverClassName");
		url = env.getProperty("Globals." + dbType + ".Url");
		userName = env.getProperty("Globals." + dbType + ".UserName");
		password = env.getProperty("Globals." + dbType + ".Password");
	}

	/**
	 * [dataSource 설정] 개발용 HSQL 설정 — <b>파일 기반 영속</b>.
	 *
	 * <pre>
	 * 기존: EmbeddedDatabaseBuilder(in-memory) 가 매 (재)기동마다 db/shtdb.sql 을 재실행 →
	 *       런타임 변경(배너 등록/수정 등)이 재기동(devtools 자동 재기동 포함) 때마다 시드값으로 초기화됐다.
	 * 변경: jdbc:hsqldb:file:./.localdb/portaldb 로 파일에 영속화하고, shtdb.sql(스키마+시드)은
	 *       DB 파일이 <b>아직 없을 때(최초 1회)만</b> 실행한다. 이후 기동에서는 디스크의 데이터를 그대로 사용 →
	 *       런타임 변경이 재기동 후에도 유지된다.
	 *
	 * 트레이드오프:
	 *  - shtdb.sql 은 'DROP TABLE IF EXISTS … / INSERT …' 형태라 매번 실행하면 데이터가 날아간다.
	 *    그래서 '파일이 없을 때만' 실행하도록 가드했다.
	 *  - 따라서 <b>스키마(DDL)나 시드를 변경했다면 ./.localdb 디렉터리를 삭제 후 재기동</b>해야 새 스키마/시드가 반영된다.
	 *  - .localdb 는 .gitignore 로 제외(커밋 금지). 운영(jar, Globals.DbType=postgresql)에는 영향 없음(이 분기는 hsql 전용).
	 * </pre>
	 *
	 * @return HSQL 파일 기반 DataSource
	 */
	private DataSource dataSourceHSQL() {
		File dbDir = new File(HSQL_DB_DIR);
		// HSQL 파일 DB 는 portaldb.script(스키마+MEMORY 테이블 데이터)를 생성한다. 그 파일 존재로 '초기화 여부'를 판단.
		File scriptFile = new File(dbDir, HSQL_DB_NAME + ".script");
		boolean needInit = !scriptFile.exists();

		if (!dbDir.exists()) {
			dbDir.mkdirs();
		}

		BasicDataSource ds = new BasicDataSource();
		ds.setDriverClassName("org.hsqldb.jdbc.JDBCDriver");
		// shutdown=true: JVM 종료 시 정상 종료(데이터 flush). hsqldb.write_delay=false: 커밋 즉시 디스크 반영.
		ds.setUrl("jdbc:hsqldb:file:" + HSQL_DB_DIR + "/" + HSQL_DB_NAME
			+ ";shutdown=true;hsqldb.write_delay=false");
		ds.setUsername("sa");
		ds.setPassword("");

		if (needInit) {
			log.info("[HSQL] 영속 DB 파일이 없어 최초 초기화: {} 에 db/shtdb.sql 실행", dbDir.getAbsolutePath());
			ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
			populator.setSqlScriptEncoding("UTF-8");
			populator.addScript(new ClassPathResource("db/shtdb.sql"));
			try {
				populator.execute(ds);
			} catch (Exception e) {
				log.error("[HSQL] 초기 스키마/시드 로드 실패 — .localdb 삭제 후 재기동 필요: {}", e.getMessage(), e);
				throw new IllegalStateException("HSQL 초기화 실패", e);
			}
		} else {
			log.info("[HSQL] 기존 영속 DB 사용(런타임 변경 유지): {}", scriptFile.getAbsolutePath());
		}
		return ds;
	}

	/**
	 * (참고) 과거 in-memory 임베디드 HSQL 설정 — 매 기동마다 초기화되어 런타임 변경이 유지되지 않아 사용 중단.
	 */
	@SuppressWarnings("unused")
	private DataSource dataSourceHSQLInMemory() {
		return new EmbeddedDatabaseBuilder()
			.setType(EmbeddedDatabaseType.HSQL)
			.setScriptEncoding("UTF8")
			.addScript("classpath:/db/shtdb.sql")
			.build();
	}

	/**
	 * @return [dataSource 설정] basicDataSource 설정
	 */
	private DataSource basicDataSource() {
		BasicDataSource basicDataSource = new BasicDataSource();
		basicDataSource.setDriverClassName(className);
		basicDataSource.setUrl(url);
		basicDataSource.setUsername(userName);
		basicDataSource.setPassword(password);
		return basicDataSource;
	}

	/**
	 * @return [DataSource 설정]
	 */
	@Bean(name = {"dataSource", "egov.dataSource", "egovDataSource"})
	public DataSource dataSource() {
		if ("hsql".equals(dbType)) {
			return dataSourceHSQL();
		} else {
			return basicDataSource();
		}
	}
}
