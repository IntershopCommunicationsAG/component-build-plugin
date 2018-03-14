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

import com.intershop.gradle.component.build.extension.items.ATypeItem
import com.intershop.gradle.component.build.extension.items.IDeployment
import com.intershop.gradle.component.build.extension.items.PropertyItem
import org.gradle.api.InvalidUserDataException
import javax.inject.Inject

/**
 * This class provides a container for properties, that will be transferred for an deployment.
 *
 * @property parentItem the parent of this container.
 * @constructor provides the property container
 */
open class PropertyItemContainer
        @Inject constructor(override val parentItem: IDeployment) :
        ATypeItem(parentItem) {

    // backing properties
    private val itemSet: MutableSet<PropertyItem> = mutableSetOf()

    /**
     * This set provides all configured properties.
     *
     * @property items set of all configured modules
     */
    val items: Set<PropertyItem>
        get() = itemSet

    /**
     * Add property key value pair to this component
     * definition for specified deployment types.
     *
     * @param key Property key of this item
     * @param value Property value of this item
     * @param types set of deployment or environment types
     */
    @Suppress("unused")
    @Throws(InvalidUserDataException::class)
    fun add(key: String, value: String, vararg types: String): PropertyItem {
        val item = PropertyItem(key, this)
        item.setTypes(types.asList())

        item.value = value

        if(itemSet.contains(item)) {
            throw InvalidUserDataException("Property $key is already part of the current configuration!")
        } else {
            itemSet.add(item)
        }

        return item
    }

    /**
     * Add property key value pair to this component
     * definition.
     *
     * @param key Property key of this item
     * @param value Property value of this item
     */
    @Throws(InvalidUserDataException::class)
    fun add(key: String, value: String) : PropertyItem {
        if(types.isEmpty() && parentItem.types.isNotEmpty()) {
            return add(key,value, *parentItem.types.toTypedArray())
        }
        return add(key,value, *this.types.toTypedArray())
    }

}
