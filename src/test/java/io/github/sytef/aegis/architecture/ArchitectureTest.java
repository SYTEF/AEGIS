package io.github.sytef.aegis.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

    @Test
    void correlationPolicyRemainsIndependentFromFrameworkAndWebAdapter() {
        var productionClasses = new ClassFileImporter().importPackages("io.github.sytef.aegis");

        noClasses()
                .that()
                .resideInAPackage("..foundation.correlation..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "jakarta..", "..foundation.web..")
                .because("a política de correlação deve permanecer pura e reutilizável")
                .check(productionClasses);
    }
}
