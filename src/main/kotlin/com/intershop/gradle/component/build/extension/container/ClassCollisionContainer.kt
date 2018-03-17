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
package com.intershop.gradle.component.build.extension.container

import com.intershop.gradle.component.build.utils.DependencyConfig

/**
 * Configuration container for class collision check.
 *
 * @constructor provides the configuration container.
 */
open class ClassCollisionContainer {

    private val excludeSet: MutableSet<DependencyConfig> = mutableSetOf()

    private val excludedClassesSet: MutableSet<String> = mutableSetOf()

    /**
     * If this property set to false, class collision will be not
     * verified. Default value is true.
     *
     * @property enabled class collision is enabled if the value is true
     */
    var enabled = true

    /**
     * This set provides exclude configuration class collision check.
     *
     * @property excludes set of excluded configurations for collision check.
     */
    @Suppress("unused")
    val excludes: Set<DependencyConfig>
        get() = excludeSet

    /**
     * With exclude it is possible to exclude libraries and modules
     * from the class collision check.
     *
     * @param group Group or oganization of the dependency
     * @param module Name or module of the dependency
     * @param version Version configuration of the dependency
     */
    @Suppress("unused")
    @JvmOverloads
    fun exclude(group: String = "", module: String = "", version: String = "") {
        excludeSet.add(DependencyConfig(
                DependencyConfContainer.createRegexStr(group),
                DependencyConfContainer.createRegexStr(module),
                DependencyConfContainer.createRegexStr(version)))
    }

    /**
     * Set of patterns to exclude classes from
     * the verification of the classpath.
     *
     * @property excludedClasses pattern set to exclude classes from check
     */
    @Suppress("unused")
    val excludedClasses: Set<String>
        get() = excludedClassesSet

    /**
     * Add a pattern, like 'com.test.*' to the set to exclude classes from
     * the verification of the classpath.
     */
    @Suppress("unused")
    fun excludeClass(classPattern: String) {
        excludedClassesSet.add(classPattern.replace(".", "\\/").replace("*", ".*"))
    }
}
