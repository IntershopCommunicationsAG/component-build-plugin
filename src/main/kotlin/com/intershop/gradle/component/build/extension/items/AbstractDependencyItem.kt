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

import com.intershop.gradle.component.build.utils.DependencyConfig

abstract class AbstractDependencyItem(override val parentItem: DeploymentObject) :
        AbstractTypeItem(parentItem), DependencyObject {

    private val excludeSet: MutableSet<DependencyConfig> = mutableSetOf()

    /**
     * This set provides exclude configuration for dependencies.
     *
     * @property excludes set of exclude configurations
     */
    @Suppress("unused")
    override val excludes: Set<DependencyConfig>
        get() = excludeSet

    /**
     * With exclude it is possible to exclude libraries from the list of dependent libraries.
     *
     * @param group Group or oganization of the dependency
     * @param module Name or module of the dependency
     * @param version Version configuration of the dependency
     */
    @Suppress("unused")
    @JvmOverloads
    fun exclude(group: String = "", module: String = "", version: String = "") {
        excludeSet.add(DependencyConfig(group, module, version))
    }

    /**
     * This property configures the dependency resolution
     * of the configured dependency during the creation
     * of the descriptor. The descriptor must be complete!
     * The default value is true.
     *
     * @property resolveTransitive if true dependencies will be resolved transitive.
     */
    override var resolveTransitive: Boolean = true

}