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

package org.gradle.play.internal.run;

import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.LogLevel;
import org.gradle.internal.Factory;
import org.gradle.process.internal.JavaExecHandleBuilder;
import org.gradle.process.internal.WorkerProcess;
import org.gradle.process.internal.WorkerProcessBuilder;

import java.io.File;
import java.util.Arrays;

public class PlayRunWorkerManager {
    public PlayApplicationRunnerToken start(File workingDir, Factory<WorkerProcessBuilder> workerFactory, FileCollection findBugsClasspath, PlayRunSpec spec) {
        WorkerProcess process = createWorkerProcess(workingDir, workerFactory, findBugsClasspath, spec);
        process.start();

        PlayWorkerClient clientCallBack = new PlayWorkerClient();
        process.getConnection().addIncoming(PlayRunWorkerClientProtocol.class, clientCallBack);
        process.getConnection().connect();
        PlayAppLifecycleUpdate result = clientCallBack.waitForRunning();
        if(result.getStatus() == PlayAppStatus.RUNNING){
            return new PlayApplicationRunnerToken(clientCallBack);
        }else{
            throw new GradleException("Unable to start Play application.", result.getException());
        }
    }

    private WorkerProcess createWorkerProcess(File workingDir, Factory<WorkerProcessBuilder> workerFactory, FileCollection playAppClasspath, PlayRunSpec spec) {
        WorkerProcessBuilder builder = workerFactory.create();
        builder.setBaseName("Gradle Play Worker");
        builder.applicationClasspath(playAppClasspath);
        builder.setLogLevel(LogLevel.DEBUG);
        builder.sharedPackages(Arrays.asList("org.gradle.play.internal.run", "play.core", "play.core.server", "play.docs", "scala"));
        JavaExecHandleBuilder javaCommand = builder.getJavaCommand();
        javaCommand.setWorkingDir(workingDir);
        javaCommand.setMaxHeapSize(spec.getMaxHeapSize());
        WorkerProcess process = builder.worker(new PlayWorkerServer(spec)).build();
        return process;
    }
}
