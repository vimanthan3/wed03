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

package org.gradle.process.internal.streams;

import org.gradle.internal.UncheckedException;
import org.gradle.internal.operations.CurrentBuildOperationPreservingRunnable;
import org.gradle.process.internal.StreamsHandler;
import org.gradle.util.DisconnectableInputStream;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

/**
 * Reads from the process' stdout and stderr (if not merged into stdout) and forwards to {@link OutputStream}.
 */
public class OutputStreamsForwarder implements StreamsHandler {
    private final OutputStream standardOutput;
    private final OutputStream errorOutput;
    private final boolean readErrorStream;
    private final CountDownLatch completed;
    private Executor executor;
    private ExecOutputHandleRunner standardOutputReader;
    private ExecOutputHandleRunner standardErrorReader;

    public OutputStreamsForwarder(OutputStream standardOutput, OutputStream errorOutput, boolean readErrorStream) {
        this.standardOutput = standardOutput;
        this.errorOutput = errorOutput;
        this.readErrorStream = readErrorStream;
        this.completed = new CountDownLatch(readErrorStream ? 2 : 1);
    }

    @Override
    public void connectStreams(Process process, String processName, Executor executor) {
        this.executor = executor;
        standardOutputReader = new ExecOutputHandleRunner("read standard output of " + processName, wrapInDisconnectedInputStream(process.getInputStream()), standardOutput, completed);
        if (readErrorStream) {
            standardErrorReader = new ExecOutputHandleRunner("read error output of " + processName, wrapInDisconnectedInputStream(process.getErrorStream()), errorOutput, completed);
        }
    }

    public void start() {
        if (readErrorStream) {
            executor.execute(wrapInBuildOperation(standardErrorReader));
        }
        executor.execute(wrapInBuildOperation(standardOutputReader));
    }

    private InputStream wrapInDisconnectedInputStream(InputStream is) {
        return new DisconnectableInputStream(is);
    }

    private Runnable wrapInBuildOperation(Runnable runnable) {
        return new CurrentBuildOperationPreservingRunnable(runnable);
    }

    public void stop() {
        try {
            completed.await();
        } catch (InterruptedException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        }
    }

    @Override
    public void disconnect() {
        standardOutputReader.disconnect();
        if (readErrorStream) {
            standardErrorReader.disconnect();
        }
    }
}
