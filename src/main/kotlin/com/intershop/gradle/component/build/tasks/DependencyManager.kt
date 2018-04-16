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
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.artifacts.ResolvedArtifact
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
import com.intershop.gradle.component.descriptor.Dependency as DependencyDescr
import com.intershop.gradle.component.descriptor.Library as LibDescr
import com.intershop.gradle.component.descriptor.Module as ModuleDescr

/**
 * This class provides the handling of the module item and library item
 * set for the CreateComponentTask task.
 *
 * @property project the current project instance
 *
 * @constructor provides a class for lib and module dependencies of this component
 */
class DependencyManager(val project: Project) {

    companion object {
        internal val logger = LoggerFactory.getLogger(DependencyManager::class.java.simpleName)

        private const val errorOutCheck = "Check your logs and configuration!"
        private const val errorOutModuleName = "The module name is the target path in the default configuration."
        private const val errorOutLibName = "The target name is 'group_name_version' in the default configuration."

        private const val errorOutVersionInfo = "There is a version conflict!"
        private const val errorOutVersionDescr = "exists in the list of resolved dependencies."
    }


    private val resModuleDeps: MutableMap<DependencyConfig, ModuleDescr> = mutableMapOf()
    private val resLibDeps: MutableMap<DependencyConfig, LibDescr> = mutableMapOf()

    private val procModuleDeps: MutableMap<DependencyConfig, ModuleDescr> = mutableMapOf()
    private val procLibDeps: MutableMap<DependencyConfig, LibDescr> = mutableMapOf()

    private var rootProject: Project = project.rootProject
    private var dependencyHandler: DependencyHandler = project.dependencies
    private var configurations: ConfigurationContainer = project.configurations

    private var modulesInitialized = false
    private var libsInitialized = false

    /**
     * Calculate a list with resolved dependencies for libraries.
     *
     * @param items a set of items from the configuration
     *
     * @return set of resolved dependency configurations
     */
    @Throws(GradleException::class)
    fun getLibDependencies(items: Set<LibraryItem>, excludes: Set<DependencyConfig>): Set<DependencyConfig> {
        if(! libsInitialized) {
            // add configured dependencies
            items.forEach {
                if(!addDependencyObjects(it, excludes)) {
                    procLibDeps.clear()
                    throw GradleException("Could not resolve library dependency for ${it.dependency.getModuleString()}")
                }
            }
            libsInitialized = true
        }
        return procLibDeps.keys
    }

    /**
     * Calculate a list with resolved dependencies for modules.
     *
     * @param items a set of items from the configuration
     *
     * @return set of resolved dependency configurations
     */
    @Throws(GradleException::class)
    fun getModuleDependencies(items: Set<ModuleItem>, excludes: Set<DependencyConfig>): Set<DependencyConfig> {
        if(! modulesInitialized) {
            // add configured dependencies
            items.forEach {
                if(!addDependencyObjects(it, excludes)) {
                    procModuleDeps.clear()
                    throw GradleException("Could not resolve module dependency for ${it.dependency.getModuleString()}")
                }
            }
            modulesInitialized = true
        }
        return procModuleDeps.keys
    }

    /**
     * Writes all dependencies - also transitive resolved dependencies - to
     * the descriptor file.
     *
     * @param component the descriptor object
     * @param excludes the exclude configuration for transitive dependencies
     */
    fun addToDescriptor(component: Component, excludes: Set<DependencyConfig>) {
        //add modules
        procModuleDeps.values.forEach {
            component.addModule(it)

            // component types and classifiers contains all available
            // types and classifiers
            component.types.addAll(it.types)
            component.classifiers.addAll(it.classifiers)
        }
        //add libs
        procLibDeps.values.forEach {
            component.addLib(it)

            // component types contains all available types
            component.types.addAll(it.types)
        }

        //resolve transitive
        procModuleDeps.forEach {
            val conf = configurationFor(it.key, excludes, it.key.transitive)
            if(it.key.transitive) {
                with(conf.resolvedConfiguration.firstLevelModuleDependencies.first()) {
                    addTransitiveDependencies(conf, it.value.types)
                }
            }
        }
        //add libs
        procLibDeps.forEach {
            val conf = configurationFor(it.key, excludes, it.key.transitive)
            if(it.key.transitive) {
                with(conf.resolvedConfiguration.firstLevelModuleDependencies.first()) {
                    addTransitiveDependencies(conf, it.value.types)
                }
            }
        }

        resModuleDeps.forEach {
            val conf = configurationFor(it.key, excludes,false)
            with(conf.resolvedConfiguration.firstLevelModuleDependencies.first()) {
                addModuleDependency(it.value, this, mutableSetOf())
            }
        }

        //add modules
        resModuleDeps.values.forEach {
            component.addModule(it)
            // component classifiers contains all available classifiers
            component.classifiers.addAll(it.classifiers)
        }
        resLibDeps.values.forEach {
            component.addLib(it)
        }
    }

    @Throws(InvalidUserDataException::class)
    private fun addDependencyObjects(item: IDependency, excludes: Set<DependencyConfig>): Boolean {
        val conf = configurationFor(item.dependency, excludes, item.resolveTransitive)

        try {
            with(conf.resolvedConfiguration.firstLevelModuleDependencies.first()) {
                val dependency = DependencyDescr(moduleGroup, moduleName, moduleVersion)
                val dependencyConf = DependencyConfig(moduleGroup, moduleName, moduleVersion,
                        item.dependency.dependency, item.resolveTransitive)

                when (item) {
                    is ModuleItem -> {
                        if (procModuleDeps.none
                                { it.value.targetPath == item.targetPath && it.key != item.dependency }) {
                            val moduleDesc = ModuleDescr(
                                    name = this@with.moduleName ?: "",
                                    targetPath = item.targetPath,
                                    dependency = dependency,
                                    targetIncluded = item.targetIncluded,
                                    contentType = ContentType.valueOf(item.contentType),
                                    updatable = item.updatable,
                                    descriptorPath = item.descriptorPath,
                                    jarPath = item.jarPath,
                                    itemType = item.itemType)
                            moduleDesc.excludes.addAll(item.excludes)
                            moduleDesc.preserves.addAll(item.preserves)

                            addModuleDependency(moduleDesc, this, item.types)
                            procModuleDeps.put(dependencyConf, moduleDesc)

                        } else {
                            throwErrorMessage("Target path '${item.targetPath}' for '" +
                                    "${item.dependency.getModuleString()}' exists in list of targets.",
                                    errorOutModuleName)
                        }
                    }

                    is LibraryItem -> {
                        if (procLibDeps.none { it.value.targetName == item.targetName && it.key != item.dependency }) {
                            val libDescr = LibDescr(dependency = dependency, targetName = item.targetName)
                            libDescr.types.addAll(item.types)
                            procLibDeps.put(dependencyConf, libDescr)
                        } else {
                            throwErrorMessage("Target name '${item.targetName}' for '" +
                                    "${item.dependency.getModuleString()}' exists in list of targets.", errorOutLibName)
                        }
                    }
                    else -> {
                        logger.warn("The item type {} is not supported.", item::class.java.simpleName)
                    }
                }
            }
            return true
        } catch (ex: ResolveException) {
            return false
        }
    }

    private fun addModuleDependency(moduleDescr: ModuleDescr,
                                    resolvedDependency: ResolvedDependency,
                                    types: Set<String>) {

        with(resolvedDependency) {
            moduleDescr.types.addAll(types)

            // filter for jars
            moduleArtifacts.filter({ it.type == "jar" })
                    .forEach { moduleDescr.jars.add(it.name) }

            // filter for fileContainers
            moduleArtifacts.filter({ it.extension == "zip" && it.type != "sources" && it.type != "javadoc" })
                    .forEach { moduleDescr.pkgs.add(calcArtifactName(it)) }

            // filter for fileContainers
            moduleArtifacts.filter({ ! it.classifier.isNullOrBlank() && it.type != "sources" && it.type != "javadoc" })
                    .forEach { moduleDescr.classifiers.add(it.classifier ?: "") }
        }
    }

    private fun calcArtifactName(artifact: ResolvedArtifact): String {
        val artifactName = StringBuilder()

        with(artifact) {
            artifactName.append(name)
            if(! artifactName.endsWith("-$type") && type != extension) {
                artifactName.append("-").append(type)
            }
            if(! classifier.isNullOrBlank()) {
                artifactName.append("-").append(classifier)
            }
        }

        return artifactName.toString()
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
            if(procModuleDeps.keys.none { it.getModuleString() == dep.getModuleString() }) {
                val moduleKey = resModuleDeps.keys.find { it.getModuleString() == dep.getModuleString() }

                if (moduleKey != null) {
                    resModuleDeps[moduleKey]?.types?.addAll(types)
                } else {
                    val availableDep = resModuleDeps.keys.find { it.group == dep.group && it.module == dep.module }
                    if (availableDep == null) {
                        if (resModuleDeps.none { it.value.targetPath == dep.module }) {
                            val moduleDesc = ModuleDescr(name = dep.module,
                                    targetPath = dep.module,
                                    dependency = DependencyDescr(dep.group, dep.module, dep.version),
                                    targetIncluded = false,
                                    contentType = ContentType.IMMUTABLE)
                            moduleDesc.types.addAll(types)
                            resModuleDeps[dep] = moduleDesc
                        } else {
                            throwErrorMessage("Target name '${dep.module}' for '${dep.getModuleString()}' " +
                                    "exists in list of targets.", errorOutModuleName)
                        }
                    } else {
                        throwErrorMessage(errorOutVersionInfo, "${dep.group}:${dep.module} $errorOutVersionDescr " +
                                "[source: $fromDep, availabble: ${dep.getModuleString()}, " +
                                "new: ${availableDep.getModuleString()}]")
                    }
                }
            }
        }

        val pomArtifactResolutionResult = dependencyHandler.createArtifactResolutionQuery().forComponents(componentIds).
                withArtifacts(MavenModule::class.java,  MavenPomArtifact::class.java).execute()

        pomArtifactResolutionResult.resolvedComponents.mapNotNull { idToDependencyConfig(it.id) }.forEach {dep ->
            if(procLibDeps.keys.none { it.getModuleString() == dep.getModuleString()}) {
                val libKey = resLibDeps.keys.find { it.getModuleString() == dep.getModuleString() }

                if (libKey != null) {
                    resLibDeps[libKey]?.types?.addAll(types)
                } else {
                    val availableDep = resLibDeps.keys.find { it.group == dep.group && it.module == dep.module }
                    if (availableDep == null) {
                        val targetName = "${dep.group}_${dep.module}_${dep.version}"
                        if (resLibDeps.none { it.value.targetName == targetName }) {
                            val libDescr = LibDescr(dependency = DependencyDescr(dep.group, dep.module, dep.version),
                                    targetName = targetName)
                            libDescr.types.addAll(types)
                            resLibDeps[dep] = libDescr
                        } else {
                            throwErrorMessage("Target path '${dep.module}' for '${dep.getModuleString()}'" +
                                    "exists in List of targets.", errorOutLibName)
                        }
                    } else {
                        throwErrorMessage(errorOutVersionInfo, "${dep.group}:${dep.module} $errorOutVersionDescr " +
                                "[source: $fromDep, availabble: ${dep.getModuleString()}, " +
                                "new: ${availableDep.getModuleString()}]")
                    }
                }
            }
        }
    }

    @Throws(InvalidUserDataException::class)
    private fun throwErrorMessage(errorMsg: String, description: String) {
        logger.error("$errorMsg $description $errorOutCheck")
        throw InvalidUserDataException("$errorMsg $errorOutCheck")
    }

    /*
     *  Project dependencies must be handled special
     */
    private fun configurationFor(dependency: DependencyConfig,
                                 excludes: Set<DependencyConfig>,
                                 transitive: Boolean) : Configuration {
        val depObj = if(dependency.dependency.isNotBlank()) {
                        dependencyHandler.create(project.project(dependency.dependency))
                     } else {
                        dependencyHandler.create(dependency.getModuleString())
                     }

        val conf = configurations.detachedConfiguration(depObj)

        if(transitive) {
            excludes.forEach {
                conf.exclude(it.getExcludeProperties())
            }
        }

        conf.description = "Configuration for ${dependency.getModuleString()}"
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
                    with(rootProject.project(id.projectPath)) {
                        depReturnValue = DependencyConfig(group.toString(),name ?: id.projectName,
                                version.toString(), id.projectPath)
                    }
                }
            }
            is LibraryBinaryIdentifier ->  {
                if(id.projectPath != ":") {
                    logger.warn("Binary libs components are currently not implemented.")
                    with(rootProject.project(id.projectPath)) {
                        depReturnValue = DependencyConfig(group.toString(), name ?: id.libraryName,
                                version.toString(), id.projectPath)
                    }
                }
            }
        }
        return depReturnValue
    }
}
