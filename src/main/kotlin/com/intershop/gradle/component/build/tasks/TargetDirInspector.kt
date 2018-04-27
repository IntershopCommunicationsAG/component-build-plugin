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

import com.intershop.gradle.component.build.utils.tree.AddStatus
import com.intershop.gradle.component.build.utils.tree.Node
import com.intershop.gradle.component.descriptor.Component
import org.slf4j.LoggerFactory

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
        internal val logger = LoggerFactory.getLogger(TargetDirInspector::class.java.simpleName)
    }

    private val root = Node("componentRootDir","", "")

    /**
     * Runs the test for all directories of a component.
     *
     * @return the error message if the test fails
     */
    fun check() : String {
        val errorMsg = StringBuilder()

        var rv = root.addTarget("", "", component.descriptorPath)
        when(rv.second) {
            AddStatus.IDENTICAL -> errorMsg.append(getIdenticalMsg(rv, "descriptor", ""))
            AddStatus.NOTSELFCONTAINED -> errorMsg.append(getSelfcontainedMsg(rv, "descriptor", ""))
            else -> logger.debug("Descriptor path {} was ok.", rv.first.getPath())
        }

        if(component.libs.isNotEmpty()) {
            rv = root.addTarget("", "", component.libsPath)
            when (rv.second) {
                AddStatus.IDENTICAL -> errorMsg.append(getIdenticalMsg(rv, "libraries", ""))
                AddStatus.NOTSELFCONTAINED -> errorMsg.append(getSelfcontainedMsg(rv, "libraries", ""))
                else -> logger.debug("Libraries path {} was ok.", rv.first.getPath())
            }
        }

        component.modules.forEach {
            rv = root.addTarget(it.value.types, it.value.classifiers, component.modulesPath, it.key)
            when(rv.second) {
                AddStatus.IDENTICAL -> {
                    errorMsg.append(getIdenticalMsg(rv, "module", it.value.dependency.toString()))
                }
                AddStatus.NOTSELFCONTAINED -> {
                    errorMsg.append(getSelfcontainedMsg(rv, "module", it.value.dependency.toString()))
                }
                else -> logger.debug("Module {} path {} was ok.", it.value.dependency, rv.first.getPath())
            }
        }

        component.fileContainers.forEach {
            rv = root.addTarget(it.types, it.classifier, component.containerPath,it.targetPath)
            when(rv.second) {
                AddStatus.IDENTICAL -> errorMsg.append(getIdenticalMsg(rv, "container", it.name))
                AddStatus.NOTSELFCONTAINED -> errorMsg.append(getSelfcontainedMsg(rv, "container", it.name))
                else -> logger.debug("Container {} path {} was ok.", it.name, rv.first.getPath())
            }
        }

        component.linkItems.forEach {
            rv = root.addTarget(it.types, it.classifier, it.name)
            when(rv.second) {
                AddStatus.IDENTICAL -> errorMsg.append(getIdenticalMsg(rv, "link", it.name))
                AddStatus.NOTSELFCONTAINED -> errorMsg.append(getSelfcontainedMsg(rv, "link", it.name))
                else -> logger.debug("Link {} path {} was ok.", it.name, rv.first.getPath())
            }
        }

        component.directoryItems.forEach {
            rv = root.addTarget(it.types, it.classifier, it.targetPath)
            when(rv.second) {
                AddStatus.IDENTICAL -> errorMsg.append(getIdenticalMsg(rv, "directory", it.targetPath))
                AddStatus.NOTSELFCONTAINED -> errorMsg.append(getSelfcontainedMsg(rv, "link", it.targetPath))
                else -> logger.debug("Directory {} path {} was ok.", it.targetPath, rv.first.getPath())
            }
        }

        return errorMsg.toString()
    }

    private fun getIdenticalMsg(result: Triple<Node,AddStatus,String>, type: String, name: String) : String {
        return "The path '${result.first.getPath()}' of $type '$name' was defined by an other component artifact! \n"
    }

    private fun getSelfcontainedMsg(result: Triple<Node,AddStatus,String>, type: String, name: String): String {
        return "The path '${result.first.getPath()}' of $type '${name}' exists. ${result.third} \n"
    }
}
