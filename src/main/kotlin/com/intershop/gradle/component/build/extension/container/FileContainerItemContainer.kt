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
import com.intershop.gradle.component.build.extension.items.AbstractTypeItem
import com.intershop.gradle.component.build.extension.items.DeploymentObject
import com.intershop.gradle.component.build.extension.items.FileContainerItem
import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.slf4j.LoggerFactory
import javax.inject.Inject
import kotlin.properties.Delegates

/**
 * This class provides a container for
 * file container (zip packages).
 *
 * @property parentItem the parent of this container.
 * @constructor provides an empty preconfigured file item container
 */
open class FileContainerItemContainer
        @Inject constructor(val project: Project, override val parentItem: DeploymentObject) :
        AbstractTypeItem(parentItem) {

    companion object {
        private val logger = LoggerFactory.getLogger(FileContainerItemContainer::class.java.simpleName)
    }

    private val itemSet: MutableSet<FileContainerItem> = mutableSetOf()

    /**
     * This path describes the installation in the default
     * installation of the component.
     *
     * @property targetPath contains the default installation path
     */
    var targetPath: String by Delegates.vetoable("") { _, _, newValue ->
        val invalidChars = Utils.getIllegalChars(newValue)
        if(!invalidChars.isEmpty()) {
            throw InvalidUserDataException("Target path of container container " +
                    "contains invalid characters '$invalidChars'.")
        }
        if(newValue.startsWith("/")) {
            throw InvalidUserDataException("Target path of container container " +
                    "starts with a leading '/' - only a relative path is allowed.")
        }
        if(newValue.length > (Utils.MAX_PATH_LENGTH / 2)) {
            logger.warn("Target path of container container is longer then ${(Utils.MAX_PATH_LENGTH / 2)}!")
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
     * This set provides all configured files.
     *
     * @property items set of all configured containers
     */
    val items: Set<FileContainerItem>
        get() = itemSet

    /**
     * Add a container (zip package) configuration to the component build.
     *
     * @param name named description of a file container
     * @param types set of deployment or environment types
     */
    @Throws(InvalidUserDataException::class)
    @Suppress("unused")
    fun add(name: String, vararg types: String): FileContainerItem {
        val item = FileContainerItem(project, name, this)
        item.types(types.asList())

        if(itemSet.contains(item)) {
            throw InvalidUserDataException("File container $name is already part of the current configuration!")
        } else {
            itemSet.add(item)
        }

        return item
    }

    /**
     * Add a dependency from any possible definition a to the configuration.
     *
     * @param name named description of a file container.
     */
    @Throws(InvalidUserDataException::class)
    fun add(name: String) : FileContainerItem {
        return add(name, *this.types.toTypedArray())
    }

    /**
     * Add a container (zip package) configuration to the component build.
     *
     * @param name named description of a file container.
     * @param action action to configure all parameters of file container item.
     */
    @Throws(InvalidUserDataException::class)
    @Suppress("unused")
    fun add(name: String, action: Action<in FileContainerItem>) {
        val item = FileContainerItem(project, name, this)

        action.execute(item)

        if(itemSet.contains(item)) {
            throw InvalidUserDataException("File container $name is already part of the current configuration!")
        } else {
            itemSet.add(item)
        }
    }
}
