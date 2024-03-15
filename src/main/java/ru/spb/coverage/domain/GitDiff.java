package ru.spb.coverage.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitDiff {
    String changedFileName;
    int beginChanges;
    int endChanges;
}
