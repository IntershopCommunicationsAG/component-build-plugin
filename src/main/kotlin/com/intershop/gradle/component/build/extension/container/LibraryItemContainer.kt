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

import com.intershop.gradle.component.build.extension.Utils.Companion.getDependencyConf
import com.intershop.gradle.component.build.extension.items.IDeployment
import com.intershop.gradle.component.build.extension.items.LibraryItem
import com.intershop.gradle.component.build.utils.DependencyConfig
import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.artifacts.dsl.DependencyHandler
import javax.inject.Inject

/**
 * This class provides all properties for
 * the library container extension.
 *
 * @property dpendencyHandler necessary for dependency handling
 * @property parentItem the parent of this container.
 * @constructor provides an empty preconfigured library container
 */
open class LibraryItemContainer
        @Inject constructor(val dpendencyHandler: DependencyHandler, override val parentItem: IDeployment) :
        AContainer(parentItem, "Library Container") {

    // backing properties
    private val itemSet: MutableSet<LibraryItem> = mutableSetOf()
    private val excludeSet: MutableSet<DependencyConfig> = mutableSetOf()

    /**
     * This set provides all configured libraries. This list will be completed
     * by the transitive dependencies of the component.
     *
     * @property items set of all configured libraries
     */
    val items: Set<LibraryItem>
        get() = itemSet

    /**
     * This set provides exclude configuration for dependencies.
     *
     * @property excludes set of exclude configurations
     */
    @Suppress("unused")
    val excludes: Set<DependencyConfig>
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
     * Add a dependency from any possible definition with
     * special type configuration to the library list.
     *
     * @param dependency a dependency object
     * @param types set of deployment or environment types
     */
    @Throws(InvalidUserDataException::class)
    @Suppress("unused")
    fun add(dependency: Any, vararg types: String) : LibraryItem {
        val depConf = getDependencyConf(dpendencyHandler, dependency,
                "It can not be added to the library container.")

        val item = LibraryItem(depConf, this)
        item.setTypes(types.asList())
        item.targetName = "${depConf.group}_${depConf.module}_${depConf.version}"
        item.resolveTransitive = resolveTransitive

        if(itemSet.find { it.dependency == item.dependency } != null) {
            throw InvalidUserDataException("Dependency '$dependency' is already part of the current configuration!")
        } else {
            itemSet.add(item)
        }

        return item
    }

    /**
     * Add a dependency from any possible definition a to the configuration.
     *
     * @param dependency a dependency object
     */
    @Throws(InvalidUserDataException::class)
    fun add(dependency: Any) : LibraryItem {
        if(types.isEmpty() && parentItem.types.isNotEmpty()) {
            return add(dependency, *parentItem.types.toTypedArray())
        }
        return add(dependency, *this.types.toTypedArray())
    }

    /**
     * Add a list of dependencies to the library list.
     *
     * @param libs a list of dependencies
     */
    @Throws(InvalidUserDataException::class)
    @Suppress("unused")
    fun add(libs: Collection<Any>) {
        libs.forEach {
            add(it, *this.types.toTypedArray())
        }
    }

    /**
     * Add a dependency from any possible definition a to the configuration.
     *
     * @param dependency a dependency object.
     * @param action action to configure all parameters of library item.
     */
    @Throws(InvalidUserDataException::class)
    @Suppress("unused")
    fun add(dependency: Any, action: Action<in LibraryItem>) {
        val depConf = getDependencyConf(dpendencyHandler, dependency,
                "It can not be added to the library container.")
        val item = LibraryItem(depConf, this)
        item.targetName = "${depConf.group}_${depConf.module}_${depConf.version}"
        item.resolveTransitive = resolveTransitive

        action.execute(item)

        if(itemSet.find { it.dependency == item.dependency } != null) {
            throw InvalidUserDataException("Dependency '$dependency' is already part of the current configuration!")
        } else {
            itemSet.add(item)
        }
    }

    /**
     * This property configures the dependency resolution
     * of the configured dependencies during the creation
     * of the descriptor. The descriptor must be complete!
     * The default value is true.
     *
     * @property resolveTransitive if true dependencies will be resolved transitive.
     */
    var resolveTransitive: Boolean = true
}
