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

import com.intershop.gradle.component.build.extension.ComponentExtension
import com.intershop.gradle.component.build.extension.Utils
import com.intershop.gradle.component.build.extension.items.AItem
import org.gradle.api.InvalidUserDataException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.slf4j.LoggerFactory
import javax.inject.Inject
import kotlin.properties.Delegates

/**
 * This class contains functions and properties for all containers
 * of the component extension.
 *
 * @property description a short description of this container for log messages.
 * @constructor provides an empty container
 */
abstract class AContainer @Inject constructor(@get:Internal protected val description: String,
                                              @get:Internal protected open val parent: ComponentExtension) : AItem() {

    companion object {
        private val logger = LoggerFactory.getLogger(AContainer::class.java.simpleName)
    }

    /**
     * This path describes the installation in the default
     * installation of the component.
     *
     * @property targetPath contains the default installation path
     */
    @get:Input
    var targetPath: String by Delegates.vetoable("") { _, _, newValue ->
        val invalidChars = Utils.getIllegalChars(newValue)
        if(!invalidChars.isEmpty()) {
            throw InvalidUserDataException("Target path of file item container contains " +
                    "invalid characters '$invalidChars'.")
        }
        if(newValue.startsWith("/")) {
            throw InvalidUserDataException("Target path of container '$description' starts " +
                    "with a leading '/' - only a relative path is allowed.")
        }
        if(newValue.length > (Utils.MAX_PATH_LENGTH / 2)) {
            logger.warn("Target path of container '$description' is longer then ${(Utils.MAX_PATH_LENGTH / 2)}!")
        }
        invalidChars.isEmpty() && ! newValue.startsWith("/")
    }

    protected fun addTypes(item: AItem) {
        if(types.isEmpty() && parent.types.isNotEmpty()) {
            item.addTypes(parent.types)
        } else {
            item.addTypes(this.types)
        }
    }
}
