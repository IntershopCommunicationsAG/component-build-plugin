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
package com.intershop.gradle.component.build.tasks


import com.intershop.gradle.component.build.extension.container.LibraryItemContainer
import com.intershop.gradle.component.build.extension.container.ModuleItemContainer
import com.intershop.gradle.component.build.utils.getValue
import com.intershop.gradle.component.build.utils.setValue
import com.intershop.gradle.component.descriptor.Component
import com.intershop.gradle.component.descriptor.util.ComponentUtil
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
//import com.intershop.gradle.component.descriptors.Library as LibDesr
//import com.intershop.gradle.component.descriptors.Module as ModuleDescr

/**
 * CreateDescriptor Gradle task 'createComponent'
 *
 * The injected dependencyHandler is used for internal dependency handling.
 * This task hast currently no declared inputs and outputs and builds
 * file not incremental.
 *
 * @property dependencyHandler is used for internal dependency handling.
 * @constructor Creates the task with a dependencyHandler
 */
open class CreateDescriptor : DefaultTask() {

    // Outputfile configurationFor
    private val descriptorFileProperty = this.newOutputFile()
    // display name configurationFor
    private val displayNameProperty = project.objects.property(String::class.java)
    // component description configurationFor
    private val componentDescriptionProperty = project.objects.property(String::class.java)

    /**
     * The output file contains the descriptor of the component.
     *
     * @property descriptorFile real file on file system with descriptor
     */
    @Suppress("private", "unused")
    @get:OutputFile
    var descriptorFile: File
        get() = descriptorFileProperty.get().asFile
        set(value) = descriptorFileProperty.set(value)

    /**
     * Set provider for descriptor file property.
     *
     * @param descriptorFile set provider for property.
     */
    fun provideDescriptorFile(descriptorFile: Provider<RegularFile>)
            = descriptorFileProperty.set(descriptorFile)

    /**
     * This property contains the display name of the component.
     * Declares a task input for incremental build.
     *
     * @property displayName name of the component
     */
    @Suppress("private", "unused")
    @get:Input
    var displayName: String by displayNameProperty

    /**
     * Set provider for display name property.
     *
     * @param displayName set provider for property.
     */
    fun provideDisplayName(displayName: Provider<String>)
            = displayNameProperty.set(displayName)

    /**
     * This property contains the description of the component.
     * Declares a task input for incremental build.
     *
     * @property componentDescription description of the component
     */
    @Suppress("private", "unused")
    @get:Input
    var componentDescription: String by componentDescriptionProperty

    /**
     * Set provider for compoent description property.
     *
     * @param description set provider for property.
     */
    fun provideComponentDescription(description: Provider<String>)
            = componentDescriptionProperty.set(description)

    /**
     * Container for all modules.
     */
    //TODO: This should be a nested property.
    @get:Internal
    var modules: ModuleItemContainer? = null

    /**
     * Container for all libs.
     */
    //TODO: This should be a nested property.
    @get:Internal
    var libs: LibraryItemContainer? = null
/**
    // Resolves module dependency from configuration
    // and identifies all artifacts
    private fun resolveModuleDependency(item: ModuleItem,
                                        depModule: MutableSet<ComponentIdentifier>,
                                        depLibraries: MutableSet<ComponentIdentifier>) : ModuleDescr {
        val conf = configurationFor(item)
        val resolvedDependency = conf.resolvedConfiguration.firstLevelModuleDependencies.first()

        with(resolvedDependency) {
            val depDescr = Dependency(moduleGroup, moduleName, moduleVersion)

            var targetPath = item.targetPath
            if (targetPath.isEmpty()) {
                targetPath = moduleName
            }

            val moduleDesc = ModuleDescr(
                    name = moduleName,
                    targetPath = targetPath,
                    dependency = depDescr,
                    targetIncluded = item.targetIncluded)

            moduleDesc.types.addAll(item.types)

            // filter for jars
            resolvedDependency.
                    moduleArtifacts.
                    filter({ it.type == "jar" })
                    .forEach {
                        moduleDesc.jars.add(it.name)
                    }

            // filter for fileContainers
            resolvedDependency.
                    moduleArtifacts.
                    filter({ it.extension == "zip" && it.type != "sources" && it.type != "javadoc" })
                    .forEach {
                        moduleDesc.pkgs.add(it.name)
                    }

            // filter for fileContainers
            resolvedDependency.
                    moduleArtifacts.
                    filter({ ! it.classifier.isNullOrBlank() && it.type != "sources" && it.type != "javadoc" })
                    .forEach {
                        moduleDesc.classifiers.add(it.classifier ?: "")
                    }

            addRepositoryDescriptor(conf, depModule, depLibraries)

            return moduleDesc
        }
    }

    // Resolves library dependency from configuration
    // and all transitive dependencies.
    private fun resolveLibraryDependency(item: LibraryItem,
                                         depModule: MutableSet<ComponentIdentifier>,
                                         depLibraries: MutableSet<ComponentIdentifier>) : LibDesr {
        val conf = configurationFor(item)
        val resolvedDependency = conf.resolvedConfiguration.firstLevelModuleDependencies.first()

        with(resolvedDependency) {
            val depDescr = Dependency(moduleGroup, moduleName, moduleVersion)

            val libDesc = LibDesr(depDescr,
                    "${moduleGroup}_${moduleName}_${moduleVersion}")
            libDesc.types.addAll(item.types)

            addRepositoryDescriptor(conf, depModule, depLibraries)

            return libDesc
        }
    }

    // provides configuration for dependency
    private fun configurationFor(item: DependencyObject) : Configuration {
        val conf = project.configurations.detachedConfiguration(dependencyHandler.create(item.dependency))
        conf.description = "Configuration for $item"
        conf.isTransitive = true
        conf.isVisible = false
        return conf
    }

    // If component contains an ivy file it is an module.
    // If a pom file is available it is an library.
    // TODO: This must be changed. An indicator should be a special descriptor file.
    private fun addRepositoryDescriptor(conf: Configuration,
                                        depModule: MutableSet<ComponentIdentifier>,
                                        depLibraries: MutableSet<ComponentIdentifier>) {
        val componentIds = conf.incoming.resolutionResult.allDependencies.map { it.from.id }

        val ivyArtifactResolutionResult = dependencyHandler.createArtifactResolutionQuery().forComponents(componentIds).
                withArtifacts(IvyModule::class.java, IvyDescriptorArtifact::class.java).execute()

        depModule.addAll(ivyArtifactResolutionResult.components.map { it.id })

        val pomArtifactResolutionResult = dependencyHandler.createArtifactResolutionQuery().forComponents(componentIds).
                withArtifacts(MavenModule::class.java,  MavenPomArtifact::class.java).execute()

        depLibraries.addAll(pomArtifactResolutionResult.components.map { it.id })
    }
 **/

    /**
     * Task method for the creation of a descriptor file.
     * This is one of the artifacts of a component.
     */
    @Suppress("unused")
    @TaskAction
    fun createDescriptor() {

        val componentDescr = Component(displayName, componentDescription)
/**
        val depModules: MutableSet<ComponentIdentifier> = mutableSetOf()
        val depLibraries: MutableSet<ComponentIdentifier> = mutableSetOf()

        if(modules != null) {
            modules?.items?.forEach {
                if(it.dependency != null) {
                    println("Add module ...")
                    componentDescr.addModule(resolveModuleDependency(it, depModules, depLibraries))
                }
            }
        }

        if(libs != null) {
            libs?.items?.forEach {
                if(it.dependency != null) {
                    println("Add lib ...")
                    componentDescr.addLib(resolveLibraryDependency(it, depModules, depLibraries))
                }
            }
        }

        //TODO: calculate add dependencies for modules and libraries
**/
        ComponentUtil.writeToFile(componentDescr, descriptorFile)
    }


}
