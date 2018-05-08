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
package com.intershop.gradle.component.build.extension

import com.intershop.gradle.component.build.ComponentBuildPlugin
import com.intershop.gradle.component.build.extension.container.*
import com.intershop.gradle.component.build.extension.items.FileContainerItem
import com.intershop.gradle.component.build.extension.items.FileItem
import com.intershop.gradle.component.build.extension.items.LibraryItem
import com.intershop.gradle.component.build.extension.items.ModuleItem
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class ComponentExtensionSpec extends Specification {

    private final Project project = ProjectBuilder.builder().build()
    ComponentExtension extension

    def setup() {
        project.pluginManager.apply(ComponentBuildPlugin)
        extension = project.extensions.getByType(ComponentExtension)
    }

    def 'extension should be added to project'() {
        expect:
        extension != null
    }

    def 'check extension'() {
        expect:
        extension.fileItems instanceof FileItemContainer
        extension.propertyItems instanceof PropertyItemContainer
        extension.libs instanceof LibraryItemContainer
        extension.modules instanceof ModuleItemContainer
        extension.containers instanceof FileContainerItemContainer
    }

    def 'add file to extension'() {
        when:
        extension.fileItems.add(new File("testname.testextension"))

        then:
        extension.fileItems.items.size() == 1
        extension.fileItems.items.first().extension == "testextension"
        extension.fileItems.items.first().name == "testname"
    }

    def 'check path input for file'() {
        when:
        FileItem item = extension.fileItems.add(new File("testname.testextension"))
        item.setTargetPath('testmodule')

        then:
        extension.fileItems.items.size() == 1
        extension.fileItems.items.first().extension == "testextension"
        extension.fileItems.items.first().name == "testname"
        extension.fileItems.items.first().targetPath == 'testmodule'
    }

    def 'check wrong path input for file'() {
        when:
        FileItem item = extension.fileItems.add(new File("testname.testextension"))
        item.setTargetPath('/testmodule')

        then:
        thrown(org.gradle.api.InvalidUserDataException)
    }

    def 'check type handling for file'() {
        when:
        extension.addType('all')
        extension.addType('development')
        extension.fileItems.add(new File("testname.testextension"))

        then:
        extension.fileItems.items.first().types.contains('all')
        extension.fileItems.items.first().types.contains('development')
        extension.fileItems.items.first().types.size() == 2
    }

    def 'add property to extension'() {
        when:
        extension.propertyItems.add('test.key', 'test.value',"**/**/appserver.properties")

        then:
        extension.propertyItems.items.size() == 1
        extension.propertyItems.items.first().key == "test.key"
        extension.propertyItems.items.first().value == "test.value"
        extension.propertyItems.items.first().pattern == "**/**/appserver.properties"
    }

    def 'check type handling for property'() {
        when:
        extension.addType('all')
        extension.addType('development')
        extension.propertyItems.add('test.key', 'test.value', "**/**/appserver.properties")

        then:
        extension.propertyItems.items.first().types.contains('all')
        extension.propertyItems.items.first().types.contains('development')
        extension.propertyItems.items.first().types.size() == 2
    }

    def 'add library to extension'() {
        when:
        extension.libs.add('com.intershop.test:testname:1.0.0')

        then:
        extension.libs.items.size() == 1
        extension.libs.items.first().dependency.moduleString == 'com.intershop.test:testname:1.0.0'
    }

    def 'check path input for library'() {
        when:
        LibraryItem item = extension.libs.add('com.intershop.test:testmodule:1.0.0')
        item.targetName = 'testmodule'

        then:
        extension.libs.items.size() == 1
        extension.libs.items.first().dependency.moduleString == 'com.intershop.test:testmodule:1.0.0'
        extension.libs.items.first().targetName == 'testmodule'
    }

    def 'check wrong path input for library'() {
        when:
        LibraryItem item = extension.libs.add('com.intershop.test:testmodule:1.0.0')
        item.targetName = '/testmodule'

        then:
        thrown(org.gradle.api.InvalidUserDataException)
    }

    def 'check type handling for library'() {
        when:
        extension.addType('all')
        extension.addType('development')
        extension.libs.add('com.intershop.test:testmodule:1.0.0')

        then:
        extension.libs.items.first().types.contains('all')
        extension.libs.items.first().types.contains('development')
        extension.libs.items.first().types.size() == 2
    }

    def 'add module to extension'() {
        when:
        extension.modules.add('com.intershop.test:testmodule:1.0.0')

        then:
        extension.modules.items.size() == 1
        extension.modules.items.first().dependency.moduleString == 'com.intershop.test:testmodule:1.0.0'
    }

    def 'check path input for module'() {
        when:
        ModuleItem item = extension.modules.add('com.intershop.test:testmodule:1.0.0')
        item.setTargetPath('testmodule')

        then:
        extension.modules.items.size() == 1
        extension.modules.items.first().dependency.moduleString == 'com.intershop.test:testmodule:1.0.0'
        extension.modules.items.first().targetPath == 'testmodule'
    }

    def 'check wrong path input for module'() {
        when:
        ModuleItem item = extension.modules.add('com.intershop.test:testmodule:1.0.0')
        item.setTargetPath('/testmodule')

        then:
        thrown(org.gradle.api.InvalidUserDataException)
    }

    def 'check type handling for module'() {
        when:
        extension.addType('all')
        extension.addType('development')
        extension.modules.add('com.intershop.test:testmodule:1.0.0')

        then:
        extension.modules.items.first().types.contains('all')
        extension.modules.items.first().types.contains('development')
        extension.modules.items.first().types.size() == 2
    }

    def 'add container to extension'() {
        when:
        extension.containers.add('cartridge')

        then:
        extension.containers.items.size() == 1
        extension.containers.items.first().name == 'cartridge'
    }

    def 'check path input for container'() {
        when:
        FileContainerItem item = extension.containers.add('cartridge')
        item.setTargetPath('testmodule')

        then:
        extension.containers.items.size() == 1
        extension.containers.items.first().name == 'cartridge'
        extension.containers.items.first().targetPath == 'testmodule'
    }

    def 'check wrong path input for container'() {
        when:
        FileContainerItem item = extension.containers.add('cartridge')
        item.setTargetPath('/testmodule')

        then:
        thrown(org.gradle.api.InvalidUserDataException)
    }

    def 'check type handling for container'() {
        when:
        extension.addType('all')
        extension.addType('development')
        extension.containers.add('cartridge')

        then:
        extension.containers.items.first().types.contains('all')
        extension.containers.items.first().types.contains('development')
        extension.containers.items.first().types.size() == 2
    }

    def 'check handling of links'() {
        when:
        extension.links.add('test1/test2', 'test3/test4')

        then:
        extension.links.items.size() == 1
    }

    def 'check handling of invalid link configs'() {
        when:
        extension.links.add('/test1/test2', 'test1/test2')

        then:
        thrown(org.gradle.api.InvalidUserDataException)
    }

    def 'check handling of doubled link configs'() {
        when:
        extension.links.add('test1/test2', 'test1/test2')
        extension.links.add('test1/test2', 'test1/test2')

        then:
        thrown(org.gradle.api.InvalidUserDataException)
    }

    def 'check handling of directories'() {
        when:
        extension.directories.add('test1/test2')

        then:
        extension.directories.items.size() == 1
    }

    def 'check handling of invalid directory configs'() {
        when:
        extension.directories.add('/test1/test2')

        then:
        thrown(org.gradle.api.InvalidUserDataException)
    }

    def 'check handling of doubled directory configs'() {
        when:
        extension.directories.add('test1/test2')
        extension.directories.add('test1/test2')

        then:
        thrown(org.gradle.api.InvalidUserDataException)
    }
}
