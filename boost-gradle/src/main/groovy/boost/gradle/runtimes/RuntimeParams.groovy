package boost.gradle.runtimes

import java.util.List

import org.gradle.api.Project

import boost.common.boosters.AbstractBoosterConfig

public class RuntimeParams {
    private Project project
    private List<AbstractBoosterConfig> boosterConfigs

    public RuntimeParams(Project project, List<AbstractBoosterConfig> boosterConfigs) {
        this.project = project
        this.boosterConfigs = boosterConfigs
    }

    public Project getProject() {
        return this.project
    }

    public List<AbstractBoosterConfig> getBoosterConfigs() {
        return this.boosterConfigs
    } 
}