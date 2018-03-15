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

import com.intershop.gradle.component.build.extension.ComponentExtension
import com.intershop.gradle.component.build.utils.getValue
import com.intershop.gradle.component.build.utils.setValue
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Zip
import java.io.File

/**
 * This class provides a preconfigured Zip task
 * to prepare the container for a component.
 */
open class ZipContainer : Zip() {

    private val inputFilesProperty = project.files()

    private val artifactBaseNameProperty = project.objects.property(String::class.java)
    private val artifactAppendixProperty = project.objects.property(String::class.java)
    private val artifactClassifierProperty = project.objects.property(String::class.java)

    /**
     * The path of the zip package.
     *
     * @return the file representation of the container.
     */
    @OutputFile
    override fun getArchivePath(): File {
        baseName = if(artifactBaseNameProperty.isPresent) artifactBaseName  else  ""
        appendix = if(artifactAppendixProperty.isPresent) artifactAppendix  else  ""
        classifier = if(artifactClassifierProperty.isPresent) artifactClassifier  else  ""

        val path = StringBuilder()
        path.append(ComponentExtension.CONTAINER_OUTPUTDIR)

        if (!appendix.isEmpty()) {
            path.append("/").append(appendix)
        }
        if (!classifier.isEmpty()) {
            path.append("_").append(classifier)
        }

        destinationDir = project.layout.buildDirectory.dir(path.toString()).get().asFile

        return File(destinationDir, archiveName)
    }

    /**
     * Input files for packaging.
     *
     * @property inputFiles file collection with all files
     */
    @get:InputFiles
    val inputFiles: FileCollection
        get() {
        // necessary to  trigger the Zip task!
        from(inputFilesProperty)
        return inputFilesProperty
    }

    /**
     * Base name of the artifact.
     *
     * @property artifactBaseName the base name
     */
    @get:Input
    var artifactBaseName by artifactBaseNameProperty

    /**
     * Appendix string of the artifact.
     *
     * @property artifactAppendix the appendix string
     */
    @get:Input
    var artifactAppendix by artifactAppendixProperty

    /**
     * Classifier string of the artifact.
     *
     * @property artifactClassifier the classifier string
     */
    @get:Input
    var artifactClassifier by artifactClassifierProperty

    /**
     * Calls the zip action to create the package.
     */
    @TaskAction
    @Suppress("unused", "MagicNumber")
    fun action() {
        // default configuration for component zip
        includeEmptyDirs = true
        setDuplicatesStrategy(DuplicatesStrategy.FAIL)

        // only three digits are configured (mask is 0000)
        fileMode = 640
        dirMode = 750

        // call super action ...
        copy()
    }
}
