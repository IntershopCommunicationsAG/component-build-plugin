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
package com.intershop.gradle.component.build.tasks


import com.intershop.gradle.component.build.extension.container.FileContainerItemContainer
import com.intershop.gradle.component.build.extension.container.FileItemContainer
import com.intershop.gradle.component.build.extension.container.LibraryItemContainer
import com.intershop.gradle.component.build.extension.container.ModuleItemContainer
import com.intershop.gradle.component.build.extension.container.PropertyItemContainer
import com.intershop.gradle.component.build.utils.DependencyConfig
import com.intershop.gradle.component.build.utils.getValue
import com.intershop.gradle.component.build.utils.setValue
import com.intershop.gradle.component.descriptor.Component
import com.intershop.gradle.component.descriptor.ContentType
import com.intershop.gradle.component.descriptor.FileContainer
import com.intershop.gradle.component.descriptor.FileItem
import com.intershop.gradle.component.descriptor.Property
import com.intershop.gradle.component.descriptor.util.ComponentUtil
import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import com.intershop.gradle.component.descriptor.Library as LibDesr
import com.intershop.gradle.component.descriptor.Module as ModuleDescr

/**
 * CreateDescriptorTask Gradle task 'createComponent'
 *
 * The injected dependencyHandler is used for internal dependency handling.
 * This task hast currently no declared inputs and outputs and builds
 * file not incremental.
 *
 * @constructor Creates the task with a dependencyHandler
 */
open class CreateDescriptorTask : DefaultTask() {

    // Outputfile configurationFor
    private val descriptorFileProperty = this.newOutputFile()
    // display name configurationFor
    private val displayNameProperty = project.objects.property(String::class.java)
    // component description configurationFor
    private val componentDescriptionProperty = project.objects.property(String::class.java)

    /**
     * The output file contains the descriptor of the component.
     *
     * @property descriptorFile real file on file system with descriptor
     */
    @Suppress("private", "unused")
    @get:OutputFile
    var descriptorFile: File
        get() = descriptorFileProperty.get().asFile
        set(value) = descriptorFileProperty.set(value)

    /**
     * Set provider for descriptor file property.
     *
     * @param descriptorFile set provider for property.
     */
    @Suppress( "unused")
    fun provideDescriptorFile(descriptorFile: Provider<RegularFile>)
            = descriptorFileProperty.set(descriptorFile)

    /**
     * This property contains the display name of the component.
     * Declares a task input for incremental build.
     *
     * @property displayName name of the component
     */
    @Suppress("private", "unused")
    @get:Input
    var displayName: String by displayNameProperty

    /**
     * Set provider for display name property.
     *
     * @param displayName set provider for property.
     */
    @Suppress( "unused")
    fun provideDisplayName(displayName: Provider<String>)
            = displayNameProperty.set(displayName)

    /**
     * This property contains the description of the component.
     * Declares a task input for incremental build.
     *
     * @property componentDescription description of the component
     */
    @Suppress("private", "unused")
    @get:Input
    var componentDescription: String by componentDescriptionProperty

    /**
     * Set provider for compoent description property.
     *
     * @param description set provider for property.
     */
    @Suppress( "unused")
    fun provideComponentDescription(description: Provider<String>)
            = componentDescriptionProperty.set(description)

    /**
     * Container for all modules.
     */
    //TODO: This should be a nested property.
    @get:Internal
    var modules: ModuleItemContainer? = null

    /**
     * Container for all libs.
     */
    @get:Internal
    var libs: LibraryItemContainer? = null

    @get:Internal
    var excludes: Set<DependencyConfig> = mutableSetOf()

    @get:Internal
    var files: FileItemContainer? = null

    @get:Internal
    var properties: PropertyItemContainer? = null

    @get:Internal
    var containers: FileContainerItemContainer? = null

    /**
     * Task method for the creation of a descriptor file.
     * This is one of the artifacts of a component.
     */
    @Suppress("unused")
    @Throws(InvalidUserDataException::class)
    @TaskAction
    fun createDescriptor() {

        val componentDescr = Component(
                displayName = displayName,
                componentDescription = componentDescription,
                modulesTarget = modules?.targetPath ?: "",
                libsTarget = libs?.targetPath ?: "",
                containerTarget = containers?.targetPath ?: "",
                fileTarget = files?.targetPath ?: "")

        val dependencyProcessor = DependencyProcessor(project.rootProject,
                                                      project.configurations,
                                                      project.dependencies,
                                                      excludes)

        dependencyProcessor.addDependencies(componentDescr, modules?.items, libs?.items)

        containers?.items?.forEach { item ->
            with(item) {
                if(! source.isEmpty) {
                    val container = FileContainer(name, targetPath, containerType, classifier, targetIncluded,
                            ContentType.valueOf(contentType))
                    container.types.addAll(types)
                    if(! componentDescr.addFileContainer(container)) {
                        logger.error("Container '{}' exists in this configuration.", name)
                        throw InvalidUserDataException("Container '$name' exists in this configuration.")
                    }
                } else {
                    logger.error("Container sources of '{}' are empty! Publishing of this container is not possibble.",
                            name)
                    throw InvalidUserDataException("Container sources of '$name' are empty!" +
                            "It will be not possible to publish this container.")
                }
            }
        }

        files?.items?.forEach { item ->
            with(item) {
                if (!file.exists() && file.isFile && file.canRead()) {
                    val file = FileItem(name, extension, targetPath, classifier, ContentType.valueOf(contentType))
                    file.types.addAll(types)
                    if(! componentDescr.addFileItem(file)) {
                        logger.error("This file '{}.{}' item exists in this configuration.", name, extension)
                        throw InvalidUserDataException("File item '$name.$extension' exists in this configuration.")
                    }
                } else {
                    logger.error("File {} does not exists or it is not readable!", file.absolutePath)
                    throw InvalidUserDataException("File ${file.absolutePath} does not exists or it is not readable!")
                }
            }
        }

        properties?.items?.forEach {
            val property = Property(it.key, it.value, it.classifier, ContentType.valueOf(it.contentType))
            property.types.addAll(it.types)
            componentDescr.addProperty(property)
        }

        ComponentUtil.writeToFile(componentDescr, descriptorFile)
    }
}
