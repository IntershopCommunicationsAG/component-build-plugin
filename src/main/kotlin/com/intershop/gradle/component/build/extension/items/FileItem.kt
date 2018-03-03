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
import org.gradle.api.tasks.Internal
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
class FileItem(@get:InputFile val file: File,
               @get:Internal override val parentItem: DeploymentObject) :
        AbstractItem(parentItem), ComponentObject, DeploymentObject, OSSpecificObject {

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
     * The complete install target of this item.
     *
     * @return a string representation of the item.
     */
    override fun getInstallPath(): String {
        val installPath = StringBuilder(parentItem.getInstallPath())

        if(! targetPath.isEmpty()) {
            if(! installPath.endsWith("/")) {
                installPath.append("/")
            }
            installPath.append(targetPath)
        }
        installPath.append(name).append('.').append(extension)

        return installPath.toString()
    }
}
