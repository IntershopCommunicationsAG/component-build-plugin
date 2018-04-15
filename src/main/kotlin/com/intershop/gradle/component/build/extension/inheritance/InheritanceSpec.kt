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
package com.intershop.gradle.component.build.extension.inheritance

import com.intershop.gradle.component.build.utils.getValue
import com.intershop.gradle.component.build.utils.setValue
import org.gradle.api.Named
import org.gradle.api.Project

/**
 * This implementation provides an Inheritance specification.
 *
 * @param project provides the current Gradle project.
 * @param specName of the container item
 */
open class InheritanceSpec(project: Project, private val specName: String) : Named {

    // backing properties
    private val dependencyProperty = project.objects.property(Any::class.java)
    private val excludePatternsProperty = project.objects.setProperty(Any::class.java)

    /**
     * The name of the item is returned by this
     * function.
     *
     * @return the name of the container item
     */
    override fun getName(): String {
        return specName
    }

    /**
     * Adds a dependency to the inheritance object.
     *
     * @param dependency object, that defines a dependency
     */
    @Suppress("unused")
    fun from(dependency: Any) {
        dependencyProperty.set(dependency)
    }

    //TODO: needs more specification
    /**
     * Defines a list of exclude expressions
     * for module artifacts of the inheritance module.
     *
     * @property excludePatterns pattern strings for dependencyExcludes
     */
    @Suppress("unused")
    var excludePatterns by excludePatternsProperty
}
