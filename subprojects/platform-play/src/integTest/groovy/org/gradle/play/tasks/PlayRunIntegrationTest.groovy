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

package org.gradle.play.tasks
import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.TestResources
import org.junit.Rule

class PlayRunIntegrationTest extends AbstractIntegrationSpec {
    @Rule
    public final TestResources resources = new TestResources(temporaryFolder)

    def setup(){
        buildFile << """
        plugins {
            id 'play-application'
        }

        model {
            components {
                myApp(PlayApplicationSpec)
            }
        }

        repositories{
            jcenter()
            maven{
                name = "typesafe-maven-release"
                url = "http://repo.typesafe.com/typesafe/maven-releases"
            }
        }

        configurations{
            playRunConf
        }

        dependencies{
            playRunConf "com.typesafe.play:play_2.10:2.3.5"
            playRunConf "com.typesafe.play:play-docs_2.10:2.3.5"
        }
"""
    }

    def "can execute play run task"(){
        resources.maybeCopy("PlayRunIntegrationTest/playNew")
        when:
        succeeds("runMyApp")
        then:
        executed(":myApp")
    }
}
