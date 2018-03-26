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

import com.intershop.gradle.component.build.extension.ComponentExtension
import com.intershop.gradle.component.build.extension.Utils.Companion.getDependencyConf
import com.intershop.gradle.component.build.extension.items.LibraryItem
import com.intershop.gradle.component.build.utils.DependencyConfig
import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.tasks.Internal
import javax.inject.Inject

/**
 * This class provides all properties for
 * the library container extension.
 *
 * @property dpendencyHandler necessary for dependency handling
 * @property parent the parent of this container.
 * @constructor provides an empty preconfigured library container
 */
open class LibraryItemContainer
        @Inject constructor(private val dpendencyHandler: DependencyHandler,
                            @get:Internal override val parent: ComponentExtension) :
        AContainer("Library Container", parent) {

    // backing properties
    private val itemSet: MutableSet<LibraryItem> = mutableSetOf()

    /**
     * This set provides all configured libraries. This list will be completed
     * by the transitive dependencies of the component.
     *
     * @property items set of all configured libraries
     */
    val items: Set<LibraryItem>
        get() = itemSet


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

        val item = getPreconfigureItem(depConf)
        item.setTypes(types.asList())
        addItemToList(item)

        return item
    }

    /**
     * Add a dependency from any possible definition a to the configuration.
     *
     * @param dependency a dependency object
     */
    @Throws(InvalidUserDataException::class)
    fun add(dependency: Any) : LibraryItem {
        if(types.isEmpty() && parent.types.isNotEmpty()) {
            return add(dependency, *parent.types.toTypedArray())
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

        val item = getPreconfigureItem(depConf)
        action.execute(item)
        addItemToList(item)
    }

    /**
     * This property configures the dependency resolution
     * of the configured dependencies during the creation
     * of the descriptor. The descriptor must be complete!
     * The default value is true.
     *
     * @property resolveTransitive if true dependencies will be resolved transitive.
     */
    @Suppress("unused")
    var resolveTransitive: Boolean = true

    /*
     * Creates a preconfigured lib item. Configuration is
     * taken from container configuration.
     */
    private fun getPreconfigureItem(depConf: DependencyConfig) : LibraryItem {
        val item = LibraryItem(depConf)
        item.targetName = "${depConf.group}_${depConf.module}_${depConf.version}"
        item.resolveTransitive = resolveTransitive

        return item
    }

    /*
     * Add item to list if the name or the special configuration does not exists in the list.
     */
    private fun addItemToList(item: LibraryItem) {
        addTypes(item)

        if(itemSet.find { it.dependency == item.dependency } != null) {
            throw InvalidUserDataException("Dependency '${item.dependency}' is already " +
                    " part of the current configuration!")
        } else {
            itemSet.add(item)
        }
    }
}
