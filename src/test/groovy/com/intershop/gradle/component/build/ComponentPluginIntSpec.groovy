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
package com.intershop.gradle.component.build

import com.intershop.gradle.test.AbstractIntegrationSpec
import com.intershop.gradle.test.builder.TestIvyRepoBuilder
import com.intershop.gradle.test.builder.TestIvyRepoBuilder.ArchiveDirectoryEntry
import com.intershop.gradle.test.builder.TestIvyRepoBuilder.ArchiveFileEntry
import com.intershop.gradle.test.builder.TestMavenRepoBuilder
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Unroll

class ComponentPluginIntSpec extends AbstractIntegrationSpec {

    public final static String ivyPattern = '[organisation]/[module]/[revision]/[type]s/ivy-[revision].xml'
    public final static String artifactPattern = '[organisation]/[module]/[revision]/[ext]s/[artifact]-[type](-[classifier])-[revision].[ext]'

    final static String baseProjetBuild = """        
        group 'com.intershop.test'
        version = '1.0.0'
        
        component {
            containers {
                add('startscripts') {
                    baseName = 'startscripts'
                    containerType = 'bin'
                    targetPath = 'bin'
                    source(fileTree(dir: 'src/bin', include: '*.sh').files)
                }
            }

            modules {
                add("com.intershop:testmodule1:1.0.0")
                add("com.intershop:testmodule2:1.0.0")
            }
            
            libs {
                add("com.intershop:library1:1.0.0")
                targetPath = "lib/release/libs"
            }

            dependencyMngt.classCollision.enabled = false
        }""".stripIndent()

    @Unroll
    def 'Test plugin without publishing - #gradleVersion'(gradleVersion){
        given:
        String projectName = "testcomponent"
        createSettingsGradle(projectName)

        createStaticProjectFiles(testProjectDir)

        buildFile << """
        plugins {
            id 'com.intershop.gradle.component.build'
        }

        ${baseProjetBuild}
        
        ${createRepo(testProjectDir)}
        """.stripIndent()

        File outputFile = new File(testProjectDir, 'build/componentBuild/descriptor/file.component')

        when:
        List<String> args = ['createComponent', '-s', '-i']
        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':createComponent').outcome == TaskOutcome.SUCCESS
        result1.task(':verifyClasspath').outcome == TaskOutcome.SKIPPED
        outputFile.exists()
        outputFile.text.contains('com.intershop:library1:1.0.0')
        outputFile.text.contains('com.intershop:library2:1.0.0')
        outputFile.text.contains('com.intershop:library3:1.0.0')
        outputFile.text.contains('com.intershop_library1_1.0.0')
        outputFile.text.contains('com.intershop_library2_1.0.0')
        outputFile.text.contains('com.intershop_library3_1.0.0')
        outputFile.text.contains('testmodule1')
        outputFile.text.contains('testmodule2')


        when:
        def result2 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':createComponent').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':verifyClasspath').outcome == TaskOutcome.SKIPPED

        where:
        gradleVersion << supportedGradleVersions
    }

    @Unroll
    def 'Test plugin ivy publishing - #gradleVersion'(gradleVersion) {
        given:
        String projectName = "testcomponent"
        createSettingsGradle(projectName)

        createStaticProjectFiles(testProjectDir)

        buildFile << """
        plugins {
            id 'com.intershop.gradle.component.build'
            id 'ivy-publish'
        }

        ${baseProjetBuild}
        
        ${createRepo(testProjectDir)}

 
        //}
        publishing {
            ${TestIvyRepoBuilder.declareRepository(new File(testProjectDir, 'repo'), 'ivyTest', ivyPattern, artifactPattern)}
        }
        
        """.stripIndent()

        File outputFile = new File(testProjectDir, 'build/componentBuild/descriptor/file.component')
        File repoFile = new File(testProjectDir, "repo/com.intershop.test/${projectName}/1.0.0/components/${projectName}-component-1.0.0.component")
        File zipFile = new File(testProjectDir, "repo/com.intershop.test/${projectName}/1.0.0/zips/startscripts-bin-1.0.0.zip")

        when:
        List<String> args = ['publish', '-s', '-i']
        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':publish').outcome == TaskOutcome.SUCCESS
        result1.task(':createComponent').outcome == TaskOutcome.SUCCESS
        result1.task(':zipContainerStartscripts').outcome == TaskOutcome.SUCCESS
        result1.task(':verifyClasspath').outcome == TaskOutcome.SKIPPED
        outputFile.exists()
        outputFile.text.contains('com.intershop:library1:1.0.0')
        outputFile.text.contains('com.intershop_library1_1.0.0')
        outputFile.text.contains('testmodule1')
        outputFile.text.contains('testmodule2')
        repoFile.exists()
        repoFile.text == outputFile.text
        zipFile.exists()
        outputFile.text.contains('"name" : "startscripts",')
        outputFile.text.contains('"containerType" : "bin"')

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':publish').outcome == TaskOutcome.SUCCESS
        result2.task(':createComponent').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':zipContainerStartscripts').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':verifyClasspath').outcome == TaskOutcome.SKIPPED

        where:
        gradleVersion << supportedGradleVersions
    }

    @Unroll
    def 'Test plugin with different ivy configuration - #gradleVersion'(gradleVersion) {
        given:
        String projectName = "testcomponent"
        createSettingsGradle(projectName)

        createStaticProjectFiles(testProjectDir)

        buildFile << """
        plugins {
            id 'com.intershop.gradle.component.build'
            id 'ivy-publish'
        }

        group 'com.intershop.test'
        version = '1.0.0'
        
        component {
            ivyPublicationName = "ivyCustomer"

            containers {
                add('startscripts') {
                    baseName = 'startscripts'
                    containerType = 'bin'
                    targetPath = 'bin'
                    source(fileTree(dir: 'src/bin', include: '*.sh').files)
                }
            }

            modules {
                add("com.intershop:testmodule1:1.0.0")
                add("com.intershop:testmodule2:1.0.0")
            }

            dependencyMngt.classCollision.enabled = false
        }

        ${createRepo(testProjectDir)}

 
        //}
        publishing {
            ${TestIvyRepoBuilder.declareRepository(new File(testProjectDir, 'repo'), 'ivyTest', ivyPattern, artifactPattern)}
        }
        
        """.stripIndent()

        File outputFile = new File(testProjectDir, 'build/componentBuild/descriptor/file.component')
        File repoFile = new File(testProjectDir, "repo/com.intershop.test/${projectName}/1.0.0/components/${projectName}-component-1.0.0.component")
        File zipFile = new File(testProjectDir, "repo/com.intershop.test/${projectName}/1.0.0/zips/startscripts-bin-1.0.0.zip")

        when:
        List<String> args = ['publish', '-s', '-i']
        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':publish').outcome == TaskOutcome.SUCCESS
        result1.task(':createComponent').outcome == TaskOutcome.SUCCESS
        result1.task(':zipContainerStartscripts').outcome == TaskOutcome.SUCCESS
        result1.task(':publishIvyCustomerPublicationToIvyTestRepository').outcome == TaskOutcome.SUCCESS
        result1.task(':verifyClasspath').outcome == TaskOutcome.SKIPPED
        outputFile.exists()
        outputFile.text.contains('testmodule1')
        outputFile.text.contains('testmodule2')
        repoFile.exists()
        repoFile.text == outputFile.text
        zipFile.exists()
        outputFile.text.contains('"name" : "startscripts",')
        outputFile.text.contains('"containerType" : "bin"')

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':publish').outcome == TaskOutcome.SUCCESS
        result2.task(':publishIvyCustomerPublicationToIvyTestRepository').outcome == TaskOutcome.SUCCESS
        result2.task(':createComponent').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':zipContainerStartscripts').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':verifyClasspath').outcome == TaskOutcome.SKIPPED

        where:
        gradleVersion << supportedGradleVersions
    }

    @Unroll
    def 'Test plugin maven publishing - #gradleVersion'(gradleVersion) {
        given:
        String projectName = "testcomponent"
        createSettingsGradle(projectName)

        createStaticProjectFiles(testProjectDir)

        buildFile << """
        plugins {
            id 'com.intershop.gradle.component.build'
            id 'maven-publish'
        }

        ${baseProjetBuild}
        
        ${createRepo(testProjectDir)}

        //}
        publishing {
            ${TestMavenRepoBuilder.declareRepository(new File(testProjectDir, 'repo'))}
        }
        """.stripIndent()

        File outputFile = new File(testProjectDir, 'build/componentBuild/descriptor/file.component')
        File repoFile = new File(testProjectDir, "repo/com/intershop/test/${projectName}/1.0.0/${projectName}-1.0.0-component.component")
        File zipFile = new File(testProjectDir, "repo/com/intershop/test/${projectName}/1.0.0/${projectName}-1.0.0-startscripts_bin.zip")

        when:
        List<String> args = ['publish', '-s', '-i']
        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':publish').outcome == TaskOutcome.SUCCESS
        result1.task(':createComponent').outcome == TaskOutcome.SUCCESS
        result1.task(':zipContainerStartscripts').outcome == TaskOutcome.SUCCESS
        result1.task(':verifyClasspath').outcome == TaskOutcome.SKIPPED
        outputFile.exists()
        outputFile.text.contains('com.intershop:library1:1.0.0')
        outputFile.text.contains('com.intershop_library1_1.0.0')
        outputFile.text.contains('testmodule1')
        outputFile.text.contains('testmodule2')
        repoFile.exists()
        repoFile.text == outputFile.text
        zipFile.exists()
        outputFile.text.contains('"name" : "startscripts",')
        outputFile.text.contains('"containerType" : "bin"')

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':publish').outcome == TaskOutcome.SUCCESS
        result2.task(':createComponent').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':zipContainerStartscripts').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':verifyClasspath').outcome == TaskOutcome.SKIPPED


        where:
        gradleVersion << supportedGradleVersions
    }

    @Unroll
    def 'Test plugin with different maven configuration - #gradleVersion'(gradleVersion) {
        given:
        String projectName = "testcomponent"
        createSettingsGradle(projectName)

        createStaticProjectFiles(testProjectDir)

        buildFile << """
        plugins {
            id 'com.intershop.gradle.component.build'
            id 'maven-publish'
        }

        group 'com.intershop.test'
        version = '1.0.0'
        
        component {
            mavenPublicationName = "mvnCustomer"

            containers {
                add('startscripts') {
                    baseName = 'startscripts'
                    containerType = 'bin'
                    targetPath = 'bin'
                    source(fileTree(dir: 'src/bin', include: '*.sh').files)
                }
            }

            modules {
                add("com.intershop:testmodule1:1.0.0")
                add("com.intershop:testmodule2:1.0.0")
            }

            dependencyMngt.classCollision.enabled = false
        }

        ${createRepo(testProjectDir)}

        //}
        publishing {
            ${TestMavenRepoBuilder.declareRepository(new File(testProjectDir, 'repo'))}
        }
        """.stripIndent()

        File outputFile = new File(testProjectDir, 'build/componentBuild/descriptor/file.component')
        File repoFile = new File(testProjectDir, "repo/com/intershop/test/${projectName}/1.0.0/${projectName}-1.0.0-component.component")
        File zipFile = new File(testProjectDir, "repo/com/intershop/test/${projectName}/1.0.0/${projectName}-1.0.0-startscripts_bin.zip")

        when:
        List<String> args = ['publish', '-s', '-i']
        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':publish').outcome == TaskOutcome.SUCCESS
        result1.task(':publishMvnCustomerPublicationToMavenRepository').outcome == TaskOutcome.SUCCESS
        result1.task(':createComponent').outcome == TaskOutcome.SUCCESS
        result1.task(':zipContainerStartscripts').outcome == TaskOutcome.SUCCESS
        result1.task(':verifyClasspath').outcome == TaskOutcome.SKIPPED
        outputFile.exists()
        outputFile.text.contains('testmodule1')
        outputFile.text.contains('testmodule2')
        repoFile.exists()
        repoFile.text == outputFile.text
        zipFile.exists()
        outputFile.text.contains('"name" : "startscripts",')
        outputFile.text.contains('"containerType" : "bin"')

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':publish').outcome == TaskOutcome.SUCCESS
        result2.task(':publishMvnCustomerPublicationToMavenRepository').outcome == TaskOutcome.SUCCESS
        result2.task(':createComponent').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':zipContainerStartscripts').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':verifyClasspath').outcome == TaskOutcome.SKIPPED


        where:
        gradleVersion << supportedGradleVersions
    }

    @Unroll
    def 'Test plugin with all artifacts - #gradleVersion'(gradleVersion) {
        given:
        String projectName = "testcomponent"
        createSettingsGradle(projectName)

        createStaticProjectFiles(testProjectDir)
        createAddStaticProjectFiles(testProjectDir)
        createSingleStaticProjectFiles(testProjectDir)

        buildFile << """
        plugins {
            id 'com.intershop.gradle.component.build'
            id 'ivy-publish'
        }

        group 'com.intershop.test'
        version = '1.0.0'
        
        component {
            containers {
                add('startscripts') {
                    baseName = 'startscripts'
                    containerType = 'bin'
                    targetPath = 'bin'
                    source(fileTree(dir: 'src/bin', include: '*.sh').files)
                }
                add('sites') {
                    baseName = 'share'
                    containerType = 'sites'
                    targetPath = 'share'
                    source(fileTree(dir: 'sites', include: '**/**/*.*').files)
                }
            }

            modules {
                add("com.intershop:testmodule1:1.0.0")
                add("com.intershop:testmodule2:1.0.0")
            }
            
            libs {
                add("com.intershop:library1:1.0.0")
                targetPath = "lib/release/libs"
            }

            dependencyMngt {
                exclude("com.intershop", "library3")
                exclude("com.intershop", "library4")

                classCollision.enabled = false
            }

            fileItems {
                add(file("conf/server/test1.properties")) {
                    targetPath = 'share/system/config'
                }
                add(file("conf/server/test2.properties")) {
                    targetPath = 'share/system/config'
                }
            }

            propertyItems {
                add("pkey1", "pvalue1")
                add("pkey2", "pvalue2")
            }
        }
        
        ${createRepo(testProjectDir)}

        //}
        publishing {
            ${TestIvyRepoBuilder.declareRepository(new File(testProjectDir, 'repo'), 'ivyTest', ivyPattern, artifactPattern)}
        }
        
        """.stripIndent()

        File outputFile = new File(testProjectDir, 'build/componentBuild/descriptor/file.component')
        File repoFile = new File(testProjectDir, "repo/com.intershop.test/${projectName}/1.0.0/components/${projectName}-component-1.0.0.component")
        File zipFile1 = new File(testProjectDir, "repo/com.intershop.test/${projectName}/1.0.0/zips/startscripts-bin-1.0.0.zip")
        File zipFile2 = new File(testProjectDir, "repo/com.intershop.test/${projectName}/1.0.0/zips/share-sites-1.0.0.zip")
        File singleFile1 = new File(testProjectDir, "repo/com.intershop.test/${projectName}/1.0.0/propertiess/test1-properties-1.0.0.properties")
        File singleFile2 = new File(testProjectDir, "repo/com.intershop.test/${projectName}/1.0.0/propertiess/test2-properties-1.0.0.properties")
        File ivyFile = new File(testProjectDir, "repo/com.intershop.test/${projectName}/1.0.0/ivys/ivy-1.0.0.xml")

        when:
        List<String> args = ['publish', '-s', '-i']
        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':publish').outcome == TaskOutcome.SUCCESS
        result1.task(':createComponent').outcome == TaskOutcome.SUCCESS
        result1.task(':zipContainerStartscripts').outcome == TaskOutcome.SUCCESS
        result1.task(':verifyClasspath').outcome == TaskOutcome.SKIPPED
        outputFile.exists()
        outputFile.text.contains('com.intershop:library1:1.0.0')
        outputFile.text.contains('com.intershop_library1_1.0.0')
        outputFile.text.contains('testmodule1')
        outputFile.text.contains('testmodule2')
        repoFile.exists()
        repoFile.text == outputFile.text
        zipFile1.exists()
        zipFile2.exists()
        singleFile1.exists()
        singleFile2.exists()
        outputFile.text.contains('"name" : "startscripts",')
        outputFile.text.contains('"name" : "sites"')
        outputFile.text.contains('"name" : "test1"')
        outputFile.text.contains('"name" : "test2"')
        outputFile.text.contains('"key" : "pkey1",')
        outputFile.text.contains('"value" : "pvalue1",')
        outputFile.text.contains('"key" : "pkey2",')
        outputFile.text.contains('"value" : "pvalue2",')
        ivyFile.exists()
        ivyFile.text.contains('<artifact name="share" type="sites" ext="zip" conf="component"/>')
        ivyFile.text.contains('<artifact name="startscripts" type="bin" ext="zip" conf="component"/>')
        ivyFile.text.contains('<artifact name="test1" type="properties" ext="properties" conf="component"/>')
        ivyFile.text.contains('<artifact name="test2" type="properties" ext="properties" conf="component"/>')
        ivyFile.text.contains('<artifact name="testcomponent" type="component" ext="component" conf="component"/>')
        ivyFile.text.contains('<conf name="component" visibility="public" extends="default"/>')
        ivyFile.text.contains('<conf name="default" visibility="public"/>')

        ! outputFile.text.contains('com.intershop:library3:1.0.0')
        ! outputFile.text.contains('com.intershop:library4:1.0.0')

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':publish').outcome == TaskOutcome.SUCCESS
        result2.task(':createComponent').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':zipContainerStartscripts').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':verifyClasspath').outcome == TaskOutcome.SKIPPED

        where:
        gradleVersion << supportedGradleVersions
    }

    @Unroll
    def 'Test descriptor configuration displayName - #gradleVersion'(gradleVersion){
        given:
        String projectName = "testcomponent"
        createSettingsGradle(projectName)

        createStaticProjectFiles(testProjectDir)

        buildFile << """
        plugins {
            id 'com.intershop.gradle.component.build'
        }

        group 'com.intershop.test'
        version = '1.0.0'
        
        component {
            displayName = "SpecialProject"
            componentDescription = "This is a description"
            targetPath = "defaultTarget"

            decriptorOutputFile = file("build/testdir/testfile.component")

            modules {
                add("com.intershop:testmodule1:1.0.0")
                add("com.intershop:testmodule2:1.0.0")
            }

            dependencyMngt.classCollision.enabled = false
        }

        ${createRepo(testProjectDir)}
        """.stripIndent()

        File outputFile = new File(testProjectDir, 'build/testdir/testfile.component')

        when:
        List<String> args = ['createComponent', '-s', '-i']
        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':createComponent').outcome == TaskOutcome.SUCCESS
        outputFile.exists()
        outputFile.text.contains('testmodule1')
        outputFile.text.contains('testmodule2')

        outputFile.text.contains('"displayName" : "SpecialProject",')
        outputFile.text.contains('"componentDescription" : "This is a description",')
        outputFile.text.contains('"target" : "defaultTarget",')
        outputFile.text.contains('testmodule2')

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':createComponent').outcome == TaskOutcome.UP_TO_DATE

        where:
        gradleVersion << supportedGradleVersions
    }

    @Unroll
    def 'Test plugin with version conflicts'(){
        given:
        String projectName = "testcomponent"
        createSettingsGradle(projectName)

        buildFile << """
        plugins {
            id 'com.intershop.gradle.component.build'
            id 'ivy-publish'
        }

        group 'com.intershop.test'
        version = '1.0.0'
        
        component {
            libs {
                add("commons-io:commons-io:2.2")
                add('com.google.code.findbugs:annotations:3.0.0')
                add('javax.persistence:persistence-api:1.0.2')
                add('javax.validation:validation-api:1.0.0.GA')
                add('org.ow2.asm:asm:5.1')
                add('org.slf4j:slf4j-api:1.7.21')
                add('junit:junit:4.12')
                add('com.netflix.servo:servo-atlas:0.12.11')
                add('net.sf.ehcache:ehcache-core:2.6.11')
                add('org.springframework:spring-web:4.1.6.RELEASE')

                // double classes
                targetPath = "lib/release/libs"
            }
        }
        
        ${createRepo(testProjectDir)}

 
        //}
        publishing {
            ${TestIvyRepoBuilder.declareRepository(new File(testProjectDir, 'repo'), 'ivyTest', ivyPattern, artifactPattern)}
        }
        

        """.stripIndent()

        when:
        List<String> args = ['publish', '-s', '-i']
        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .buildAndFail()

        then:
        true

        where:
        gradleVersion << supportedGradleVersions
    }

    @Unroll
    def 'Test plugin with class collision'(){
        given:
        String projectName = "testcomponent"
        createSettingsGradle(projectName)

        buildFile << """
        plugins {
            id 'com.intershop.gradle.component.build'
            id 'ivy-publish'
        }

        group 'com.intershop.test'
        version = '1.0.0'
        
        component {
            libs {
                add("commons-io:commons-io:2.2")
                add('com.google.code.findbugs:annotations:3.0.0')
                add('javax.persistence:persistence-api:1.0.2')
                add('javax.validation:validation-api:1.0.0.GA')
                add('commons-logging:commons-logging:1.2')
                add('org.ow2.asm:asm:5.1')
                add('org.slf4j:slf4j-api:1.7.21')
                add('junit:junit:4.12')
                add('com.netflix.servo:servo-atlas:0.12.11')
                add('net.sf.ehcache:ehcache-core:2.6.11')
                add('org.springframework:spring-web:4.1.6.RELEASE')

                // double classes
                add('org.ow2.asm:asm-all:4.2')
                targetPath = "lib/release/libs"
            }
        }
        
        ${createRepo(testProjectDir)}

 
        //}
        publishing {
            ${TestIvyRepoBuilder.declareRepository(new File(testProjectDir, 'repo'), 'ivyTest', ivyPattern, artifactPattern)}
        }
        

        """.stripIndent()

        when:
        List<String> args = ['publish', '-s', '-i']
        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .buildAndFail()

        then:
        true

        where:
        gradleVersion << supportedGradleVersions
    }

    @Unroll
    def 'Test plugin with multi project and project dependencies'() {
        given:
        String projectName = "testproject"
        File settingsfile = createSettingsGradle(projectName)

        createSubProjectJava('project1a', settingsfile, 'com.intereshop.a')
        createSubProjectJava('project2b', settingsfile, 'com.intereshop.b')

        buildFile << """
                plugins {
                    id 'ivy-publish'
                    id 'com.intershop.gradle.component.build'
                }
                

                group 'com.intershop.test'
                version = '1.0.0'
                
                ${createRepo(testProjectDir)}
        """.stripIndent()

        def compProjectBuild = """
                apply plugin: 'ivy-publish'
                apply plugin: 'com.intershop.gradle.component.build'
                
                group 'com.intershop.testcomp'
                version = '1.0.0'
                
                component {
                    
                    modules {
                        add(project(':project1a'))
                        add(project(':project2b'))
                    }
                    
                    libs {
                        add("com.intershop:library1:1.0.0")
                        targetPath = "lib/release/libs"
                    }
       
                    dependencyMngt.classCollision.enabled = false
                }
                
                publishing {                     
                     ${TestIvyRepoBuilder.declareRepository(new File(testProjectDir, 'repo'), 'ivyTest', ivyPattern, artifactPattern)}
                }

                ${TestIvyRepoBuilder.declareRepository(new File(testProjectDir, 'repo'), 'ivyTest', ivyPattern, artifactPattern)}
                ${TestMavenRepoBuilder.declareRepository(new File(testProjectDir, 'repo'))}

        """.stripIndent()

        File subProject = createSubProject('projectComponent', settingsfile, compProjectBuild)

        when:
        List<String> args = ['publish', '-s', '-i']
        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        true

        where:
        gradleVersion << supportedGradleVersions
    }

    private File createSubProjectJava(String projectPath, File settingsGradle, String packageName){

        String buildFileContentBase =
                """
                plugins {
                     id 'java'
                     id 'ivy-publish'
                }

                task zipbin(type: Zip) {
                    from 'staticfiles'
                }    

                task zipsites(type: Zip) {
                    from 'sites'
                } 
          
                group 'com.intershop.test'
                version = '1.0.0'

                artifacts.add("runtime", zipsites) {
                    type = 'sites'
                }
                artifacts.add("runtime", zipbin) {
                    type = 'bin'
                }

                publishing {
                     publications {
                         ivyIntershop(IvyPublication) {
                             from components.java

                             artifact(zipbin) {
                                type "bin"
                                conf "runtime"
                             }
                             artifact(zipsites) {
                                type "sites"
                                conf "runtime"
                             }
                         }
                     }
                     
                     ${TestIvyRepoBuilder.declareRepository(new File(testProjectDir, 'repo'), 'ivyTest', ivyPattern, artifactPattern)}
                }       
                """.stripIndent()

        File subProject = createSubProject(projectPath, settingsGradle, buildFileContentBase)
        writeJavaTestClass(packageName, subProject)

        def testFile1 = new File(subProject, 'staticfiles/bin/test1.sh')
        def testFile2 = new File(subProject, 'staticfiles/bin/test2.sh')
        testFile1.parentFile.mkdirs()

        testFile1 << """
        # testfile1.sh
        """.stripIndent()

        testFile2 << """
        # testfile2.sh
        """.stripIndent()

        def testFile3 = new File(subProject, 'sites/test/test3.txt')
        def testFile4 = new File(subProject, 'sites/test/test4.txt')
        testFile3.parentFile.mkdirs()

        testFile3 << """
        # testfile3 text
        """.stripIndent()

        testFile4 << """
        # testfile4 text
        """.stripIndent()

        return subProject
    }

    private File createSettingsGradle(String projectName) {
        File settingsFile = new File(testProjectDir, 'settings.gradle')
        settingsFile << """
        rootProject.name = '${projectName}'
        """.stripIndent()

        return settingsFile
    }

    private File createStaticProjectFiles(File projectDir) {
        def testFile1 = new File(projectDir, 'src/bin/test1.sh')
        def testFile2 = new File(projectDir, 'src/bin/test2.sh')
        testFile1.parentFile.mkdirs()

        testFile1 << """
        # testfile1.sh
        """.stripIndent()

        testFile2 << """
        # testfile2.sh
        """.stripIndent()
    }

    private File createAddStaticProjectFiles(File projectDir) {
        def testFile1 = new File(projectDir, 'sites/import/test1.txt')
        def testFile2 = new File(projectDir, 'sites/import/test2.txt')
        testFile1.parentFile.mkdirs()

        testFile1 << """
        # testfile1.txt
        """.stripIndent()

        testFile2 << """
        # testfile2.txt
        """.stripIndent()
    }

    private File createSingleStaticProjectFiles(File projectDir) {
        def testFile1 = new File(projectDir, 'conf/server/test1.properties')
        def testFile2 = new File(projectDir, 'conf/server/test2.properties')
        testFile1.parentFile.mkdirs()

        testFile1 << """
        # testfile1.properties
        """.stripIndent()

        testFile2 << """
        # testfile2.properties
        """.stripIndent()
    }

    def createRepo(File dir) {

        File repoDir = new File(dir, 'repo')

        new TestIvyRepoBuilder().repository( ivyPattern: ivyPattern, artifactPattern: artifactPattern ) {
            module(org: 'com.intershop', name: 'testmodule1', rev: '1.0.0') {
                artifact name: 'testmodule1', type: 'cartridge', ext: 'zip', entries: [
                        ArchiveFileEntry.newInstance(path: 'testmodule/testfiles/test1.file', content: 'test1.file'),
                        ArchiveFileEntry.newInstance(path: 'testmodule/testconf/test2.conf', content: 'test2.conf'),
                        ArchiveDirectoryEntry.newInstance(path: 'testmodule/empttestdir')
                ]
                artifact name: 'testmodule1', type: 'jar', ext: 'jar', entries: [
                        ArchiveFileEntry.newInstance(path: 'com/class/intern/test1.file', content: 'interntest1.file'),
                        ArchiveFileEntry.newInstance(path: 'com/class/intern/test2.file', content: 'interntest2.file')
                ]
                artifact name: 'extlib', type: 'jar', ext: 'jar', entries: [
                        ArchiveFileEntry.newInstance(path: 'com/class/ext/test1.file', content: 'exttest1.file'),
                        ArchiveFileEntry.newInstance(path: 'com/class/ext/test2.file', content: 'exttest2.file')
                ]
                dependency org: 'com.intershop', name: 'library1', rev: '1.0.0'
                dependency org: 'com.intershop', name: 'library2', rev: '1.0.0'
            }
            module(org: 'com.intershop', name: 'testmodule2', rev: '1.0.0') {
                artifact name: 'testmodule2', type: 'cartridge', ext: 'zip', entries: [
                        ArchiveFileEntry.newInstance(path: 'testmodule/testfiles/test21.file', content: 'test21.file'),
                        ArchiveFileEntry.newInstance(path: 'testmodule/testconf/test22.conf', content: 'test22.conf'),
                        ArchiveDirectoryEntry.newInstance(path: 'testmodule/empttestdir')
                ]
                artifact name: 'testmodule1', type: 'jar', ext: 'jar', entries: [
                        ArchiveFileEntry.newInstance(path: 'com/class/intern/test1.file', content: 'interntest1.file'),
                        ArchiveFileEntry.newInstance(path: 'com/class/intern/test2.file', content: 'interntest2.file')
                ]
                dependency org: 'com.intershop', name: 'library3', rev: '1.0.0'
            }
        }.writeTo(repoDir)


        new TestMavenRepoBuilder().repository {
            project(groupId: 'com.intershop', artifactId: 'library1', version: '1.0.0') {
                artifact entries: [
                        ArchiveFileEntry.newInstance(path: 'com/class/test1.file', content: 'test1.file'),
                        ArchiveFileEntry.newInstance(path: 'com/class/test2.file', content: 'test2.file'),
                ]
            }
            project(groupId: 'com.intershop', artifactId: 'library2', version: '1.0.0'){
                artifact entries: [
                        ArchiveFileEntry.newInstance(path: 'com/class/test1.file', content: 'test1.file'),
                        ArchiveFileEntry.newInstance(path: 'com/class/test2.file', content: 'test2.file'),
                ]
            }
            project(groupId: 'com.intershop', artifactId: 'library3', version: '1.0.0'){
                artifact entries: [
                        ArchiveFileEntry.newInstance(path: 'com/class/test1.file', content: 'test1.file'),
                        ArchiveFileEntry.newInstance(path: 'com/class/test2.file', content: 'test2.file'),
                ]
            }
            project(groupId: 'com.intershop', artifactId: 'library4', version: '1.0.0'){
                artifact entries: [
                        ArchiveFileEntry.newInstance(path: 'com/class/test1.file', content: 'test1.file'),
                        ArchiveFileEntry.newInstance(path: 'com/class/test2.file', content: 'test2.file'),
                ]
            }
        }.writeTo(repoDir)


        String repostr = """
            repositories {
                ivy {
                    name 'ivyLocal'
                    url "file://${repoDir.absolutePath.replace('\\', '/')}"
                    layout('pattern') {
                        ivy "${ivyPattern}"
                        artifact "${artifactPattern}"
                        artifact "${ivyPattern}"
                    }
                }
                maven {
                    url "file://${repoDir.absolutePath.replace('\\\\', '/')}"
                }
                jcenter()
            }""".stripIndent()
    }
}
