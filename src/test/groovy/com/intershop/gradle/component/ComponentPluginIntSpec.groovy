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
package com.intershop.gradle.component

import com.intershop.gradle.test.AbstractIntegrationSpec
import spock.lang.Unroll

class ComponentPluginIntSpec extends AbstractIntegrationSpec {

    @Unroll
    def 'Test plugin happy path'(){
        given:
        String projectName = "testcomponent"

        buildFile << """
        plugins {
            id 'com.intershop.gradle.component'
        }
        
        component {
            
            module("com.intershop.platform:core:14.0.2")
        }
        
        repositories {
            ivy {
                url "http://rnd-repo.rnd.j.intershop.de/ivy-internal/"
                layout('pattern') {
                    ivy '[organisation]/[module]/[revision]/[type]s/ivy-[revision].xml'
                    artifact '[organisation]/[module]/[revision]/[ext]s/[artifact]-[type](-[classifier])-[revision].[ext]'
                    artifact '[organisation]/[module]/[revision]/[type]s/ivy-[revision].xml'
                }
            }
            maven {
                url "http://rnd-repo.rnd.j.intershop.de/mvn-internal/"
            }
            jcenter()
        }
        """.stripIndent()

        when:
        List<String> args = ['createComponent', '-s', '-i']
        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        true

        where:
        gradleVersion << supportedGradleVersions
    }
}
