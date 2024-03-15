package ru.spb.coverage.domain.report;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class CoverageChange {
    String fileName;
    String newCodeBlock;
    @Builder.Default
    List<CodeLine> uncoveredCodeLines = List.of();
    @Builder.Default
    List<CodeLine> partlyCoveredCodeLines = List.of();
    Integer acceptableLinesForCovering;
    Integer coveredBranches;
    Integer missedBranches;
}
