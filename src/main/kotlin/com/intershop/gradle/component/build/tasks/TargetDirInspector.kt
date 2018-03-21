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

import com.intershop.gradle.component.build.utils.TargetDirInfo
import com.intershop.gradle.component.descriptor.Component

/**
 * This class verifies that directories of the configured
 * component are unique.
 *
 * @property component generated Component descriptor
 *
 * @constructor provides a preconfigured class
 */
class TargetDirInspector(val component: Component) {

    companion object {
        private fun checkInputForSuffix(input: String, suffix: String) : String {
            return if(input.endsWith(suffix) || input.isBlank()) {
                input
            } else {
                input.trim() + "/"
            }
        }
    }

    private val containerTarget = checkInputForSuffix(component.containerTarget,"/")
    private val libsTarget = checkInputForSuffix(component.libsTarget,"/")
    private val modulesTarget = checkInputForSuffix(component.modulesTarget,"/")

    private val targetDirInfos = mutableSetOf<TargetDirInfo>()

    /**
     * Runs the test for all directories of a component.
     *
     * @return the error message if the test fails
     */
    fun check(): String {
        var errorMsg = ""

        addTarget(libsTarget, "", mutableSetOf(), "Libraries target")

        if(containerTarget.startsWith(libsTarget) && containerTarget.isNotBlank()) {
            errorMsg = "The target of containers is located in the library folder. This is not allowed!"
        }

        if( modulesTarget.startsWith(libsTarget) && modulesTarget.isNotBlank() && errorMsg.isBlank()) {
            errorMsg = "The target of modules is located in the library folder. This is not allowed!"
        }

        if(errorMsg.isBlank()) {
            component.modules.forEach {
                    errorMsg = addTarget("$modulesTarget${it.key}/", "",
                            it.value.types, "Module ${it.key}")
                    ! errorMsg.isBlank()
            }
        }

        if(errorMsg.isBlank()) {
            component.fileContainers.any {
                errorMsg = addTarget("$containerTarget${it.targetPath}/", it.classifier,
                        it.types, "Container ${it.name} (${it.containerType})")
                ! errorMsg.isBlank()
            }
        }

        return errorMsg
    }

    private fun addTarget(target: String,
                          classifier: String,
                          types: MutableSet<String>,
                          owner: String) : String {

        var errorMsg = ""

        val targetInfos: List<TargetDirInfo> = targetDirInfos.filter { it.target == target }

        if(targetInfos.isEmpty()) {
            val similarTargetInfo: TargetDirInfo? = targetDirInfos.find { it.target.startsWith(target) }

            if (similarTargetInfo != null) {
                with(similarTargetInfo) {
                    errorMsg = "The target dir '$target' of '$owner' starts with '$target' of '$owner'."
                }
            }
        }

            if (errorMsg.isBlank() && targetInfos.isNotEmpty()) {
                targetInfos.forEach {
                    if(it.classifiers.contains(classifier)) {

                        if (it.types.isEmpty() && types.isEmpty()) {
                            errorMsg = "The target dir '$target' of '$owner' exists always in the list. " +
                                    "See '${it.target}' of '${it.owner}' ($classifier)."
                        } else if(it.types.intersect(types).isNotEmpty()) {
                            errorMsg = "The target dir '$target' of '$owner' exists always in the list. " +
                                    "See '${it.target}' of '${it.owner}' ($types)."
                        }
                    }
                }
            }


        if(errorMsg.isBlank()) {
            addNewTarget(target, classifier, types, owner)
        }

        return errorMsg
    }

    private fun addNewTarget(target: String,
                             classifier: String,
                             types: MutableSet<String>,
                             owner: String) {
        val targetInfo = TargetDirInfo(target, mutableSetOf(), mutableSetOf(), owner)
        targetInfo.classifiers.add(classifier)
        targetInfo.types.addAll(types)
        targetDirInfos.add(targetInfo)
    }
}
