/*
 * Copyright 2012 the original author or authors.
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
package org.gradle.api.publish.maven

import org.gradle.integtests.fixtures.CrossVersionIntegrationSpec
import org.gradle.integtests.fixtures.TargetVersions
import org.gradle.test.fixtures.maven.MavenFileRepository

@TargetVersions('0.9+')
class MavenPublishCrossVersionIntegrationTest extends CrossVersionIntegrationSpec {

    final MavenFileRepository repo = new MavenFileRepository(file("maven-repo"))

    def "maven java publication generated by maven-publish plugin can be consumed by previous versions of Gradle"() {
        given:
        projectPublishedUsingMavenPublishPlugin('java')

        expect:
        consumePublicationWithPreviousVersion()

        file('build/resolved').assertHasDescendants('published-1.9.jar', 'test-project-1.2.jar')
    }

    def "maven war publication generated by maven-publish plugin can be consumed by previous versions of Gradle"() {
        given:
        projectPublishedUsingMavenPublishPlugin('web')

        expect:
        consumePublicationWithPreviousVersion()

        file('build/resolved').assertHasDescendants('published-1.9.war')
    }

    def projectPublishedUsingMavenPublishPlugin(def componentToPublish) {
        repo.module("org.gradle", "test-project", "1.2").publish()

        settingsFile.text = "rootProject.name = 'published'"

        buildFile.text = """
apply plugin: 'war'
apply plugin: 'maven-publish'

group = 'org.gradle.crossversion'
version = '1.9'

repositories {
    maven { url "${repo.uri}" }
}
dependencies {
    compile "org.gradle:test-project:1.2"
}
publishing {
    repositories {
        maven { url "${repo.uri}" }
    }
    publications {
        maven(MavenPublication) {
            from components.${componentToPublish}
        }
    }
}
"""

        version current withTasks 'publish' run()
    }

    def consumePublicationWithPreviousVersion() {
        settingsFile.text = "rootProject.name = 'consumer'"

        buildFile.text = """
configurations {
    lib
}
repositories {
    if (repositories.metaClass.respondsTo(repositories, 'maven')) {
        maven { url "${repo.uri}" }
    } else {
        mavenRepo urls: ["${repo.uri}"]
    }
}
dependencies {
    lib 'org.gradle.crossversion:published:1.9'
}
task retrieve(type: Sync) {
    into 'build/resolved'
    from configurations.lib
}
"""

        version previous requireOwnGradleUserHomeDir() withDeprecationChecksDisabled() withTasks 'retrieve' run()
    }
}
