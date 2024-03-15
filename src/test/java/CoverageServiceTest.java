import org.assertj.core.api.Assertions;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import ru.spb.coverage.coverage.CoverageService;
import ru.spb.coverage.coverage.ReportPrinter;

import java.io.File;

@Disabled
class CoverageServiceTest {

    CoverageService coverageService = new CoverageService("master");
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
