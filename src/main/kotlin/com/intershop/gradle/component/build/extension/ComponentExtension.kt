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

import com.intershop.gradle.component.build.extension.container.DependencyMngtContainer
import com.intershop.gradle.component.build.extension.container.DirectoryContainer
import com.intershop.gradle.component.build.extension.container.FileContainerItemContainer
import com.intershop.gradle.component.build.extension.container.FileItemContainer
import com.intershop.gradle.component.build.extension.container.LibraryItemContainer
import com.intershop.gradle.component.build.extension.container.LinkItemContainer
import com.intershop.gradle.component.build.extension.container.ModuleItemContainer
import com.intershop.gradle.component.build.extension.container.PropertyItemContainer
import com.intershop.gradle.component.build.extension.inheritance.InheritanceSpec
import com.intershop.gradle.component.build.extension.inheritance.InheritanceSpecFactory
import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet
import java.io.File
import javax.inject.Inject
import kotlin.properties.Delegates

/**
 * This class provides the main extension for the
 * component build plugin.
 *
 * @param project provides the current Gradle project instance.
 * @constructor Creates a pre configured extension
 */
open class ComponentExtension @Inject constructor(project: Project) {

    companion object {

        private const val PLUGIN_OUTPUTDIR = "componentBuild"

        /**
         * default path for descriptor file.
         */
        const val DESCRIPTOR_FILE = "$PLUGIN_OUTPUTDIR/descriptor/file.component"
        /**
         * default path for report file.
         */
        const val CLASSCOLLISION_REPORT = "$PLUGIN_OUTPUTDIR/reports/classcollision/collisionReport.txt"

        internal const val CONTAINER_OUTPUTDIR = "$PLUGIN_OUTPUTDIR/container"

        /**
         * Default name for ivy publication.
         */
        const val DEFAULT_IVYPUBLICATION = "ivyIntershop"
        /**
         * Default name for maven publication.
         */
        const val DEFAULT_MAVENPUBLICATION = "mvnIntershop"
    }

    private val inheritContainer = project.container(InheritanceSpec::class.java, InheritanceSpecFactory(project))

    private val typeList: MutableSet<String> = mutableSetOf()

    // set of central deployment exclude patterns
    private val excludesSetProperty: SetProperty<String> = project.objects.setProperty(String::class.java)

    // set of central deployment preserve patterns
    private val preserveProperty: PatternSet  = project.objects.newInstance(PatternSet::class.java)

    private val libContainer =
            project.objects.newInstance(LibraryItemContainer::class.java, project.dependencies, this)

    private val moduleContainer =
            project.objects.newInstance(ModuleItemContainer::class.java, project.dependencies, project, this)

    private val dependencyMngtContainer =
            project.objects.newInstance(DependencyMngtContainer::class.java, project)

    private val fileContainer =
            project.objects.newInstance(FileItemContainer::class.java, this)

    private val containerContainer =
            project.objects.newInstance(FileContainerItemContainer::class.java, project, this)

    private val linkContainer =
            project.objects.newInstance(LinkItemContainer::class.java, this)

    private val directoryContainer =
            project.objects.newInstance(DirectoryContainer::class.java, this)

    private val propertyContainer =
            project.objects.newInstance(PropertyItemContainer::class.java, this)

    /**
     * This is used for the publishing configuration of this plugin.
     *
     * @property ivyPublicationName name is used for ivy publishing.
     */
    var ivyPublicationName: String = DEFAULT_IVYPUBLICATION

    /**
     * This is used for the publishing configuration of this plugin.
     *
     * @property mavenPublicationName name is used for maven publishing.
     */
    var mavenPublicationName: String = DEFAULT_MAVENPUBLICATION

    /**
     * This attribute defines a component's display name.
     *
     * @property displayName provides the display name of the component
     */
    @Suppress("unused")
    var displayName: String = project.name

    /**
     * This attribute defines the description for a component.
     *
     * @property componentDescription provides the description of the component
     */
    @Suppress("unused")
    var componentDescription: String = ""

    /**
     * This attribute defines a predefined install target.
     * The default value is an empty string.
     *
     * @property targetPath a relative path
     */
    @Suppress("unused")
    var targetPath: String by Delegates.vetoable("") { _, _, newValue ->
        val invalidChars = Utils.getIllegalChars(newValue)
        if(!invalidChars.isEmpty()) {
            throw InvalidUserDataException("Target path of component '$displayName'" +
                    "contains invalid characters '$invalidChars'.")
        }
        if(newValue.startsWith("/")) {
            throw InvalidUserDataException("Target path of compoent '$displayName'" +
                    "starts with a leading '/' - only a relative path is allowed.")
        }
        if(newValue.length > (Utils.MAX_PATH_LENGTH / 2)) {
            project.logger.warn("Target path of container container is longer then ${(Utils.MAX_PATH_LENGTH / 2)}!")
        }
        invalidChars.isEmpty() && ! newValue.startsWith("/")
    }

    /**
     * The install path for the component descriptor file.
     * Default value is 'component'.
     *
     * @property descriptorPath path for all descriptor files of the component
     */

    var descriptorPath: String = "component"

    /**
     * This file configuration is used for the output
     * of the descriptor file.
     *
     * @property decriptorOutputFile descriptor output file
     */
    @Suppress("unused")
    var decriptorOutputFile: File = File(project.buildDir, DESCRIPTOR_FILE)

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
     * Files that matches to one of patterns will be
     * excluded from the update installation.
     *
     * @property excludes Set of Ant based file patterns
     */
    @Suppress("unused")
    val excludes: Set<String>
        get() = excludesSetProperty.get()

    /**
     * This is a provider for dependencyExcludes property.
     *
     * @property excludesProvider Provider for dependencyExcludes
     */
    val excludesProvider: Provider<Set<String>>
        get() = excludesSetProperty

    /**
     * Adds a pattern to the set of exclude patterns.
     * Files that matches to one of patterns will be
     * excluded from the installation.
     *
     * @param pattern Ant based file pattern
     */
    @Suppress("unused")
    fun exclude(pattern: String) {
        excludesSetProperty.add(pattern)
    }

    /**
     * Adds a set of patterns to the set of exclude patterns.
     * Files that matches to one of patterns will be
     * excluded from the installation.
     * If one of the patterns is part of the list, the method
     * returns false.
     *
     * @param patterns set of Ant based file pattern
     */
    @Suppress("unused")
    fun exclude(patterns: Set<String>) {
        patterns.forEach {
            excludesSetProperty.add(it)
        }
    }

    /**
     * This is the configuration for preserved patterns.
     *
     * @property preserve pattern filter set to preserve files
     */
    val preserve: PatternFilterable
        get() = preserveProperty

    /**
     * Configure patternset for perserve files from update.
     * Files that matches to one of patterns will be
     * excluded or included to the update installation.
     *
     * @param action Action for configuring the preserve filter
     */
    @Suppress("unused")
    fun preserve(action: Action<in PatternFilterable>) {
        action.execute(preserveProperty)
    }

    /**
     * This set contains deployment or environment type
     * definitions, like 'production', 'test' etc. The set
     * can be extended.
     * The set is empty per default.
     * It is defined as an task input property.
     *
     * @property types the set of deployment or environment types
     */
     val types: Set<String>
        get() = typeList

    /**
     * Adds a new deployment or environment type. The characters will
     * be changed to lower cases.
     *
     * @param type a deployment or environment type
     * @return if the environment type is available, false will be returned.
     */
    fun addType(type: String): Boolean {
        return typeList.add(type)
    }

    /**
     * Adds a list of new deployment or environment types. The
     * characters will be changed to lower cases.
     *
     * @param types a list of deployment or environment types
     * @return if one environment type of the list is available, false will be returned.
     */
    @Suppress("unused")
    fun addTypes(types: Collection<String>): Boolean {
        return typeList.addAll(types.map { it })
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
     * The container for dependencies configurations.
     *
     * @property dependencyMngt container of dependencies configurations
     */
    val dependencyMngt: DependencyMngtContainer
        get() = dependencyMngtContainer

    /**
     * Configures dependencies container of a component.
     *
     * @param action execute the dependencies container configuration
     */
    @Suppress("unused")
    fun dependencyMngt(action: Action<in DependencyMngtContainer>) {
        action.execute(dependencyMngtContainer)
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
    @Suppress("unused")
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
    @Suppress("unused")
    fun containers(action: Action<in FileContainerItemContainer>) {
        action.execute(containerContainer)
    }

    /**
     * The container for all single link configurations.
     * These links will be created as they are.
     *
     * @property links container of link configurations
     */
    val links: LinkItemContainer
        get() = linkContainer

    /**
     * Configures links of a component.
     *
     * @param action execute the link container configuration
     */
    @Suppress("unused")
    fun links(action: Action<in LinkItemContainer>) {
        action.execute(linkContainer)
    }

    /**
     * The container for all single directory configurations.
     * These directories will be created as they are.
     *
     * @property links container of link configurations
     */
    val directories: DirectoryContainer
        get() = directoryContainer

    /**
     * Configures directories of a component.
     *
     * @param action execute the directory container configuration
     */
    @Suppress("unused")
    fun directories(action: Action<in DirectoryContainer>) {
        action.execute(directoryContainer)
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
}
