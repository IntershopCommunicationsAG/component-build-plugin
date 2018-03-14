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

import com.intershop.gradle.component.build.extension.items.IDeployment
import com.intershop.gradle.component.build.extension.items.FileContainerItem
import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import javax.inject.Inject

/**
 * This class provides a container for
 * file container (zip packages).
 *
 * @property parentItem the parent of this container.
 * @constructor provides an empty preconfigured file item container
 */
open class FileContainerItemContainer
        @Inject constructor(val project: Project, override val parentItem: IDeployment) :
        AContainer(parentItem, "File Container Container") {

    private val itemSet: MutableSet<FileContainerItem> = mutableSetOf()

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
        item.setTypes(types.asList())

        if(itemSet.find { it.name == item.name } != null) {
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
        if(types.isEmpty() && parentItem.types.isNotEmpty()) {
            return add(name, *parentItem.types.toTypedArray())
        }
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

        if(itemSet.find { it.name == item.name } != null) {
            throw InvalidUserDataException("File container $name is already part of the current configuration!")
        } else {
            itemSet.add(item)
        }
    }
}
