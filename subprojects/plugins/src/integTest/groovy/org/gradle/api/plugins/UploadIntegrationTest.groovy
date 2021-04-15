/*
 * Copyright 2021 the original author or authors.
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


package org.gradle.api.plugins

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.TestResources
import org.gradle.integtests.fixtures.UnsupportedWithConfigurationCache
import org.junit.Rule
import spock.lang.Issue
import spock.lang.Unroll

class UploadIntegrationTest extends AbstractIntegrationSpec {

    def "Upload task is deprecated"() {
        given:
        buildFile << """
            plugins {
                id 'base'
            }

            tasks.register('myUpload', Upload) {
                configuration = configurations.default
            }
        """

        expect:
        executer.expectDocumentedDeprecationWarning("The task type org.gradle.api.tasks.Upload (used by the :myUpload task) has been deprecated. This is scheduled to be removed in Gradle 8.0. Consult the upgrading guide for further information: https://docs.gradle.org/current/userguide/upgrading_version_7.html#upload_task_deprecation")
        succeeds 'myUpload'
    }
}
