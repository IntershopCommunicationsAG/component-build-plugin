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

import com.intershop.gradle.component.build.extension.items.IDependency
import com.intershop.gradle.component.build.extension.items.LibraryItem
import com.intershop.gradle.component.build.extension.items.ModuleItem
import com.intershop.gradle.component.build.utils.DependencyConfig
import com.intershop.gradle.component.descriptor.Component
import com.intershop.gradle.component.descriptor.ContentType
import com.intershop.gradle.component.descriptor.Dependency
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.LibraryBinaryIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.ivy.IvyDescriptorArtifact
import org.gradle.ivy.IvyModule
import org.gradle.maven.MavenModule
import org.gradle.maven.MavenPomArtifact
import org.slf4j.LoggerFactory
import com.intershop.gradle.component.descriptor.Library as LibDesr
import com.intershop.gradle.component.descriptor.Module as ModuleDescr

/**
 * This class provides the handling of the module item and library item
 * set for the CreateDescriptor task.
 *
 * @property rootProj root project of the current project
 * @property configurations ConfigurationHandler of this project
 * @property dependencyHandler DependencyHandler of this project
 * @property libExcludes list of all excludes of the library configuration
 * @property moduleExcludes list of all excludes of the module configuration
 *
 * @constructor provides a class with configured exclude list based on regex with
 * a different maps for handling the dependencies.
 */
class DependencyProcessor(val rootProj: Project,
                          val configurations: ConfigurationContainer,
                          val dependencyHandler: DependencyHandler,
                          libExcludes: Set<DependencyConfig>?,
                          moduleExcludes: Set<DependencyConfig>?) {

    private val resModuleDeps: MutableMap<DependencyConfig, ExtendedDepConfig> = mutableMapOf()
    private val procModuleDeps: MutableMap<DependencyConfig, ExtendedDepConfig> = mutableMapOf()
    private val excludeModuleSet: MutableSet<ExcludeConfig> = mutableSetOf()
    private val resLibDeps: MutableMap<DependencyConfig, ExtendedDepConfig> = mutableMapOf()
    private val procLibDeps: MutableMap<DependencyConfig, ExtendedDepConfig> = mutableMapOf()
    private val excludeLibSet: MutableSet<ExcludeConfig> = mutableSetOf()

    companion object {
        private val logger = LoggerFactory.getLogger(DependencyProcessor::class.java.simpleName)

        private fun createRegex(input: String): Regex {
            if (input.isBlank()) {
                return ".*".toRegex()
            } else {
                return input.replace(".", "\\.").replace("*", ".*").toRegex()
            }
        }
    }

    init {
        if (moduleExcludes != null) {
            moduleExcludes.forEach {
                excludeModuleSet.add(ExcludeConfig(createRegex(it.group),
                        createRegex(it.module), createRegex(it.version)))
            }
        }
        if (libExcludes != null) {
            libExcludes.forEach {
                excludeLibSet.add(ExcludeConfig(createRegex(it.group),
                        createRegex(it.module), createRegex(it.version)))
            }
        }
    }

    /**
     * Add all dependencies - modules and libs - to the descriptor. The dependencies will
     * be validiert.
     * @param compDescr the descriptor class
     * @param modules a set of configured modules
     * @param libs a set of configured libraries
     */
    fun addDependencies(compDescr: Component, modules: Set<ModuleItem>?, libs: Set<LibraryItem>?) {
        // reset all sets
        resModuleDeps.clear()
        procModuleDeps.clear()
        resLibDeps.clear()
        procLibDeps.clear()

        // add configured dependencies
        if(libs != null) {
            libs.forEach {
                addDependencyObjects(compDescr, it)
            }
        }

        if(modules != null) {
            modules.forEach {
                addDependencyObjects(compDescr, it)
            }
        }

        procModuleDeps.keys.forEach { resModuleDeps.remove(it) }
        procLibDeps.keys.forEach { resLibDeps.remove(it) }

        // add transitive dependencies
        resLibDeps.values.forEach {
            val conf = configurationFor(it.dep, false)
            with(conf.resolvedConfiguration.firstLevelModuleDependencies.first()) {
                val libDescr = LibDesr(dependency = Dependency(moduleGroup, moduleName, moduleVersion),
                        targetName = "${moduleGroup}_${moduleName}_${moduleVersion}")

                addLibDependency(libDescr, this, it.types)
                compDescr.addLib(libDescr)
            }
        }

        resModuleDeps.values.forEach {
            val conf = configurationFor(it.dep, false)
            with(conf.resolvedConfiguration.firstLevelModuleDependencies.first()) {
                val moduleDesc = ModuleDescr(name = moduleName,
                    targetPath = moduleName,
                    dependency = Dependency(moduleGroup, moduleName, moduleVersion),
                    targetIncluded = false,
                    contentType = ContentType.IMMUTABLE)

                addModuleDependency(moduleDesc, this, it.types)

                compDescr.addModule(moduleDesc)
            }
        }
    }

    private fun addDependencyObjects(compDescr: Component, item: IDependency) {
        val conf = configurationFor(item.dependency, item.resolveTransitive)

        with(conf.resolvedConfiguration.firstLevelModuleDependencies.first()) {
            when (item) {
                is ModuleItem -> {
                    val moduleDesc = ModuleDescr(name = moduleName,
                            targetPath = item.targetPath,
                            dependency = Dependency(moduleGroup, moduleName, moduleVersion),
                            targetIncluded = item.targetIncluded,
                            contentType = ContentType.valueOf(item.contentType))

                    addModuleDependency(moduleDesc, this, item.types)
                    compDescr.addModule(moduleDesc)
                    procModuleDeps.put(item.dependency, ExtendedDepConfig(item.dependency, item.types))
                }
                is LibraryItem -> {
                    val libDescr = LibDesr(dependency = Dependency(moduleGroup, moduleName, moduleVersion),
                            targetName = item.targetName)
                    addLibDependency(libDescr, this, item.types)
                    compDescr.addLib(libDescr)
                    procLibDeps.put(item.dependency, ExtendedDepConfig(item.dependency, item.types))
                }
                else -> {
                    logger.warn("The item type {} is not supported.", item::class.java.simpleName)
                }
            }
        }

        val excludeItemSet: MutableSet<ExcludeConfig> = mutableSetOf()

        item.excludes.forEach {
            excludeItemSet.add(ExcludeConfig(createRegex(it.group),createRegex(it.module),createRegex(it.version)))
        }

        addTransitiveDependencies(conf, excludeItemSet, item.types)
    }

    private fun addModuleDependency(moduleDesc: ModuleDescr,
                                    resolvedDependency: ResolvedDependency,
                                    types: Set<String>) {

        with(resolvedDependency) {
            moduleDesc.types.addAll(types)

            // filter for jars
            moduleArtifacts.
                    filter({ it.type == "jar" })
                    .forEach {
                        moduleDesc.jars.add(it.name)
                    }

            // filter for fileContainers
            moduleArtifacts.
                    filter({ it.extension == "zip" && it.type != "sources" && it.type != "javadoc" })
                    .forEach { moduleDesc.pkgs.add(it.name) }

            // filter for fileContainers
            moduleArtifacts.
                    filter({ ! it.classifier.isNullOrBlank() && it.type != "sources" && it.type != "javadoc" })
                    .forEach { moduleDesc.classifiers.add(it.classifier ?: "") }
        }
    }

    private fun addLibDependency(libDesc: LibDesr,
                                 resolvedDependency: ResolvedDependency,
                                 types: Set<String>) {

        with(resolvedDependency) {
            // filter for jars
            moduleArtifacts.
                    filter({ it.type == "jar" })
                    .forEach {
                        // validate jar ...
                    }

            libDesc.types.addAll(types)
        }
    }

    private fun addTransitiveDependencies(conf: Configuration,
                                          excludeItemSet:  MutableSet<ExcludeConfig>,
                                          types: Set<String>) {
        val componentIds = conf.incoming.resolutionResult.allDependencies.mapNotNull {
            if(it is ResolvedDependencyResult) { it.selected.id } else { null }
        }

        val ivyArtifactResolutionResult = dependencyHandler.createArtifactResolutionQuery().forComponents(componentIds).
                withArtifacts(IvyModule::class.java, IvyDescriptorArtifact::class.java).execute()

        ivyArtifactResolutionResult.resolvedComponents.mapNotNull { idToDependencyConfig(it.id) }.forEach {
            val module: ExtendedDepConfig? = resModuleDeps.get(it)
            if(module != null) {
                module.addTypes(types)
            } else {
                if(isNotExcluded(it, excludeItemSet, excludeModuleSet)) {
                    resModuleDeps.put(it, ExtendedDepConfig(it, types))
                }
            }
        }

        val pomArtifactResolutionResult = dependencyHandler.createArtifactResolutionQuery().forComponents(componentIds).
                withArtifacts(MavenModule::class.java,  MavenPomArtifact::class.java).execute()

        pomArtifactResolutionResult.resolvedComponents.mapNotNull { idToDependencyConfig(it.id) }.forEach {
            val lib: ExtendedDepConfig? = resLibDeps.get(it)
            if(lib != null) {
                lib.addTypes(types)
            } else {
                if(isNotExcluded(it, excludeLibSet, excludeItemSet)) {
                    resLibDeps.put(it, ExtendedDepConfig(it, types))
                }
            }
        }
    }

    private fun isNotExcluded(dep: DependencyConfig,
                              excludeItemSet:  MutableSet<ExcludeConfig>,
                              excludeSet:  MutableSet<ExcludeConfig>) : Boolean {

        val resItem = excludeItemSet.filter {
            it.group.matches(dep.group) && it.module.matches(dep.module) && it.version.matches(dep.version)
        }.isEmpty()

        if (resItem) {
            return true
        }

        return excludeSet.filter {
            it.group.matches(dep.group) && it.module.matches(dep.module) && it.version.matches(dep.version)
        }.isEmpty()
    }

    private fun configurationFor(dependency: DependencyConfig, transitive: Boolean) : Configuration {
        val conf = configurations.detachedConfiguration(dependencyHandler.create(dependency.moduleString))
        conf.description = "Configuration for ${dependency.moduleString}"
        conf.isTransitive = transitive
        conf.isVisible = false
        return conf
    }

    private fun idToDependencyConfig(id: ComponentIdentifier) : DependencyConfig? {
        var depReturnValue: DependencyConfig? = null
        when(id) {
            is ModuleComponentIdentifier -> return DependencyConfig(id.group, id.module, id.version)
            is ProjectComponentIdentifier ->  {
                if(id.projectPath != ":") {
                    logger.warn("Project components are currently not implemented.")
                    val projectComp = rootProj.project(id.projectPath)
                    depReturnValue = DependencyConfig(
                            projectComp.group.toString(),
                            projectComp.name,
                            projectComp.version.toString(),
                            id.displayName)
                }
            }
            is LibraryBinaryIdentifier ->  {
                if(id.projectPath != ":") {
                    logger.warn("Binary libs components are currently not implemented.")
                    val projectComp = rootProj.project(id.projectPath)
                    depReturnValue = DependencyConfig(
                            projectComp.group.toString(),
                            projectComp.name,
                            projectComp.version.toString(),
                            id.displayName)
                }
            }
        }
        return depReturnValue
    }

    private data class ExtendedDepConfig(val dep: DependencyConfig,
                                         val types: Set<String>) {
        internal fun addTypes(types: Set<String>) {
            if(! types.isEmpty()) {
                this.types.plus(types)
            }
        }
    }

    private data class ExcludeConfig(val group: Regex,
                                     val module: Regex,
                                     val version: Regex)
}
