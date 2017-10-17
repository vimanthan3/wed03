/*
 * Copyright 2016 the original author or authors.
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

package org.gradle.api.tasks

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import spock.lang.Unroll

class TaskInputFilePropertiesIntegrationTest extends AbstractIntegrationSpec {

    @Unroll
    def "allows optional @#annotation.simpleName to have null value"() {
        buildFile << """
            class CustomTask extends DefaultTask {
                @Optional @$annotation.simpleName input
                @TaskAction void doSomething() {
                    assert inputs.files.empty
                }
            }

            task customTask(type: CustomTask) {
                input = null
            }
        """

        expect:
        succeeds "customTask"

        where:
        annotation << [ InputFile, InputDirectory, InputFiles ]
    }

    def "allows optional @InputFile to have unset Provider<File> value"() {
        buildFile << """
            class CustomTask extends DefaultTask {
                @Optional @InputFile final RegularFileVar input = newInputFile()
                @TaskAction void doSomething() {
                    assert inputs.files.empty
                }
            }

            task customTask(type: CustomTask)
        """

        expect:
        succeeds "customTask"
    }

    def "fails with meaningful message when @InputFile have unset Provider<File> value"() {
        buildFile << """
            class CustomTask extends DefaultTask {
                @InputFile final RegularFileVar input = newInputFile()
                @TaskAction void doSomething() {
                    throw new GradleException("This task shouldn't execute")
                }
            }

            task customTask(type: CustomTask)
        """

        expect:
        fails "customTask"

        failure.assertHasDescription("A problem was found with the configuration of task ':customTask'.")
        failure.assertHasCause("No value has been specified for property 'input'.")
    }
}
