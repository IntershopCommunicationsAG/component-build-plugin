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
import org.gradle.api.tasks.InputFile
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.properties.Delegates

/**
 * This class provides a file object configuration
 * of the component.
 *
 * @property file the real file item of the component. This is marked as task input.
 * @constructor provides a configured file item
 */
class FileItem(@get:InputFile val file: File) : AItem(), IItem, IOSSpecific {

    companion object {
        private val logger = LoggerFactory.getLogger(FileItem::class.java.simpleName)
    }
    /**
     * The file name of the item. This is marked as task input.
     *
     * @property name default value is the real file name.
     */
    @get:Input
    var name = file.nameWithoutExtension

    /**
     * The file extension of the item. This is marked as task input.
     *
     * @property name default value is the real file extension.
     */
    @get:Input
    var extension = file.extension

    /**
     * The target path of this file. This is marked as task input.
     *
     * @property name default value is an empty string.
     */
    @get:Input
    var targetPath by Delegates.vetoable("") { _, _, newValue ->
        val invalidChars = Utils.getIllegalChars(newValue)
        if(!invalidChars.isEmpty()) {
            throw InvalidUserDataException("Target path of file '$name' " +
                    "contains invalid characters '$invalidChars'.")
        }
        if(newValue.startsWith("/")) {
            throw InvalidUserDataException("Target path of file '$name' " +
                    "starts with a leading '/' - only a relative path is allowed.")
        }
        if(newValue.length > (Utils.MAX_PATH_LENGTH / 2)) {
            logger.warn("The target path of file '$name' is longer then ${(Utils.MAX_PATH_LENGTH / 2)}!")
        }
        invalidChars.isEmpty() && ! newValue.startsWith("/")
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
     * If an item should not be part of an update installation, this property is set to true.
     *
     * @property excludeFromUpdate If this value is true, the item will be not part of an update installation.
     */
    var excludeFromUpdate: Boolean = false
}
