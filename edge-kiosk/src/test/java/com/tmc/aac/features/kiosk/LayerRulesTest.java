package com.tmc.aac.features.kiosk;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class LayerRulesTest {
    @Test void controllerAndDomainImportsRespectRepositoryRules() throws Exception {
        try (var files = Files.walk(Path.of("src/main/java/com/tmc/aac/features/kiosk"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                if (file.toString().contains("controller"))
                    assertThat(source).as(file.toString()).doesNotContain(".repository.", ".domain.", "java.sql.", "org.springframework.web.client");
                if (file.toString().contains("domain"))
                    assertThat(source).as(file.toString()).doesNotContain("org.springframework", "jakarta.persistence", ".repository.", ".service.", ".controller.");
                assertThat(source).as(file.toString()).doesNotContain("com.tmc.aac.features.attendance", "com.tmc.aac.features.integration", "com.tmc.aac.configuration");
            }
        }
    }
}
