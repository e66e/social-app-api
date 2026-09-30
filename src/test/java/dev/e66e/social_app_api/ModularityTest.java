package dev.e66e.social_app_api;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {

    @Test
    void verifiesModularStructure() {
        ApplicationModules.of(SocialAppAPIApplication.class).verify();
    }
}
