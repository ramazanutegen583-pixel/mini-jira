package com.teamflow;
import org.springframework.test.context.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
@Testcontainers(disabledWithoutDocker=true)
class PostgresIntegrationTest extends ApiContract {
    @Container static final PostgreSQLContainer<?> database = new PostgreSQLContainer<>("postgres:17-alpine");
    @DynamicPropertySource static void databaseProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", database::getJdbcUrl);
        properties.add("spring.datasource.username", database::getUsername);
        properties.add("spring.datasource.password", database::getPassword);
    }
}
