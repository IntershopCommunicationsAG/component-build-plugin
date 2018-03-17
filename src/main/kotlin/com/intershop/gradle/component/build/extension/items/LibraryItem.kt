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
import org.gradle.api.tasks.Nested
import kotlin.properties.Delegates

/**
 * This class provides a library object for the component
 * extension of the component build plugin.
 *
 * @property dependency a dependency object of tis library
 * @constructor initialize a library based on the dependency
 */
class LibraryItem(@get:Nested override val dependency: DependencyConfig) :
        ADependencyItem(), IItem, IDependency {

    /**
     * This will be configured for the deployment
     * in a component directory.
     *
     * @property targetName file name of the deployed jar file.
     */
    var targetName by Delegates.vetoable("${Utils.getDependencyString(dependency, "-")}.jar")
        { _, _, newValue ->
            if(newValue.startsWith("/")) {
                throw InvalidUserDataException("Target name of file '$dependency' starts " +
                        "with a leading '/' - only a relative path is allowed.")
            }
            ! newValue.startsWith("/")
        }

}
