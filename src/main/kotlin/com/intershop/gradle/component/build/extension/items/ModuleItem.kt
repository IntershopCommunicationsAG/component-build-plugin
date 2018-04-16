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
import com.intershop.gradle.component.build.utils.DependencyConfig
import org.gradle.api.InvalidUserDataException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.internal.impldep.org.bouncycastle.asn1.x500.style.RFC4519Style.name
import org.slf4j.LoggerFactory
import kotlin.properties.Delegates

/**
 * This class provides a module object for the component
 * extension of the component build plugin.
 *
 * @property dependency a dependency configuration of this module
 * @constructor initialize an module with a defined dependency
 */
class ModuleItem(@get:Nested override val dependency: DependencyConfig) :
        ADependencyItem(), IItem, IContainer, IDependency {

    companion object {
        private val logger = LoggerFactory.getLogger(ModuleItem::class.java.simpleName)
    }

    private val excludeSet: MutableSet<String> = mutableSetOf()
    private val preserveSet: MutableSet<String> = mutableSetOf()

    /**
     * The default target path of the component.
     * This is sub path in the component.
     * Default value is an empty string.
     *
     * @property targetPath target path
     */
    override var targetPath by Delegates.vetoable("") { _, _, newValue ->
        val invalidChars = Utils.getIllegalChars(newValue)
        if(!invalidChars.isEmpty()) {
            throw InvalidUserDataException("Target path of module '$name' " +
                    "contains invalid characters '$invalidChars'.")
        }
        if(newValue.startsWith("/")) {
            throw InvalidUserDataException("Target path of module '$name' " +
                    "starts with a leading '/' - only a relative path is allowed.")
        }
        if(newValue.length > (Utils.MAX_PATH_LENGTH / 2)) {
            logger.warn("The target path of module '$name' is longer then ${(Utils.MAX_PATH_LENGTH / 2)}!")
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
     * Files that matches to one of patterns will be
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
     * excluded from the update installation.
     *
     * @property preserves Set of Ant based file patterns
     */
    @get:Input
    override val preserves: Set<String>
        get() = preserveSet

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
    fun preserve(pattern: String): Boolean {
        return preserveSet.add(pattern)
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
    fun preserve(patterns: Set<String>): Boolean {
        return preserveSet.addAll(patterns)
    }

    /**
     * This property can be used to add a special type
     * description for a module.
     *
     * @property itemType Module type property
     */
    @get:Input
    override var itemType: String = ""

    /**
     * If an item should not be part of an update installation, this property is set to false.
     *
     * @property updatable If this value is false, the item will be not part of an update installation.
     */
    @get:Input
    var updatable: Boolean = true

    /**
     * The target path for all jar files of this module.
     *
     * @property jarPath path for all jar files of this module
     */
    @get:Input
    var jarPath: String = "libs"

    /**
     * The target path for descriptor files of this module.
     *
     * @property descriptorPath path for all descriptor files of this module
     */
    @get:Input
    var descriptorPath: String = ""
}
