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

import com.intershop.gradle.component.build.extension.Utils
import com.intershop.gradle.component.build.extension.items.AbstractTypeItem
import com.intershop.gradle.component.build.extension.items.DeploymentObject
import com.intershop.gradle.component.build.extension.items.FileItem
import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.slf4j.LoggerFactory
import java.io.File
import javax.inject.Inject
import kotlin.properties.Delegates

/**
 * This class provides a container for
 * deployable single files.
 *
 * @property parentItem the parent of this container.
 * @constructor provides an empty preconfigured file item container
 */
open class FileItemContainer
        @Inject constructor(override val parentItem: DeploymentObject) :
        AbstractTypeItem(parentItem) {

    companion object {
        private val logger = LoggerFactory.getLogger(FileItemContainer::class.java.simpleName)
    }

    private val itemSet: MutableSet<FileItem> = mutableSetOf()

    /**
     * This path describes the installation in the default
     * installation of the component.
     *
     * @property targetPath contains the default installation path
     */
    var targetPath: String by Delegates.vetoable("") { _, _, newValue ->
        val invalidChars = Utils.getIllegalChars(newValue)
        if(!invalidChars.isEmpty()) {
            throw InvalidUserDataException("Target path of file item container contains " +
                    "invalid characters '$invalidChars'.")
        }
        if(newValue.startsWith("/")) {
            throw InvalidUserDataException("Target path of file item container starts " +
                    "with a leading '/' - only a relative path is allowed.")
        }
        if(newValue.length > (Utils.MAX_PATH_LENGTH / 2)) {
            logger.warn("Target path of file item container is longer then ${(Utils.MAX_PATH_LENGTH / 2)}!")
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

        return installPath.toString()
    }

    /**
     * This set provides all configured files.
     *
     * @property items set of all configured files
     */
    val items: Set<FileItem>
        get() = itemSet

    /**
     * Add a single file to the component. This
     * kind of files will be copied as they are.
     *
     * @param file a real file on the file system
     * @param types set of deployment or environment types
     */
    @Throws(InvalidUserDataException::class)
    @Suppress("unused")
    fun add(file: File, vararg types: String): FileItem {
        val item = FileItem(file, this)
        item.types(types.asList())

        if(itemSet.contains(item)) {
            throw InvalidUserDataException("File ${file.nameWithoutExtension}.${file.extension} " +
                    "is already part of the current configuration!")
        } else {
            itemSet.add(item)
        }

        return item
    }

    /**
     * Add a dependency from any possible definition a to the configuration.
     *
     * @param file a real file on the file system
     */
    @Throws(InvalidUserDataException::class)
    fun add(file: File) : FileItem {
        return add(file, *this.types.toTypedArray())
    }

    /**
     * Add a dependency from any possible definition a to the configuration.
     *
     * @param file a real file on the file system.
     * @param action action to configure all parameters of file container item.
     */
    @Throws(InvalidUserDataException::class)
    @Suppress("unused")
    fun add(file: File, action: Action<in FileItem>) {
        val item = FileItem(file, this)

        action.execute(item)

        if(itemSet.contains(item)) {
            throw InvalidUserDataException("File ${file.nameWithoutExtension}.${file.extension} " +
                    "is already part of the current configuration!")
        } else {
            itemSet.add(item)
        }
    }
}
