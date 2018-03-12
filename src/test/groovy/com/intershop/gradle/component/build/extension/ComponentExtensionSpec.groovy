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
import com.intershop.gradle.component.build.extension.container.FileItemContainer
import com.intershop.gradle.component.build.extension.container.LibraryItemContainer
import com.intershop.gradle.component.build.extension.container.ModuleItemContainer
import com.intershop.gradle.component.build.extension.container.PropertyItemContainer
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
    }

    def 'add file to extension'() {
        when:
        extension.fileItems.add(new File("testname.testextension"))

        then:
        extension.fileItems.items.size() == 1
        extension.fileItems.items.first().extension == "testextension"
        extension.fileItems.items.first().name == "testname"
    }

    def 'add property to extension'() {
        when:
        extension.propertyItems.add('test.key', 'test.value')

        then:
        extension.propertyItems.items.size() == 1
        extension.propertyItems.items.first().key == "test.key"
        extension.propertyItems.items.first().value == "test.value"
    }

    def 'add library to extension'() {
        when:
        extension.libs.add('com.intershop.test:testname:1.0.0')

        then:
        extension.libs.items.size() == 1
        extension.libs.items.first().dependency.moduleString == 'com.intershop.test:testname:1.0.0'
    }

    def 'add module to extension'() {
        when:
        extension.modules.add('com.intershop.test:testmodule:1.0.0')

        then:
        extension.modules.items.size() == 1
        extension.modules.items.first().dependency.moduleString == 'com.intershop.test:testmodule:1.0.0'
    }
}
