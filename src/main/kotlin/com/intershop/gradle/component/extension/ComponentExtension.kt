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
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import javax.inject.Inject

open class ComponentExtension @Inject constructor(project: Project) {

    companion object {
        const val COMPONENT_EXTENSION_NAME = "component"
        const val DEPLOYMENT_GROUP_NAME = "Deployment"

        const val DESCRIPTOR_FILE = "component/descriptor/file.component"
    }

    private val displayNameProperty = project.objects.property(String::class.java)
    private val componentDescriptionProperty = project.objects.property(String::class.java)

    private val inheritContainer = project.container(InheritanceSpec::class.java, InheritanceSpecFactory(project))

    private val moduleDependenciesProperty = project.objects.setProperty(Any::class.java)
    private val libraryDependenciesProperty = project.objects.setProperty(Any::class.java)

    init {
        displayNameProperty.set(project.name)
        componentDescriptionProperty.set("")
    }

    /**
     * This attribute defines a component's display name.
     *
     * @return provides the display name
     */
    val displayNameProvider: Provider<String>
        get() = displayNameProperty

    var displayName: String by displayNameProperty

    /**
     * This attribute defines the description for a component.
     *
     * @return provides the description
     */
    val componentDescriptionProvider: Provider<String>
        get() = componentDescriptionProperty

    var componentDescription: String by componentDescriptionProperty

    // ineritance configuration
    val inherits: NamedDomainObjectContainer<InheritanceSpec>
        get() = inheritContainer

    fun inherit(name: String, inheritFrom: Action<in InheritanceSpec>) {
        inheritFrom.execute(inheritContainer.maybeCreate(name))
    }

    // cartridge dependencies
    val moduleDependenciesProvider: Provider<Set<Any>>
        get() = moduleDependenciesProperty

    var moduleDependencies by moduleDependenciesProperty

    fun module(moduleItem: Any) {
        moduleDependenciesProperty.add(moduleItem)
    }

    // cartridge dependencies
    val libraryDependenciesProvider: Provider<Set<Any>>
        get() = libraryDependenciesProperty

    var libraryDependencies by libraryDependenciesProperty

    fun lib(library: Any) {
        libraryDependenciesProperty.add(library)
    }
}