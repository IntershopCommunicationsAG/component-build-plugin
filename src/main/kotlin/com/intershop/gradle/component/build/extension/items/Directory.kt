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
 * This class represents a directory configuration of a component.
 *
 * @property targetPath the target path of the directory
 *
 * @constructor initializes a configuration from target name and target path.
 */

class Directory constructor(
        @get:Input val targetPath: String) : AItem(), IItem, IOSSpecific {

    companion object {
        private val logger = LoggerFactory.getLogger(Directory::class.java.simpleName)
    }

    private var internalTargetPath by Delegates.vetoable("") { _, _, newValue ->
        val invalidChars = Utils.getIllegalChars(newValue)
        if(!invalidChars.isEmpty()) {
            throw InvalidUserDataException("Target path of directory '${targetPath}'" +
                    "contains invalid characters '$invalidChars'.")
        }
        if(newValue.startsWith("/")) {
            throw InvalidUserDataException("Target path of directory '${targetPath}'" +
                    "starts with a leading '/' - only a relative path is allowed.")
        }
        if(newValue.length > (Utils.MAX_PATH_LENGTH / 2)) {
            Directory.logger.warn("The target path of directory '${targetPath}' "
                    + "is longer then ${(Utils.MAX_PATH_LENGTH / 2)}!")
        }
        invalidChars.isEmpty() && ! newValue.startsWith("/")
    }

    init {
        internalTargetPath = targetPath
    }

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
