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
package com.intershop.gradle.component

import com.intershop.gradle.component.extension.ComponentExtension
import com.intershop.gradle.component.tasks.CreateComponentDescriptor
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.model.internal.registry.ModelRegistry
import javax.inject.Inject

/**
 * Plugin Class
 */
class ComponentPlugin @Inject constructor(val modelRegistry: ModelRegistry, val dependencyHandler: DependencyHandler) : Plugin<Project> {

    companion object {
        const val TASKDESCRIPTION = "Generate component file from configuration"
        const val TASKNAME = "createComponent"
    }

    override fun apply(project: Project) {
        with(project) {
            logger.info("Component plugin adds extension {} to {}", ComponentExtension.COMPONENT_EXTENSION_NAME, name)
            val extension = extensions.findByType(ComponentExtension::class.java)
                    ?: extensions.create(ComponentExtension.COMPONENT_EXTENSION_NAME, ComponentExtension::class.java, this)

            configureTask(project, extension)
        }
    }

    private fun configureTask(project: Project, extension: ComponentExtension) {
        project.tasks.maybeCreate(TASKNAME, CreateComponentDescriptor::class.java).apply {
            group = ComponentExtension.DEPLOYMENT_GROUP_NAME
            description = TASKDESCRIPTION

            provideDisplayName(extension.displayNameProvider)
            provideComponentDescription(extension.componentDescriptionProvider)
            provideModuleDependencies(extension.moduleDependenciesProvider)
            provideLibraryDependencies(extension.libraryDependenciesProvider)
            provideDescriptorFile((project.layout.buildDirectory.file(ComponentExtension.DESCRIPTOR_FILE)))
        }
    }
}