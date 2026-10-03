package com.teamflow;
import org.springframework.test.context.TestPropertySource;
@TestPropertySource(properties={"spring.datasource.url=jdbc:h2:mem:tests;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH", "spring.datasource.username=sa", "spring.datasource.password="})
class ApiIntegrationTest extends ApiContract {}
