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
package com.intershop.gradle.component.build.extension

import com.intershop.gradle.component.build.utils.DependencyConfig
import org.gradle.api.InvalidUserDataException
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.dsl.DependencyHandler

/**
 * Util class with static methods to support
 * the creation of a DSL.
 */
class Utils {

    companion object {
        val ILLEGAL_CHARACTERS = arrayListOf(' ', '\n', '\r', '\t', '`', '?', '*', '\\', '<', '>', '|', '\"', ':')
        val MAX_PATH_LENGTH = 4096

        /**
         * Calculates illegal characters for path configuration.
         *
         * @param path relative path configuration
         */
        @JvmStatic
        fun getIllegalChars(path: String) = path.filter { ILLEGAL_CHARACTERS.contains(it) }

        /**
         * Calculate the string of a dependency configuration with
         * a special separator configuration.
         *
         * @param dep dependency configuration in a package
         * @param sep separator for the new string
         */
        @JvmStatic
        fun getDependencyString(dep: DependencyConfig, sep: String ): String {
            val depStr = StringBuilder()
            if(!dep.group.isEmpty()) {
                depStr.append(dep.group).append(sep)
            }
            depStr.append(dep.module)
            if(!dep.version.isEmpty()) {
                depStr.append(sep).append(dep.version)
            }
            return depStr.toString()
        }

        /**
         * Calculates a dependency configuration from any object with Gradle dependency helper.
         *
         * @param handler project dependency handler.
         * @param dependency object that represents dependencies.
         * @param errormessage additional message for error output.
         */
        @JvmStatic
        @Throws(InvalidUserDataException::class)
        fun getDependencyConf(handler: DependencyHandler, dependency: Any, errormessage: String): DependencyConfig {
            val dep = handler.create(dependency)

            val depStr = if(dep is ProjectDependency) dependency.toString() else ""
            val depConf = DependencyConfig(dep.group
                    ?: "", dep.name, dep.version ?: "", depStr)

            if(depConf.emptyConfig) {
                throw InvalidUserDataException("Dependency '$dependency' is empty. $errormessage")
            }

            return depConf
        }
    }
}
