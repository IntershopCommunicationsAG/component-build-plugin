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

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class UtilsSpec extends Specification {

    private final Project project = ProjectBuilder.builder().build()

    def 'Check for illegal characters in target path'() {
        when:
        def returnValue = Utils.getIllegalChars(input)

        then:
        returnValue == output

        where:
        input | output
        "That is a Test" | "   "
        "Other\test" | "\t"
        "Other/test" | ""
    }

    def 'verify dependency helper'() {
        when:
        def dep = Utils.getDependencyConf(project.dependencies, input, "It is not possible to process the dependency.")

        then:
        dep.moduleString == output

        where:
        input | output
        "com.intershop:test:1.2.3" | "com.intershop:test:1.2.3"
        [group: "com.intershop", name: "test", version: "1.2.3" ] | "com.intershop:test:1.2.3"
        [name: "test", version: "1.2.3" ] | ":test:1.2.3"
        [group: "com.intershop", name: "test" ] | "com.intershop:test:"

    }
}
