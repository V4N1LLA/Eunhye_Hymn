package com.eunhyehymn;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ContextLoadTest {
    @Test
    void contextLoads() {
        // Why: verify Flyway and JPA wiring initializes in tests.
    }
}
