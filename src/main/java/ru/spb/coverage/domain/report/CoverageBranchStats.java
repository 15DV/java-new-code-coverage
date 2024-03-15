package ru.spb.coverage.domain.report;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class CoverageBranchStats {
    Integer missed;
    Integer covered;
}
