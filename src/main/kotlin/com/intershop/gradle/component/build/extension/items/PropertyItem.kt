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

import org.gradle.api.tasks.Input

/**
 * This class provides a property object for the component
 * extension of the component build plugin.
 *
 * @property key the property key of the item
 * @constructor initialize an property item without a value.
 */
class PropertyItem(@get:Input val key: String): AItem(), IItem, IOSSpecific {

    /**
     * The value of the item. This is marked as task input.
     * @property value the value representation
     */
    @get:Input
    var value: String = ""

    /**
     * The ANT based pattern for the property file. This is marked as task input.
     * @property pattern the ANT based pattern for the property file.
     */
    @get:Input
    var pattern: String = ""

    /**
     * This set contains OS specific descriptions.
     * The set is empty per default.
     * It is defined as an task input property.
     *
     * @property classifier the set of OS specific strings
     */
    @get:Input
    override var classifier: String = ""


    /**
     * If an item should not be part of an update installation, this property is set to false.
     *
     * @property updatable If this value is false, the item will be not part of an update installation.
     */
    @get:Input
    var updatable: Boolean = true
}
