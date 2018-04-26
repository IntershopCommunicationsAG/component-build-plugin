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
package com.intershop.gradle.component.build.extension.items

import com.intershop.gradle.component.build.extension.Utils
import com.intershop.gradle.component.build.extension.Utils.Companion.MAX_PATH_LENGTH
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet
import org.slf4j.LoggerFactory
import kotlin.properties.Delegates

/**
 * Provides a zip package definition for the component extension.
 *
 * @param project provides the current Gradle project.
 * @param name package name for identification. It is also used for the installed path of the package
 * @constructor provides a preconfigured package with a name
 */
open class FileContainerItem(private val project: Project, @get:Input val name: String) :
        AItem(), IItem, IOSSpecific, IContainer {

    companion object {
        private val logger = LoggerFactory.getLogger(FileContainerItem::class.java.simpleName)
    }

    private val sourceProperty: ConfigurableFileCollection = project.files()
    private val preserveProperty: PatternSet  = project.objects.newInstance(PatternSet::class.java)
    private val excludeSet: MutableSet<String> = mutableSetOf()

    /**
     * The package type describes the usage of this
     * special package. The default value is the package name.
     *
     * @property itemType type of the package
     */
    @Suppress("unused")
    @get:Input
    override var itemType: String = ""

    /**
     * This property is used for the creation of
     * the container artifact. The default value is
     * the project name.
     *
     * @property baseName the base name of the artifact.
     */
    @get:Input
    var baseName: String = project.name

    /**
     * The default target path of the component.
     * This is sub path in the component.
     * Default value is an empty string.
     *
     * @property targetPath target path
     */
    @get:Input
    override var targetPath by Delegates.vetoable("") {_, _, newValue ->
        val invalidChars = Utils.getIllegalChars(newValue)
        if(!invalidChars.isEmpty()) {
            throw InvalidUserDataException("Target path of file container '$name' " +
                    "contains invalid characters '$invalidChars'.")
        }
        if(newValue.startsWith("/")) {
            throw InvalidUserDataException("Target path of file container '$name' " +
                    "starts with a leading '/' - only a relative path is allowed.")
        }
        if(newValue.length > (MAX_PATH_LENGTH / 2)) {
            logger.warn("The target path of file container '$name' is longer then ${(MAX_PATH_LENGTH / 2)}!")
        }
        invalidChars.isEmpty() && ! newValue.startsWith("/")
    }

    /**
     * Is the target path included in the component?
     * This is an important information for the
     * deployment of the component.
     * Default value is false - target is not included in the module.
     *
     * @property targetIncluded if true, the target path is included in the module fileContainers.
     */
    @get:Input
    override var targetIncluded:Boolean = false

    /**
     * All files that will be packaged to a
     * zip file for this special package item.
     *
     * @property source file collection with all source files
     */
    @Suppress("unused")
    @get:InputFiles
    var source: FileCollection
        get() = sourceProperty
        set(value) {
            sourceProperty.setFrom(value)
        }

    /**
     * Add source files to the package definition.
     *
     * @param paths Gradle file objects for the zip package
     */
    @Suppress("unused")
    fun source(vararg paths: Any) {
        sourceProperty.from(*paths)
    }

    /**
     * Files that matches to one of the patterns will be
     * excluded from the update installation.
     *
     * @property excludes Set of Ant based file patterns
     */
    @get:Input
    override val excludes: Set<String>
        get() = excludeSet

    /**
     * Adds a pattern to the set of exclude patterns.
     * Files that matches to one of patterns will be
     * excluded from the update installation.
     * If the pattern is part of the list, the method
     * returns false.
     *
     * @param pattern Ant based file pattern
     */
    @Suppress("unused")
    fun exclude(pattern: String): Boolean {
        return excludeSet.add(pattern)
    }

    /**
     * Adds a set of patterns to the set of exclude patterns.
     * Files that matches to one of patterns will be
     * excluded from the update installation.
     * If one of the patterns is part of the list, the method
     * returns false.
     *
     * @param patterns set of Ant based file pattern
     */
    @Suppress("unused")
    fun exclude(patterns: Set<String>): Boolean {
        return excludeSet.addAll(patterns)
    }

    /**
     * This patterns are used for the update.
     * Files that matches to one of patterns will be
     * excluded in the preserve set of the update installation.
     *
     * @property preserveExcludes Set of Ant based file patterns
     */
    @get:Input
    override val preserveExcludes: Set<String>
        get() = preserveProperty.excludes

    /**
     * This patterns are used for the update.
     * Files that matches to one of patterns will be
     * included in the preserve set of the update installation.
     *
     * @property preserveIncludes Set of Ant based file patterns
     */
    @get:Input
    override val preserveIncludes: Set<String>
        get() = preserveProperty.includes

    /**
     * Get patternset to preserve files from update.
     * Files that matches to one of patterns will be
     * excluded/included from the update installation.
     *
     * @property preserve pattern set to preserve files.
     */
    @Suppress("unused")
    val preserve: PatternFilterable
        get() = preserveProperty

    /**
     * Configure preserve pattern set, to preserve
     * files during the update installation of
     * this module.
     *
     * @param action action to configure pattern set
     */
    @Suppress("unused")
    fun preserve(action: Action<in PatternFilterable>) {
        action.execute(preserveProperty)
    }

    /**
     * Configure preserve pattern set, to preserve
     * files during the update installation of
     * this module.
     *
     * @param closure closure to configure pattern set
     */
    @Suppress("unused")
    fun preserve(closure: Closure<Any>) {
        project.configure(preserveProperty, closure)
    }

    /**
     * This set contains OS specific descriptions.
     * The set is empty per default.
     * It is defined as an task input property.
     *
     * @property classifier the set of OS specific strings
     */
    @get:Input
    override var classifier: String = ""

    /**
     * If an item should not be part of an update installation, this property is set to false.
     *
     * @property updatable If this value is false, the item will be not part of an update installation.
     */
    @get:Input
    var updatable: Boolean = true
}
