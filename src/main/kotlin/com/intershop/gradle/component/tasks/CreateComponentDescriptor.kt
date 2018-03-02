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
package com.intershop.gradle.component.tasks

import com.intershop.gradle.component.getValue
import com.intershop.gradle.component.setValue
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

open class CreateComponentDescriptor @Inject constructor(private val dependencyHandler: DependencyHandler) : DefaultTask() {

    // Outputfile configuration
    private val descriptorFileProperty = this.newOutputFile()

    @get:OutputFile
    var descriptorFile: File
        get() = descriptorFileProperty.get().asFile
        set(value) = descriptorFileProperty.set(value)

    fun provideDescriptorFile(descriptorFile: Provider<RegularFile>)
            = descriptorFileProperty.set(descriptorFile)

    // display name configuration
    private val displayNameProperty = project.objects.property(String::class.java)

    @get:Input
    var displayName: String by displayNameProperty

    fun provideDisplayName(displayName: Provider<String>)
            = displayNameProperty.set(displayName)

    // component description configuration
    private val componentDescriptionProperty = project.objects.property(String::class.java)

    @get:Input
    var componentDescription: String by componentDescriptionProperty

    fun provideComponentDescription(description: Provider<String>)
            = componentDescriptionProperty.set(description)

    // module dependencies configuration
    private val moduleDependenciesProperty = project.objects.setProperty(Any::class.java)

    @get:Input
    var moduleDependencies by moduleDependenciesProperty

    fun provideModuleDependencies(moduleDependencies: Provider<Set<Any>>)
        = moduleDependenciesProperty.set(moduleDependencies)

    // library dependencies configuration
    private val libraryDependenciesProperty = project.objects.setProperty(Any::class.java)

    @get:Input
    var libraryDependencies by libraryDependenciesProperty

    fun provideLibraryDependencies(librayDependencies: Provider<Set<Any>>)
            = libraryDependenciesProperty.set(librayDependencies)

    @TaskAction
    fun createDescriptor() {
        println(displayName)
        println(componentDescription)

        moduleDependencies.forEach( {
            println(dependencyHandler.create(it))
        })

        libraryDependencies.forEach( {
            println(dependencyHandler.create(it))
        })
    }

}