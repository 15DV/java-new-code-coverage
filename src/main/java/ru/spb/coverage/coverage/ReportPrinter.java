package ru.spb.coverage.coverage;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import ru.spb.coverage.domain.report.CodeLine;
import ru.spb.coverage.domain.report.CoverageChange;
import ru.spb.coverage.domain.report.Report;
import ru.spb.coverage.domain.report.ReportResults;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

@Slf4j
public class ReportPrinter {

    private static final String SEPARATOR = "============================================\n";
    private static final String SUMMARIZE_BRANCHES_COVERED_PREFIX = "Summarize: Branches covered ";
    private static final String SUMMARIZE_LINES_COVERED_PREFIX = "Summarize: Lines covered ";
    private static final String UNCOVERED_LINES_PREFIX = "Uncovered lines:";
    private static final String BRANCH_COVERED_PREFIX = "Branch covered:";
    private static final String REPORT_FILE_NAME = "report.txt";
    private static final String SOURCE_FILENAME_PREFIX = "Source filename: ";
    private static final String TOTAL_COVERAGE_BRANCHES_PREFIX = "TOTAL COVERAGE BRANCHES: ";
    private static final String TOTAL_COVERAGE_LINES_PREFIX = "TOTAL COVERAGE LINES: ";

    public ReportResults saveReportAsFile(Report report) {
        var coverageChanges = report.getChanges();

        log.info("Starting creating and saving report file");

        try (FileWriter fileWriter = new FileWriter(REPORT_FILE_NAME);
             PrintWriter printWriter = new PrintWriter(fileWriter)) {

            if (coverageChanges.isEmpty()) {
                log.info("No data to make coverageChanges. CoverageChange file would be empty");
                return new ReportResults(100.0, 100.0);
            }

            printWriter.println(SEPARATOR);

            coverageChanges.forEach(r -> {
                printWriter.println(SOURCE_FILENAME_PREFIX + r.getFileName() + "\n");

                printWriter.println(r.getNewCodeBlock());

                printLineCoverage(r, printWriter);
                printWriter.println();
                printBranchCoverage(r, printWriter);

                printWriter.println(SEPARATOR);
            });

            printTotalBranchCoverage(report, printWriter);
            printTotalLinesCoverage(report, printWriter);

        } catch (IOException ex) {
            log.error("Could not make and save report file");
            throw new CoverageTaskException(ex);
        }

        log.info("Report file successfully saved");

        return new ReportResults(
                calcCoveragePercent(report.getCoveredLines(), report.getAllLines()),
                calcCoveragePercent(report.getCoveredBranches(), report.getAllBranches())
        );
    }

    private void printTotalBranchCoverage(Report report, PrintWriter printWriter) {
        var totalBranches = coveragePercent(report.getCoveredBranches(),
                report.getAllBranches(),
                TOTAL_COVERAGE_BRANCHES_PREFIX
        );
        printWriter.println(totalBranches);
    }

    private void printTotalLinesCoverage(Report report, PrintWriter printWriter) {
        var totalLines = coveragePercent(report.getCoveredLines(),
                report.getAllLines(),
                TOTAL_COVERAGE_LINES_PREFIX
        );
        printWriter.println(totalLines);
    }

    private void printLineCoverage(CoverageChange r, PrintWriter printWriter) {
        var allLines = r.getAcceptableLinesForCovering();
        var coveredLines = allLines - r.getUncoveredCodeLines().size();

        var format = coveragePercent(coveredLines, allLines, SUMMARIZE_LINES_COVERED_PREFIX);
        printWriter.println(format);

        printWriter.println(UNCOVERED_LINES_PREFIX);
        for (var line : r.getUncoveredCodeLines()) {
            printWriter.println(formatCodeLine(line));
        }
    }

    private void printBranchCoverage(CoverageChange r, PrintWriter printWriter) {
        if (!r.getPartlyCoveredCodeLines().isEmpty()) {
            var allBranches = r.getMissedBranches() + r.getCoveredBranches();
            var formatBranches = coveragePercent(r.getCoveredBranches(), allBranches, SUMMARIZE_BRANCHES_COVERED_PREFIX);
            printWriter.println(formatBranches);
        }

        for (var line : r.getPartlyCoveredCodeLines()) {
            var allBranches = line.getBranch().getMissed() + line.getBranch().getCovered();
            var formatStats = coveragePercent(line.getBranch().getCovered(), allBranches, BRANCH_COVERED_PREFIX);
            printWriter.println(formatStats);
            printWriter.println(formatCodeLine(line));
        }
    }

    private String formatCodeLine(CodeLine codeLine) {
        return (codeLine.getLineNumber() + 1) + ": " + StringUtils.trim(codeLine.getText());
    }

    private String coveragePercent(int covered, int all, String messagePrefix) {
        return String.format("%s %d of %d (%.2f %%)",
                messagePrefix,
                covered,
                all,
                calcCoveragePercent(covered, all)
        );
    }

    private static double calcCoveragePercent(int covered, int all) {
        return ((double) covered / all) * 100;
    }
}
