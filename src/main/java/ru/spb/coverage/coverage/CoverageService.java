package ru.spb.coverage.coverage;

import lombok.extern.slf4j.Slf4j;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.ILine;
import org.jacoco.core.analysis.ISourceFileCoverage;
import org.jacoco.core.tools.ExecFileLoader;
import ru.spb.coverage.domain.GitDiff;
import ru.spb.coverage.domain.report.CodeLine;
import ru.spb.coverage.domain.report.CoverageBranchStats;
import ru.spb.coverage.domain.report.CoverageChange;
import ru.spb.coverage.domain.report.Report;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class CoverageService {

    //TODO: exec file can have another name
    private static final String JACOCO_EXEC_FILE = "/build/jacoco/test.exec";
    private static final String BUILD_CLASSES = "/build/classes/java/main";
    private static final String SOURCE_FILES = "/src/main/java/";
    private final String targetBranch;
    private static final String GIT_PATH = ".git";

    public CoverageService(String targetBranch) {
        this.targetBranch = "refs/heads/" + targetBranch;
    }

    public Report createReportFromGitDiffs() {
        var coverageBuilder = analyzeCode();

        var gitDiffs = GitUtils.getChangesInNewBranch(targetBranch, GIT_PATH);
        if (gitDiffs.isEmpty()) {
            log.info("No diffs between current branch and target branch");
            return Report.builder().build();
        }

        var gitChangedClassesName = gitDiffs.stream()
                .map(GitDiff::getChangedFileName)
                .collect(Collectors.toSet());

        var sourceFileNameToClassCoverage = filterAndMapGitChangedClasses(coverageBuilder, gitChangedClassesName);

        var coverageChanges = new ArrayList<CoverageChange>();

        log.info("Starting processing diffs from git");

        for (var diff : gitDiffs) {
            var classCoverage = sourceFileNameToClassCoverage.get(diff.getChangedFileName());

            if (classCoverage == null) {
                continue;
            }

            var fileName = fullFileNameWithPackage(classCoverage);
            var classCodeLines = readClassAsStrings(fileName);

            var uncoveredCodeLines = new ArrayList<CodeLine>();
            var partlyCoveredCodeLines = new ArrayList<CodeLine>();

            var acceptableCoveringLines = 0;
            var diffMissedBranches = 0;
            var diffCoveredBranches = 0;

            for (var i = diff.getBeginChanges(); i < diff.getEndChanges(); i++) {
                var coverageReportLine = classCoverage.getLine(i + 1);
                var coverageLineStatus = coverageReportLine.getStatus();
                var currentCodeLine = classCodeLines.get(i);

                if (coverageLineStatus != ICounter.EMPTY) {
                    acceptableCoveringLines++;
                }

                if (coverageLineStatus == ICounter.NOT_COVERED) {
                    uncoveredCodeLines.add(createLine(currentCodeLine, i));
                }

                // if branch is not covered at all it has status = 1(NOT_COVERED) and has missed branches
                if (coverageLineStatus == ICounter.PARTLY_COVERED || hasBranches(coverageReportLine)) {
                    var coveredBranches = coverageReportLine.getBranchCounter().getCoveredCount();
                    var missedBranches = coverageReportLine.getBranchCounter().getMissedCount();

                    var line = createLineForMissBranch(currentCodeLine, i, coveredBranches, missedBranches);

                    diffCoveredBranches += coveredBranches;
                    diffMissedBranches += missedBranches;
                    partlyCoveredCodeLines.add(line);
                }
            }

            if (!uncoveredCodeLines.isEmpty()) {
                var codeBlock = buildCodeBlock(diff.getBeginChanges(), diff.getEndChanges(), classCodeLines);
                var report = CoverageChange.builder()
                        .newCodeBlock(codeBlock)
                        .uncoveredCodeLines(uncoveredCodeLines)
                        .acceptableLinesForCovering(acceptableCoveringLines)
                        .partlyCoveredCodeLines(partlyCoveredCodeLines)
                        .coveredBranches(diffCoveredBranches)
                        .missedBranches(diffMissedBranches)
                        .fileName(fileName)
                        .build();
                coverageChanges.add(report);
            }
        }

        return createReport(coverageChanges);
    }

    private List<String> readClassAsStrings(String fileName) {
        List<String> classCodeLines;
        try {
            log.info("Read source class file as strings");
            classCodeLines = Files.readAllLines(Path.of(SOURCE_FILES + fileName));
        } catch (IOException e) {
            log.error("Could not open source file");
            throw new CoverageTaskException(e);
        }
        return classCodeLines;
    }

    private String fullFileNameWithPackage(ISourceFileCoverage classCoverage) {
        return String.join("/", classCoverage.getPackageName(), classCoverage.getName());
    }

    private boolean hasBranches(ILine lineInfo) {
        return lineInfo.getBranchCounter().getTotalCount() != 0;
    }

    private CodeLine createLine(String text, int lineNumber) {
        return createLineForMissBranch(text, lineNumber, 0, 0);
    }

    private CodeLine createLineForMissBranch(String text,
                                             int lineNumber,
                                             int coveredBranches,
                                             int missedBranches) {
        var coverageStats = CoverageBranchStats.builder()
                .covered(coveredBranches)
                .missed(missedBranches)
                .build();
        return CodeLine.builder()
                .text(text)
                .lineNumber(lineNumber)
                .branch(coverageStats)
                .build();
    }

    /**
     * Read data form Jacoco generated file *.exec
     * Analyze data and write it to CoverageBuilder
     *
     * @return CoverageBuilder with classes which were analyzed
     */
    private CoverageBuilder analyzeCode() {
        var execFileLoader = new ExecFileLoader();
        var coverageBuilder = new CoverageBuilder();
        var sourceDir = new File(BUILD_CLASSES);

        try {
            log.info("Starting analyzing classes using jacoco report");
            execFileLoader.load(new File(JACOCO_EXEC_FILE));
            var analyzer = new Analyzer(execFileLoader.getExecutionDataStore(), coverageBuilder);
            analyzer.analyzeAll(sourceDir);
        } catch (IOException ex) {
            log.error("Something goes wrong during analyzing class");
            throw new CoverageTaskException(ex);
        }

        return coverageBuilder;
    }

    private String buildCodeBlock(int start, int end, List<String> stringList) {
        var strBuilder = new StringBuilder();
        for (var i = start; i < end; i++) {
            strBuilder.append(i + 1).append(": ").append(stringList.get(i)).append("\n");
        }
        return strBuilder.toString();
    }

    private Map<String, ISourceFileCoverage> filterAndMapGitChangedClasses(CoverageBuilder coverageBuilder,
                                                                           Set<String> gitChangedClassesNames) {
        return coverageBuilder.getSourceFiles()
                .stream()
                .filter(sf -> gitChangedClassesNames.contains(sf.getName()))
                .collect(Collectors.toMap(ISourceFileCoverage::getName, Function.identity()));
    }

    private int sumAllLines(List<CoverageChange> result) {
        return result.stream()
                .mapToInt(CoverageChange::getAcceptableLinesForCovering)
                .sum();
    }

    private int sumCoveredLines(List<CoverageChange> result, int allLines) {
        var uncoveredLines = result.stream()
                .mapToInt(r -> r.getUncoveredCodeLines().size())
                .sum();
        return allLines - uncoveredLines;
    }

    private int sumAllBranches(List<CoverageChange> result, int coveredBranches) {
        var missedBranches = result.stream()
                .mapToInt(CoverageChange::getMissedBranches)
                .sum();
        return missedBranches + coveredBranches;
    }

    private int sumCoveredBranches(List<CoverageChange> result) {
        return result.stream()
                .mapToInt(CoverageChange::getCoveredBranches)
                .sum();
    }

    private Report createReport(List<CoverageChange> coverageChanges) {
        var coveredBranches = sumCoveredBranches(coverageChanges);
        var allBranches = sumAllBranches(coverageChanges, coveredBranches);
        var allLines = sumAllLines(coverageChanges);
        var coveredLines = sumCoveredLines(coverageChanges, allLines);

        return Report.builder()
                .changes(coverageChanges)
                .allLines(allLines)
                .coveredLines(coveredLines)
                .allBranches(allBranches)
                .coveredBranches(coveredBranches)
                .build();
    }
}
