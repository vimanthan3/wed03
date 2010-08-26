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

import org.fusesource.jansi.Ansi
import org.gradle.util.JUnit4GroovyMockery
import org.jmock.integration.junit4.JMock
import org.junit.Test
import org.junit.runner.RunWith
import org.gradle.logging.StyledTextOutput
import org.fusesource.jansi.Ansi.Attribute
import org.fusesource.jansi.Ansi.Color

@RunWith(JMock.class)
class AnsiConsoleTest {
    private static final String EOL = System.getProperty('line.separator')
    private final JUnit4GroovyMockery context = new JUnit4GroovyMockery()
    private final Ansi ansi = context.mock(Ansi.class)
    private final Appendable target = {} as Appendable
    private final Flushable flushable = {} as Flushable
    private final AnsiConsole console = new AnsiConsole(target, flushable) {
        def Ansi createAnsi() {
            return ansi
        }
    }

    @Test
    public void appendsTextToMainArea() {
        context.checking {
            one(ansi).a('message')
        }

        console.mainArea.append('message')

        context.checking {
            one(ansi).a('message2')
            one(ansi).a(EOL)
            one(ansi).a('message3')
        }

        console.mainArea.append("message2${EOL}message3")
    }

    @Test
    public void appendsStyledTextToMainArea() {
        context.checking {
            one(ansi).fg(Color.YELLOW)
            one(ansi).a('message')
            one(ansi).fg(Color.DEFAULT)
        }

        console.mainArea.style(StyledTextOutput.Style.Header).append('message')
    }

    @Test
    public void displaysStatusBarWithNonEmptyText() {
        def statusBar = console.addStatusBar()

        context.checking {
            one(ansi).a(Attribute.INTENSITY_BOLD)
            one(ansi).a('text')
            one(ansi).a(Attribute.INTENSITY_BOLD_OFF)
        }

        statusBar.text = 'text'
    }

    @Test
    public void displaysStatusBarWhenTextInMainArea() {
        context.checking {
            one(ansi).a('message')
            one(ansi).a(EOL)
        }

        console.mainArea.append("message${EOL}")

        def statusBar = console.addStatusBar()

        context.checking {
            one(ansi).a(Attribute.INTENSITY_BOLD)
            one(ansi).a('text')
            one(ansi).a(Attribute.INTENSITY_BOLD_OFF)
        }

        statusBar.text = 'text'
    }

    @Test
    public void redrawsStatusBarWhenTextChangesValue() {
        def statusBar = console.addStatusBar()

        context.checking {
            one(ansi).a(Attribute.INTENSITY_BOLD)
            one(ansi).a('123')
            one(ansi).a(Attribute.INTENSITY_BOLD_OFF)
        }

        statusBar.text = '123'

        context.checking {
            one(ansi).cursorLeft(3)
            one(ansi).a(Attribute.INTENSITY_BOLD)
            one(ansi).a('abc')
            one(ansi).a(Attribute.INTENSITY_BOLD_OFF)
        }

        statusBar.text = 'abc'
    }

    @Test
    public void redrawsStatusBarWhenTextChangesSuffix() {
        def statusBar = console.addStatusBar()

        context.checking {
            one(ansi).a(Attribute.INTENSITY_BOLD)
            one(ansi).a('text 1')
            one(ansi).a(Attribute.INTENSITY_BOLD_OFF)
        }

        statusBar.text = 'text 1'

        context.checking {
            one(ansi).cursorLeft(1)
            one(ansi).a(Attribute.INTENSITY_BOLD)
            one(ansi).a('2')
            one(ansi).a(Attribute.INTENSITY_BOLD_OFF)
        }

        statusBar.text = 'text 2'
    }

    @Test
    public void redrawsStatusBarWhenTextAdded() {
        def statusBar = console.addStatusBar()

        context.checking {
            one(ansi).a(Attribute.INTENSITY_BOLD)
            one(ansi).a('text')
            one(ansi).a(Attribute.INTENSITY_BOLD_OFF)
        }

        statusBar.text = 'text'

        context.checking {
            one(ansi).a(Attribute.INTENSITY_BOLD)
            one(ansi).a(' 2')
            one(ansi).a(Attribute.INTENSITY_BOLD_OFF)
        }

        statusBar.text = 'text 2'
    }

    @Test
    public void redrawsStatusBarWhenTextRemoved() {
        def statusBar = console.addStatusBar()

        context.checking {
            one(ansi).a(Attribute.INTENSITY_BOLD)
            one(ansi).a('text 1')
            one(ansi).a(Attribute.INTENSITY_BOLD_OFF)
        }

        statusBar.text = 'text 1'

        context.checking {
            one(ansi).cursorLeft(3)
            one(ansi).eraseLine(Ansi.Erase.FORWARD)
        }

        statusBar.text = 'tex'
    }
    
    @Test
    public void redrawsStatusBarWhenTextSetToEmpty() {
        def statusBar = console.addStatusBar()

        context.checking {
            one(ansi).a(Attribute.INTENSITY_BOLD)
            one(ansi).a('text')
            one(ansi).a(Attribute.INTENSITY_BOLD_OFF)
        }

        statusBar.text = 'text'

        context.checking {
            one(ansi).cursorLeft(4)
            one(ansi).eraseLine(Ansi.Erase.FORWARD)
        }

        statusBar.text = ''
    }

    @Test
    public void removesStatusBarWhenClosed() {
        def statusBar = console.addStatusBar()

        context.checking {
            one(ansi).a(Attribute.INTENSITY_BOLD)
            one(ansi).a('text')
            one(ansi).a(Attribute.INTENSITY_BOLD_OFF)
        }

        statusBar.text = 'text'

        context.checking {
            one(ansi).cursorLeft(4)
            one(ansi).eraseLine(Ansi.Erase.FORWARD)
        }

        statusBar.close();
    }

    @Test
    public void showsMostRecentlyCreatedStatusBarOnly() {
        context.checking {
            one(ansi).a(Attribute.INTENSITY_BOLD)
            one(ansi).a('first')
            one(ansi).a(Attribute.INTENSITY_BOLD_OFF)
        }

        console.addStatusBar().text = 'first'

        context.checking {
            one(ansi).cursorLeft(5)
            one(ansi).eraseLine(Ansi.Erase.FORWARD)
        }

        Label second = console.addStatusBar()

        context.checking {
            one(ansi).a(Attribute.INTENSITY_BOLD)
            one(ansi).a('second')
            one(ansi).a(Attribute.INTENSITY_BOLD_OFF)
        }

        second.text = 'second'

        context.checking {
            one(ansi).cursorLeft(6)
            one(ansi).eraseLine(Ansi.Erase.FORWARD)
            one(ansi).a(Attribute.INTENSITY_BOLD)
            one(ansi).a('first')
            one(ansi).a(Attribute.INTENSITY_BOLD_OFF)
        }

        second.close()
    }

    @Test
    public void appendsTextWhenStatusBarIsPresent() {
        context.checking {
            one(ansi).a(Attribute.INTENSITY_BOLD)
            one(ansi).a('status')
            one(ansi).a(Attribute.INTENSITY_BOLD_OFF)
        }

        console.addStatusBar().text = 'status'

        context.checking {
            one(ansi).cursorLeft(6)
            one(ansi).eraseLine(Ansi.Erase.FORWARD)
            one(ansi).a('message')
            one(ansi).a(EOL)
            one(ansi).a(Attribute.INTENSITY_BOLD)
            one(ansi).a('status')
            one(ansi).a(Attribute.INTENSITY_BOLD_OFF)
        }

        console.mainArea.append("message$EOL");
    }

    @Test
    public void appendsTextWithNoEOLWhenStatusBarIsPresent() {
        context.checking {
            one(ansi).a(Attribute.INTENSITY_BOLD)
            one(ansi).a('status')
            one(ansi).a(Attribute.INTENSITY_BOLD_OFF)
        }

        console.addStatusBar().text = 'status'

        context.checking {
            one(ansi).cursorLeft(6)
            one(ansi).eraseLine(Ansi.Erase.FORWARD)
            one(ansi).a('message')
            one(ansi).newline()
            one(ansi).a(Attribute.INTENSITY_BOLD)
            one(ansi).a('status')
            one(ansi).a(Attribute.INTENSITY_BOLD_OFF)
        }

        console.mainArea.append('message');

        context.checking {
            one(ansi).cursorLeft(6)
            one(ansi).eraseLine(Ansi.Erase.FORWARD)
            one(ansi).cursorUp(1)
            one(ansi).cursorRight(7)
            one(ansi).a('message2')
            one(ansi).newline()
            one(ansi).a(Attribute.INTENSITY_BOLD)
            one(ansi).a('status')
            one(ansi).a(Attribute.INTENSITY_BOLD_OFF)
        }

        console.mainArea.append('message2');
    }

    @Test
    public void addsStatusBarWhenNoTrailingEOLInMainArea() {
        context.checking {
            one(ansi).a('message')
        }

        console.mainArea.append('message')

        context.checking {
            one(ansi).newline()
            one(ansi).a(Attribute.INTENSITY_BOLD)
            one(ansi).a('status')
            one(ansi).a(Attribute.INTENSITY_BOLD_OFF)
        }

        console.addStatusBar().text = 'status'

        context.checking {
            one(ansi).cursorLeft(6)
            one(ansi).eraseLine(Ansi.Erase.FORWARD)
            one(ansi).cursorUp(1)
            one(ansi).cursorRight(7)
            one(ansi).a('message2')
            one(ansi).a(EOL)
            one(ansi).a(Attribute.INTENSITY_BOLD)
            one(ansi).a('status')
            one(ansi).a(Attribute.INTENSITY_BOLD_OFF)
        }

        console.mainArea.append("message2${EOL}")
    }
}

