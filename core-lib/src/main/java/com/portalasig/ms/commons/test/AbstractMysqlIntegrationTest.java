package com.portalasig.ms.commons.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests backed by a real MySQL 8 database.
 *
 * <p>A shared {@link MySQLContainer} is started once per test JVM and reused across test classes.
 * The datasource and Flyway connection properties are wired to the container through
 * {@link DynamicPropertySource}, and Flyway executes the service migrations on context startup,
 * so tests run against the same schema and SQL dialect used in production.
 *
 * <p>Each test method runs inside a transaction that is rolled back on completion, guaranteeing
 * isolation between tests without depending on execution order.
 *
 * <p>The Spring Cloud Config client is disabled so tests do not require a running Config Server.
 * Services may refine test settings in {@code src/test/resources/application-test.yml}.
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(
    properties = {
      "spring.cloud.config.enabled=false",
      "spring.cloud.config.import-check.enabled=false",
      "spring.config.import=",
      "spring.security.oauth2.client.provider.portalasig_engine.token-uri=http://localhost:5860/portalasig/uaa/oauth2/token",
      "spring.security.oauth2.client.registration.portalasig_engine.client-id=portalasig",
      "spring.security.oauth2.client.registration.portalasig_engine.client-secret=secret",
      "spring.security.oauth2.client.registration.portalasig_engine.authorization-grant-type=client_credentials"
    })
public abstract class AbstractMysqlIntegrationTest {

  /*
   * Singleton container: started once per test JVM and shared by every test class, so the
   * expensive MySQL startup is paid only once. The container is destroyed by Ryuk when the
   * test JVM exits. Managed manually instead of via @Container because the JUnit extension
   * stops static containers after each test class, breaking subsequent classes.
   */
  static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

  static {
    MYSQL.start();
  }

  /**
   * Points the service datasource and Flyway at the Testcontainers MySQL instance.
   *
   * @param registry
   *     the Spring dynamic property registry
   */
  @DynamicPropertySource
  static void mysqlProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
    registry.add("spring.flyway.url", MYSQL::getJdbcUrl);
    registry.add("spring.flyway.user", MYSQL::getUsername);
    registry.add("spring.flyway.password", MYSQL::getPassword);
  }
}
