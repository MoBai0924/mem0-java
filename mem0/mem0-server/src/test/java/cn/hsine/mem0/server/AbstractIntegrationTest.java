package cn.hsine.mem0.server;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests.
 * Provides PostgreSQL container with pgvector extension.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = ServerApplication.class
)
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {

    private static final DockerImageName POSTGRES_IMAGE =
        DockerImageName.parse("pgvector/pgvector:pg16")
            .asCompatibleSubstituteFor("postgres");

    @Container
    protected static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>(POSTGRES_IMAGE)
            .withDatabaseName("mem0_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
