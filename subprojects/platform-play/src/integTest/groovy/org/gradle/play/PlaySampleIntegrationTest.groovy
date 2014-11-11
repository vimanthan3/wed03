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

package org.gradle.play
import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.Sample
import org.junit.Rule

class PlaySampleIntegrationTest extends AbstractIntegrationSpec {
    @Rule
    Sample playSample = new Sample(temporaryFolder, "play")

    def "can build play sample"() {
        given:
        sample playSample
        playSample.dir.file("build.gradle") << """
        tasks.withType(ScalaCompile) {
            scalaCompileOptions.forkOptions.memoryMaximumSize = '1g'
            scalaCompileOptions.forkOptions.jvmArgs = ['-XX:MaxPermSize=512m']
        }

        tasks.withType(TwirlCompile){
            forkOptions.memoryMaximumSize =  "256m"
        }"""
        expect:
        succeeds "assemble"
    }
}