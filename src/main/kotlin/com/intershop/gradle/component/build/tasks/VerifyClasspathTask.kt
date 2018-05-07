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

import com.intershop.gradle.component.build.extension.items.LibraryItem
import com.intershop.gradle.component.build.extension.items.ModuleItem
import com.intershop.gradle.component.build.utils.DependencyConfig
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.workers.ForkMode
import org.gradle.workers.IsolationMode
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

/**
 * The task implementation for the check class collision task.
 */
open class VerifyClasspathTask @Inject constructor(private val workerExecutor: WorkerExecutor): DefaultTask() {

    private val reportOutputProperty: RegularFileProperty = this.newOutputFile()

    private val dependencyJarManager = DependencyJarManager(project)

    /**
     * This is the output file with the report list of this task.
     *
     * @property reportOutput the file property
     */
    @get:OutputFile
    var reportOutput: File
        get() = reportOutputProperty.get().asFile
        set(value)= reportOutputProperty.set(value)

    /**
     * Projects will be handled incremental without considering build
     * configuration of the project. the parameter '--recreate' will enable an
     * automatic recreation if project dependencies are available.
     *
     * @property recreate holds the property for the parameter
     */
    @Suppress("unused")
    @set:Option(option = "recreate",
            description = "Runs 'Component Build' tasks without considering incremental configuration.")
    @get:Internal
    var recreate: Boolean = false

    /**
     * Set of all configured modules.
     *
     * @property moduleSet set of all modules
     */
    @get:Internal
    var moduleSet: Set<ModuleItem> = mutableSetOf()

    /**
     * Set of all configured libs.
     *
     * @property libSet set of all libs
     */
    @get:Internal
    var libSet: Set<LibraryItem> = mutableSetOf()

    /**
     * Set of all configured dependencyExcludes patterns.
     *
     * @property excludes set of exclude patterns
     */
    @get:Nested
    var excludes: Set<DependencyConfig> = mutableSetOf()

    /**
     * Set of special exclude patterns for the
     * class collision check.
     *
     * @property collisionExcludes set of exclude patterns
     */
    @get:Nested
    var collisionExcludes: Set<DependencyConfig> = mutableSetOf()

    /**
     * Set of excluded class patterns for the class
     * collision check.
     *
     * @property excludedClasses set of class patterns
     */
    @get:Input
    var excludedClasses: Set<String> = mutableSetOf()

    /**
     * Contains all resolved configured dependencies.
     * @property resolvedDependencies resolved dependency configurations
     */
    @get:Nested
    @Suppress("unused")
    val resolvedDependencies: Set<DependencyConfig>
        get() {
            val deps = dependencyJarManager.getDependencies( libSet, moduleSet, excludes)

            this.outputs.upToDateWhen {
                deps.none {
                    it.version.endsWith("SNAPSHOT") ||
                            it.version.endsWith("LOCAL") ||
                            (it.dependency.isNotBlank() && recreate)
                }
            }
            return dependencyJarManager.getDependencies(libSet, moduleSet, excludes)
        }

    /**
     * The task action starts a runner in the background.
     */
    @Suppress("unused")
    @TaskAction
    fun verify() {

        val jarFileList = dependencyJarManager.collectJarFiles(excludes, collisionExcludes)

        workerExecutor.submit(VerifyClasspathRunner::class.java, {
            it.displayName = "'Check jars for class collisions.'"
            it.setParams(
                    jarFileList,
                    excludedClasses,
                    reportOutput)
            it.isolationMode = IsolationMode.CLASSLOADER
            it.forkMode = ForkMode.AUTO
        })

        workerExecutor.await()
    }
}
