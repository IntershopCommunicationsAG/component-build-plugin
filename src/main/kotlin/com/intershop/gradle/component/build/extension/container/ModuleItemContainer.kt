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
import com.intershop.gradle.component.build.extension.Utils
import com.intershop.gradle.component.build.extension.items.ModuleItem
import com.intershop.gradle.component.build.utils.DependencyConfig
import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.tasks.Internal
import javax.inject.Inject

/**
 * This class provides all properties for
 * the module container extension.
 *
 * @property dependencyHandler necessary for dependency handling
 * @property parent the parent of this container.
 * @constructor provides an empty preconfigured module container
 */
open class ModuleItemContainer
        @Inject constructor(private val dependencyHandler: DependencyHandler,
                            @get:Internal val project: Project,
                            @get:Internal override val parent: ComponentExtension) :
        AContainer( "Module Container", "", parent) {

    // backing properties
    private val itemSet: MutableSet<ModuleItem> = mutableSetOf()

    /**
     * This set provides all configured modules. This list will be completed
     * by the transitive dependencies of the component.
     *
     * @property items set of all configured modules
     */
    val items: Set<ModuleItem>
        get() = itemSet

    /**
     * Add a dependency of a module from any possible definition with
     * special type configuration to the library list.
     * This dependency will be resolved transitive!
     *
     * @param dependency a dependency object
     * @param types set of deployment or environment types
     */
    @Throws(InvalidUserDataException::class)
    @Suppress("unused")
    fun add(dependency: Any, vararg types: String) : ModuleItem {
        val depConf = Utils.getDependencyConf(dependencyHandler, dependency,
                "It can not be added to the module container.")

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
    fun add(dependency: Any) : ModuleItem {
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
     * @param dependency library dependency.
     * @param action action to configure all parameters of module item.
     */
    @Throws(InvalidUserDataException::class)
    @Suppress("unused")
    fun add(dependency: Any, action: Action<in ModuleItem>) {
        val depConf = Utils.getDependencyConf(dependencyHandler, dependency,
                "It can not be added to the module container.")

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
     * Creates a preconfigured module item. Configuration is
     * taken from container configuration.
     */
    private fun getPreconfigureItem(depConf: DependencyConfig) : ModuleItem {
        val item = ModuleItem(project, depConf)
        item.updatable = updatable
        item.targetPath = depConf.module
        item.resolveTransitive = resolveTransitive
        item.descriptorPath = descriptorPath
        item.jarPath = jarPath

        return item
    }

    /*
     * Add item to list if the name does not exists in the list.
     */
    private fun addItemToList(item: ModuleItem) {
        addTypes(item)

        if(itemSet.find { it.dependency.module == item.dependency.module } != null)  {
            throw InvalidUserDataException("Dependency '${item.dependency.module}' is " +
                    "already part of the current configuration!")
        } else {
            itemSet.add(item)
        }
    }

    /**
     * If an item should not be part of an update installation, this property is set to false.
     *
     * @property updatable If this value is false, the item will be not part of an update installation.
     */
    @Suppress("unused")
    var updatable: Boolean = true

    /**
     * The target path for jar files of each module.
     * This value is used to preconfigure a module item.
     *
     * @property jarPath path for all jar files of a module
     */
    @Suppress("unused")
    var jarPath: String = "libs"

    /**
     * The target path for descriptor files of each module.
     * This value is used to preconfigure a module item.
     *
     * @property descriptorPath path for all descriptor files of a module
     */
    @Suppress("unused")
    var descriptorPath: String = ""
}
