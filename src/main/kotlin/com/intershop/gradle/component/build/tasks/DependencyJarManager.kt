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
import com.intershop.gradle.component.build.utils.JarFileInfo
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.artifacts.dsl.DependencyHandler

/**
 * This class contains all methods for the calculation of all
 * information about the configurated jar files.
 *
 * @property project the current project instance
 *
 * @constructor provides a class to collect all jars with necessary information
 */
class DependencyJarManager (val project: Project) {

    private var dependencyHandler: DependencyHandler = project.dependencies
    private var configurations: ConfigurationContainer = project.configurations

    private val procDeps: MutableMap<DependencyConfig, MutableSet<JarFileInfo>> = mutableMapOf()

    private var dependenciesInitialized = false

    /**
     * Returns all resolved dependencies from configuration.
     *
     * @param libItems set of configured library items
     * @param moduleItems set of configured module items
     *
     * @return set of resolved dependency configurations
     */
    fun getDependencies(libItems: Set<LibraryItem>,
                        moduleItems: Set<ModuleItem>,
                        excludes: Set<DependencyConfig>): Set<DependencyConfig> {
        if(! dependenciesInitialized) {
            // add configured dependencies
            libItems.forEach {
                if(!addDependencyObjects(it, excludes)) {
                    procDeps.clear()
                    throw GradleException("Could not resolve library dependency for ${it.dependency.getModuleString()}")
                }
            }
            moduleItems.forEach {
                if(!addDependencyObjects(it, excludes)) {
                    procDeps.clear()
                    throw GradleException("Could not resolve module dependency for ${it.dependency.getModuleString()}")
                }
            }
            dependenciesInitialized = true
        }
        return procDeps.keys
    }

    /**
     * Collect all jars with dependencies information from the configured
     * parameters.
     *
     * @param excludes set of exclude patterns
     * @param collisionExcludes set of exclude patterns
     */
    fun collectJarFiles(excludes: Set<DependencyConfig>,
                        collisionExcludes: Set<DependencyConfig>): MutableSet<JarFileInfo> {
        val jarFileInfos = mutableSetOf<JarFileInfo>()

        procDeps.forEach{
            jarFileInfos.addAll(it.value)
            val conf = configurationFor(it.key, excludes, it.key.transitive)

            addTransitiveDependencies(conf, jarFileInfos, collisionExcludes)
        }

        return jarFileInfos
    }

    private fun addDependencyObjects(item: IDependency, excludes: Set<DependencyConfig>): Boolean {
        val conf = configurationFor(item.dependency, excludes, item.resolveTransitive)

        val jarFileSet = mutableSetOf<JarFileInfo>()

        return try {
            with(conf.resolvedConfiguration.firstLevelModuleDependencies.first()) {
                val dependencyConf = DependencyConfig(moduleGroup, moduleName,
                        moduleVersion, "", item.resolveTransitive)

                moduleArtifacts.filter({ it.type == "jar" }).forEach {
                    jarFileSet.add(JarFileInfo(dependencyConf.getModuleString(), "", it.file))
                }
                procDeps.put(dependencyConf, jarFileSet)
            }
            true
        } catch (ex: ResolveException) {
            false
        }
    }

    private fun addTransitiveDependencies(conf: Configuration,
                                          jarFileSet: MutableSet<JarFileInfo>,
                                          collisionExcludes: Set<DependencyConfig>) {

        conf.incoming.resolutionResult.allDependencies.forEach { resDep ->
            if(resDep is ResolvedDependency) {
                val dep = DependencyConfig(resDep.moduleGroup, resDep.moduleName, resDep.moduleVersion)

                if(! procDeps.keys.contains(dep) && isNotExcluded(dep, collisionExcludes)) {
                    resDep.moduleArtifacts.filter({ it.type == "jar" }).forEach {
                        jarFileSet.add(JarFileInfo(dep.getModuleString(), resDep.from.toString(), it.file))
                    }
                }
            }
        }
    }

    private fun isNotExcluded(dep: DependencyConfig,
                              collisionExcludes: Set<DependencyConfig>) : Boolean {

        return collisionExcludes.none { it.group.toRegex().matches(dep.group)
                && it.module.toRegex().matches(dep.module)
                && it.version.toRegex().matches(dep.version) }
    }

    private fun configurationFor(dependency: DependencyConfig,
                                 excludes: Set<DependencyConfig>,
                                 transitive: Boolean) : Configuration {
        val conf = configurations.detachedConfiguration(dependencyHandler.create(dependency.getModuleString()))

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

}
