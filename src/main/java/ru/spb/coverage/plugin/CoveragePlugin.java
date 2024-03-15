package ru.spb.coverage.plugin;

import lombok.extern.slf4j.Slf4j;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import ru.spb.coverage.coverage.ReportPrinter;
import ru.spb.coverage.coverage.CoverageService;

@Slf4j
public class CoveragePlugin implements Plugin<Project> {

    private static final String EXTENSION_NAME = "coverage";
    private static final String NEW_CODE_EX_MESSAGE = """
            New code coverage is %.2f %%
            Minimum required new code coverage is %.2f %%
            Please, increase coverage of new code
            """;

    @Override
    public void apply(Project project) {
        var coverageSettings = project.getExtensions()
                .create(EXTENSION_NAME, CoveragePluginExtension.class);

        project.getTasks()
                .register("newCodeCoverage")
                .get()
                .doLast(task -> {
                    var minCoverage = coverageSettings.getMinCoverage();
                    var coverageService = new CoverageService(coverageSettings.getBranch());

                    var reportFromGitDiffs = coverageService.createReportFromGitDiffs();
                    var reportPrinter = new ReportPrinter();
                    var results = reportPrinter.saveReportAsFile(reportFromGitDiffs);

                    if (results.newCodeCoverage() < minCoverage) {
                        var exMessage = NEW_CODE_EX_MESSAGE.formatted(results.newCodeCoverage(), minCoverage);
                        throw new GradleException(exMessage);
                    }
                });
    }
}

