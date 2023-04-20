/*
 * Copyright 2023 the original author or authors.
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

package org.gradle.configurationcache.serialization.codecs

import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Test
import java.util.Properties


class BaseTypeCodecTest : AbstractUserTypeCodecTest() {

    @Test
    fun `can handle Properties`() {
        val properties = Properties()
        properties.put("prop1", "value1")
        properties.put("prop2", "value2")
        configurationCacheRoundtripOf(properties).run {
            MatcherAssert.assertThat(properties, CoreMatchers.equalTo(this))
        }
    }
}
