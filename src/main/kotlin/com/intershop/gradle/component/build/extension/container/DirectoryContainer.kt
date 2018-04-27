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
import com.intershop.gradle.component.build.extension.items.AItem
import com.intershop.gradle.component.build.extension.items.Directory
import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import javax.inject.Inject

/**
 * This class provides a container for
 * deployable directory configurations.
 *
 * @property parent the parent of this container.
 * @constructor provides an empty preconfigured file item container
 */
open class DirectoryContainer @Inject constructor(@get: Internal val parent: ComponentExtension) : AItem() {

    private val itemSet: MutableSet<Directory> = mutableSetOf()

    /**
     * This set provides all configured directories.
     *
     * @property items set of all configured directories
     */
    @get:Nested
    val items: Set<Directory>
        get() = itemSet

    /**
     * Add a single directory to the component.
     *
     * @param targetPath a relative path for the component
     * @param types set of deployment or environment types
     */
    @Throws(InvalidUserDataException::class)
    @Suppress("unused")
    fun add(targetPath: String, vararg types: String): Directory {

        val item = getPreconfigureItem(targetPath)
        item.setTypes(types.asList())
        addItemToList(item)

        return item
    }

    /**
     * Add a single directory to the component.
     *
     * @param targetPath a relative path for the component
     */
    @Throws(InvalidUserDataException::class)
    fun add(targetPath: String) : Directory {
        if(types.isEmpty() && parent.types.isNotEmpty()) {
            return add(targetPath, *parent.types.toTypedArray())
        }
        return add(targetPath, *this.types.toTypedArray())
    }

    /**
     * Add a single directory to the component and configures this component.
     *
     * @param targetPath a relative path for the component
     * @param action action to configure all parameters of directory item.
     */
    @Throws(InvalidUserDataException::class)
    @Suppress("unused")
    fun add(targetPath: String, action: Action<in Directory>) {

        val item = getPreconfigureItem(targetPath)
        action.execute(item)
        addItemToList(item)
    }

    /*
     * Creates a preconfigured directory. Configuration is
     * taken from container configuration.
     */
    private fun getPreconfigureItem(targetPath: String) : Directory {
        val item = Directory(targetPath)
        item.updatable = updatable

        return item
    }

    /*
     * Add item to list if the name or the special configuration does not exists in the list.
     */
    private fun addItemToList(item: Directory) {
        addTypes(item)

        if(itemSet.find { it.targetPath == item.targetPath &&
                        it.classifier == item.classifier} != null) {
            throw InvalidUserDataException("Directory ${item.targetPath} is already part of the current configuration!")
        } else {
            itemSet.add(item)
        }
    }

    /**
     * If an item should not be part of an update installation, this property is set to true.
     *
     * @property updatable If this value is true, the item will be not part of an update installation.
     */
    @get:Input
    var updatable: Boolean = true

    private fun addTypes(item: AItem) {
        if(types.isEmpty() && parent.types.isNotEmpty()) {
            item.addTypes(parent.types)
        } else {
            item.addTypes(this.types)
        }
    }
}
