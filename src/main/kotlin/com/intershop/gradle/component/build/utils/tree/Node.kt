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
package com.intershop.gradle.component.build.utils.tree

/**
 * An implementation for a tree data to configure
 * the target configuration.
 *
 * @param name name of the tree node
 */
class Node(val name: String, val environment: String, val classifier: String) {

    /**
     * If this value is true the node is a configured target item.
     *
     * @property target true if the tree node a configured a target.
     */
    var target:Boolean = false

    /**
     * The parent node of this node.
     *
     * @property parent the parent object.
     */
    var parent: Node? = null

    /**
     * Set of child objects of this tree node.
     *
     * @property children set of child tree nodes
     */
    var children:MutableSet<Node> = mutableSetOf()

    /**
     * Add a child node to the current node, if the
     * node does not exists in the list.
     *
     * @param node tree node instance
     *
     * @return the node that was added or the available node
     */
    fun addChild(node: Node) : Node {
        var rnode: Node? = children.find { it.name == node.name &&
                it.environment == node.environment &&
                (it.classifier == node.classifier || it.classifier == "")}

        if(rnode == null) {
            children.add(node)
            rnode = node
        }

        rnode.parent = this
        return rnode
    }

    /**
     * Add target path elements to the current node.
     *
     * @param pathEntry a list of path elements.
     *
     * @return the latest added node.
     */
    fun addPath(environment: String, classifier: String, vararg pathEntry: String): Node {
        var node = this
        pathEntry.forEach {
            if(it.isNotBlank()) {
                it.split("/").forEach {
                    node = node.addChild(Node(it, environment, classifier))
                }
            }
        }
        return node
    }

    fun addTarget(environment: String, classifier: String, vararg pathEntry: String): Triple<Node,AddStatus,String> {
        var state = AddStatus.ADDED
        var msg = ""
        val node = addPath(environment, classifier, *pathEntry)
        if(node.target) {
            state = AddStatus.IDENTICAL
        } else {
            node.target = true

            var pNode: Node? = node.parent
            while(pNode != null) {
                if(pNode.target) {
                    state = AddStatus.NOTSELFCONTAINED
                    msg = "There is configured parent target '${pNode.getPath()}' in the path '${node.getPath()}'"
                    break
                } else {
                    pNode = pNode.parent
                }
            }

            if(node.children.isNotEmpty() && state != AddStatus.NOTSELFCONTAINED) {
                val doubleNodes = mutableSetOf<Node>()
                recursiveChildCheck(this.children, doubleNodes)
                if(doubleNodes.isNotEmpty()) {
                    state = AddStatus.NOTSELFCONTAINED
                    msg = "There is configured child target '${doubleNodes.first().getPath()}' in the path '${node.getPath()}'"
                }
            }
        }
        return Triple(node, state, msg)
    }

    fun addTarget(environment: Set<String>, classifier: String, vararg pathEntry: String): Triple<Node,AddStatus,String> {
        return if(environment.isEmpty()) {
            addTarget("", classifier, *pathEntry)
        } else {
            val rs: MutableSet<Triple<Node,AddStatus,String>> = mutableSetOf()
            environment.forEach {
                val r = addTarget(it, classifier, *pathEntry)
                rs.add(r)
                if (r.second != AddStatus.ADDED) {
                    return@forEach
                }
            }
            rs.last()
        }
    }

    fun addTarget(environment: Set<String>, classifier: Set<String>, vararg pathEntry: String): Triple<Node,AddStatus,String> {
        return if(classifier.isEmpty()) {
            addTarget(environment, "", *pathEntry)
        } else {
            val rs: MutableSet<Triple<Node,AddStatus,String>> = mutableSetOf()
            classifier.forEach {
                val r = addTarget(environment, it, *pathEntry)
                rs.add(r)
                if (r.second != AddStatus.ADDED) {
                    return@forEach
                }
            }
            rs.last()
        }
    }

    /**
     * Returns the name of the node.
     *
     * @return the string representation of the node
     */
    override fun toString(): String {
        return name
    }

    /**
     * Returns the path of a this node.
     *
     * @return path of the current node.
     */
    fun getPath(): String {
        val reversPath =  mutableListOf<String>()
        reversPath.add(name)
        var p = parent

        while(p != null) {
            reversPath.add(p.toString())
            p = p.parent
        }
        reversPath.reverse()
        return reversPath.joinToString("/")
    }

    /**
     * Verify if the name a child of the current node.
     *
     * @param childname the name of the searched child
     *
     * @return true if the name is a child
     */
    fun isChild(childname: String): Boolean {
        return children.any { it.name == childname }
    }

    /**
     * Returns the tree node with the given child
     * name or null, if the child name is not available.
     *
     * @param childname name of the searched child
     *
     * @return the node or null
     */
    fun getChild(childname: String): Node? {
        return children.find { it.name == childname }
    }

    private fun recursiveChildCheck(children: Set<Node>, rValues: MutableSet<Node>) {
        if(children.isNotEmpty() && rValues.isEmpty()) {
            children.forEach {
                if(it.target) {
                    rValues.add(it)
                }
                if(! rValues.isEmpty()) {
                    return@forEach
                } else {
                    recursiveChildCheck(it.children, rValues)
                }
            }
        }
    }
}