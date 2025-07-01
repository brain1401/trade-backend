package com.hscoderadar.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.voyageai.VoyageAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
import jakarta.annotation.PostConstruct;

/**
 * LangChain4j 설정 클래스
 * - Voyage AI의 voyage-3-large 모델 사용 (1024차원)
 * - PostgreSQL + pgvector 기반 벡터 저장소
 */
@Configuration
@Slf4j
public class LangChain4jConfig {

  @Value("${langchain4j.voyage-ai.embedding-model.api-key}")
  private String voyageApiKey;

  @Value("${langchain4j.voyage-ai.embedding-model.model-name:voyage-3-large}")
  private String modelName;

  @Value("${langchain4j.pgvector.table:langchain4j_embedding}")
  private String tableName;

  @Value("${langchain4j.pgvector.dimension:1024}")
  private int dimension;

  @Value("${spring.datasource.url}")
  private String datasourceUrl;

  @Value("${spring.datasource.username}")
  private String datasourceUsername;

  @Value("${spring.datasource.password}")
  private String datasourcePassword;

  private final DataSource dataSource;
  private final JdbcTemplate jdbcTemplate;

  public LangChain4jConfig(DataSource dataSource) {
    this.dataSource = dataSource;
    this.jdbcTemplate = new JdbcTemplate(dataSource);
  }

  /**
   * LangChain4j가 기대하는 테이블 구조 생성 또는 뷰 생성
   */
  @PostConstruct
  public void initializePgVectorTable() {
    try {
      // LangChain4j가 기대하는 표준 테이블 구조 생성
      String createTableSql = """
          CREATE TABLE IF NOT EXISTS langchain4j_embedding (
              embedding_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
              embedding VECTOR(%d) NOT NULL,
              text TEXT,
              metadata JSONB
          )
          """.formatted(dimension);

      jdbcTemplate.execute(createTableSql);
      log.info("✅ LangChain4j 임베딩 테이블 생성/확인 완료");

      // 벡터 인덱스 생성
      String createIndexSql = """
          CREATE INDEX IF NOT EXISTS idx_langchain4j_embedding_vector
          ON langchain4j_embedding
          USING hnsw (embedding vector_cosine_ops)
          WITH (m = 16, ef_construction = 64)
          """;

      jdbcTemplate.execute(createIndexSql);
      log.info("✅ 벡터 인덱스 생성/확인 완료");

      // 기존 hscode_vectors 데이터를 langchain4j_embedding으로 마이그레이션 (필요한 경우)
      String migrationCheckSql = """
          SELECT COUNT(*) FROM langchain4j_embedding
          """;

      Integer count = jdbcTemplate.queryForObject(migrationCheckSql, Integer.class);
      if (count == 0) {
        log.info("🔄 기존 HSCode 벡터 데이터 마이그레이션 시작...");
        String migrationSql = """
            INSERT INTO langchain4j_embedding (embedding, text, metadata)
            SELECT
                embedding,
                CONCAT('HSCode: ', hscode, E'\\n품목명: ', product_name, E'\\n설명: ', description) as text,
                jsonb_build_object(
                    'hscode', hscode,
                    'product_name', product_name,
                    'classification_basis', classification_basis,
                    'confidence_score', confidence_score,
                    'verified', verified
                ) as metadata
            FROM hscode_vectors
            ON CONFLICT DO NOTHING
            """;

        int migrated = jdbcTemplate.update(migrationSql);
        log.info("✅ {} 개의 HSCode 벡터 데이터 마이그레이션 완료", migrated);
      }

    } catch (Exception e) {
      log.error("❌ PgVector 테이블 초기화 실패: {}", e.getMessage(), e);
    }
  }

  /**
   * Voyage AI 임베딩 모델 빈 생성 (voyage-3-large 1024차원)
   */
  @Bean
  public EmbeddingModel embeddingModel() {
    log.info(
        "Voyage AI 임베딩 모델 초기화 - 모델: {}, 차원: {}", modelName, dimension);

    return VoyageAiEmbeddingModel.builder()
        .apiKey(voyageApiKey)
        .modelName(modelName)
        .build();
  }

  /**
   * PostgreSQL + pgvector 기반 임베딩 저장소 빈 생성
   * 
   * LangChain4j 표준 테이블 구조 사용:
   * - embedding_id UUID: 고유 식별자
   * - embedding VECTOR(1024): 벡터 데이터
   * - text TEXT: 텍스트 내용
   * - metadata JSONB: 메타데이터
   */
  @Bean
  public EmbeddingStore<TextSegment> embeddingStore() {
    log.info(
        "PgVector 임베딩 저장소 초기화 - 테이블: {}, 차원: {}", tableName, dimension);

    // PostgreSQL 연결 정보 파싱
    String[] urlParts = datasourceUrl.replace("jdbc:postgresql://", "").split("/");
    String[] hostPort = urlParts[0].split(":");
    String host = hostPort[0];
    int port = hostPort.length > 1 ? Integer.parseInt(hostPort[1]) : 5432;
    String database = urlParts[1].split("\\?")[0];

    return PgVectorEmbeddingStore.builder()
        .host(host)
        .port(port)
        .database(database)
        .user(datasourceUsername)
        .password(datasourcePassword)
        .table(tableName)
        .dimension(dimension)
        // pgvector HNSW 인덱스 사용 (고성능 벡터 검색)
        .useIndex(true)
        .indexListSize(100)
        // 이미 테이블을 @PostConstruct에서 생성했으므로 false
        .createTable(false)
        .dropTableFirst(false)
        .build();
  }
}