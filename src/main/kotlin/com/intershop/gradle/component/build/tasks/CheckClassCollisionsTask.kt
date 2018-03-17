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
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.ForkMode
import org.gradle.workers.IsolationMode
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

/**
 * The task implementation for the check class collision task.
 */
open class CheckClassCollisionsTask @Inject constructor(private val workerExecutor: WorkerExecutor): DefaultTask() {

    private val reportOutputProperty: RegularFileProperty = this.newOutputFile()

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
     * Set of all configured excludes patterns.
     *
     * @property excludes set of exclude patterns
     */
    @get:Internal
    var excludes: Set<DependencyConfig> = mutableSetOf()

    /**
     * Set of special exclude patterns for the
     * class collision check.
     *
     * @property collisionExcludes set of exclude patterns
     */
    @get:Internal
    var collisionExcludes: Set<DependencyConfig> = mutableSetOf()

    /**
     * Set of excluded class patterns for the class
     * collision check.
     *
     * @property excludedClasses set of class patterns
     */
    @get:Internal
    var excludedClasses: Set<String> = mutableSetOf()

    /**
     * The task action starts a runner in the backround.
     */
    @Suppress("unused")
    @TaskAction
    fun verify() {
        val processor = DependencyJarProcessor(
                project.configurations,
                project.dependencies,
                excludes,
                collisionExcludes)
        val jarFileList = processor.collectJarFiles(moduleSet, libSet)

        workerExecutor.submit(ClassCollisionRunner::class.java, {
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
