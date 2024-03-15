package ru.spb.coverage.domain.report;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Builder
@Value
public class Report {
    @Builder.Default
    List<CoverageChange> changes = List.of();
    Integer allBranches;
    Integer coveredBranches;
    Integer allLines;
    Integer coveredLines;
}
