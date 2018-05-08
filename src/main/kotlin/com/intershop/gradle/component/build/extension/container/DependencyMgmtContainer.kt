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
import org.gradle.api.Action
import org.gradle.api.Project
import javax.inject.Inject

/**
 * Container for all additional dependency configurations, like
 * dependencyExcludes and the configuration for the verification of the classpath.
 *
 * @constructor provides the configuration
 */
open class DependencyMgmtContainer @Inject constructor(project: Project) {

    private val excludeSet: MutableSet<DependencyConfig> = mutableSetOf()

    private val classpathVerificationContainer =
            project.objects.newInstance(ClasspathVerificationContainer::class.java)

    /**
     * This set provides exclude configuration for all
     * dependency configurations - modules and libraries.
     *
     * @property excludes set of exclude configurations
     */
    @Suppress("unused")
    val excludes: Set<DependencyConfig>
        get() = excludeSet

    /**
     * With exclude it is possible to exclude libraries and modules
     * from the list of dependent objects.
     *
     * @param group Group or organization of the dependency
     * @param module Name or module of the dependency
     * @param version Version configuration of the dependency
     */
    @Suppress("unused")
    @JvmOverloads
    fun exclude(group: String = "", module: String = "", version: String = "") {
        excludeSet.add(DependencyConfig(group, module, version))
    }

    /**
     * Property (read only) of the classpath verification configuration.
     *
     * @property classpathVerification configuration container.
     */
    val classpathVerification: ClasspathVerificationContainer
        get() = classpathVerificationContainer

    /**
     * Configures classpath verification container of a component.
     *
     * @param action execute the classpath verification configuration
     */
    @Suppress("unused")
    fun classpathVerification(action: Action<in ClasspathVerificationContainer>) {
        action.execute(classpathVerificationContainer)
    }
}
