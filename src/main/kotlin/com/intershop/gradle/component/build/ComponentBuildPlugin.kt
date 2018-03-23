/*
 * Copyright 2018 Intershop Communications AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intershop.gradle.component.build

import com.intershop.gradle.component.build.extension.ComponentExtension
import com.intershop.gradle.component.build.extension.items.FileContainerItem
import com.intershop.gradle.component.build.extension.items.FileItem
import com.intershop.gradle.component.build.tasks.CreateComponentTask
import com.intershop.gradle.component.build.tasks.VerifyClasspathTask
import com.intershop.gradle.component.build.tasks.ZipContainerTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.ivy.IvyPublication
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.model.Defaults
import org.gradle.model.ModelMap
import org.gradle.model.Path
import org.gradle.model.RuleSource
import org.gradle.model.internal.core.ModelPath
import org.gradle.model.internal.core.ModelReference
import org.gradle.model.internal.core.ModelRegistrations
import org.gradle.model.internal.registry.ModelRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import javax.inject.Inject

/**
 * Plugin Class for 'com.intershop.gradle.component.build'
 *
 * This class contains also Rules for publishing task outputs.
 */
@Suppress("unused")
class ComponentBuildPlugin @Inject constructor(private val modelRegistry: ModelRegistry?) : Plugin<Project> {

    companion object {
        const val TASKDESCRIPTION = "Generate component file from configuration"
        const val COMPONENT_TASKNAME = "createComponent"
        const val VERIFYCP_TASKNAME = "verifyClasspath"

        const val COMPONENT_EXTENSION_NAME = "component"
        const val COMPONENT_GROUP_NAME = "Component Build"
    }

    /**
     * Applies the plugin functionality to the configured project.
     *
     * @param project the current project
     */
    override fun apply(project: Project) {
        with(project) {
            pluginManager.apply(PublishingPlugin::class.java)

            logger.info("Component plugin adds extension {} to {}", COMPONENT_EXTENSION_NAME, name)
            val extension = extensions.findByType(ComponentExtension::class.java)
                    ?: extensions.create(COMPONENT_EXTENSION_NAME, ComponentExtension::class.java, this)

            val defaultConfiguration = project.configurations.maybeCreate("default")
            project.configurations.maybeCreate("component").extendsFrom(defaultConfiguration)

            if(modelRegistry?.state(ModelPath.nonNullValidatedPath("componentBuildConf")) == null) {
                modelRegistry?.register(ModelRegistrations.bridgedInstance(
                        ModelReference.of("componentBuildConf", ComponentExtension::class.java), extension)
                        .descriptor("Deployment configuration").build())
            }


        }
    }

    /**
     * This RuleSource adds rules for publishing task output
     * of CreateComponentTask Task.
     */
    @Suppress("unused")
    class ComponentBuildRule: RuleSource() {

        companion object {
            val logger: Logger = LoggerFactory.getLogger(ComponentBuildRule::class.java.simpleName)

            private fun createContainerTask(tasks: ModelMap<Task>, container: FileContainerItem): String {
                val taskName = "zipContainer${container.name.capitalize()}"
                if(! tasks.containsKey(taskName)) {
                    tasks.create(taskName, ZipContainerTask::class.java) {
                        with(it) {
                            group = COMPONENT_GROUP_NAME
                            description = "Creates zip for container configuration '${container.name}'."

                            inputFiles = container.source

                            if(container.containerType.isNotBlank()) {
                                artifactAppendix = container.containerType
                            }

                            if(container.baseName.isNotBlank()) {
                                artifactBaseName = container.baseName
                            } else {
                                logger.error("The base name of container configuration '{}' is emtpy!", container.name)
                                throw InvalidUserDataException("The base name of container configuration '" +
                                        container.name + "' is empty!")
                            }

                            if(container.classifier.isNotBlank()) {
                                artifactClassifier = container.classifier
                            }
                        }
                    }
                } else {
                    logger.error("The task '{}' for container configuration '{}' exists already!",
                            taskName, container.name)
                    throw InvalidUserDataException("The task '" + taskName + "' for container configuration '"
                            + container.name + "' exists already!")
                }
                return taskName

            }

            private fun createClassifierForFile(item: FileItem): String {
                val classifier = StringBuilder(item.name)
                if(item.classifier.isNotBlank()) {
                    classifier.append("_").append(item.classifier)
                }
                return classifier.toString()
            }

            private fun createDescriptorTask(tasks: ModelMap<Task>,
                                             extension: ComponentExtension) {
                if(! tasks.containsKey(COMPONENT_TASKNAME)) {
                    tasks.create(COMPONENT_TASKNAME, CreateComponentTask::class.java) {
                        with(it) {
                            group = COMPONENT_GROUP_NAME
                            description = "Creates descriptor for component deployment '${extension.displayName}'"

                            displayName = extension.displayName
                            componentDescription = extension.componentDescription

                            libs = extension.libs
                            modules = extension.modules
                            excludes = extension.dependencyMngt.excludes
                            properties = extension.propertyItems
                            containers = extension.containers
                            files = extension.fileItems

                            defaultTarget = extension.targetPath

                            descriptorFile = extension.decriptorOutputFile

                            project.artifacts.add("component", it.descriptorFile) {
                                it.type = "component"
                                it.extension
                                it.name
                            }
                        }
                    }
                }
            }

            private fun createVerifyClassCollisionTask(tasks: ModelMap<Task>,
                                                       extension: ComponentExtension,
                                                       buildDir: File) {
                if(! tasks.containsKey(VERIFYCP_TASKNAME)) {
                    tasks.create(VERIFYCP_TASKNAME, VerifyClasspathTask::class.java) {
                        with(it) {
                            group = COMPONENT_GROUP_NAME
                            description = "Check jars for class collisions of '${extension.displayName}'"

                            enabled = extension.dependencyMngt.classpathVerification.enabled

                            libSet = extension.libs.items
                            moduleSet = extension.modules.items
                            excludes = extension.dependencyMngt.excludes
                            excludedClasses = extension.dependencyMngt.classpathVerification.excludedClasses
                            collisionExcludes = extension.dependencyMngt.classpathVerification.excludes

                            reportOutput = File(buildDir, ComponentExtension.CLASSCOLLISION_REPORT)
                        }
                    }
                }
            }

            private fun configureMvnPublishing(mvnPublication: MavenPublication,
                                               tasks: ModelMap<Task>,
                                               componentBuildConf: ComponentExtension) {
                with(mvnPublication) {
                    tasks.withType(ZipContainerTask::class.java).forEach { task ->
                        this.artifact(task) { mvnArtifact ->
                            mvnArtifact.extension = task.extension

                            val classifier = StringBuilder()
                            classifier.append( task.artifactBaseName )

                            if(task.artifactAppendix.isNotBlank()) {
                                if(classifier.isNotBlank()) {
                                    classifier.append("_")
                                }
                                classifier.append(task.artifactAppendix)
                            }
                            if(task.artifactClassifier.isNotBlank()) {
                                if(classifier.isNotBlank()) {
                                    classifier.append("_")
                                }
                                classifier.append(task.artifactClassifier)
                            }

                            if(classifier.isNotBlank()) {
                                mvnArtifact.classifier = classifier.toString()
                            }

                            task.project.artifacts.add("component", task) {
                                it.classifier = mvnArtifact.classifier
                                it.name = task.project.name
                                it.extension = mvnArtifact.extension

                                it.builtBy(task)
                            }
                        }
                    }

                    tasks.withType(CreateComponentTask::class.java).forEach { task ->

                        componentBuildConf.fileItems.items.forEach { item ->
                            this.artifact(item.file) { mvnArtifact ->
                                mvnArtifact.extension = item.extension
                                mvnArtifact.classifier = createClassifierForFile(item)

                                task.project.artifacts.add("component", item.file) {
                                    it.classifier = mvnArtifact.classifier
                                    it.name = task.project.name
                                    it.extension = item.extension
                                }
                            }
                        }

                        this.artifact(task.outputs.files.singleFile) { mvnArtifact ->
                            mvnArtifact.builtBy(task)
                            mvnArtifact.classifier = "component"

                            task.project.artifacts.add("component", task.descriptorFile) {
                                it.classifier = mvnArtifact.classifier
                                it.name = task.project.name
                                it.extension = task.descriptorFile.extension

                                it.builtBy(task)
                            }
                        }
                    }
                }
            }

            private fun configureIvyPublishing(ivyPublication: IvyPublication,
                                               tasks: ModelMap<Task>,
                                               componentBuildConf: ComponentExtension) {
                with(ivyPublication) {
                    configurations {
                        it.maybeCreate("default")
                        it.maybeCreate("component").extend("default")
                    }
                    tasks.withType(ZipContainerTask::class.java).forEach { task ->
                        this.artifact(task) { ivyArtifact ->
                            ivyArtifact.name = task.artifactBaseName
                            ivyArtifact.type = task.artifactAppendix

                            if(task.artifactClassifier.isNotBlank()) {
                                ivyArtifact.classifier = task.artifactClassifier
                            }

                            ivyArtifact.conf = "component"

                            task.project.artifacts.add("component", task) {
                                if(task.artifactClassifier.isNotBlank()) {
                                    it.classifier = ivyArtifact.classifier
                                }
                                it.name = ivyArtifact.name
                                it.extension = ivyArtifact.extension
                                it.type = ivyArtifact.type

                                it.builtBy(task)
                            }
                        }
                    }

                    tasks.withType(CreateComponentTask::class.java).forEach { task ->

                        componentBuildConf.fileItems.items.forEach { item ->
                            this.artifact(item.file) { ivyArtifact ->
                                ivyArtifact.name = item.name
                                ivyArtifact.type = item.extension

                                if(item.classifier.isNotBlank()) {
                                    ivyArtifact.classifier = item.classifier
                                }
                                ivyArtifact.conf = "component"

                                task.project.artifacts.add("component", item.file) {
                                    if(item.classifier.isNotBlank()) {
                                        it.classifier = ivyArtifact.classifier
                                    }

                                    it.name = ivyArtifact.name
                                    it.type = ivyArtifact.type
                                    it.extension = ivyArtifact.extension
                                }
                            }
                        }

                        this.artifact(task.outputs.files.singleFile, { ivyArtifact ->
                            ivyArtifact.builtBy(task)
                            ivyArtifact.name = task.project.name
                            ivyArtifact.type = "component"

                            ivyArtifact.conf = "component"

                            task.project.artifacts.add("component", task.descriptorFile) {
                                it.name = task.project.name
                                it.type = ivyArtifact.type
                                it.extension = ivyArtifact.extension

                                it.builtBy(task)
                            }
                        })
                    }
                }
            }

            private fun configureProjectArtifactsOnly(tasks: ModelMap<Task>, componentBuildConf: ComponentExtension) {
                tasks.withType(ZipContainerTask::class.java).forEach { task ->
                    task.project.artifacts.add("component", task) {
                        if(task.artifactClassifier.isNotBlank()) {
                            it.classifier = task.artifactClassifier
                        }

                        it.name = task.artifactBaseName
                        it.extension = task.extension
                        it.type = task.artifactAppendix

                        it.builtBy(task)
                    }
                }

                tasks.withType(CreateComponentTask::class.java).forEach { task ->
                    componentBuildConf.fileItems.items.forEach { item ->
                        task.project.artifacts.add("component", item.file) {
                            if(item.classifier.isNotBlank()) {
                                it.classifier = item.classifier
                            }

                            it.name = item.name
                            it.type = item.extension
                            it.extension = item.extension
                        }
                    }

                    task.project.artifacts.add("component", task.descriptorFile) {
                        it.name = task.descriptorFile.nameWithoutExtension
                        it.type = task.descriptorFile.extension
                        it.extension = task.descriptorFile.extension

                        it.builtBy(task)
                    }
                }
            }
        }

        /**
         * Configures publishing of task output
         * depending on configured publishing.
         */
        @Suppress("unused")
        @Defaults
        fun configureComponentBuildPublishing(tasks: ModelMap<Task>,
                                              publishing: PublishingExtension,
                                              componentBuildConf: ComponentExtension,
                                              @Path("buildDir") buildDir: File) {

            val publications = publishing.publications

            // create container, if configuration is available
            componentBuildConf.containers.items.forEach { container ->
                createContainerTask(tasks, container)
            }

            createDescriptorTask(tasks, componentBuildConf)

            createVerifyClassCollisionTask(tasks, componentBuildConf, buildDir)

            tasks.get(COMPONENT_TASKNAME)?.dependsOn(VERIFYCP_TASKNAME)

            var configuredPublishing = false

            try {
                publications.maybeCreate(componentBuildConf.mavenPublicationName, MavenPublication::class.java).apply {
                    configureMvnPublishing(this, tasks, componentBuildConf )
                    configuredPublishing = true
                }
            } catch(ex: InvalidUserDataException) {
                logger.debug("Maven Publishing is not applied for component build plugin.")
            }

            try {
                publications.maybeCreate(componentBuildConf.ivyPublicationName, IvyPublication::class.java).apply {
                    configureIvyPublishing(this, tasks, componentBuildConf)
                    configuredPublishing = true
                }
            } catch(ex: InvalidUserDataException) {
                logger.debug("Ivy Publishing is not applied for component build plugin.")
            }

            if(! configuredPublishing) {
                // configure only project artifacts
                configureProjectArtifactsOnly(tasks, componentBuildConf)
            }
        }
    }
}
