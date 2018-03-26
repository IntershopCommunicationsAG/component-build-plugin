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
import com.intershop.gradle.component.build.extension.items.FileItem
import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import java.io.File
import javax.inject.Inject

/**
 * This class provides a container for
 * deployable single files.
 *
 * @property parent the parent of this container.
 * @constructor provides an empty preconfigured file item container
 */
open class FileItemContainer @Inject constructor(@get: Internal override val parent: ComponentExtension) :
        AContainer("File Item Container", parent) {

    private val itemSet: MutableSet<FileItem> = mutableSetOf()

    /**
     * This set provides all configured files.
     *
     * @property items set of all configured files
     */
    @get:Nested
    val items: Set<FileItem>
        get() = itemSet

    /**
     * Add a single file to the component. This
     * kind of files will be copied as they are.
     *
     * @param file a real file on the file system
     * @param types set of deployment or environment types
     */
    @Throws(InvalidUserDataException::class)
    @Suppress("unused")
    fun add(file: File, vararg types: String): FileItem {

        val item = getPreconfigureItem(file)
        item.setTypes(types.asList())
        addItemToList(item)

        return item
    }

    /**
     * Add a dependency from any possible definition a to the configuration.
     *
     * @param file a real file on the file system
     */
    @Throws(InvalidUserDataException::class)
    fun add(file: File) : FileItem {
        if(types.isEmpty() && parent.types.isNotEmpty()) {
            return add(file, *parent.types.toTypedArray())
        }
        return add(file, *this.types.toTypedArray())
    }

    /**
     * Add a dependency from any possible definition a to the configuration.
     *
     * @param file a real file on the file system.
     * @param action action to configure all parameters of file container item.
     */
    @Throws(InvalidUserDataException::class)
    @Suppress("unused")
    fun add(file: File, action: Action<in FileItem>) {

        val item = getPreconfigureItem(file)
        action.execute(item)
        addItemToList(item)
    }

    /*
     * Creates a preconfigured file item. Configuration is
     * taken from container configuration.
     */
    private fun getPreconfigureItem(file: File) : FileItem {
        val item = FileItem(file)
        item.excludedFromUpdate = excludedFromUpdate

        return item
    }

    /*
     * Add item to list if the name or the special configuration does not exists in the list.
     */
    private fun addItemToList(item: FileItem) {
        addTypes(item)

        if(itemSet.find { it.name == item.name &&
                        it.extension == item.extension &&
                        it.classifier == item.classifier} != null) {
            throw InvalidUserDataException("File ${item.file.nameWithoutExtension}.${item.file.extension} " +
                    "is already part of the current configuration!")
        } else {
            itemSet.add(item)
        }
    }

    /**
     * If an item should not be part of an update installation, this property is set to true.
     *
     * @property excludedFromUpdate If this value is true, the item will be not part of an update installation.
     */
    var excludedFromUpdate: Boolean = false
}
