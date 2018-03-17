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
import org.gradle.api.InvalidUserDataException
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
 * set for the CreateDescriptorTask task.
 *
 * @property rootProj root project of the current project
 * @property configurations ConfigurationHandler of this project
 * @property dependencyHandler DependencyHandler of this project
 * @property excludes set of dependency patterns for excluding dependencies
 *
 * @constructor provides a class with configured exclude list based on regex with
 * a different maps for handling the dependencies.
 */
class DependencyProcessor(private val rootProj: Project,
                          private val configurations: ConfigurationContainer,
                          private val dependencyHandler: DependencyHandler,
                          private val excludes: Set<DependencyConfig>) {

    private val resModuleDeps: MutableMap<DependencyConfig, ExtendedDepConfig> = mutableMapOf()
    private val procModuleDeps: MutableMap<DependencyConfig, ExtendedDepConfig> = mutableMapOf()
    private val resLibDeps: MutableMap<DependencyConfig, ExtendedDepConfig> = mutableMapOf()
    private val procLibDeps: MutableMap<DependencyConfig, ExtendedDepConfig> = mutableMapOf()

    private var modulesDeps: List<DependencyConfig>? = null
    private var libsDeps: List<DependencyConfig>? = null

    companion object {
        private val logger = LoggerFactory.getLogger(DependencyProcessor::class.java.simpleName)
    }

    /**
     * Add all dependencies - modules and libs - to the descriptor. The dependencies will
     * be validiert.
     * @param compDescr the descriptor class
     * @param modules a set of configured modules
     * @param libs a set of configured libraries
     */
    @Throws(InvalidUserDataException::class)
    fun addDependencies(compDescr: Component, modules: Set<ModuleItem>?, libs: Set<LibraryItem>?) {
        // reset all sets
        resModuleDeps.clear()
        procModuleDeps.clear()
        resLibDeps.clear()
        procLibDeps.clear()

        libsDeps = libs?.map { it.dependency }
        modulesDeps = modules?.map { it.dependency }

        // add configured dependencies
        libs?.forEach {
            addDependencyObjects(compDescr, it)
        }

        modules?.forEach {
            addDependencyObjects(compDescr, it)
        }

        procModuleDeps.keys.forEach { resModuleDeps.remove(it) }
        procLibDeps.keys.forEach { resLibDeps.remove(it) }

        // add transitive dependencies
        resLibDeps.values.forEach {
            val conf = configurationFor(it.dep, false)
            with(conf.resolvedConfiguration.firstLevelModuleDependencies.first()) {
                val targetName = "${moduleGroup}_${moduleName}_$moduleVersion"
                if(! compDescr.libs.keys.contains(targetName)) {
                    val libDescr = LibDesr(dependency = Dependency(moduleGroup, moduleName, moduleVersion),
                            targetName = targetName)
                    libDescr.types.addAll(it.types)
                    compDescr.addLib(libDescr)
                } else {
                    logger.error("Target name '{}' for '{}' exists in list of targets." +
                        "Check your configuration! The target name is module_name_version in " +
                        "the default configuration", targetName, it.dep.moduleString)
                    throw InvalidUserDataException("Target name '$targetName' for '${it.dep.moduleString}' exists " +
                            "in list of targets. Check your configuration!")
                }
            }
        }

        resModuleDeps.values.forEach {
            val conf = configurationFor(it.dep, false)
            with(conf.resolvedConfiguration.firstLevelModuleDependencies.first()) {
                if(! compDescr.modules.keys.contains(moduleName)) {
                    val moduleDesc = ModuleDescr(name = moduleName,
                            targetPath = moduleName,
                            dependency = Dependency(moduleGroup, moduleName, moduleVersion),
                            targetIncluded = false,
                            contentType = ContentType.IMMUTABLE)

                    addModuleDependency(moduleDesc, this, it.types)

                    compDescr.addModule(moduleDesc)
                } else {
                    logger.error("Target path '{}' for '{}' exists in list of targets." +
                            "Check your configuration! The module name is the target path in " +
                            "the default configuration", moduleName, it.dep.moduleString)
                    throw InvalidUserDataException("Target path '$moduleName' for '${it.dep.moduleString}' exists " +
                            "in List of targets. Check your configuration.")
                }
            }
        }
    }

    @Throws(InvalidUserDataException::class)
    private fun addDependencyObjects(compDescr: Component, item: IDependency) {
        val conf = configurationFor(item.dependency, item.resolveTransitive)

        with(conf.resolvedConfiguration.firstLevelModuleDependencies.first()) {
            when (item) {
                is ModuleItem -> {
                    if(! compDescr.modules.keys.contains(item.targetPath)) {
                        val moduleDesc = ModuleDescr(name = moduleName,
                            targetPath = item.targetPath,
                            dependency = Dependency(moduleGroup, moduleName, moduleVersion),
                            targetIncluded = item.targetIncluded,
                            contentType = ContentType.valueOf(item.contentType))

                        addModuleDependency(moduleDesc, this, item.types)
                        compDescr.addModule(moduleDesc)
                        procModuleDeps.put(item.dependency, ExtendedDepConfig(item.dependency, item.types))
                    } else {
                        logger.error("Target path '{}' for '{}' exists in list of targets." +
                                "Check your configuration! The module name is the target path in " +
                                "the default configuration", item.targetPath, item.dependency.moduleString)
                        throw InvalidUserDataException("Target path '${item.targetPath}' for '" +
                                "${item.dependency.moduleString}' exists in list of targets. Check your" +
                                " logs and configuration!")
                    }
                }
                is LibraryItem -> {
                    if(! compDescr.libs.keys.contains(item.targetName)) {
                        val libDescr = LibDesr(dependency = Dependency(moduleGroup, moduleName, moduleVersion),
                                targetName = item.targetName)
                        libDescr.types.addAll(item.types)
                        compDescr.addLib(libDescr)
                        procLibDeps.put(item.dependency, ExtendedDepConfig(item.dependency, item.types))
                    } else {
                        logger.error("Target name '{}' for '{}' exists in list of targets." +
                                "Check your configuration! The target name is module_name_version in " +
                                "the default configuration", item.targetName, item.dependency.moduleString)
                        throw InvalidUserDataException("Target name '${item.targetName}' for '" +
                                "${item.dependency.moduleString}' exists in list of targets. Check your " +
                                " logs and configuration!")
                    }
                }
                else -> {
                    logger.warn("The item type {} is not supported.", item::class.java.simpleName)
                }
            }
        }

        addTransitiveDependencies(conf, item.types)
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

    @Throws(InvalidUserDataException::class)
    private fun addTransitiveDependencies(conf: Configuration, types: Set<String>) {
        var fromDep = ""

        val componentIds = conf.incoming.resolutionResult.allDependencies.mapNotNull {
            fromDep = it.from.toString()
            (it as? ResolvedDependencyResult)?.selected?.id
        }

        val ivyArtifactResolutionResult = dependencyHandler.createArtifactResolutionQuery().forComponents(componentIds).
                withArtifacts(IvyModule::class.java, IvyDescriptorArtifact::class.java).execute()

        ivyArtifactResolutionResult.resolvedComponents.mapNotNull { idToDependencyConfig(it.id) }.forEach {dep ->
            val module: ExtendedDepConfig? = resModuleDeps[dep]
            if(module != null) {
                module.addTypes(types)
            } else {
                if ( isNotExcluded(dep) ) {
                    val availableDep = resModuleDeps.keys.find { it.group == dep.group && it.module == dep.module }
                    if( availableDep == null) {
                        resModuleDeps[dep] = ExtendedDepConfig(dep, types)
                    } else {
                        logger.error("There is a version conflict! {}:{} exists " +
                                "in the list of resolved Depedendencies. [source: {}, availabble: {}, new: {}]",
                                dep.group, dep.module, fromDep, dep.moduleString, availableDep.moduleString)

                        throw InvalidUserDataException("There is a version conflict! " + dep.group + ":" + dep.module +
                                " exists in the list of resolved Depedendencies.")
                    }
                }
            }
        }

        val pomArtifactResolutionResult = dependencyHandler.createArtifactResolutionQuery().forComponents(componentIds).
                withArtifacts(MavenModule::class.java,  MavenPomArtifact::class.java).execute()

        pomArtifactResolutionResult.resolvedComponents.mapNotNull { idToDependencyConfig(it.id) }.forEach {dep ->
            val lib: ExtendedDepConfig? = resLibDeps[dep]
            if(lib != null) {
                lib.addTypes(types)
            } else {
                if ( isNotExcluded(dep) ) {
                    val availableDep = resLibDeps.keys.find { it.group == dep.group && it.module == dep.module }
                    if( availableDep == null ) {
                        resLibDeps[dep] = ExtendedDepConfig(dep, types)
                    } else {
                        logger.error("There is a version conflict! {}:{} exists " +
                                "in the list of resolved Depedendencies. [source: {}, availabble: {}, new: {}]",
                                dep.group, dep.module, fromDep, dep.moduleString, availableDep.moduleString)

                        throw InvalidUserDataException("There is a version conflict! " + dep.group + ":" + dep.module +
                                " exists in the list of resolved Depedendencies.")
                    }
                }
            }
        }
    }

    private fun isNotExcluded(dep: DependencyConfig) : Boolean {

        val confDepIncluded = modulesDeps?.find { it.group == dep.group && it.module == dep.module } != null &&
                libsDeps?.find { it.group == dep.group && it.module == dep.module } != null

        return confDepIncluded || excludes.none {
            it.group.toRegex().matches(dep.group)
                    && it.module.toRegex().matches(dep.module)
                    && it.version.toRegex().matches(dep.version)
        }
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
}
