/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.boost.maven.plugin;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;

import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.pluginsupport.MojoSupport;
import org.codehaus.mojo.pluginsupport.util.ArtifactItem;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.twdata.maven.mojoexecutor.MojoExecutor.ExecutionEnvironment;

import io.openliberty.boost.common.runtimes.RuntimeI;
import io.openliberty.boost.maven.runtimes.LibertyRuntime;
import io.openliberty.boost.maven.runtimes.TomeeRuntime;
import io.openliberty.boost.maven.utils.BoostLogger;
import io.openliberty.boost.maven.utils.MavenProjectUtil;

public abstract class AbstractMojo extends MojoSupport {
    
    private static RuntimeI runtime;

    protected String mavenDependencyPluginGroupId = "org.apache.maven.plugins";
    protected String mavenDependencyPluginArtifactId = "maven-dependency-plugin";

    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    protected String projectBuildDir;

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true)
    protected MavenSession session;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    protected RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    protected List<RemoteRepository> remoteRepos;

    @Component
    protected BuildPluginManager pluginManager;

    @Component
    protected RepositorySystem repoSystem;

    @Parameter
    protected ArtifactItem runtimeArtifact;

    @Parameter(property = "useDefaultHost", defaultValue = "false", readonly = true)
    protected boolean useDefaultHost;
    
    protected Map<String, String> dependencies;

    protected Plugin getMavenDependencyPlugin() throws MojoExecutionException {
        return plugin(groupId(mavenDependencyPluginGroupId), artifactId(mavenDependencyPluginArtifactId));
    }

    protected ExecutionEnvironment getExecutionEnvironment() {
        return executionEnvironment(project, session, pluginManager);
    }

    /**
     * Create default runtime artifact, if one has not been provided by the user
     * TODO REMOVE
     */
    private void createDefaultRuntimeArtifactIfNeeded() {
        if (runtimeArtifact == null) {
            runtimeArtifact = new ArtifactItem();
            runtimeArtifact.setGroupId("io.openliberty");
            runtimeArtifact.setArtifactId("openliberty-runtime");
            runtimeArtifact.setVersion("RELEASE");
            runtimeArtifact.setType("zip");
        }
    }
    
    @Override
    public void execute() throws MojoExecutionException {
        try {
            createDefaultRuntimeArtifactIfNeeded();
            
            this.dependencies = MavenProjectUtil.getAllDependencies(project, repoSystem, repoSession,
                        remoteRepos, BoostLogger.getInstance());
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
    
    protected RuntimeI getRuntimeInstance() throws MojoExecutionException {
        if(runtime == null) {
            BoostLogger logger = BoostLogger.getInstance();
            if (dependencies.containsKey("io.openliberty.boosters:tomee")) {
                logger.info("Detected TomEE as target Boost runtime");
                String tomeeInstallDir = projectBuildDir + "/apache-tomee/";
                String tomeeConfigDir = tomeeInstallDir + "conf";
                runtime = new TomeeRuntime(dependencies, getExecutionEnvironment(), tomeeInstallDir, tomeeConfigDir, getMavenDependencyPlugin());
            } else {
                logger.info("No target Boost runtime found, defaulting to Liberty");
                runtime = new LibertyRuntime(dependencies, getExecutionEnvironment(), project, getLog(), repoSystem, repoSession, remoteRepos, getMavenDependencyPlugin(), useDefaultHost);
            }
        }
        
        return runtime;
    }

}
