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

import com.intershop.gradle.component.build.extension.Utils
import org.gradle.api.InvalidUserDataException
import org.gradle.api.tasks.Input
import org.slf4j.LoggerFactory
import kotlin.properties.Delegates

/**
 * This class represents a link configuration of a component.
 *
 * @property name the name of the link
 * @property targetPath the target path of the link
 *
 * @constructor initializes a configuration from target name and target path.
 */
class LinkItem constructor(
        @get:Input val name: String,
        @get:Input val targetPath: String) : AItem(), IItem {

    companion object {
        private val logger = LoggerFactory.getLogger(LinkItem::class.java.simpleName)
    }

    private var internalTargetPath by Delegates.vetoable("") { _, _, newValue ->
        val invalidChars = Utils.getIllegalChars(newValue)
        if(!invalidChars.isEmpty()) {
            throw InvalidUserDataException("Target path of link '$name' " +
                    "contains invalid characters '$invalidChars'.")
        }
        if(newValue.startsWith("/")) {
            throw InvalidUserDataException("Target path of link '$name' " +
                    "starts with a leading '/' - only a relative path is allowed.")
        }
        if(newValue.length > (Utils.MAX_PATH_LENGTH / 2)) {
            LinkItem.logger.warn("The target path of file '$name' is longer then ${(Utils.MAX_PATH_LENGTH / 2)}!")
        }
        invalidChars.isEmpty() && ! newValue.startsWith("/")
    }

    private var internalName by Delegates.vetoable("") { _, _, newValue ->
        val invalidChars = Utils.getIllegalChars(newValue)
        if(!invalidChars.isEmpty()) {
            throw InvalidUserDataException("Link name '$name' contains invalid characters '$invalidChars'.")
        }
        if(newValue.startsWith("/")) {
            throw InvalidUserDataException("Link name '$name' starts with a leading '/' " +
                    " - only a relative path is allowed.")
        }
        if(newValue.length > (Utils.MAX_PATH_LENGTH / 2)) {
            LinkItem.logger.warn("Link name '$name' is longer then ${(Utils.MAX_PATH_LENGTH / 2)}!")
        }
        invalidChars.isEmpty() && ! newValue.startsWith("/")
    }

    init {
        internalTargetPath = targetPath
        internalName = name

        if(internalName == internalTargetPath) {
            throw InvalidUserDataException("Link name '$name' and taret '$targetPath' are identical.")
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
    var classifiers: Set<String> = mutableSetOf()

    /**
     * If an item should not be part of an update installation, this property is set to false.
     *
     * @property updatable If this value is false, the item will be not part of an update installation.
     */
    @get:Input
    var updatable: Boolean = true
}
