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

import com.intershop.gradle.component.build.extension.container.FileContainerItemContainer
import com.intershop.gradle.component.build.extension.container.FileItemContainer
import com.intershop.gradle.component.build.extension.container.LibraryItemContainer
import com.intershop.gradle.component.build.extension.container.ModuleItemContainer
import com.intershop.gradle.component.build.extension.container.PropertyItemContainer
import com.intershop.gradle.component.build.extension.inheritance.InheritanceSpec
import com.intershop.gradle.component.build.extension.inheritance.InheritanceSpecFactory
import com.intershop.gradle.component.build.extension.items.DeploymentObject
import com.intershop.gradle.component.build.utils.getValue
import com.intershop.gradle.component.build.utils.setValue
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import javax.inject.Inject

/**
 * This class provides the main extension for the
 * component build plugin.
 *
 * @param project provides the current Gradle project.
 * @constructor Creates a pre configured extension
 */
open class ComponentExtension @Inject constructor(project: Project) : DeploymentObject {

    companion object {
        const val COMPONENT_EXTENSION_NAME = "component"
        const val DEPLOYMENT_GROUP_NAME = "Component Deployment"

        const val DESCRIPTOR_FILE = "component/descriptor/file.component"
    }

    private val displayNameProperty = project.objects.property(String::class.java)
    private val componentDescriptionProperty = project.objects.property(String::class.java)

    private val inheritContainer = project.container(InheritanceSpec::class.java, InheritanceSpecFactory(project))

    private val libContainer =
            project.objects.newInstance(LibraryItemContainer::class.java, project.dependencies, this)

    private val moduleContainer =
            project.objects.newInstance(ModuleItemContainer::class.java, project.dependencies, this)

    private val fileContainer =
            project.objects.newInstance(FileItemContainer::class.java, this)

    private val containerContainer =
            project.objects.newInstance(FileContainerItemContainer::class.java, project, this)

    private val propertyContainer =
            project.objects.newInstance(PropertyItemContainer::class.java, this)


    init {
        displayNameProperty.set(project.name)
        componentDescriptionProperty.set("")
    }

    /**
     * The provider for the display name of the component.
     *
     * @property displayNameProvider (read only) extension provider for display name
     */
    val displayNameProvider: Provider<String>
        get() = displayNameProperty

    /**
     * This attribute defines a component's display name.
     *
     * @property displayName provides the display name of the component
     */
    @Suppress("unused")
    var displayName: String by displayNameProperty


    /**
     * The provider for the component description.
     *
     * @property componentDescriptionProvider (read only) extension provider for component description
     */
    val componentDescriptionProvider: Provider<String>
        get() = componentDescriptionProperty

    /**
     * This attribute defines the description for a component.
     *
     * @property componentDescription provides the description of the component
     */
    @Suppress("unused")
    var componentDescription: String by componentDescriptionProperty

    /**
     * The container for all inherit configurations.
     * The component can inherit configuration from other components.
     *
     * @property inherits container of inherit configurations
     */
    @Suppress("unused")
    val inherits: NamedDomainObjectContainer<InheritanceSpec>
        get() = inheritContainer

    /**
     * Add an inherit configuration to the component.
     *
     * @param name of the inherit component
     * @param inheritFrom inherit specification of the component
     */
    @Suppress("unused")
    fun inherit(name: String, inheritFrom: Action<in InheritanceSpec>) {
        inheritFrom.execute(inheritContainer.maybeCreate(name))
    }

    /**
     * The container for all library configurations.
     *
     * @property libs container of library configurations
     */
    val libs: LibraryItemContainer
        get() = libContainer

    /**
     * Configures lib container of a component.
     *
     * @param action execute the lib container configuration
     */
    fun libs(action: Action<in LibraryItemContainer>) {
        action.execute(libContainer)
    }

    /**
     * The container for all modules configurations.
     *
     * @property modules container of module configurations
     */
    val modules: ModuleItemContainer
        get() = moduleContainer

    /**
     * Configures module container of a component.
     *
     * @param action execute the module container configuration
     */
    fun modules(action: Action<in ModuleItemContainer>) {
        action.execute(moduleContainer)
    }

    /**
     * The container for all single file configurations.
     * These files will be installed as they are
     *
     * @property fileItems container of file configurations
     */
    @Suppress("unused")
    val fileItems: FileItemContainer
        get() = fileContainer

    /**
     * Configures file container of a component.
     *
     * @param action execute the file container configuration
     */
    fun fileItems(action: Action<in FileItemContainer>) {
        action.execute(fileContainer)
    }

    /**
     * The container for all single file configurations.
     * These files will be installed as they are
     *
     * @property containers container of file container (zip packages) configurations
     */
    val containers: FileContainerItemContainer
        get() = containerContainer

    /**
     * Configures zip packages of a component.
     *
     * @param action execute the file container container configuration
     */
    fun containers(action: Action<in FileContainerItemContainer>) {
        action.execute(containerContainer)
    }

    /**
     * The container for all single file configurations.
     * These files will be installed as they are
     *
     * @property containers container of file container (zip packages) configurations
     */
    @Suppress("unused")
    val propertyItems: PropertyItemContainer
        get() = propertyContainer

    /**
     * Configures zip packages of a component.
     *
     * @param action execute the file container container configuration
     */
    @Suppress("unused")
    fun propertyItems(action: Action<in PropertyItemContainer>) {
        action.execute(propertyContainer)
    }


    override fun getInstallPath(): String {
        return ""
    }

    override val parentItem = this

    override val types: Set<String> = mutableSetOf()
}
