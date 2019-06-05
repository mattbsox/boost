/*******************************************************************************
 * Copyright (c) 2018,2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package boost.gradle.tasks

import org.codehaus.groovy.GroovyException

import java.util.ArrayList
import java.io.OutputStream
import java.io.FileOutputStream
import java.io.IOException

import org.apache.commons.io.FileUtils

import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.TransformerException

import org.gradle.api.GradleException
import org.gradle.api.logging.LogLevel
import org.gradle.api.artifacts.Dependency
import org.gradle.tooling.BuildException

import org.gradle.maven.MavenModule
import org.gradle.maven.MavenPomArtifact
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.tasks.Copy

import boost.gradle.utils.BoostLogger
import boost.gradle.utils.GradleProjectUtil
import boost.common.boosters.AbstractBoosterConfig;
import boost.common.config.BoosterConfigurator
import boost.common.config.BoostProperties;
import boost.common.utils.BoostUtil
import org.gradle.api.Task

import net.wasdev.wlp.gradle.plugins.extensions.PackageAndDumpExtension

public class BoostPackageTask extends AbstractBoostTask {

    List<AbstractBoosterConfig> boosterPackConfigurators

    String libertyServerPath = null

    BoostPackageTask() {
        configure({
            description 'Packages the application into an executable Liberty jar.'
            logging.level = LogLevel.INFO
            group 'Boost'

            dependsOn 'libertyCreate'

            //There are some things that this task does before we can package up the server into a JAR
            PackageAndDumpExtension boostPackage = new PackageAndDumpExtension()

            //We have to check these properties after the build.gradle file has been evaluated.
            //Some properties could get set after our plugin is applied.
            project.afterEvaluate {
                if (project.plugins.hasPlugin('war')) {
                    boostPackage.archive = project.war.archiveName.substring(0, project.war.archiveName.lastIndexOf("."))

                    if (isPackageConfigured()) {
                        //Assemble works for the ear task too
                        project.war.finalizedBy 'boostPackage'
                    }
                } //ear check here when supported
                //Configuring liberty plugin task dependencies and parameters
                //installFeature should check the server.xml in the server directory and install the missing feature
                project.tasks.getByName('libertyPackage').dependsOn 'installApps', 'installFeature'
                project.tasks.getByName('installApps').mustRunAfter 'installFeature'
                boostPackage.include = "runnable, minify"
                 if (!project.plugins.hasPlugin('war')) {
                    finalizedBy 'libertyPackage'
                }
            }

            //The task will perform this before any other task actions
            doFirst {

                libertyServerPath = "${project.buildDir}/wlp/usr/servers/${project.liberty.server.name}"
                if (isPackageConfigured()) {
                    if(project.boost.packaging.packageName != null && !project.boost.packaging.packageName.isEmpty()) {
                        boostPackage.archive = "${project.buildDir}/libs/${project.boost.packaging.packageName}"
                    }
                }

                project.liberty.server.packageLiberty = boostPackage

                if (project.plugins.hasPlugin('war')) {
                    // Get booster dependencies from project
                    Map<String, String> dependencies = GradleProjectUtil.getAllDependencies(project, BoostLogger.getInstance())
                    
                    // Determine the Java compiler target version and set this internally 
                    System.setProperty(BoostProperties.INTERNAL_COMPILER_TARGET, project.findProperty("targetCompatibility").toString())
            
                    boosterPackConfigurators = BoosterConfigurator.getBoosterPackConfigurators(dependencies, BoostLogger.getInstance())

                    copyBoosterDependencies()

                    generateServerConfigEE()

                } else {
                    throw new GradleException('Could not package the project with boostPackage. The boostPackage task must be used with a Java EE project.')
                }

                logger.info('Packaging the applicaiton.')
            }
        })
    }

    protected void generateServerConfigEE() throws GradleException {
        String warName = null

        if (project.war != null) {

            if (project.war.version == null) {
                warName = project.war.baseName
            } else {
                warName = project.war.baseName + "-" + project.war.version
            }
        }

        try {

            BoosterConfigurator.generateLibertyServerConfig(libertyServerPath, boosterPackConfigurators, Arrays.asList(warName), BoostLogger.getInstance());

        } catch (Exception e) {
            throw new GradleException("Unable to generate server configuration for the Liberty server.", e);
        }
    }

    protected void copyBoosterDependencies() {

        List<String> dependenciesToCopy = BoosterConfigurator.getDependenciesToCopy(boosterPackConfigurators, BoostLogger.getInstance());

        def boosterConfig = project.getConfigurations().create('boosterDependency')

        dependenciesToCopy.each { dep ->

            project.getDependencies().add(boosterConfig.name, dep)

        }

        project.copy {
            from project.configurations.boosterDependency
            into "${project.buildDir}/wlp/usr/servers/BoostServer/resources"
            include '*.jar'
        }
    }

}
