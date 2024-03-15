package ru.spb.coverage.domain.report;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CodeLine {
    String text;
    Integer lineNumber;
    CoverageBranchStats branch;
}
