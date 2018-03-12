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
import com.intershop.gradle.component.build.extension.Utils.Companion.getDependencyConf
import com.intershop.gradle.component.build.extension.items.AbstractTypeItem
import com.intershop.gradle.component.build.extension.items.DependencyConfig
import com.intershop.gradle.component.build.extension.items.DeploymentObject
import com.intershop.gradle.component.build.extension.items.LibraryItem
import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.slf4j.LoggerFactory
import javax.inject.Inject
import kotlin.properties.Delegates

/**
 * This class provides all properties for
 * the library container extension.
 *
 * @property dpendencyHandler necessary for dependency handling
 * @property parentItem the parent of this container.
 * @constructor provides an empty preconfigured library container
 */
open class LibraryItemContainer
        @Inject constructor(val dpendencyHandler: DependencyHandler, override val parentItem: DeploymentObject) :
        AbstractTypeItem(parentItem) {

    companion object {
        private val logger = LoggerFactory.getLogger(LibraryItemContainer::class.java.simpleName)
    }

    // backing properties
    private val itemSet: MutableSet<LibraryItem> = mutableSetOf()
    private val excludeList: MutableList<DependencyConfig> = mutableListOf()

    /**
     * This path describes the installation in the default
     * installation of the component.
     *
     * @property targetPath contains the default installation path
     */
    var targetPath: String by Delegates.vetoable("") { _, _, newValue ->
        val invalidChars = Utils.getIllegalChars(newValue)
        if(!invalidChars.isEmpty()) {
            throw InvalidUserDataException("Target path of library container " +
                    "contains invalid characters '$invalidChars'.")
        }
        if(newValue.startsWith("/")) {
            throw InvalidUserDataException("Target path of library container " +
                    "starts with a leading '/' - only a relative path is allowed.")
        }
        if(newValue.length > (Utils.MAX_PATH_LENGTH / 2)) {
            logger.warn("Target path of library container is longer then ${(Utils.MAX_PATH_LENGTH / 2)}!")
        }
        invalidChars.isEmpty() && ! newValue.startsWith("/")
    }

    /**
     * The complete install target of this item.
     *
     * @return a string representation of the item.
     */
    override fun getInstallPath(): String {
        val installPath = StringBuilder(parentItem.getInstallPath())

        if(! targetPath.isEmpty()) {
            if(! installPath.endsWith("/")) {
                installPath.append("/")
            }
            installPath.append(targetPath)
        }

        return installPath.toString()
    }

    /**
     * This set provides all configured libraries. This list will be completed
     * by the transitive dependencies of the component.
     *
     * @property items set of all configured libraries
     */
    val items: Set<LibraryItem>
        get() = itemSet

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
        excludeList.add(DependencyConfig(group, module, version))
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
        item.types(types.asList())

        if(itemSet.contains(item)) {
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

        action.execute(item)

        if(itemSet.contains(item)) {
            throw InvalidUserDataException("Dependency '$dependency' is already part of the current configuration!")
        } else {
            itemSet.add(item)
        }
    }
}