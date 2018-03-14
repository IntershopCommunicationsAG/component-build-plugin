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
import com.intershop.gradle.component.build.extension.items.IDependency
import com.intershop.gradle.component.build.extension.items.LibraryItem
import com.intershop.gradle.component.build.extension.items.ModuleItem
import com.intershop.gradle.component.build.utils.DependencyConfig
import com.intershop.gradle.component.build.utils.ExtDependencyConfig
import com.intershop.gradle.component.build.utils.RegexExcludeConfig
import com.intershop.gradle.component.build.utils.getValue
import com.intershop.gradle.component.build.utils.setValue
import com.intershop.gradle.component.descriptor.Component
import com.intershop.gradle.component.descriptor.ContentType
import com.intershop.gradle.component.descriptor.Dependency
import com.intershop.gradle.component.descriptor.util.ComponentUtil
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.LibraryBinaryIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.ivy.IvyDescriptorArtifact
import org.gradle.ivy.IvyModule
import org.gradle.maven.MavenModule
import org.gradle.maven.MavenPomArtifact
import java.io.File
import com.intershop.gradle.component.descriptor.Library as LibDesr
import com.intershop.gradle.component.descriptor.Module as ModuleDescr

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

    private fun addDependencyObjects(compDescr: Component, item: IDependency,
                                     procModules: MutableMap<DependencyConfig, ExtDependencyConfig>,
                                     procLibs: MutableMap<DependencyConfig, ExtDependencyConfig>,
                                     resModules: MutableMap<DependencyConfig, ExtDependencyConfig>,
                                     resLibs: MutableMap<DependencyConfig, ExtDependencyConfig>,
                                     excludeModuleSet: Set<RegexExcludeConfig>,
                                     excludeLibSet: Set<RegexExcludeConfig>) {

        val conf = configurationFor(item.dependency, item.resolveTransitive)
        val resolvedDependency = conf.resolvedConfiguration.firstLevelModuleDependencies.first()

        when(item) {
            is ModuleItem -> {
                addModuleDependency(compDescr, resolvedDependency,
                        item.targetPath, item.targetIncluded,
                        ContentType.valueOf(item.contentType), item.types)
                procModules.put(item.dependency, ExtDependencyConfig(item.dependency, item.types))
            }
            is LibraryItem -> {
                addLibDependency(compDescr, resolvedDependency, item.targetName, item.types)
                procLibs.put(item.dependency, ExtDependencyConfig(item.dependency, item.types))
            }
        }

        addRepositoryDescriptor(conf, resModules, resLibs, excludeModuleSet, excludeLibSet,
                createRegexExcludeSetFrom(item.excludes), item.types)
    }

    private fun addModuleDependency(compDescr: Component, dependency: DependencyConfig, types: Set<String>) {
        val conf = configurationFor(dependency, false)
        val resolvedDependency = conf.resolvedConfiguration.firstLevelModuleDependencies.first()

        addModuleDependency(compDescr, resolvedDependency,
                resolvedDependency.moduleName, false,
                ContentType.IMMUTABLE, types)
    }

    private fun addModuleDependency(compDescr: Component,
                                    resolvedDependency: ResolvedDependency,
                                    targetPath: String,
                                    targetIncluded: Boolean,
                                    contentType: ContentType,
                                    types: Set<String>) {

        with(resolvedDependency) {

            val moduleDesc = ModuleDescr(name = moduleName,
                    targetPath = targetPath,
                    dependency =  Dependency(moduleGroup, moduleName, moduleVersion),
                    targetIncluded = targetIncluded,
                    contentType = contentType)

            moduleDesc.types.addAll(types)

            // filter for jars
            this.moduleArtifacts.
                    filter({ it.type == "jar" })
                    .forEach { moduleDesc.jars.add(it.name) }

            // filter for fileContainers
            this.moduleArtifacts.
                    filter({ it.extension == "zip" && it.type != "sources" && it.type != "javadoc" })
                    .forEach { moduleDesc.pkgs.add(it.name) }

            // filter for fileContainers
            this.moduleArtifacts.
                    filter({ ! it.classifier.isNullOrBlank() && it.type != "sources" && it.type != "javadoc" })
                    .forEach { moduleDesc.classifiers.add(it.classifier ?: "") }

            compDescr.addModule( moduleDesc )
        }
    }

    private fun addLibDependency(compDescr: Component,
                                 resolvedDependency: ResolvedDependency,
                                 targetName: String,
                                 types: Set<String>) {

        with(resolvedDependency) {
            val libDesc = LibDesr(dependency = Dependency(moduleGroup, moduleName, moduleVersion),
                    targetName = targetName)

            libDesc.types.addAll(types)

            compDescr.addLib(libDesc)
        }
    }

    // If component contains an ivy file it is an module.
    // If a pom file is available it is an library.
    // TODO: This must be changed. An indicator should be a special descriptor file.
    private fun addRepositoryDescriptor(conf: Configuration,
                                        resModules: MutableMap<DependencyConfig, ExtDependencyConfig>,
                                        resLibraries: MutableMap<DependencyConfig, ExtDependencyConfig>,
                                        excludeModuleSet: Set<RegexExcludeConfig>,
                                        excludeLibSet: Set<RegexExcludeConfig>,
                                        excludeItemSet: Set<RegexExcludeConfig>,
                                        types: Set<String>) {

        val componentIds = conf.incoming.resolutionResult.allDependencies.map { (it as ResolvedDependencyResult).selected.id }

        val ivyArtifactResolutionResult = project.dependencies.createArtifactResolutionQuery().forComponents(componentIds).
                withArtifacts(IvyModule::class.java, IvyDescriptorArtifact::class.java).execute()

        ivyArtifactResolutionResult.resolvedComponents.mapNotNull { idToDependencyConfig(it.id) }.forEach {
            val module: ExtDependencyConfig? = resModules.get(it)
            if(module != null) {
                module.addTypes(types)
            } else {
                if(isNotExcluded(it, excludeModuleSet, excludeItemSet)) {
                    resModules.put(it, ExtDependencyConfig(it, types))
                }
            }
        }

        val pomArtifactResolutionResult = project.dependencies.createArtifactResolutionQuery().forComponents(componentIds).
                withArtifacts(MavenModule::class.java,  MavenPomArtifact::class.java).execute()

        pomArtifactResolutionResult.resolvedComponents.mapNotNull { idToDependencyConfig(it.id) }.forEach {
            val lib: ExtDependencyConfig? = resLibraries.get(it)
            if(lib != null) {
                lib.addTypes(types)
            } else {
                if(isNotExcluded(it, excludeLibSet, excludeItemSet)) {
                    resLibraries.put(it, ExtDependencyConfig(it, types))
                }
            }
        }
    }

    private fun isNotExcluded(dep: DependencyConfig,
                              excludeItemSet: Set<RegexExcludeConfig>,
                              excludeContainerSet: Set<RegexExcludeConfig>) : Boolean {

        val resItem = excludeItemSet.filter { it.group.matches(dep.group) &&
                                              it.module.matches(dep.module) &&
                                              it.version.matches(dep.version) }.isEmpty()
        if(resItem) {
            return true
        }

        return excludeContainerSet.filter { it.group.matches(dep.group) &&
                                            it.module.matches(dep.module) &&
                                            it.version.matches(dep.version) }.isEmpty()
    }

    // provides configuration for dependency
    private fun configurationFor(dependency: DependencyConfig, transitive: Boolean) : Configuration {
        val conf = project.configurations.detachedConfiguration(project.dependencies.create(dependency.moduleString))
        conf.description = "Configuration for ${dependency.moduleString}"
        conf.isTransitive = transitive
        conf.isVisible = false
        return conf
    }

    private fun idToDependencyConfig(id: ComponentIdentifier) : DependencyConfig? {
        when(id) {
            is ModuleComponentIdentifier -> return DependencyConfig(id.group, id.module, id.version)
            is ProjectComponentIdentifier ->  return projectIdToDependencyConfig(id)
            is LibraryBinaryIdentifier -> return libBinaryIdToDependencyConfig(id)
        }
        return null
    }

    private fun projectIdToDependencyConfig(id: ProjectComponentIdentifier) : DependencyConfig? {
        if(id.projectPath != ":") {
            logger.warn("Project components are currently not implemented.")
            val projectComp = project.rootProject.project(project.path)
            return DependencyConfig(
                    projectComp.group.toString(),
                    projectComp.name,
                    projectComp.version.toString(),
                    id.displayName)
        }
        return null
    }

    private fun libBinaryIdToDependencyConfig(id: LibraryBinaryIdentifier) : DependencyConfig? {
        if(id.projectPath != ":") {
            logger.warn("Binary libs components are currently not implemented.")
            val projectComp = project.rootProject.project(project.path)
            return DependencyConfig(
                    projectComp.group.toString(),
                    projectComp.name,
                    projectComp.version.toString(),
                    id.displayName)
        }
        return null
    }

    private fun createRegexExcludeSetFrom(excludes: Set<DependencyConfig>?): Set<RegexExcludeConfig> {
        if(excludes == null) {
            return mutableSetOf()
        }
        return excludes.map {
            it -> RegexExcludeConfig(createRegex(it.group),createRegex(it.module),createRegex(it.version))
        }.toSet()
    }

    private fun createRegex(input: String) : Regex {
        if(input.isBlank()) {
            return ".*".toRegex()
        } else {
            return input.replace(".", "\\.").replace("*", ".*").toRegex()
        }
    }

    /**
     * Task method for the creation of a descriptor file.
     * This is one of the artifacts of a component.
     */
    @Suppress("unused")
    @TaskAction
    fun createDescriptor() {

        val componentDescr = Component(displayName, componentDescription)

        val procModules: MutableMap<DependencyConfig, ExtDependencyConfig> = mutableMapOf()
        val procLibs: MutableMap<DependencyConfig, ExtDependencyConfig> = mutableMapOf()
        val resModules: MutableMap<DependencyConfig, ExtDependencyConfig> = mutableMapOf()
        val resLibs: MutableMap<DependencyConfig, ExtDependencyConfig> = mutableMapOf()

        val excludeLibSet = createRegexExcludeSetFrom(libs?.excludes)
        val excludeModuleSet = createRegexExcludeSetFrom(modules?.excludes)


        if(modules != null) {
            modules?.items?.forEach {
                addDependencyObjects(componentDescr, it,
                        procModules, procLibs, resModules, resLibs,
                        excludeModuleSet, excludeLibSet)
            }
        }

        if(libs != null) {
            libs?.items?.forEach {
                addDependencyObjects(componentDescr, it,
                        procModules, procLibs, resModules, resLibs,
                        excludeModuleSet, excludeLibSet)
            }
        }

        procModules.keys.forEach { resModules.remove(it) }
        procLibs.keys.forEach { resLibs.remove(it) }

        resModules.values.forEach {
            addModuleDependency(componentDescr, it.dep, it.types)
        }
        resLibs.values.forEach {
            val libDesc = LibDesr(dependency = Dependency(it.dep.group, it.dep.module, it.dep.version),
                    targetName = "${it.dep.group}_${it.dep.module}_${it.dep.version}")

            libDesc.types.addAll(it.types)
            componentDescr.addLib(libDesc)
        }

        ComponentUtil.writeToFile(componentDescr, descriptorFile)
    }


}
