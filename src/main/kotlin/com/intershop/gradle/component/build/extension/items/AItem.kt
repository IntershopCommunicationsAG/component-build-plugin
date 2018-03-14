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
abstract class AItem(override val parentItem: IDeployment) :
        ATypeItem(parentItem), IComponent, IDeployment {
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
}
