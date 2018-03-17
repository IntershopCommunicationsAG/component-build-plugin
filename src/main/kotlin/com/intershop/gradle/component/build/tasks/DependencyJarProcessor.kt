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
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.artifacts.dsl.DependencyHandler

/**
 * This class contains all methods for the calculation of all
 * information about the configurated jar files.
 *
 * @property configurations ConfigurationHandler of this project
 * @property dependencyHandler DependencyHandler of this project
 * @property excludes set of dependency patterns for excluding dependencies
 * @property collisionExcludes set of dependency patterns for excluding dependencies for check
 *
 * @constructor provides a processor for class collision check
 */
class DependencyJarProcessor (private val configurations: ConfigurationContainer,
                              private val dependencyHandler: DependencyHandler,
                              private val excludes: Set<DependencyConfig>,
                              private var collisionExcludes: Set<DependencyConfig>) {

    private val procDeps: MutableSet<DependencyConfig> = mutableSetOf()
    private var inputDeps: MutableSet<DependencyConfig> = mutableSetOf()

    /**
     * Collect all jars with dependencies information from the configured
     * parameters.
     *
     * @param modules set of module items
     * @param libs set of library items
     */
    fun collectJarFiles(modules: Set<ModuleItem>?, libs: Set<LibraryItem>?): MutableSet<JarFileInfo> {
        procDeps.clear()
        inputDeps.clear()

        if(libs != null) {
            inputDeps.addAll(libs.map { it.dependency }.asIterable())
        }
        if(modules != null) {
            inputDeps.addAll(modules.map { it.dependency }.asIterable())
        }

        val jarFiles: MutableSet<JarFileInfo> = mutableSetOf()

        // add configured dependencies
        libs?.forEach {
            addDependencyObjects(it, jarFiles)
        }

        modules?.forEach {
            addDependencyObjects(it, jarFiles)
        }

        return jarFiles
    }

    private fun addDependencyObjects(item: IDependency, jarFileSet: MutableSet<JarFileInfo>) {
        val conf = configurationFor(item.dependency, item.resolveTransitive)

        with(conf.resolvedConfiguration.firstLevelModuleDependencies.first()) {
            moduleArtifacts.filter({ it.type == "jar" }).forEach {
                jarFileSet.add(JarFileInfo(item.dependency.moduleString, "", it.file))
            }
        }
        procDeps.add(item.dependency)
        addTransitiveDependencies(conf, jarFileSet)
    }

    private fun addTransitiveDependencies(conf: Configuration, jarFileSet: MutableSet<JarFileInfo>) {
        conf.incoming.resolutionResult.allDependencies.forEach { resDep ->
            if(resDep is ResolvedDependency) {
                val dep = DependencyConfig(resDep.moduleGroup, resDep.moduleName, resDep.moduleName)

                if(! procDeps.contains(dep) && isNotExcluded(dep)) {
                    resDep.moduleArtifacts.filter({ it.type == "jar" }).forEach {
                        jarFileSet.add(JarFileInfo(dep.moduleString, resDep.from.toString(), it.file))
                    }
                    procDeps.add(dep)
                }
            }
        }
    }

    private fun isNotExcluded(dep: DependencyConfig) : Boolean {

        val confDepIncluded = inputDeps.find { it.group == dep.group && it.module == dep.module } != null

        val collisionExcluded = collisionExcludes.none {
            it.group.toRegex().matches(dep.group)
                    && it.module.toRegex().matches(dep.module)
                    && it.version.toRegex().matches(dep.version)
        }

        return confDepIncluded || collisionExcluded || excludes.none {
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
}
