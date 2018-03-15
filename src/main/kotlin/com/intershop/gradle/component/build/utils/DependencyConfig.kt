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

package com.intershop.gradle.component.build.utils

import org.gradle.api.tasks.Input

/**
 * This class provides a class of a module dependency
 * for the component extension. The default value of
 * all properties is "".
 *
 * @param group     group or organization of a dependency
 * @param module    name or module of a dependency
 * @param version   version of the dependency
 * @constructor provides an empty dependency configuration.
 */
data class DependencyConfig @JvmOverloads constructor(
        @get:Input
        val group: String = "",
        @get:Input
        val module: String = "",
        @get:Input
        val version: String = "",
        @get:Input
        val dependency: String = "") {

    val emptyConfig: Boolean
        get() = group.isEmpty() && module.isEmpty() && version.isEmpty() && dependency.isEmpty()

    val moduleString: String
        get() = "$group:$module:$version"
}
