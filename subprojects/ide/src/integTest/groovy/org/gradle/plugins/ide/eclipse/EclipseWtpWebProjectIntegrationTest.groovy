/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.gradle.plugins.ide.eclipse

import spock.lang.Issue

class EclipseWtpWebProjectIntegrationTest extends AbstractEclipseIntegrationSpec {
    def "generates configuration files for a web project"() {
        file('src/main/java').mkdirs()
        file('src/main/resources').mkdirs()
        file('src/main/webapp').mkdirs()

        settingsFile << "rootProject.name = 'web'"

        buildFile << """
apply plugin: 'eclipse-wtp'
apply plugin: 'war'

sourceCompatibility = 1.6

repositories {
    jcenter()
}

dependencies {
    compile 'com.google.guava:guava:18.0'
    providedCompile 'javax.servlet:javax.servlet-api:3.1.0'
    testCompile "junit:junit:4.12"
}
"""

        when:
        run "eclipse"

        then:
        // Builders and natures
        def project = project
        project.assertHasJavaFacetNatures()
        project.assertHasJavaFacetBuilders()

        // Classpath
        def classpath = classpath
        classpath.assertHasLibs('guava-18.0.jar', 'javax.servlet-api-3.1.0.jar', 'junit-4.12.jar', 'hamcrest-core-1.3.jar')
        classpath.lib('guava-18.0.jar').assertIsDeployedTo('/WEB-INF/lib')
        classpath.lib('javax.servlet-api-3.1.0.jar').assertIsExcludedFromDeployment()
        classpath.lib('junit-4.12.jar').assertIsExcludedFromDeployment()
        classpath.lib('hamcrest-core-1.3.jar').assertIsExcludedFromDeployment()

        // Facets
        def facets = wtpFacets
        facets.assertHasFixedFacets("jst.java", "jst.web")
        facets.assertHasInstalledFacets("jst.web", "jst.java")
        facets.assertFacetVersion("jst.web", "2.4")
        facets.assertFacetVersion("jst.java", "6.0")

        // Deployment
        def component = wtpComponent
        component.deployName == 'web'
        component.resources.size() == 3
        component.sourceDirectory('src/main/java').assertDeployedAt('/WEB-INF/classes')
        component.sourceDirectory('src/main/resources').assertDeployedAt('/WEB-INF/classes')
        component.sourceDirectory('src/main/webapp').assertDeployedAt('/')
        component.modules.size() == 0
    }


    @Issue('GRADLE-2123')
    def 'eclipse-wtp with war project should add "org.eclipse.jst.component.dependency" to all wtp dependencies instead of adding them to the WTP component'() {
        given:
        def repoDir = file('repo')
        maven(repoDir).module('dep', 'provided', '0.9').publish()
        maven(repoDir).module('dep', 'compile', '1.0').publish()
        maven(repoDir).module('dep', 'test', '2.0').publish()
        maven(repoDir).module('dep', 'no-wtp', '3.0').publish()
        maven(repoDir).module('dep', 'only-wtp', '4.0').publish()

        buildFile << """\
            apply plugin: 'war'
            apply plugin: 'eclipse-wtp'

            configurations {
                noWtp
                onlyWtp
            }

            repositories {
                maven {
                    url "${repoDir.toURI()}"
                }
            }

            dependencies {
                providedCompile 'dep:provided:0.9'
                compile 'dep:compile:1.0'
                testCompile 'dep:test:2.0'

                compile 'dep:no-wtp:3.0'
                noWtp 'dep:no-wtp:3.0'

                onlyWtp 'dep:only-wtp:4.0'
            }

            eclipse {
                wtp.component {
                    plusConfigurations << configurations.onlyWtp
                    minusConfigurations << configurations.noWtp
                }
            }
            """.stripIndent()

        when:
        run 'eclipse'

        then:
        def classpath = classpath
        classpath.assertHasLibs('provided-0.9.jar', 'compile-1.0.jar', 'test-2.0.jar', 'no-wtp-3.0.jar')
        classpath.lib('provided-0.9.jar').assertIsExcludedFromDeployment()
        classpath.lib('compile-1.0.jar').assertIsDeployedTo('/WEB-INF/lib')
        classpath.lib('test-2.0.jar').assertIsExcludedFromDeployment()
        classpath.lib('no-wtp-3.0.jar').assertIsExcludedFromDeployment()

        def component = wtpComponent
        component.modules.size() == 1
        component.lib('only-wtp-4.0.jar').assertDeployedAt('/WEB-INF/lib')
    }
}
