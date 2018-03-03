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
abstract class AbstractItem(override val parentItem: DeploymentObject) :
        AbstractTypeItem(parentItem), ComponentObject, DeploymentObject, OSSpecificObject {

    private val classifiersList: MutableSet<String> = mutableSetOf()

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
     * This set contains OS specific descriptions.
     * The set is empty per default.
     * It is defined as an task input property.
     *
     * @property classifiers the set of OS specific strings
     */
    @get:Input
    override val classifiers: Set<String>
        get() = classifiersList


    /**
     * Adds a new classifier string. The characters will
     * be changed to lower cases.
     *
     * @param classifier a classifier string like win, linux, macos
     * @return if the classifier string is available, false will be returned.
     */
    fun classifier(classifier: String): Boolean {
        return classifiersList.add(classifier.toLowerCase())
    }

    /**
     * Adds a list of classifier strings. The
     * characters will be changed to lower cases.
     *
     * @param classifiers a list of classifier strings like win, linux, macos
     * @return if one classifier string of the list is available, false will be returned.
     */
    fun classifiers(classifiers: Collection<String>): Boolean {
        return classifiersList.addAll(classifiers.map { it.toLowerCase() })
    }
}
