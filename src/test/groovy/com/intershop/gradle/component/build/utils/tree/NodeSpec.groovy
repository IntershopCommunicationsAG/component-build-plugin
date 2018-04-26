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

import com.intershop.gradle.component.build.utils.tree.Node
import spock.lang.Specification

class NodeSpec extends Specification {

    def "test tree add path to root"()  {
        setup:
        def root = new Node('root', '','')

        when:
        def child = root.addPath('production', '',"child1/child2/child3")

        then:
        child.getPath() == "root/child1/child2/child3"
    }

    def "test tree add path elemensts"() {
        setup:
        def root = new Node('root', '','')

        when:
        def child = root.addPath('production', '',"child1/child2", "child3", "", "child4/child5", "child6", "")

        then:
        child.getPath() == "root/child1/child2/child3/child4/child5/child6"
    }

    def "test target analysis - ok"() {
        setup:
        def root = new Node('root', '','')
        root.addPath('production', '',"modules/module1").target = true
        root.addPath('production', '',"modules/module2").target = true

        when:
        def child = root.addTarget('production', '',"modules/log")

        then:
        child.first.target == true
        child.second == AddStatus.ADDED
        child.third == ""
    }

    def "test target analysis - found node - parent"() {
        setup:
        def root = new Node('root', '','')
        def parent1 = root.addPath('production', '',"modules/module1")
        parent1.target = true
        root.addPath('production', '', "modules/module2").target = true

        when:
        def child = root.addTarget('production', '',"modules/module1/share/system/log")

        then:
        child.second == AddStatus.NOTSELFCONTAINED
        child.third != ""
    }

    def "test target analysis - found node - children"() {
        setup:
        def root = new Node('root', '','')
        def parent1 = root.addPath('production', '',"modules/module1/share/system/log")
        parent1.target = true
        root.addPath('production', '',"modules/module2").target = true

        when:
        def child = root.addTarget('production', '',"modules/module1")

        then:
        child.second == AddStatus.NOTSELFCONTAINED
        child.third != ""
    }

    def "test target analysis - environments"() {
        setup:
        def root = new Node('root', '','')
        def parent1 = root.addTarget('test', '',"modules/module1/share/system/log")


        when:
        def child = root.addTarget('production', '',"modules/module1/share/system/log")

        then:
        child.second == AddStatus.ADDED
        child.third == ""
    }

    def "test target analysis - environments, diff classifier"() {
        setup:
        def root = new Node('root', '','')
        def parent1 = root.addTarget('test', 'linux',"modules/module1/share/system/log")

        when:
        def child = root.addTarget('test', 'win',"modules/module1/share/system/log")

        then:
        child.second == AddStatus.ADDED
        child.third == ""
    }

    def "test target analysis - environments, same classifier"() {
        setup:
        def root = new Node('root', '','')
        def parent1 = root.addTarget('test', 'linux',"modules/module1/share/system/log")

        when:
        def child = root.addTarget('test', 'linux',"modules/module1/share/system/log")

        then:
        child.second == AddStatus.IDENTICAL
        child.third == ""
    }

    def "test target analysis - environment set, classifier set"() {
        setup:
        def root = new Node('root', '','')

        when:
        def child = root.addTarget(['test', 'production'] as Set<String>, ['linux', 'win'] as Set<String>,"modules/module1/share/system/log")

        then:
        child.second == AddStatus.ADDED
        child.third == ""
    }

    def "test target analysis - environment set, classifier set - failed"() {
        setup:
        def root = new Node('root', '','')
        root.addTarget(['test', 'production'] as Set<String>, ['linux', 'win'] as Set<String>,"modules/module1/share/system/log")

        when:
        def child = root.addTarget(['production'] as Set<String>, ['win'] as Set<String>,"modules/module1/share")

        then:
        child.second == AddStatus.NOTSELFCONTAINED
        child.third != ""
    }
}
