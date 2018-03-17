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
import com.intershop.gradle.component.build.utils.DependencyConfig
import org.gradle.api.InvalidUserDataException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.internal.impldep.org.bouncycastle.asn1.x500.style.RFC4519Style.name
import org.slf4j.LoggerFactory
import kotlin.properties.Delegates

/**
 * This class provides a module object for the component
 * extension of the component build plugin.
 *
 * @property dependency a dependency configuration of this module
 * @constructor initialize an module with a defined dependency
 */
class ModuleItem(@get:Nested override val dependency: DependencyConfig) :
        ADependencyItem(), IItem, IContainer, IDependency {

    companion object {
        private val logger = LoggerFactory.getLogger(ModuleItem::class.java.simpleName)
    }

    /**
     * The default target path of the component.
     * This is sub path in the component.
     * Default value is an empty string.
     *
     * @property targetPath target path
     */
    override var targetPath by Delegates.vetoable("") { _, _, newValue ->
        val invalidChars = Utils.getIllegalChars(newValue)
        if(!invalidChars.isEmpty()) {
            throw InvalidUserDataException("Target path of module '$name' " +
                    "contains invalid characters '$invalidChars'.")
        }
        if(newValue.startsWith("/")) {
            throw InvalidUserDataException("Target path of module '$name' " +
                    "starts with a leading '/' - only a relative path is allowed.")
        }
        if(newValue.length > (Utils.MAX_PATH_LENGTH / 2)) {
            logger.warn("The target path of module '$name' is longer then ${(Utils.MAX_PATH_LENGTH / 2)}!")
        }
        invalidChars.isEmpty() && ! newValue.startsWith("/")
    }

    /**
     * Is the target path included in the component?
     * This is an important information for the
     * deployment of the component.
     * Default value is false - target is not included in the module.
     *
     * @property targetIncluded if true, the target path is included in the module fileContainers.
     */
    override var targetIncluded:Boolean = false

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
