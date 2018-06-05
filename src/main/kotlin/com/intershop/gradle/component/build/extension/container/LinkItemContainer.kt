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
import com.intershop.gradle.component.build.extension.items.LinkItem
import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import javax.inject.Inject

/**
 * This class provides a container for
 * deployable link configurations.
 *
 * @property parent the parent of this container.
 * @constructor provides an empty preconfigured link item container
 */
open class LinkItemContainer @Inject constructor(@get: Internal val parent: ComponentExtension) : AItem() {

    private val itemSet: MutableSet<LinkItem> = mutableSetOf()

    /**
     * This set provides all configured links.
     *
     * @property items set of all configured links
     */
    @get:Nested
    val items: Set<LinkItem>
        get() = itemSet

    /**
     * Add a single link to the component. This
     * links will be created during the installation.
     *
     * @param name name of the link
     * @param targetPath the target path of the link
     * @param types set of deployment or environment types
     */
    @Throws(InvalidUserDataException::class)
    @Suppress("unused")
    fun add(name: String, targetPath: String, vararg types: String): LinkItem {

        val item = getPreconfigureItem(name, targetPath)
        item.setTypes(types.asList())
        addItemToList(item)

        return item
    }

    /**
     * Add a link configuration to the configuration.
     *
     * @param name name of the link
     * @param targetPath the target path of the link
     */
    @Throws(InvalidUserDataException::class)
    fun add(name: String, targetPath: String) : LinkItem {
        if(types.isEmpty() && parent.types.isNotEmpty()) {
            return add(name, targetPath, *parent.types.toTypedArray())
        }
        return add(name, targetPath, *this.types.toTypedArray())
    }

    /**
     * Add a link configuration to the configuration and configures this.
     *
     * @param name name of the link
     * @param targetPath the target path of the link
     * @param action action to configure all parameters of link item.
     */
    @Throws(InvalidUserDataException::class)
    @Suppress("unused")
    fun add(name: String, targetPath: String, action: Action<in LinkItem>) {

        val item = getPreconfigureItem(name, targetPath)
        action.execute(item)
        addItemToList(item)
    }

    /*
     * Creates a preconfigured file item. Configuration is
     * taken from container configuration.
     */
    @Throws(InvalidUserDataException::class)
    private fun getPreconfigureItem(name: String, targetPath: String) : LinkItem {
        val item = LinkItem(name, targetPath)
        item.updatable = updatable

        return item
    }

    /*
     * Add item to list if the name or the special configuration does not exists in the list.
     */
    @Throws(InvalidUserDataException::class)
    private fun addItemToList(item: LinkItem) {
        addTypes(item)

        if(itemSet.any { it.name == item.name && it.classifiers.intersect(item.classifiers).isNotEmpty() }) {
            throw InvalidUserDataException("Link with ${item.name} is already part of the current configuration!")
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
