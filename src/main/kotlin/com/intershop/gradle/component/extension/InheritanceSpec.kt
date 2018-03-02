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
package com.intershop.gradle.component.extension

import com.intershop.gradle.component.getValue
import com.intershop.gradle.component.setValue
import org.gradle.api.Named
import org.gradle.api.Project

open class InheritanceSpec(project: Project, private val specName: String) : Named {

    private val dependencyProperty = project.objects.property(Any::class.java)
    private val includePatternsProperty = project.objects.setProperty(Any::class.java)
    private val excludePatternsProperty = project.objects.setProperty(Any::class.java)

    override fun getName(): String {
        return specName
    }

    fun from(dependency: Any) {
        dependencyProperty.set(dependency)
    }

    var includePatterns by includePatternsProperty

    var excludePatterns by excludePatternsProperty
}