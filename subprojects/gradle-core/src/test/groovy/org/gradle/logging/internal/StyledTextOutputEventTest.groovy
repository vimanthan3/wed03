/*
 * Copyright 2010 the original author or authors.
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
package org.gradle.logging.internal

import org.gradle.api.logging.LogLevel
import spock.lang.Specification
import org.gradle.logging.StyledTextOutput

class StyledTextOutputEventTest extends Specification {

    def canSetLogLevel() {
        def event = new StyledTextOutputEvent(100, 'category', 'message').withLogLevel(LogLevel.DEBUG)

        expect:
        event.logLevel == LogLevel.DEBUG
    }
    
    def rendersToTextOutput() {
        OutputEventTextOutput output = Mock()
        def event = new StyledTextOutputEvent(StyledTextOutput.Style.UserInput, 100, 'category', 'message')

        when:
        event.render(output)

        then:
        1 * output.style(StyledTextOutput.Style.UserInput)
        1 * output.text('message')
        0 * output._
    }
    
    def rendersMultipleSpansToTextOutput() {
        OutputEventTextOutput output = Mock()
        List spans = [new StyledTextOutputEvent.Span(StyledTextOutput.Style.UserInput, 'UserInput'),
                new StyledTextOutputEvent.Span(StyledTextOutput.Style.Normal, 'Normal'),
                new StyledTextOutputEvent.Span(StyledTextOutput.Style.Header, 'Header')
        ]
        def event = new StyledTextOutputEvent(100, 'category', spans)

        when:
        event.render(output)

        then:
        1 * output.style(StyledTextOutput.Style.UserInput)
        1 * output.text('UserInput')
        1 * output.style(StyledTextOutput.Style.Normal)
        1 * output.text('Normal')
        1 * output.style(StyledTextOutput.Style.Header)
        1 * output.text('Header')
        0 * output._
    }
}
