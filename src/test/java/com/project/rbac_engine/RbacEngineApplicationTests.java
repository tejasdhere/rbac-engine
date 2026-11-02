package com.project.rbac_engine;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
class RbacEngineApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void mainMethodStartsSuccessfully() {
        assertDoesNotThrow(() -> RbacEngineApplication.main(new String[]{"--spring.profiles.active=test"}));
    }
}
