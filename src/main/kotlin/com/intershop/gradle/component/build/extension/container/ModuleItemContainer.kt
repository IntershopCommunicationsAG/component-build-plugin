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

import com.intershop.gradle.component.build.extension.Utils
import com.intershop.gradle.component.build.extension.items.DependencyConfig
import com.intershop.gradle.component.build.extension.items.DeploymentObject
import com.intershop.gradle.component.build.extension.items.ModuleItem
import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.artifacts.dsl.DependencyHandler
import javax.inject.Inject

/**
 * This class provides all properties for
 * the module container extension.
 *
 * @property dpendencyHandler necessary for dependency handling
 * @property parentItem the parent of this container.
 * @constructor provides an empty preconfigured module container
 */
open class ModuleItemContainer
        @Inject constructor(val dpendencyHandler: DependencyHandler, override val parentItem: DeploymentObject) :
        AbstractContainer(parentItem, "Module Container") {

    // backing properties
    private val itemSet: MutableSet<ModuleItem> = mutableSetOf()
    private val excludeList: MutableList<DependencyConfig> = mutableListOf()

    /**
     * This set provides all configured modules. This list will be completed
     * by the transitive dependencies of the component.
     *
     * @property items set of all configured modules
     */
    val items: Set<ModuleItem>
        get() = itemSet

    /**
     * With exclude it is possible to exclude modules from the list of dependent modules.
     *
     * @param group Group or oganization of the dependency
     * @param module Name or module of the dependency
     * @param version Version configuration of the dependency
     */
    @Suppress("unused")
    @JvmOverloads
    fun exclude(group: String = "", module: String = "", version: String = "") {
        excludeList.add(DependencyConfig(group, module, version))
    }

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
        val depConf = Utils.getDependencyConf(dpendencyHandler, dependency,
                "It can not be added to the module container.")

        val item = ModuleItem(depConf, this)
        item.setTypes(types.asList())

        if(itemSet.find { it.dependency == item.dependency } != null)  {
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
    fun add(dependency: Any) : ModuleItem {
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
     * @param dependency library dependency.
     * @param action action to configure all parameters of module item.
     */
    @Throws(InvalidUserDataException::class)
    @Suppress("unused")
    fun add(dependency: Any, action: Action<in ModuleItem>) {
        val depConf = Utils.getDependencyConf(dpendencyHandler, dependency,
                "It can not be added to the module container.")
        val item = ModuleItem(depConf, this)

        action.execute(item)

        if(itemSet.contains(item)) {
            throw InvalidUserDataException("Dependency '$dependency' is already part of the current configuration!")
        } else {
            itemSet.add(item)
        }
    }
}
