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
import org.gradle.api.tasks.Internal

/**
 * This class provides a class of a module dependency
 * for the component extension. The default value of
 * all properties is "".
 *
 * @param group     group or organization of a dependency
 * @param module    name or module of a dependency
 * @param version   version of the dependency
 * @param dependency string represenation if dependency is not module dependency
 * @param transitive if dependency should or was transitive resolved
 *
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
        val dependency: String = "",
        @get:Input
        val transitive: Boolean = false) {

    /**
     * Returns true if the dependency information is empty.
     *
     * @return true if the dependency information is empty
     */
    @Internal
    fun isEmptyConfig(): Boolean {
        return (group.isEmpty() && module.isEmpty() && version.isEmpty() && dependency.isEmpty())
    }

    /**
     * Returns the module string of the dependency.
     *
     * @return the module string
     */
    @Internal
    fun getModuleString(): String {
        return "$group:$module:$version"
    }

    /**
     * Returns a map of properties for the Gradle
     * exclude configuration.
     *
     * @return a property map
     */
    @Internal
    fun getExcludeProperties(): Map<String, String> {
        val excludeMap = mutableMapOf<String,String>()
        if(group.isNotBlank()) {
            excludeMap["group"] = group
        }
        if(module.isNotBlank()) {
            excludeMap["module"] = module
        }
        return excludeMap
    }
}
