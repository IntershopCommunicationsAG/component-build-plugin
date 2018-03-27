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

import com.intershop.gradle.component.build.utils.JarFileInfo
import com.intershop.gradle.component.build.utils.iterator
import org.gradle.api.InvalidUserDataException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import javax.inject.Inject

/**
 * Runner for the class collision check.
 *
 * @property jarFiles set of object with jar files and dependency information
 * @property excludedClasses set of patterns of excluded classes
 * @property outputFile the output file of the report task
 *
 * @constructor provides the runner of the CheckClassCollisions task
 */
open class VerifyClasspathRunner @Inject constructor(private val jarFiles: Set<JarFileInfo>,
                                                     private val excludedClasses: Set<String>,
                                                     private val outputFile: File) : Runnable {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(VerifyClasspathRunner::class.java.name)
    }

    /**
     * The runner function with the main functionality.
     */
    @Throws(InvalidUserDataException::class)
    override fun run() {
        val data = readJars(jarFiles)
        val duplicates = data.calculateDuplicates()

        if(duplicates.isNotEmpty()) {
            outputFile.parentFile.mkdirs()
            outputFile.delete()
            outputFile.createNewFile()

            logger.info("Start creation of class collision report to {}", outputFile)
            duplicates.values.flatten().toSet().forEach { info ->
                with(info) {
                    outputFile.appendText("\n\n== Duplicates for $dependency " +
                            "${if (parentDependency.isNotBlank()) " from " else ""}$parentDependency\n\n")
                }
                // duplicated classes
                duplicates.filterValues { it.contains(info) }.forEach { entry ->
                    outputFile.appendText(" - ${entry.key} also in " +
                            "${entry.value.filterNot { it == info }.joinToString { it.dependency }} \n")
                }
            }
            throw InvalidUserDataException("There are class collisions! Check " + outputFile.absolutePath)
        } else {
            logger.debug("No class collisions!")
        }
    }

    private fun readJars(jarFiles: Set<JarFileInfo>) : SimpleMultiMap {
        val threads = Runtime.getRuntime().availableProcessors()
        val exec = Executors.newFixedThreadPool(threads)
        val data = SimpleMultiMap()

        jarFiles.forEach {
            exec.execute {
                val classes = readSingleJar(it.jarFile)

                classes.forEach {clazz ->
                    data.put(clazz, it)
                }
            }
        }

        exec.shutdown()
        assert(exec.awaitTermination(1, TimeUnit.MINUTES))

        return data
    }

    private fun readSingleJar(jar: File) : Collection<String> {
        val content: MutableList<String> = mutableListOf()

        val zipIn = ZipInputStream(jar.inputStream())
        for(entry in zipIn) {
            if(entry.name.endsWith(".class")) {
                if(excludedClasses.none { it.toRegex().matches(entry.name) }) {
                    content.add(entry.name)
                }
            }
        }

        return content.filter { entry -> excludedClasses.filterNot { it.toRegex().matches(entry) }.isEmpty() }
    }

    private class SimpleMultiMap {
        private val dataMap: MutableMap<String, MutableSet<JarFileInfo>> = mutableMapOf()

        @Synchronized
        internal fun put(clazz: String, fileInfo: JarFileInfo) {
            val info = dataMap.getOrDefault(clazz, mutableSetOf())
            info.add(fileInfo)
            dataMap[clazz] = info
        }

        @Synchronized
        internal fun calculateDuplicates() : Map<String, MutableSet<JarFileInfo>> {
            return dataMap.filter { it.value.size > 1 }
        }
    }
}
