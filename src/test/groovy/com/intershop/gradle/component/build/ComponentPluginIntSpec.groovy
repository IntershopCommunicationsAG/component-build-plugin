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

import com.intershop.gradle.component.descriptor.Component
import com.intershop.gradle.component.descriptor.util.ComponentUtil
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
                    itemType = 'bin'
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

            dependencyMngt.classpathVerification.enabled = false
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
        outputFile.text.contains('"itemType" : "bin"')

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
                    itemType = 'bin'
                    targetPath = 'bin'
                    source(fileTree(dir: 'src/bin', include: '*.sh').files)
                }
            }

            modules {
                add("com.intershop:testmodule1:1.0.0")
                add("com.intershop:testmodule2:1.0.0")
            }

            dependencyMngt.classpathVerification.enabled = false
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
        outputFile.text.contains('"itemType" : "bin"')

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
        outputFile.text.contains('"itemType" : "bin"')

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
                    itemType = 'bin'
                    targetPath = 'bin'
                    source(fileTree(dir: 'src/bin', include: '*.sh').files)
                }
            }

            modules {
                add("com.intershop:testmodule1:1.0.0")
                add("com.intershop:testmodule2:1.0.0")
            }

            dependencyMngt.classpathVerification.enabled = false
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
        outputFile.text.contains('"itemType" : "bin"')

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
    def 'Test plugin with all artifacts and ivy configuration - #gradleVersion'(gradleVersion) {
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
                    itemType = 'bin'
                    targetPath = 'bin'
                    source(fileTree(dir: 'src/bin', include: '*.sh').files)
                }
                add('sites') {
                    baseName = 'share'
                    itemType = 'sites'
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

                classpathVerification.enabled = false
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
    def 'Test plugin with all artifacts and maven configuration - #gradleVersion'(gradleVersion) {
        given:
        String projectName = "testcomponent"
        createSettingsGradle(projectName)

        createStaticProjectFiles(testProjectDir)
        createAddStaticProjectFiles(testProjectDir)
        createSingleStaticProjectFiles(testProjectDir)

        buildFile << """
        plugins {
            id 'com.intershop.gradle.component.build'
            id 'maven-publish'
        }

        group 'com.intershop.test'
        version = '1.0.0'
        
        component {
            containers {
                add('startscripts') {
                    baseName = 'startscripts'
                    itemType = 'bin'
                    targetPath = 'bin'
                    source(fileTree(dir: 'src/bin', include: '*.sh').files)
                }
                add('sites') {
                    baseName = 'share'
                    itemType = 'sites'
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

                classpathVerification.enabled = false
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

        publishing {
            ${TestMavenRepoBuilder.declareRepository(new File(testProjectDir, 'repo'))}
        }
        
        """.stripIndent()

        File outputFile = new File(testProjectDir, 'build/componentBuild/descriptor/file.component')
        File repoFile = new File(testProjectDir, "repo/com/intershop/test/${projectName}/1.0.0/${projectName}-1.0.0-component.component")
        File zipFile1 = new File(testProjectDir, "repo/com/intershop/test/${projectName}/1.0.0/${projectName}-1.0.0-startscripts_bin.zip")
        File zipFile2 = new File(testProjectDir, "repo/com/intershop/test/${projectName}/1.0.0/${projectName}-1.0.0-share_sites.zip")
        File singleFile1 = new File(testProjectDir, "repo/com/intershop/test/${projectName}/1.0.0/${projectName}-1.0.0-test1.properties")
        File singleFile2 = new File(testProjectDir, "repo/com/intershop/test/${projectName}/1.0.0/${projectName}-1.0.0-test2.properties")

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
    def 'Test types and excludeFromUpdate handling in container - #gradleVersion'(gradleVersion) {
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
                addType('production')
                addType('test')
                addTypes(['test1', 'test2'])
                excludeFromUpdate = true
                add('startscripts') {
                    baseName = 'startscripts'
                    itemType = 'bin'
                    targetPath = 'bin'
                    source(fileTree(dir: 'src/bin', include: '*.sh').files)
                }
                add('sites') {
                    baseName = 'share'
                    itemType = 'sites'
                    targetPath = 'share'
                    source(fileTree(dir: 'sites', include: '**/**/*.*').files)
                }
            }

            modules {
                addType('production')
                excludeFromUpdate = true
                add("com.intershop:testmodule1:1.0.0")
                add("com.intershop:testmodule2:1.0.0")
            }
            
            libs {
                addType('test')
                add("com.intershop:library1:1.0.0")
                targetPath = "lib/release/libs"
            }

            dependencyMngt.classpathVerification.enabled = false

            fileItems {
                excludeFromUpdate = true
                addType('intTest')
                add(file("conf/server/test1.properties")) {
                    targetPath = 'share/system/config'
                }
                add(file("conf/server/test2.properties")) {
                    targetPath = 'share/system/config'
                }
            }

            propertyItems {
                excludeFromUpdate = true

                add("pkey1", "pvalue1", 'perfTest')
                add("pkey2", "pvalue2", 'production')
            }
        }
        
        ${createRepo(testProjectDir)}

        publishing {
            ${TestIvyRepoBuilder.declareRepository(new File(testProjectDir, 'repo'), 'ivyTest', ivyPattern, artifactPattern)}
        }
        
        """.stripIndent()

        File outputFile = new File(testProjectDir, 'build/componentBuild/descriptor/file.component')

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
        Component comp = ComponentUtil.INSTANCE.componentFromFile(outputFile)
        comp.types.containsAll([ "production", "test", "test1", "test2", "intTest", "perfTest" ])
        comp.modules['testmodule1'].types.containsAll([ "production" ])
        comp.modules['testmodule1'].types.size() == 1
        comp.modules['testmodule2'].types.containsAll([ "production" ])
        comp.modules['testmodule1'].types.size() == 1
        comp.modules.values().findAll {it.excludeFromUpdate }.size() == 2
        comp.libs['com.intershop:library1:1.0.0'].types.containsAll([ "test" ])
        comp.libs['com.intershop:library1:1.0.0'].types.size() == 1
        comp.libs['com.intershop:library2:1.0.0'].types.containsAll([ "production" ])
        comp.libs['com.intershop:library2:1.0.0'].types.size() == 1
        comp.fileContainers.find {it.name == 'startscripts'}.types.containsAll([ "production", "test", "test1", "test2" ])
        comp.fileContainers.find {it.name == 'startscripts'}.types.size() == 4
        comp.fileContainers.findAll {it.excludeFromUpdate }.size() == 2
        comp.fileItems.findAll {it.types.size() == 1 && it.types.containsAll(["intTest"]) }.size() == 2
        comp.fileItems.findAll {it.excludeFromUpdate }.size() == 2
        comp.properties.findAll {it.types.size() == 1 && it.types.containsAll(["perfTest"]) }.size() == 1
        comp.properties.findAll {it.types.size() == 1 && it.types.containsAll(["production"]) }.size() == 1
        comp.properties.findAll {it.excludeFromUpdate }.size() == 2

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
    def 'Test types and excludesFromUpdate handling - #gradleVersion'(gradleVersion) {
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
                addType('test')
                add('startscripts') {
                    addType('production')
                    baseName = 'startscripts'
                    itemType = 'bin'
                    targetPath = 'bin'
                    source(fileTree(dir: 'src/bin', include: '*.sh').files)

                    addUpdateExcludePattern("**/dir1/*.com")
                    addUpdateExcludePattern([ "**/dir2/*.com", "**/dir3/*.com" ] as Set)
                }
                add('sites') {
                    baseName = 'share'
                    itemType = 'sites'
                    targetPath = 'share'
                    source(fileTree(dir: 'sites', include: '**/**/*.*').files)

                    addUpdateExcludePattern("**/dir1/*.com")
                }
            }

            modules {
                addType('test')
                add("com.intershop:testmodule1:1.0.0") {
                    addUpdateExcludePattern("**/dir4/*.com")
                }
                add("com.intershop:testmodule2:1.0.0")
            }
            
            libs {
                addType('test')
                add("com.intershop:library1:1.0.0")
                targetPath = "lib/release/libs"
            }

            dependencyMngt.classpathVerification.enabled = false

            fileItems {
                addType('intTest')
                add(file("conf/server/test1.properties")) {
                    targetPath = 'share/system/config'
                }
                add(file("conf/server/test2.properties")) {
                    targetPath = 'share/system/config'
                }
            }

            propertyItems {
                addType('perfTest')
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
        Component comp = ComponentUtil.INSTANCE.componentFromFile(outputFile)
        comp.types.containsAll([ "test", "production", "intTest", "perfTest" ])
        comp.types.size() == 4
        comp.modules['testmodule1'].types.containsAll([ "test" ])
        comp.modules['testmodule1'].types.size() == 1
        comp.modules['testmodule1'].excludesFromUpdate.containsAll(["**/dir4/*.com"])
        comp.modules['testmodule1'].excludesFromUpdate.size() == 1
        comp.modules['testmodule2'].types.containsAll([ "test" ])
        comp.modules['testmodule2'].types.size() == 1
        comp.libs.values().findAll { it.types.size() == 1 && it.types.containsAll([ "test" ]) }.size() == 3
        comp.fileContainers.find {it.name == 'startscripts'}.types.containsAll([ "test", "production" ])
        comp.fileContainers.find {it.name == 'startscripts'}.types.size() == 2
        comp.fileContainers.find {it.name == 'startscripts'}.excludesFromUpdate.containsAll(["**/dir1/*.com", "**/dir2/*.com", "**/dir3/*.com"])
        comp.fileContainers.find {it.name == 'startscripts'}.excludesFromUpdate.size() == 3
        comp.fileContainers.find {it.name == 'sites'}.excludesFromUpdate.containsAll(["**/dir1/*.com"])
        comp.fileContainers.find {it.name == 'sites'}.excludesFromUpdate.size() == 1
        comp.fileItems.findAll {it.types.size() == 1 && it.types.containsAll(["intTest"]) }.size() == 2
        comp.properties.findAll {it.types.size() == 1 && it.types.containsAll(["perfTest"]) }.size() == 2

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
    def 'Test classifiers handling - #gradleVersion'(gradleVersion) {
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
                addType('test')
                add('startscriptsLinux') {
                    addType('production')
                    baseName = 'startscripts'
                    itemType = 'bin'
                    targetPath = 'bin'
                    source(fileTree(dir: 'src/bin', include: '*.sh').files)
                    classifier = 'linux'
                }
                add('startscriptsWin') {
                    addType('production')
                    baseName = 'startscripts'
                    itemType = 'bin'
                    targetPath = 'bin'
                    source(fileTree(dir: 'src/bin', include: '*.bat').files)
                    classifier = 'win'
                }
                add('sites') {
                    baseName = 'share'
                    itemType = 'sites'
                    targetPath = 'share'
                    source(fileTree(dir: 'sites', include: '**/**/*.*').files)
                }
            }

            modules {
                addType('test')
                add("com.intershop:testmodule1:1.0.0")
                add("com.intershop:testmodule2:1.0.0")
                add("com.intershop:testmodule3:1.0.0")
            }
            
            libs {
                addType('test')
                add("com.intershop:library1:1.0.0")
                targetPath = "lib/release/libs"
            }

            dependencyMngt.classpathVerification.enabled = false

            fileItems {
                add(file("conf/server/test1.properties")) {
                    targetPath = 'share/system/config'
                    classifier = 'win'
                }
                add(file("conf/server/test2.properties")) {
                    targetPath = 'share/system/config'
                    classifier = 'linux'
                }
            }

            propertyItems {
                addType('perfTest')
                add("pkey1") {
                    value = "pvalues1"
                    classifier = "win"
                }
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

        when:
        List<String> args = ['publish', '-s', '-i']
        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':publish').outcome == TaskOutcome.SUCCESS
        result1.task(':createComponent').outcome == TaskOutcome.SUCCESS
        result1.task(':zipContainerStartscriptsWin').outcome == TaskOutcome.SUCCESS
        result1.task(':zipContainerStartscriptsLinux').outcome == TaskOutcome.SUCCESS
        result1.task(':verifyClasspath').outcome == TaskOutcome.SKIPPED
        outputFile.exists()
        Component comp = ComponentUtil.INSTANCE.componentFromFile(outputFile)
        comp.classifiers.containsAll([ "linux", "win", "" ])
        comp.fileContainers.find {it.name == 'startscriptsLinux'}.classifier == 'linux'
        comp.fileContainers.find {it.name == 'startscriptsWin'}.classifier == 'win'
        comp.fileItems.findAll { it.classifier == 'linux' }.size() == 1
        comp.properties.findAll { it.classifier == 'win' }.size() == 1
        comp.modules['testmodule3'].classifiers.containsAll([ "linux", "win" ])

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':publish').outcome == TaskOutcome.SUCCESS
        result2.task(':createComponent').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':zipContainerStartscriptsWin').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':zipContainerStartscriptsLinux').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':verifyClasspath').outcome == TaskOutcome.SKIPPED

        where:
        gradleVersion << supportedGradleVersions
    }

    @Unroll
    def 'Test descriptor configuration - #gradleVersion'(gradleVersion){
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

            addUpdateExcludePattern("**/testexclude1/*.com")
            addUpdateExcludePattern([ "**/testexclude2/*.com", "**/testexclude3/*.com" ] as Set)

            modules {
                add("com.intershop:testmodule1:1.0.0")
                add("com.intershop:testmodule2:1.0.0")
            }

            dependencyMngt.classpathVerification.enabled = false
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
        outputFile.text.contains('**/testexclude1/*.com')
        outputFile.text.contains('**/testexclude2/*.com')
        outputFile.text.contains('**/testexclude3/*.com')

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
    def 'Test plugin with version conflicts - #gradleVersion'(gradleVersion) {
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

        publishing {
            ${TestIvyRepoBuilder.declareRepository(new File(testProjectDir, 'repo'), 'ivyTest', ivyPattern, artifactPattern)}
        }
        """.stripIndent()

        when:
        List<String> args = ['publish']
        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .buildAndFail()

        then:
        result1.task(':createComponent').outcome == TaskOutcome.FAILED
        result1.output.contains("There is a version conflict! commons-logging:commons-logging exists in the list of resolved dependencies. [source: org.springframework:spring-web:4.1.6.RELEASE, availabble: commons-logging:commons-logging:1.2, new: commons-logging:commons-logging:1.1.3] Check your logs and configuration!")
        result1.output.contains("There is a version conflict! Check your logs and configuration!")

        where:
        gradleVersion << supportedGradleVersions
    }

    @Unroll
    def 'Test plugin with class collision - #gradleVersion'(gradleVersion) {
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

        publishing {
            ${TestIvyRepoBuilder.declareRepository(new File(testProjectDir, 'repo'), 'ivyTest', ivyPattern, artifactPattern)}
        }
        """.stripIndent()

        File report = new File(testProjectDir, "/build/componentBuild/reports/classcollision/collisionReport.txt")

        when:
        List<String> args = ['publish']
        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .buildAndFail()

        then:
        result1.task(':verifyClasspath').outcome == TaskOutcome.FAILED
        result1.output.contains("There are class collisions! Check ${testProjectDir.absolutePath}/build/componentBuild/reports/classcollision/collisionReport.txt")
        report.exists()

        report.text.contains("== Duplicates for org.ow2.asm:asm:5.1")
        report.text.contains("- org/objectweb/asm/AnnotationVisitor.class also in org.ow2.asm:asm-all:4.2")
        report.text.contains("== Duplicates for org.ow2.asm:asm-all:4.2")
        report.text.contains("- org/objectweb/asm/AnnotationVisitor.class also in org.ow2.asm:asm:5.1")

        where:
        gradleVersion << supportedGradleVersions
    }

    @Unroll
    def 'Test class collision with missing lib - #gradleVersion'(gradleVersion) {
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
                add('javax.validation:validation-api:10.0.0.GA')
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

        publishing {
            ${TestIvyRepoBuilder.declareRepository(new File(testProjectDir, 'repo'), 'ivyTest', ivyPattern, artifactPattern)}
        }
        """.stripIndent()

        File report = new File(testProjectDir, "/build/componentBuild/reports/classcollision/collisionReport.txt")

        when:
        List<String> args = ['publish']
        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .buildAndFail()

        then:
        result1.task(':verifyClasspath').outcome == TaskOutcome.FAILED
        result1.output.contains("Could not resolve library dependency for javax.validation:validation-api")

        where:
        gradleVersion << supportedGradleVersions
    }

    @Unroll
    def 'Test incremtal build for SNAPSHOT modules - #gradleVersion'(gradleVersion) {
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
            containers {
                add('startscripts') {
                    baseName = 'startscripts'
                    itemType = 'bin'
                    targetPath = 'bin'
                    source(fileTree(dir: 'src/bin', include: '*.sh').files)
                }
            }

            modules {
                add("com.intershop:testmodule1:1.0.0")
                add("com.intershop:testmodule4:1.0.0-SNAPSHOT")
            }

            libs {
                add("com.intershop:library1:1.0.0")
                targetPath = "lib/release/libs"
            }

            dependencyMngt.classpathVerification.enabled = false
        }
        
        ${createRepo(testProjectDir)}

 
        //}
        publishing {
            ${TestIvyRepoBuilder.declareRepository(new File(testProjectDir, 'repo'), 'ivyTest', ivyPattern, artifactPattern)}
        }
        
        """.stripIndent()

        File outputFile = new File(testProjectDir, 'build/componentBuild/descriptor/file.component')

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

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':publish').outcome == TaskOutcome.SUCCESS
        result2.task(':createComponent').outcome == TaskOutcome.SUCCESS
        result2.task(':zipContainerStartscripts').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':verifyClasspath').outcome == TaskOutcome.SKIPPED

        where:
        gradleVersion << supportedGradleVersions
    }

    @Unroll
    def 'Test incremtal build for LOCAL modules - #gradleVersion'(gradleVersion) {
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
            containers {
                add('startscripts') {
                    baseName = 'startscripts'
                    itemType = 'bin'
                    targetPath = 'bin'
                    source(fileTree(dir: 'src/bin', include: '*.sh').files)
                }
            }

            modules {
                add("com.intershop:testmodule1:1.0.0")
                add("com.intershop:testmodule5:1.0.0-LOCAL")
            }

            libs {
                add("com.intershop:library1:1.0.0")
                targetPath = "lib/release/libs"
            }

            dependencyMngt.classpathVerification.enabled = false
        }
        
        ${createRepo(testProjectDir)}

 
        //}
        publishing {
            ${TestIvyRepoBuilder.declareRepository(new File(testProjectDir, 'repo'), 'ivyTest', ivyPattern, artifactPattern)}
        }
        
        """.stripIndent()

        File outputFile = new File(testProjectDir, 'build/componentBuild/descriptor/file.component')

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

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':publish').outcome == TaskOutcome.SUCCESS
        result2.task(':createComponent').outcome == TaskOutcome.SUCCESS
        result2.task(':zipContainerStartscripts').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':verifyClasspath').outcome == TaskOutcome.SKIPPED

        where:
        gradleVersion << supportedGradleVersions
    }

    @Unroll
    def 'Test incremtal build for SNAPSHOT libraries - #gradleVersion'(gradleVersion) {
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
            containers {
                add('startscripts') {
                    baseName = 'startscripts'
                    itemType = 'bin'
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
                add("com.intershop:library5:1.0.0-SNAPSHOT")
                targetPath = "lib/release/libs"
            }

            dependencyMngt.classpathVerification.enabled = false
        }
        
        ${createRepo(testProjectDir)}

 
        //}
        publishing {
            ${TestIvyRepoBuilder.declareRepository(new File(testProjectDir, 'repo'), 'ivyTest', ivyPattern, artifactPattern)}
        }
        
        """.stripIndent()

        File outputFile = new File(testProjectDir, 'build/componentBuild/descriptor/file.component')

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

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':publish').outcome == TaskOutcome.SUCCESS
        result2.task(':createComponent').outcome == TaskOutcome.SUCCESS
        result2.task(':zipContainerStartscripts').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':verifyClasspath').outcome == TaskOutcome.SKIPPED

        where:
        gradleVersion << supportedGradleVersions
    }

    @Unroll
    def 'Test incremtal build for LOCAL libraries - #gradleVersion'(gradleVersion) {
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
            containers {
                add('startscripts') {
                    baseName = 'startscripts'
                    itemType = 'bin'
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
                add("com.intershop:library6:1.0.0-LOCAL")
                targetPath = "lib/release/libs"
            }

            dependencyMngt.classpathVerification.enabled = false
        }
        
        ${createRepo(testProjectDir)}

 
        //}
        publishing {
            ${TestIvyRepoBuilder.declareRepository(new File(testProjectDir, 'repo'), 'ivyTest', ivyPattern, artifactPattern)}
        }
        
        """.stripIndent()

        File outputFile = new File(testProjectDir, 'build/componentBuild/descriptor/file.component')

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

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':publish').outcome == TaskOutcome.SUCCESS
        result2.task(':createComponent').outcome == TaskOutcome.SUCCESS
        result2.task(':zipContainerStartscripts').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':verifyClasspath').outcome == TaskOutcome.SKIPPED

        where:
        gradleVersion << supportedGradleVersions
    }

    @Unroll
    def 'Test missing module handling - #gradleVersion'(gradleVersion) {
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
            containers {
                add('startscripts') {
                    baseName = 'startscripts'
                    itemType = 'bin'
                    targetPath = 'bin'
                    source(fileTree(dir: 'src/bin', include: '*.sh').files)
                }
            }

            modules {
                add("com.intershop:testmodule1:1.0.0")
                add("com.intershop:testmodule4:1.0.0")
            }

            libs {
                add("com.intershop:library1:1.0.0")
                targetPath = "lib/release/libs"
            }

            dependencyMngt.classpathVerification.enabled = false
        }
        
        ${createRepo(testProjectDir)}

 
        //}
        publishing {
            ${TestIvyRepoBuilder.declareRepository(new File(testProjectDir, 'repo'), 'ivyTest', ivyPattern, artifactPattern)}
        }
        
        """.stripIndent()

        File outputFile = new File(testProjectDir, 'build/componentBuild/descriptor/file.component')

        when:
        List<String> args = ['publish', '-s', '-i']
        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .buildAndFail()

        then:
        result1.task(':createComponent').outcome == TaskOutcome.FAILED
        result1.output.contains("Could not resolve module dependency for com.intershop")

        where:
        gradleVersion << supportedGradleVersions
    }

    @Unroll
    def 'Test missing lib handling - #gradleVersion'(gradleVersion) {
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
            containers {
                add('startscripts') {
                    baseName = 'startscripts'
                    itemType = 'bin'
                    targetPath = 'bin'
                    source(fileTree(dir: 'src/bin', include: '*.sh').files)
                }
            }

            modules {
                add("com.intershop:testmodule1:1.0.0")
                add("com.intershop:testmodule2:1.0.0")
            }

            libs {
                add("com.intershop:library5:1.0.0")
                add("com.intershop:library1:1.0.0")
                targetPath = "lib/release/libs"
            }

            dependencyMngt.classpathVerification.enabled = false
        }
        
        ${createRepo(testProjectDir)}

 
        //}
        publishing {
            ${TestIvyRepoBuilder.declareRepository(new File(testProjectDir, 'repo'), 'ivyTest', ivyPattern, artifactPattern)}
        }
        
        """.stripIndent()

        File outputFile = new File(testProjectDir, 'build/componentBuild/descriptor/file.component')

        when:
        List<String> args = ['publish', '-s', '-i']
        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .buildAndFail()

        then:
        result1.task(':createComponent').outcome == TaskOutcome.FAILED
        result1.output.contains("Could not resolve library dependency for com.intershop")

        where:
        gradleVersion << supportedGradleVersions
    }

    @Unroll
    def 'Test plugin with multi project and project dependencies and ivy configuration - #gradleVersion'(gradleVersion) {
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
       
                    dependencyMngt.classpathVerification.enabled = false
                }
                
                publishing {                     
                     ${TestIvyRepoBuilder.declareRepository(new File(testProjectDir, 'repo'), 'ivyTest', ivyPattern, artifactPattern)}
                }

                ${TestIvyRepoBuilder.declareRepository(new File(testProjectDir, 'repo'), 'ivyTest', ivyPattern, artifactPattern)}
                ${TestMavenRepoBuilder.declareRepository(new File(testProjectDir, 'repo'))}

        """.stripIndent()

        File subProject = createSubProject('projectComponent', settingsfile, compProjectBuild)
        File compFile = new File(testProjectDir, "projectComponent/build/componentBuild/descriptor/file.component")

        when:
        List<String> args = ['publish', '-s', '-i']
        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':projectComponent:createComponent').outcome == TaskOutcome.SUCCESS
        compFile.exists()
        compFile.text.contains('"pkgs" : [ "project1a-bin", "project1a-sites" ],')
        compFile.text.contains('"jars" : [ "project1a" ],')
        compFile.text.contains('"pkgs" : [ "project2b-bin", "project2b-sites" ],')
        compFile.text.contains('"jars" : [ "project2b" ],')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Unroll
    def 'Test plugin with multi project and project dependencies and maven configuration - #gradleVersion'(gradleVersion) {
        given:
        String projectName = "testproject"
        File settingsfile = createSettingsGradle(projectName)

        createSubProjectJava('project1a', settingsfile, 'com.intereshop.a','maven')
        createSubProjectJava('project2b', settingsfile, 'com.intereshop.b', 'maven')

        buildFile << """
                plugins {
                    id 'maven-publish'
                    id 'com.intershop.gradle.component.build'
                }
               
                group 'com.intershop.test'
                version = '1.0.0'
                
                ${createRepo(testProjectDir)}
        """.stripIndent()

        def compProjectBuild = """
                apply plugin: 'maven-publish'
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
       
                    dependencyMngt.classpathVerification.enabled = false
                }
                
                publishing {
                    ${TestMavenRepoBuilder.declareRepository(new File(testProjectDir, 'repo'))}
                }

                ${TestIvyRepoBuilder.declareRepository(new File(testProjectDir, 'repo'), 'ivyTest', ivyPattern, artifactPattern)}
                ${TestMavenRepoBuilder.declareRepository(new File(testProjectDir, 'repo'))}

        """.stripIndent()

        File subProject = createSubProject('projectComponent', settingsfile, compProjectBuild)
        File compFile = new File(testProjectDir, "projectComponent/build/componentBuild/descriptor/file.component")

        when:
        List<String> args = ['publish', '-s', '-i']
        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':projectComponent:createComponent').outcome == TaskOutcome.SUCCESS
        compFile.exists()
        compFile.text.contains('"pkgs" : [ "project1a-bin", "project1a-sites" ],')
        compFile.text.contains('"jars" : [ "project1a" ],')
        compFile.text.contains('"pkgs" : [ "project2b-bin", "project2b-sites" ],')
        compFile.text.contains('"jars" : [ "project2b" ],')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Unroll
    def 'Test plugin with multi project and project dependencies without publishing - #gradleVersion'(gradleVersion) {
        given:
        String projectName = "testproject"
        File settingsfile = createSettingsGradle(projectName)

        createSubProjectJava('project1a', settingsfile, 'com.intereshop.a', "")
        createSubProjectJava('project2b', settingsfile, 'com.intereshop.b', "")

        buildFile << """
                plugins {
                    id 'com.intershop.gradle.component.build'
                }
                
                group 'com.intershop.test'
                version = '1.0.0'
                
                ${createRepo(testProjectDir)}
        """.stripIndent()

        def compProjectBuild = """
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
       
                    dependencyMngt.classpathVerification.enabled = false
                }
                
                publishing {                     
                     ${TestIvyRepoBuilder.declareRepository(new File(testProjectDir, 'repo'), 'ivyTest', ivyPattern, artifactPattern)}
                }

                ${TestIvyRepoBuilder.declareRepository(new File(testProjectDir, 'repo'), 'ivyTest', ivyPattern, artifactPattern)}
                ${TestMavenRepoBuilder.declareRepository(new File(testProjectDir, 'repo'))}

        """.stripIndent()

        File subProject = createSubProject('projectComponent', settingsfile, compProjectBuild)

        File compFile = new File(testProjectDir, "projectComponent/build/componentBuild/descriptor/file.component")

        when:
        List<String> args = ['createComponent', '-s', '-i']
        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':projectComponent:createComponent').outcome == TaskOutcome.SUCCESS
        compFile.exists()
        compFile.text.contains('"pkgs" : [ "project1a-bin", "project1a-sites" ],')
        compFile.text.contains('"jars" : [ "project1a" ],')
        compFile.text.contains('"pkgs" : [ "project2b-bin", "project2b-sites" ],')
        compFile.text.contains('"jars" : [ "project2b" ],')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Unroll
    def 'Test incremental build with multi project and project dependencies without publishing - #gradleVersion'(gradleVersion) {
        given:
        String projectName = "testproject"
        File settingsfile = createSettingsGradle(projectName)

        createSubProjectJava('project1a', settingsfile, 'com.intereshop.a', "")
        createSubProjectJava('project2b', settingsfile, 'com.intereshop.b', "")

        buildFile << """
                plugins {
                    id 'com.intershop.gradle.component.build'
                }
                
                group 'com.intershop.test'
                version = '1.0.0'
                
                ${createRepo(testProjectDir)}
        """.stripIndent()

        def compProjectBuild = """
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
       
                    dependencyMngt.classpathVerification.enabled = false
                }
                
                publishing {                     
                     ${TestIvyRepoBuilder.declareRepository(new File(testProjectDir, 'repo'), 'ivyTest', ivyPattern, artifactPattern)}
                }

                ${TestIvyRepoBuilder.declareRepository(new File(testProjectDir, 'repo'), 'ivyTest', ivyPattern, artifactPattern)}
                ${TestMavenRepoBuilder.declareRepository(new File(testProjectDir, 'repo'))}

        """.stripIndent()

        File subProject = createSubProject('projectComponent', settingsfile, compProjectBuild)
        File compFile = new File(testProjectDir, "projectComponent/build/componentBuild/descriptor/file.component")

        when:
        List<String> args1 = ['createComponent', '-s', '-i']
        def result1 = getPreparedGradleRunner()
                .withArguments(args1)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':projectComponent:createComponent').outcome == TaskOutcome.SUCCESS
        compFile.exists()
        compFile.text.contains('"pkgs" : [ "project1a-bin", "project1a-sites" ],')
        compFile.text.contains('"jars" : [ "project1a" ],')
        compFile.text.contains('"pkgs" : [ "project2b-bin", "project2b-sites" ],')
        compFile.text.contains('"jars" : [ "project2b" ],')

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments(args1)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':projectComponent:createComponent').outcome == TaskOutcome.UP_TO_DATE

        when:
        List<String> args3 = ['createComponent', '--recreate', '-s', '-i']
        def result3 = getPreparedGradleRunner()
                .withArguments(args3)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result3.task(':projectComponent:createComponent').outcome == TaskOutcome.SUCCESS

        where:
        gradleVersion << supportedGradleVersions
    }

    @Unroll
    def 'Test plugin with multi project and missing project dependencies - #gradleVersion'(gradleVersion) {
        given:
        String projectName = "testproject"
        File settingsfile = createSettingsGradle(projectName)

        createSubProjectJava('project1a', settingsfile, 'com.intereshop.a', "")
        createSubProjectJava('project2b', settingsfile, 'com.intereshop.b', "")

        buildFile << """
                plugins {
                    id 'com.intershop.gradle.component.build'
                }
                
                group 'com.intershop.test'
                version = '1.0.0'
                
                ${createRepo(testProjectDir)}
        """.stripIndent()

        def compProjectBuild = """
                apply plugin: 'com.intershop.gradle.component.build'
                
                group 'com.intershop.testcomp'
                version = '1.0.0'
                
                component {
                    
                    modules {
                        add(project(':project1a'))
                        add(project(':project3b'))
                    }
                    
                    libs {
                        add("com.intershop:library1:1.0.0")
                        targetPath = "lib/release/libs"
                    }
       
                    dependencyMngt.classpathVerification.enabled = false
                }
                
                publishing {                     
                     ${TestIvyRepoBuilder.declareRepository(new File(testProjectDir, 'repo'), 'ivyTest', ivyPattern, artifactPattern)}
                }

                ${TestIvyRepoBuilder.declareRepository(new File(testProjectDir, 'repo'), 'ivyTest', ivyPattern, artifactPattern)}
                ${TestMavenRepoBuilder.declareRepository(new File(testProjectDir, 'repo'))}

        """.stripIndent()

        File subProject = createSubProject('projectComponent', settingsfile, compProjectBuild)

        File compFile = new File(testProjectDir, "projectComponent/build/componentBuild/descriptor/file.component")

        when:
        List<String> args = ['createComponent', '-s', '-i']
        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .buildAndFail()

        then:
        result1.output.contains("Project with path ':project3b' could not be found in project ':projectComponent")


        where:
        gradleVersion << supportedGradleVersions
    }

    private File createSubProjectJava(String projectPath, File settingsGradle, String packageName, String publish = 'ivy'){
        String ivyPublishing = """
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

        String mavenPublishing = """
            publishing {
                publications {
                    mvnIntershop(MavenPublication) {
                        from components.java
    
                        artifact(zipbin) {
                            classifier "bin"
                        }
                        artifact(zipsites) {
                            classifier "sites"
                        }
                    }
                }
                
                ${TestMavenRepoBuilder.declareRepository(new File(testProjectDir, 'repo'))}
            }
            """.stripIndent()

        String buildFileContentBase =
                """
                plugins {
                     id 'java'
                     ${publish == 'ivy' ? 'id "ivy-publish"' : ""}
                     ${publish == 'maven' ? 'id "maven-publish"' : ""}
                     
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
                ${publish == 'ivy' ? ivyPublishing : "" }
                ${publish == 'maven' ? mavenPublishing : ""}    
                """.stripIndent()

        File subProject = createSubProject(projectPath, settingsGradle, buildFileContentBase)
        writeJavaTestClass(packageName, subProject)

        def testFile1 = new File(subProject, 'staticfiles/bin/test1.sh')
        def testFile2 = new File(subProject, 'staticfiles/bin/test2.sh')
        def testFile3 = new File(subProject, 'staticfiles/bin/test1.bat')
        def testFile4 = new File(subProject, 'staticfiles/bin/test2.bat')
        testFile1.parentFile.mkdirs()

        testFile1 << """
        # testfile1.sh
        """.stripIndent()

        testFile2 << """
        # testfile2.sh
        """.stripIndent()

        testFile3 << """
        # testfile3.bat
        """.stripIndent()

        testFile4 << """
        # testfile4.bat
        """.stripIndent()

        def testFile5 = new File(subProject, 'sites/test/test3.txt')
        def testFile6 = new File(subProject, 'sites/test/test4.txt')
        testFile5.parentFile.mkdirs()

        testFile5 << """
        # testfile5 text
        """.stripIndent()

        testFile6 << """
        # testfile6 text
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

    private static File createStaticProjectFiles(File projectDir) {
        def testFile1 = new File(projectDir, 'src/bin/test1.sh')
        def testFile2 = new File(projectDir, 'src/bin/test2.sh')
        def testFile3 = new File(projectDir, 'src/bin/test1.bat')
        def testFile4 = new File(projectDir, 'src/bin/test2.bat')

        testFile1.parentFile.mkdirs()

        testFile1 << """
        # testfile1.sh
        """.stripIndent()

        testFile2 << """
        # testfile2.sh
        """.stripIndent()

        testFile3 << """
        # testfile3.bat
        """.stripIndent()

        testFile4 << """
        # testfile4.bat
        """.stripIndent()
    }

    private static File createAddStaticProjectFiles(File projectDir) {
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

    private static File createSingleStaticProjectFiles(File projectDir) {
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
            module(org: 'com.intershop', name: 'testmodule3', rev: '1.0.0') {
                artifact name: 'testmodule3', type: 'local', classifier: 'win', ext: 'zip', entries: [
                        ArchiveFileEntry.newInstance(path: 'testmodule/testfiles/test21.file', content: 'test21.file'),
                        ArchiveFileEntry.newInstance(path: 'testmodule/testconf/test22.conf', content: 'test22.conf'),
                        ArchiveDirectoryEntry.newInstance(path: 'testmodule/empttestdir')
                ]
                artifact name: 'testmodule3', type: 'local', classifier: 'linux', ext: 'zip', entries: [
                        ArchiveFileEntry.newInstance(path: 'testmodule/testfiles/test21.file', content: 'test21.file'),
                        ArchiveFileEntry.newInstance(path: 'testmodule/testconf/test22.conf', content: 'test22.conf'),
                        ArchiveDirectoryEntry.newInstance(path: 'testmodule/empttestdir')
                ]
                dependency org: 'com.intershop', name: 'library1', rev: '1.0.0'
            }
            module(org: 'com.intershop', name: 'testmodule41', rev: '1.0.0') {
                artifact name: 'testmodule41', type: 'cartridge', ext: 'zip', entries: [
                        ArchiveFileEntry.newInstance(path: 'testmodule/testfiles/test1.file', content: 'test1.file'),
                        ArchiveFileEntry.newInstance(path: 'testmodule/testconf/test2.conf', content: 'test2.conf'),
                        ArchiveDirectoryEntry.newInstance(path: 'testmodule/empttestdir')
                ]
                artifact name: 'testmodule41', type: 'jar', ext: 'jar', entries: [
                        ArchiveFileEntry.newInstance(path: 'com/class/intern/test1.file', content: 'interntest1.file'),
                        ArchiveFileEntry.newInstance(path: 'com/class/intern/test2.file', content: 'interntest2.file')
                ]
                dependency org: 'com.intershop', name: 'testmodule4', rev: '1.0.0-SNAPSHOT'
            }
            module(org: 'com.intershop', name: 'testmodule4', rev: '1.0.0-SNAPSHOT') {
                artifact name: 'testmodule4', type: 'cartridge', ext: 'zip', entries: [
                        ArchiveFileEntry.newInstance(path: 'testmodule/testfiles/test1.file', content: 'test1.file'),
                        ArchiveFileEntry.newInstance(path: 'testmodule/testconf/test2.conf', content: 'test2.conf'),
                        ArchiveDirectoryEntry.newInstance(path: 'testmodule/empttestdir')
                ]
                artifact name: 'testmodule4', type: 'jar', ext: 'jar', entries: [
                        ArchiveFileEntry.newInstance(path: 'com/class/intern/test1.file', content: 'interntest1.file'),
                        ArchiveFileEntry.newInstance(path: 'com/class/intern/test2.file', content: 'interntest2.file')
                ]
            }
            module(org: 'com.intershop', name: 'testmodule5', rev: '1.0.0-LOCAL') {
                artifact name: 'testmodule4', type: 'cartridge', ext: 'zip', entries: [
                        ArchiveFileEntry.newInstance(path: 'testmodule/testfiles/test1.file', content: 'test1.file'),
                        ArchiveFileEntry.newInstance(path: 'testmodule/testconf/test2.conf', content: 'test2.conf'),
                        ArchiveDirectoryEntry.newInstance(path: 'testmodule/empttestdir')
                ]
                artifact name: 'testmodule4', type: 'jar', ext: 'jar', entries: [
                        ArchiveFileEntry.newInstance(path: 'com/class/intern/test1.file', content: 'interntest1.file'),
                        ArchiveFileEntry.newInstance(path: 'com/class/intern/test2.file', content: 'interntest2.file')
                ]
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
            project(groupId: 'com.intershop', artifactId: 'library5', version: '1.0.0-SNAPSHOT'){
                artifact entries: [
                        ArchiveFileEntry.newInstance(path: 'com/class/test1.file', content: 'test1.file'),
                        ArchiveFileEntry.newInstance(path: 'com/class/test2.file', content: 'test2.file'),
                ]
            }
            project(groupId: 'com.intershop', artifactId: 'library6', version: '1.0.0-LOCAL'){
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
