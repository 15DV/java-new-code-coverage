package ru.spb.coverage.coverage;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

class CoverageServiceTest {

    CoverageService coverageService = new CoverageService("main");
    ReportPrinter reportPrinter = new ReportPrinter();

    @Test
    void testGenerateJacocoReport() {
        var jacocoReport = coverageService.createReportFromGitDiffs();

        reportPrinter.saveReportAsFile(jacocoReport);

        var file = new File("report.txt");
        Assertions.assertThat(file)
                .exists();
    }
}