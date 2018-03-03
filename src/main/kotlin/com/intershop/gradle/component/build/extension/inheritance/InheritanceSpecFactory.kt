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

import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Project

/**
 * This class provide a factory for InheritancSpec items
 * of the inherit container.
 *
 * @param project the current project
 * @constructor provides a item factory for a named container.
 */
class InheritanceSpecFactory(private val project: Project) : NamedDomainObjectFactory<InheritanceSpec> {

    /**
     * Creates an empty InheritanceSpec with a name.
     *
     * @param name name of the new item
     */
    override fun create(name: String): InheritanceSpec {
        return InheritanceSpec(project, name)
    }
}
