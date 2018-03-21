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

import com.intershop.gradle.component.descriptor.*
import spock.lang.Specification

class TargetDirInspectorSpec extends Specification {

    def 'Test TargetDirInspector - HappyPath without external configuration'() {
        when:
            Set<String> types = [] as Set<String>
            Set<String> classifiers = [] as Set<String>

            Map<String, Module> modules= [:]
            Map<String, Library> libs = [:]
            Set<FileContainer> fileContainers = []

            Component comp = new Component("testdescriptor", "",
                    types,  classifiers,
                    "modules",
                    "modules/libs",
                    "properties",
                    "container",
                    "defaultTarget",
                     modules,  libs, fileContainers
            )
            TargetDirInspector inspector = new TargetDirInspector(comp)

        then:
            inspector.check() == ""
    }

    def 'Test TargetDirInspector - HappyPath with external configuration'() {
        when:
        Set<String> types = [] as Set<String>
        Set<String> classifiers = [] as Set<String>

        Map<String, Module> modules= [:]
        modules.put("core", new Module("core", "core", new Dependency("com.intershop", "core", "1.0.0")))
        modules.put("isml", new Module("isml", "isml", new Dependency("com.intershop", "isml", "1.0.0")))

        Map<String, Library> libs = [:]
        libs.put("com.intershop_lib1_1.0.0", new Library(new Dependency("com.intershop", "lib1", "1.0.0"), "com.intershop_lib1_1.0.0"))
        libs.put("com.intershop_lib2_1.0.0", new Library(new Dependency("com.intershop", "lib2", "1.0.0"), "com.intershop_lib2_1.0.0"))

        Set<FileContainer> fileContainers = []
        fileContainers.add(new FileContainer("container1", "container1", "bin"))

        Component comp = new Component("testdescriptor", "",
                types,  classifiers,
                "modules",
                "modules/libs",
                "properties",
                "container",
                "defaultTarget",
                modules,  libs, fileContainers
        )
        TargetDirInspector inspector = new TargetDirInspector(comp)

        then:
        inspector.check() == ""
    }

    def 'Module and container are located in the same directory'() {
        when:
        Set<String> types = [] as Set<String>
        Set<String> classifiers = [] as Set<String>

        Map<String, Module> modules= [:]
        modules.put("core", new Module("core", "core", new Dependency("com.intershop", "core", "1.0.0")))
        modules.put("isml", new Module("isml", "isml", new Dependency("com.intershop", "isml", "1.0.0")))

        Map<String, Library> libs = [:]
        libs.put("com.intershop_lib1_1.0.0", new Library(new Dependency("com.intershop", "lib1", "1.0.0"), "com.intershop_lib1_1.0.0"))
        libs.put("com.intershop_lib2_1.0.0", new Library(new Dependency("com.intershop", "lib2", "1.0.0"), "com.intershop_lib2_1.0.0"))

        Set<FileContainer> fileContainers = []
        fileContainers.add(new FileContainer("container1", "container1", "bin"))
        fileContainers.add(new FileContainer("container2", "container2", "conf"))

        Component comp = new Component("testdescriptor", "",
                types,  classifiers,
                "same",
                "modules/libs",
                "",
                "same",
                "defaultTarget",
                modules,  libs, fileContainers
        )
        TargetDirInspector inspector = new TargetDirInspector(comp)

        then:
        inspector.check() == ""
    }

    def 'Container with different OS'() {
        when:
        Set<String> types = [] as Set<String>
        Set<String> classifiers = [] as Set<String>

        Map<String, Module> modules= [:]
        modules.put("core", new Module("core", "core", new Dependency("com.intershop", "core", "1.0.0")))
        modules.put("isml", new Module("isml", "isml", new Dependency("com.intershop", "isml", "1.0.0")))

        Map<String, Library> libs = [:]
        libs.put("com.intershop_lib1_1.0.0", new Library(new Dependency("com.intershop", "lib1", "1.0.0"), "com.intershop_lib1_1.0.0"))
        libs.put("com.intershop_lib2_1.0.0", new Library(new Dependency("com.intershop", "lib2", "1.0.0"), "com.intershop_lib2_1.0.0"))

        Set<FileContainer> fileContainers = []
        fileContainers.add(new FileContainer("container1", "container1", "bin", "linux"))
        fileContainers.add(new FileContainer("container1", "container1", "bin", "win"))
        fileContainers.add(new FileContainer("container2", "container2", "conf"))

        Component comp = new Component("testdescriptor", "",
                types,  classifiers,
                "same",
                "modules/libs",
                "",
                "same",
                "defaultTarget",
                modules,  libs, fileContainers
        )
        TargetDirInspector inspector = new TargetDirInspector(comp)

        then:
        inspector.check() == ""
    }

    def 'Container with different OS - failed'() {
        when:
        Set<String> types = [] as Set<String>
        Set<String> classifiers = [] as Set<String>

        Map<String, Module> modules= [:]
        modules.put("core", new Module("core", "core", new Dependency("com.intershop", "core", "1.0.0")))
        modules.put("isml", new Module("isml", "isml", new Dependency("com.intershop", "isml", "1.0.0")))

        Map<String, Library> libs = [:]
        libs.put("com.intershop_lib1_1.0.0", new Library(new Dependency("com.intershop", "lib1", "1.0.0"), "com.intershop_lib1_1.0.0"))
        libs.put("com.intershop_lib2_1.0.0", new Library(new Dependency("com.intershop", "lib2", "1.0.0"), "com.intershop_lib2_1.0.0"))

        Set<FileContainer> fileContainers = []
        fileContainers.add(new FileContainer("container1", "container1", "bin", "linux"))
        fileContainers.add(new FileContainer("container1", "container1", "bin", "win"))
        fileContainers.add(new FileContainer("container2", "container1", "bin", "win"))
        fileContainers.add(new FileContainer("container2", "container2", "conf"))

        Component comp = new Component("testdescriptor", "",
                types,  classifiers,
                "same",
                "modules/libs",
                "",
                "same",
                "defaultTarget",
                modules,  libs, fileContainers
        )
        TargetDirInspector inspector = new TargetDirInspector(comp)

        then:
        inspector.check() != ""
    }

    def 'Container with different OS and types'() {
        when:
        Set<String> types = [] as Set<String>
        Set<String> classifiers = [] as Set<String>

        Map<String, Module> modules= [:]
        modules.put("core", new Module("core", "core", new Dependency("com.intershop", "core", "1.0.0")))
        modules.put("isml", new Module("isml", "isml", new Dependency("com.intershop", "isml", "1.0.0")))

        Map<String, Library> libs = [:]
        libs.put("com.intershop_lib1_1.0.0", new Library(new Dependency("com.intershop", "lib1", "1.0.0"), "com.intershop_lib1_1.0.0"))
        libs.put("com.intershop_lib2_1.0.0", new Library(new Dependency("com.intershop", "lib2", "1.0.0"), "com.intershop_lib2_1.0.0"))

        Set<FileContainer> fileContainers = []
        def fc1 = new FileContainer("container1", "container1", "bin", "linux")
        fileContainers.add(fc1)

        def fc2 = new FileContainer("container1", "container1", "bin", "win")
        fc2.types.add("production")
        fileContainers.add(fc2)

        def fc3 = new FileContainer("container2", "container1", "bin", "win")
        fc3.types.add("test")
        fileContainers.add(fc3)

        def fc4 = new FileContainer("container2", "container2", "conf")
        fileContainers.add(fc4)

        Component comp = new Component("testdescriptor", "",
                types,  classifiers,
                "same",
                "modules/libs",
                "",
                "same",
                "defaultTarget",
                modules,  libs, fileContainers
        )
        TargetDirInspector inspector = new TargetDirInspector(comp)

        then:
        inspector.check() == ""
    }

    def 'Container with different OS and types - failed'() {
        when:
        Set<String> types = [] as Set<String>
        Set<String> classifiers = [] as Set<String>

        Map<String, Module> modules= [:]
        modules.put("core", new Module("core", "core", new Dependency("com.intershop", "core", "1.0.0")))
        modules.put("isml", new Module("isml", "isml", new Dependency("com.intershop", "isml", "1.0.0")))

        Map<String, Library> libs = [:]
        libs.put("com.intershop_lib1_1.0.0", new Library(new Dependency("com.intershop", "lib1", "1.0.0"), "com.intershop_lib1_1.0.0"))
        libs.put("com.intershop_lib2_1.0.0", new Library(new Dependency("com.intershop", "lib2", "1.0.0"), "com.intershop_lib2_1.0.0"))

        Set<FileContainer> fileContainers = []
        def fc1 = new FileContainer("container1", "container1", "bin", "linux")
        fileContainers.add(fc1)

        def fc2 = new FileContainer("container1", "container1", "bin", "win")
        fc2.types.add("production")
        fileContainers.add(fc2)

        def fc3 = new FileContainer("container2", "container1", "bin", "win")
        fc3.types.add("production")
        fileContainers.add(fc3)

        def fc4 = new FileContainer("container2", "container2", "conf")
        fileContainers.add(fc4)

        Component comp = new Component("testdescriptor", "",
                types,  classifiers,
                "same",
                "modules/libs",
                "",
                "same",
                "defaultTarget",
                modules,  libs, fileContainers
        )
        TargetDirInspector inspector = new TargetDirInspector(comp)

        then:
        inspector.check() != ""
    }
}
