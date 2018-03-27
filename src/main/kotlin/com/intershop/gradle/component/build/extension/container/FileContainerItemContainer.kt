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
import com.intershop.gradle.component.build.extension.items.FileContainerItem
import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import javax.inject.Inject

/**
 * This class provides a container for
 * file container (zip packages).
 *
 * @property parent the parent of this container.
 * @constructor provides an empty preconfigured file item container
 */
open class FileContainerItemContainer
        @Inject constructor(@get:Internal val project: Project,
                            @get:Internal override val parent: ComponentExtension) :
        AContainer("File Container Container", parent) {

    private val itemSet: MutableSet<FileContainerItem> = mutableSetOf()

    /**
     * This set provides all configured files.
     *
     * @property items set of all configured containers
     */
    @get:Nested
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

        val item = getPreconfigureItem(project, name)
        item.setTypes(types.asList())
        addItemToList(item)
        return item
    }

    /**
     * Add a dependency from any possible definition a to the configuration.
     *
     * @param name named description of a file container.
     */
    @Throws(InvalidUserDataException::class)
    fun add(name: String) : FileContainerItem {
        if(types.isEmpty() && parent.types.isNotEmpty()) {
            return add(name, *parent.types.toTypedArray())
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

        val item = getPreconfigureItem(project, name)
        action.execute(item)
        addItemToList(item)
    }

    /*
     * Creates a preconfigured file container item. Configuration is
     * taken from container configuration.
     */
    private fun getPreconfigureItem(project: Project, name: String) : FileContainerItem {
        val item = FileContainerItem(project, name)
        item.excludeFromUpdate = excludeFromUpdate

        return item
    }

    /*
     * Add item to list if the name or the special configuration does not exists in the list.
     */
    private fun addItemToList(item: FileContainerItem) {
        addTypes(item)

        if(itemSet.find { it.name == item.name} != null ||
                itemSet.find { it.baseName == item.baseName &&
                        it.itemType == item.itemType &&
                        it.classifier == item.classifier} != null) {
            throw InvalidUserDataException("File container ${item.name} is already part of the current configuration!")
        } else {
            itemSet.add(item)
        }
    }

    /**
     * If an item should not be part of an update installation, this property is set to true.
     *
     * @property excludeFromUpdate If this value is true, the item will be not part of an update installation.
     */
    @get:Input
    var excludeFromUpdate: Boolean = false
}
