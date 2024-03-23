package ru.spb.coverage.coverage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

@Disabled
class CoverageServiceTest {

    CoverageService coverageService = new CoverageService("main");
    ReportPrinter reportPrinter = new ReportPrinter();

    @Test
    void testGenerateJacocoReport() {
        var jacocoReport = coverageService.createReportFromGitDiffs();

        reportPrinter.saveReportAsFile(jacocoReport);

        var file = new File("report.txt");
        Assertions.assertTrue(file.exists());
    }
}