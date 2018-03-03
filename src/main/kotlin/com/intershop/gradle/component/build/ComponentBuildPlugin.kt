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
import com.intershop.gradle.component.build.tasks.CreateComponentDescriptor
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.ivy.IvyPublication
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.model.Defaults
import org.gradle.model.ModelMap
import org.gradle.model.RuleSource

/**
 * Plugin Class for 'com.intershop.gradle.component.build'
 *
 * This class contains also Rules for publishing task outputs.
 */
@Suppress("unused")
class ComponentBuildPlugin : Plugin<Project> {

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
        }
    }

    // configure task for the creation of the file
    private fun configureTask(project: Project, extension: ComponentExtension) {
        project.tasks.maybeCreate(TASKNAME, CreateComponentDescriptor::class.java).apply {
            group = ComponentExtension.DEPLOYMENT_GROUP_NAME
            description = TASKDESCRIPTION

            provideDisplayName(extension.displayNameProvider)
            provideComponentDescription(extension.componentDescriptionProvider)

            libs = extension.libs
            modules = extension.modules

            provideDescriptorFile((project.layout.buildDirectory.file(ComponentExtension.DESCRIPTOR_FILE)))
        }
    }

    /**
     * This RuleSource adds rules for publishing task output
     * of CreateComponentDescriptor Task.
     */
    @Suppress("unused")
    class Rules: RuleSource() {

        /**
         * Configures publishing of task output
         * depending on configured publishing.
         */
        @Suppress("unused")
        @Defaults
        fun configurePublishingPublications(tasks: ModelMap<Task>, publishing: PublishingExtension) {
            val publications = publishing.publications

            //TODO: only ivy will be used. It is necessary also Maven - depending on configured publishing.
            publications.maybeCreate("ivy", IvyPublication::class.java).apply {
                tasks.withType(CreateComponentDescriptor::class.java).forEach { task ->
                    this.artifact(task.outputs.files.singleFile, {
                        it.builtBy(task)
                        it.name = task.project.name
                        it.type = "component"
                    })
                }
            }
        }
    }
}
