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
import com.intershop.gradle.component.build.tasks.CreateDescriptor
import com.intershop.gradle.component.build.tasks.ZipContainer
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
        const val TASKNAME = "createComponent"
    }

    /**
     * Applies the plugin functionality to the configured project.
     *
     * @param project the current project
     */
    override fun apply(project: Project) {
        with(project) {
            pluginManager.apply(PublishingPlugin::class.java)

            logger.info("Component plugin adds extension {} to {}", ComponentExtension.COMPONENT_EXTENSION_NAME, name)
            val extension = extensions.findByType(ComponentExtension::class.java)
                    ?: extensions.create(ComponentExtension.COMPONENT_EXTENSION_NAME,
                            ComponentExtension::class.java,
                            this)

            if(modelRegistry?.state(ModelPath.nonNullValidatedPath("componentBuildConf")) == null) {
                modelRegistry?.register(ModelRegistrations.bridgedInstance(
                        ModelReference.of("componentBuildConf", ComponentExtension::class.java), extension)
                        .descriptor("Deployment configuration").build())
            }
        }
    }

    /**
     * This RuleSource adds rules for publishing task output
     * of CreateDescriptor Task.
     */
    @Suppress("unused")
    class ComponentBuildRule: RuleSource() {

        companion object {
            val logger = LoggerFactory.getLogger(ComponentBuildRule::class.java.simpleName)

            private fun createContainerTask(tasks: ModelMap<Task>, container: FileContainerItem): String {
                val taskName = "zipContainer${container.name.capitalize()}"
                if(! tasks.containsKey(taskName)) {
                    tasks.create(taskName, ZipContainer::class.java) {
                        with(it) {
                            group = ComponentExtension.COMPONENT_GROUP_NAME
                            description = "Creates zip for container configuration '${container.name}'."

                            source.add(container.source)

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
                                             extension: ComponentExtension,
                                             buildDir: File): String {

                val componentName = extension.displayName.replace(" ", "_").capitalize()
                val taskName = "deploymentDescr$componentName"
                if(! tasks.containsKey(taskName)) {
                    tasks.create(taskName, CreateDescriptor::class.java) {
                        with(it) {
                            group = ComponentExtension.COMPONENT_GROUP_NAME
                            description = "Creates descriptor for component deployment '${extension.displayName}'"

                            displayName = extension.displayName
                            componentDescription = extension.componentDescription

                            libs = extension.libs
                            modules = extension.modules
                            properties = extension.propertyItems
                            containers = extension.containers
                            files = extension.fileItems

                            descriptorFile = File(buildDir, ComponentExtension.DESCRIPTOR_FILE)

                        }
                    }
                }
                return taskName
            }

            private fun configureMvnPublishing(mvnPublication: MavenPublication,
                                               tasks: ModelMap<Task>,
                                               componentBuildConf: ComponentExtension) {
                with(mvnPublication) {
                    tasks.withType(ZipContainer::class.java).forEach { task ->
                        this.artifact(task) { mvnArtifact ->
                            mvnArtifact.extension = task.extension

                            val classifier = StringBuilder()
                            if (task.artifactAppendix.isNotBlank()) {
                                classifier.append(task.artifactAppendix)
                            }
                            if (classifier.isNotBlank() && task.artifactClassifier.isNotBlank()) {
                                classifier.append("_")
                            }
                            if (task.artifactClassifier.isNotBlank()) {
                                classifier.append(task.artifactClassifier)
                            }

                            if (classifier.isNotBlank()) {
                                mvnArtifact.classifier = classifier.toString()
                            }
                        }
                    }

                    componentBuildConf.fileItems.items.forEach { item ->
                        this.artifact(item.file) { mvnArtifact ->
                            mvnArtifact.extension = item.extension
                            mvnArtifact.classifier = createClassifierForFile(item)
                        }
                    }

                    tasks.withType(CreateDescriptor::class.java).forEach {
                        this.artifact(it.outputs.files.singleFile) {
                            it.builtBy(it)
                            it.classifier = "component"
                        }
                    }
                }
            }

            private fun configureIvyPublishing(ivyPublication: IvyPublication,
                                               tasks: ModelMap<Task>,
                                               componentBuildConf: ComponentExtension) {
                with(ivyPublication) {
                    tasks.withType(ZipContainer::class.java).forEach { task ->
                        this.artifact(task) { ivyArtifact ->
                            ivyArtifact.name = task.artifactBaseName
                            ivyArtifact.type = task.artifactAppendix

                            if(task.artifactClassifier.isNotBlank()) {
                                ivyArtifact.classifier = task.artifactClassifier
                            }
                        }
                    }

                    componentBuildConf.fileItems.items.forEach { item ->
                        this.artifact(item.file) { ivyArtifact ->
                            ivyArtifact.name = item.name
                            ivyArtifact.type = item.extension

                            if(item.classifier.isNotBlank()) {
                                ivyArtifact.classifier = item.classifier
                            }
                        }
                    }

                    tasks.withType(CreateDescriptor::class.java).forEach { task ->
                        this.artifact(task.outputs.files.singleFile, {
                            it.builtBy(task)
                            it.name = task.project.name
                            it.type = "component"
                        })
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
        fun configurePublishingPublications(tasks: ModelMap<Task>,
                                            publishing: PublishingExtension,
                                            componentBuildConf: ComponentExtension,
                                            @Path("buildDir") buildDir: File) {

            val publications = publishing.publications

            // create container, if configuration is available
            componentBuildConf.containers.items.forEach { container ->
                createContainerTask(tasks, container)
            }

            createDescriptorTask(tasks, componentBuildConf, buildDir)

            try {
                publications.maybeCreate(componentBuildConf.mavenPublicationName, MavenPublication::class.java).apply {
                    configureMvnPublishing(this, tasks, componentBuildConf )
                }
            } catch(ex: InvalidUserDataException) {
                logger.debug("Maven Publishing is not applied for component build plugin.")
            }

            try {
                publications.maybeCreate(componentBuildConf.ivyPublicationName, IvyPublication::class.java).apply {
                    configureIvyPublishing(this, tasks, componentBuildConf)
                }
            } catch(ex: InvalidUserDataException) {
                logger.debug("Ivy Publishing is not applied for component build plugin.")
            }
        }
    }
}
