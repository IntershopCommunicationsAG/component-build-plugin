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
package com.intershop.gradle.component.build.extension.items

import org.gradle.api.InvalidUserDataException
import org.gradle.api.tasks.Input
import kotlin.properties.Delegates

/**
 * This class provides the basic properties of any component item.
 */
abstract class AItem: IItem {

    private val typeList: MutableSet<String> = mutableSetOf()

    /**
     * This property contains the content type of the item.
     * The following values are allowed:
     *  - IMMUTABLE/STATIC
     *  - DATA
     *  - CONFIGURATION
     *  - UNSPECIFIED
     */
    @get:Input
    override var contentType by Delegates.vetoable(ContentType.IMMUTABLE.name) { _, _, newValue ->
        try {
            ContentType.values().map { it.name }.contains(newValue)
        } catch (ex: IllegalArgumentException) {
            throw InvalidUserDataException("Content type must be 'IMMUTABLE', 'DATA', " +
                    "'CONFIGURATION', but it is $newValue", ex)
        }
    }

    /**
     * This set contains deployment or environment type
     * definitions, like 'production', 'test' etc. The set
     * can be extended.
     * The set is empty per default.
     * It is defined as an task input property.
     *
     * @property types the set of deployment or environment types
     */
    @get:Input
    override val types: Set<String>
        get() = typeList

    /**
     * Adds a new deployment or environment type. The characters will
     * be changed to lower cases.
     *
     * @param type a deployment or environment type
     * @return if the environment type is available, false will be returned.
     */
    fun addType(type: String): Boolean {
        return typeList.add(type)
    }

    /**
     * Reset the set with new values from input.
     *
     * @param types a new list of types
     */
    fun setTypes(types: Collection<String>) {
        typeList.clear()
        typeList.addAll(types)
    }

    /**
     * Adds a list of new deployment or environment types. The
     * characters will be changed to lower cases.
     *
     * @param types a list of deployment or environment types
     * @return if one environment type of the list is available, false will be returned.
     */
    fun addTypes(types: Collection<String>): Boolean {
        return typeList.addAll(types.map { it.toLowerCase() })
    }
}
