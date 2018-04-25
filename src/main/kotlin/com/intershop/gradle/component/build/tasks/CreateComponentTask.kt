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
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet
import java.io.File
import com.intershop.gradle.component.descriptor.Library as LibDesr
import com.intershop.gradle.component.descriptor.Module as ModuleDescr

/**
 * CreateComponentTask Gradle task 'createComponent'
 *
 * The injected dependencyHandler is used for internal dependency handling.
 * This task hast currently no declared inputs and outputs and builds
 * file not incremental.
 *
 * @constructor Creates the task with a dependencyHandler
 */
open class CreateComponentTask : DefaultTask() {

    // Outputfile
    private val descriptorFileProperty = this.newOutputFile()
    // display name
    private val displayNameProperty = project.objects.property(String::class.java)
    // component description
    private val componentDescriptionProperty = project.objects.property(String::class.java)
    // preconfigured install target of this component
    private val defaultTargetProperty = project.objects.property(String::class.java)

    // install path of the descriptor file of this component
    private val descriptorPathProperty = project.objects.property(String::class.java)

    // set of central deployment exclude patterns
    private val updateExcludeSetProperty: SetProperty<String> = project.objects.setProperty(String::class.java)

    // set of central deployment exclude patterns
    private val preserveProperty: PatternSet  = project.objects.newInstance(PatternSet::class.java)

    private val dependencyManager = DependencyManager(project)

    /**
     * The output file contains the descriptor of the component.
     *
     * @property descriptorFile real file on file system with descriptor
     */
    @Suppress("unused")
    @get:OutputFile
    var descriptorFile: File
        get() = descriptorFileProperty.get().asFile
        set(value) = descriptorFileProperty.set(value)

    /**
     * Projects will be handled incremental without considering build
     * configuration of the project. the parameter '--recreate' will enable an
     * automatic recreation if project dependencies are available.
     *
     * @property recreate holds the property for the parameter
     */
    @set:Option(option = "recreate",
            description = "Runs 'Component Build' tasks without considering incrementall configuration.")
    @get:Internal
    var recreate: Boolean = false

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
    @Suppress("unused")
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
    @Suppress("unused")
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
     * This property contains the the default target path
     * for the installation.
     *
     * @property defaultTarget default installation target of the component
     */
    @Suppress( "unused")
    @get:Input
    var defaultTarget: String by defaultTargetProperty

    /**
     * This is the path for the descriptor file of an
     * installed component.
     *
     * @property descriptorPath path for all descriptor files
     */
    @Suppress( "unused")
    @get:Input
    var descriptorPath: String by descriptorPathProperty

    /**
     * Set provider for default target property.
     *
     * @param defaultTarget set provider for property.
     */
    @Suppress( "unused")
    fun provideDefaultTarget(defaultTarget: Provider<String>)
            = defaultTargetProperty.set(defaultTarget)

    /**
     * This patterns are used for the update.
     * Files that matches to one of patterns will be
     * excluded from the update installation.
     *
     * @property updateExcludes Set of Ant based file patterns
     */
    @Suppress( "unused")
    @get:Input
    val updateExcludes: Set<String>
        get() = updateExcludeSetProperty.get()

    /**
     * Adds a pattern to the set of exclude patterns.
     * Files that matches to one of patterns will be
     * excluded from the update installation.
     *
     * @param pattern Ant based file pattern
     */
    @Suppress("unused")
    fun updateExclude(pattern: String) {
        updateExcludeSetProperty.add(pattern)
    }

    /**
     * Adds a set of patterns to the set of exclude patterns.
     * Files that matches to one of patterns will be
     * excluded from the update installation.
     *
     * @param patterns  set of Ant based file pattern
     */
    @Suppress("unused")
    fun updateExclude(patterns: Set<String>) {
        patterns.forEach {
            updateExcludeSetProperty.add(it)
        }
    }

    /**
     * Set provider for default property of
     * exclude pattern set from update.
     *
     * @param pattern set provider for property.
     */
    @Suppress( "unused")
    fun provideUpdateExcludes(pattern: Provider<Set<String>>)
            = updateExcludeSetProperty.set(pattern)

    /**
     * This patterns are used for the update.
     * Files that matches to one of patterns will be excluded
     * from the preserved files.
     *
     * @property preserveExcludes Set of Ant based file patterns
     */
    @Suppress( "unused")
    @get:Input
    val preserveExcludes: Set<String>
        get() = preserveProperty.excludes

    /**
     * This patterns are used for the update.
     * Files that matches to one of patterns will be included
     * from the preserved files.
     *
     * @property preserveIncludes Set of Ant based file patterns
     */
    @Suppress( "unused")
    @get:Input
    val preserveIncludes: Set<String>
        get() = preserveProperty.includes

    /**
     * Add pattern to the set of patterns.
     * Files that matches to one of patterns will be
     * excluded or included to the update installation.
     *
     * @param action Action for configuring the preserve filter
     */
    @Suppress("unused")
    fun preserve(action: Action<in PatternFilterable>) {
        action.execute(preserveProperty)
    }

    /**
     * Set provider for default property of
     * the update preserve pattern set.
     *
     * @param pattern set provider for property.
     */
    @Suppress( "unused")
    fun providePreserve(patternSet: PatternFilterable) {
        preserveProperty.copyFrom(patternSet)
    }

    /**
     * Container for all modules. This contains dependencies
     * that must be resolved. Depending on the final version
     * of this dependency, the descriptor must be new generated.
     * Therefore this input is marked as internal and the task
     * is not incremental.
     *
     * @property modules container for all modules
     */
    @get:Internal
    var modules: ModuleItemContainer? = null

    /**
     * Container for all libs. This contains dependencies
     * that must be resolved. Depending on the final version
     * of this dependency, the descriptor must be new generated.
     * Therefore this input is marked as internal and the task
     * is not incremental.
     *
     * @property libs container for all libraries
     */
    @get:Internal
    var libs: LibraryItemContainer? = null

    /**
     * This is a set with all eexclude patterns
     * for all dependencies.
     *
     * @property dependencyExcludes set of all exclude patterns
     */
    @get:Nested
    var dependencyExcludes: Set<DependencyConfig> = mutableSetOf()

    /**
     * This is the container for all single files
     * of this component.
     *
     * @property files container for single file items
     */
    @get:Nested
    var files: FileItemContainer? = null

    /**
     * This is the container for all properties
     * of this component.
     *
     * @property properties container for properties
     */
    @get:Nested
    var properties: PropertyItemContainer? = null

    /**
     * This is the container for all zip container
     * of this component.
     *
     * @property containers container for file containers (zip) packages
     */
    @get:Nested
    var containers: FileContainerItemContainer? = null

    /**
     * Resolved modules from list of items. (*read only*).
     *
     * @property resolvedModules list with dependency configurations
     */
    @get:Nested
    @Suppress("unused")
    val resolvedModules: Set<DependencyConfig>
        get() {
            val moduleDeps = dependencyManager.
                    getModuleDependencies(modules?.items ?: mutableSetOf(), dependencyExcludes)

            this.outputs.upToDateWhen {
                moduleDeps.none {
                    it.version.endsWith("SNAPSHOT") ||
                            it.version.endsWith("LOCAL") ||
                            (it.dependency.isNotBlank() && recreate)
                }
            }
            return moduleDeps
        }

    /**
     * Resolved libraries from list of items. (*read only*).
     *
     * @property resolvedLibs list with dependency configurations
     */
    @get:Nested
    @Suppress("unused")
    val resolvedLibs: Set<DependencyConfig>
        get() {
            val libDeps = dependencyManager.getLibDependencies( libs?.items ?: mutableSetOf(), dependencyExcludes)
            this.outputs.upToDateWhen {
                libDeps.none {
                    it.version.endsWith("SNAPSHOT") ||
                            it.version.endsWith("LOCAL") ||
                            (it.dependency.isNotBlank() && recreate)
                }
            }
            return libDeps
        }

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
                modulesPath = modules?.targetPath ?: "",
                libsPath = libs?.targetPath ?: "",
                containerPath = containers?.targetPath ?: "",
                target = defaultTarget,
                descriptorPath = descriptorPath,
                metadata = ComponentUtil.metadata(project.group.toString(),
                        project.name,
                        project.version.toString()))

        componentDescr.excludes.addAll(updateExcludes)
        componentDescr.preserveExcludes.addAll(preserveExcludes)
        componentDescr.preserveIncludes.addAll(preserveIncludes)

        dependencyManager.addToDescriptor(componentDescr, dependencyExcludes)

        containers?.items?.forEach { item ->
            with(item) {
                if(! source.isEmpty) {
                    val container = FileContainer(
                            name = baseName,
                            targetPath =  targetPath,
                            itemType = itemType,
                            classifier = classifier,
                            targetIncluded = targetIncluded,
                            contentType = ContentType.valueOf(contentType),
                            updatable = updatable
                            )
                    container.excludes.addAll(excludes)
                    container.preserveExcludes.addAll(preserveExcludes)
                    container.preserveIncludes.addAll(preserveIncludes)
                    container.types.addAll(types)

                    if(! componentDescr.addFileContainer(container)) {
                        logger.error("Container '{}' exists in this configuration.", name)
                        throw InvalidUserDataException("Container '$name' exists in this configuration.")
                    }

                    // component types and classifiers contains all available
                    // types and classifiers
                    componentDescr.types.addAll(types)
                    componentDescr.classifiers.add(classifier)
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
                if (file.exists() && file.isFile && file.canRead()) {
                    val file = FileItem(
                            name = name,
                            extension = extension,
                            targetPath = targetPath,
                            classifier = classifier,
                            contentType = ContentType.valueOf(contentType),
                            updatable = updatable)
                    file.types.addAll(types)

                    if(! componentDescr.addFileItem(file)) {
                        logger.error("This file '{}.{}' item exists in this configuration.", name, extension)
                        throw InvalidUserDataException("File item '$name.$extension' exists in this configuration.")
                    }

                    // component types and classifiers contains all available
                    // types and classifiers
                    componentDescr.types.addAll(types)
                    componentDescr.classifiers.add(classifier)
                } else {
                    logger.error("File {} does not exists or it is not readable!", file.absolutePath)
                    throw InvalidUserDataException("File ${file.absolutePath} does not exists or it is not readable!")
                }
            }
        }

        properties?.items?.forEach {
            val property = Property(
                    key = it.key,
                    value = it.value,
                    classifier = it.classifier,
                    contentType = ContentType.valueOf(it.contentType),
                    updatable = it.updatable)
            property.types.addAll(it.types)
            componentDescr.addProperty(property)

            // component types and classifiers contains all available
            // types and classifiers
            componentDescr.types.addAll(it.types)
            componentDescr.classifiers.add(it.classifier)
        }

        // validate targets
        val inspector = TargetDirInspector(componentDescr)
        val errorMsg = inspector.check()
        if(errorMsg.isNotBlank()) {
            logger.error(errorMsg)
            throw InvalidUserDataException(errorMsg)
        }

        ComponentUtil.writeToFile(componentDescr, descriptorFile)
    }
}
