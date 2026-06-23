package egovframework.com.config;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;

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
	 * 자동 재시드(체크섬):
	 *  - shtdb.sql 의 SHA-256 을 .localdb/.seedhash 에 기록해 두고, 기동 시 현재 shtdb.sql 체크섬과 비교한다.
	 *  - <b>shtdb.sql(스키마/시드)이 바뀌면 자동으로 .localdb 를 삭제하고 재시드</b>한다(수동 삭제 불필요).
	 *    바뀌지 않았으면 디스크 데이터를 그대로 사용 → 런타임 변경(배너 등록/수정 등) 유지.
	 *  - 매 기동마다 무조건 초기화하려면 properties 에 <code>Globals.localdb.resetEachStart=true</code>.
	 *  - .localdb 는 .gitignore 로 제외(커밋 금지). 운영(jar, Globals.DbType=postgresql)에는 영향 없음(이 분기는 hsql 전용).
	 * </pre>
	 *
	 * @return HSQL 파일 기반 DataSource
	 */
	private DataSource dataSourceHSQL() {
		File dbDir = new File(HSQL_DB_DIR);
		// HSQL 파일 DB 는 portaldb.script(스키마+MEMORY 테이블 데이터)를 생성한다. 그 파일 존재로 '초기화 여부'를 판단.
		File scriptFile = new File(dbDir, HSQL_DB_NAME + ".script");
		File hashFile = new File(dbDir, ".seedhash");

		// shtdb.sql(스키마+시드) 현재 체크섬 — 변경되면 자동으로 .localdb 삭제 후 재시드한다.
		String currentHash = computeSeedHash();
		boolean forceReset = Boolean.parseBoolean(env.getProperty("Globals.localdb.resetEachStart", "false"));
		boolean seedChanged = !currentHash.isEmpty() && !currentHash.equals(readText(hashFile));

		// 기존 DB 파일이 있는데 (강제초기화 || shtdb.sql 변경) 이면 .localdb 자동 삭제 → 새 스키마/시드 반영
		if (scriptFile.exists() && (forceReset || seedChanged)) {
			log.info("[HSQL] {} → .localdb 자동 삭제 후 재시드",
				forceReset ? "강제초기화(Globals.localdb.resetEachStart=true)" : "shtdb.sql 변경 감지");
			deleteLocalDbFiles(dbDir);
		}

		if (!dbDir.exists()) {
			dbDir.mkdirs();
		}

		boolean needInit = !scriptFile.exists();

		BasicDataSource ds = new BasicDataSource();
		ds.setDriverClassName("org.hsqldb.jdbc.JDBCDriver");
		// shutdown=true: JVM 종료 시 정상 종료(데이터 flush). hsqldb.write_delay=false: 커밋 즉시 디스크 반영.
		ds.setUrl("jdbc:hsqldb:file:" + HSQL_DB_DIR + "/" + HSQL_DB_NAME
			+ ";shutdown=true;hsqldb.write_delay=false");
		ds.setUsername("sa");
		ds.setPassword("");

		if (needInit) {
			log.info("[HSQL] 영속 DB 초기화: {} 에 db/shtdb.sql 실행", dbDir.getAbsolutePath());
			ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
			populator.setSqlScriptEncoding("UTF-8");
			populator.addScript(new ClassPathResource("db/shtdb.sql"));
			try {
				populator.execute(ds);
			} catch (Exception e) {
				log.error("[HSQL] 초기 스키마/시드 로드 실패 — .localdb 삭제 후 재기동 필요: {}", e.getMessage(), e);
				throw new IllegalStateException("HSQL 초기화 실패", e);
			}
			writeText(hashFile, currentHash); // 시드 체크섬 기록(다음 기동 시 변경 감지용)
		} else {
			log.info("[HSQL] 기존 영속 DB 사용(런타임 변경 유지): {}", scriptFile.getAbsolutePath());
		}
		return ds;
	}

	/** classpath db/shtdb.sql 내용의 SHA-256(hex). 실패 시 빈 문자열(=변경감지 비활성, 기존 동작 유지). */
	private String computeSeedHash() {
		try (InputStream in = new ClassPathResource("db/shtdb.sql").getInputStream()) {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] buf = new byte[8192];
			int n;
			while ((n = in.read(buf)) > 0) {
				md.update(buf, 0, n);
			}
			StringBuilder sb = new StringBuilder();
			for (byte b : md.digest()) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (Exception e) {
			log.warn("[HSQL] shtdb.sql 체크섬 계산 실패(자동 재시드 비활성): {}", e.getMessage());
			return "";
		}
	}

	/** HSQL 파일 DB 산출물(portaldb.*)과 .seedhash 삭제. */
	private void deleteLocalDbFiles(File dbDir) {
		File[] files = dbDir.listFiles((d, name) -> name.startsWith(HSQL_DB_NAME) || name.equals(".seedhash"));
		if (files != null) {
			for (File f : files) {
				if (f.isDirectory()) {
					File[] inner = f.listFiles();
					if (inner != null) {
						for (File g : inner) {
							g.delete();
						}
					}
				}
				f.delete();
			}
		}
	}

	private String readText(File f) {
		try {
			return f.exists() ? new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).trim() : "";
		} catch (Exception e) {
			return "";
		}
	}

	private void writeText(File f, String text) {
		try {
			Files.write(f.toPath(), text.getBytes(StandardCharsets.UTF_8));
		} catch (Exception e) {
			log.warn("[HSQL] .seedhash 기록 실패: {}", e.getMessage());
		}
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
