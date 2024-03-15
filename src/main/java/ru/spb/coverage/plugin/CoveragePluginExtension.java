package ru.spb.coverage.plugin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CoveragePluginExtension {
    private String moduleName;

    private String branch = "master";

    private Double minCoverage = 75.0;
}
