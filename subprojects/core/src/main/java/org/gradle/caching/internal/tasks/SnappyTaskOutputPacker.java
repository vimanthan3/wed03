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

package org.gradle.caching.internal.tasks;

import org.apache.commons.io.IOUtils;
import org.gradle.api.internal.tasks.ResolvedTaskOutputFilePropertySpec;
import org.gradle.caching.internal.tasks.origin.TaskOutputOriginReader;
import org.gradle.caching.internal.tasks.origin.TaskOutputOriginWriter;
import org.iq80.snappy.SnappyFramedInputStream;
import org.iq80.snappy.SnappyFramedOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.SortedSet;

/**
 * Adds compression and CRC32C checks to the packed task output using Google's Snappy compressor.
 * Implementation is from https://github.com/dain/snappy.
 */
public class SnappyTaskOutputPacker implements TaskOutputPacker {
    private final TaskOutputPacker delegate;

    public SnappyTaskOutputPacker(TaskOutputPacker delegate) {
        this.delegate = delegate;
    }

    @Override
    public PackResult pack(SortedSet<ResolvedTaskOutputFilePropertySpec> propertySpecs, OutputStream output, TaskOutputOriginWriter writeOrigin) throws IOException {
        OutputStream compressedOutput = new SnappyFramedOutputStream(output, SnappyFramedOutputStream.DEFAULT_BLOCK_SIZE, 1.0);
        try {
            return delegate.pack(propertySpecs, compressedOutput, writeOrigin);
        } finally {
            IOUtils.closeQuietly(compressedOutput);
        }
    }

    @Override
    public UnpackResult unpack(SortedSet<ResolvedTaskOutputFilePropertySpec> propertySpecs, InputStream input, TaskOutputOriginReader readOrigin) throws IOException {
        InputStream uncompressedInput = new SnappyFramedInputStream(input, true);
        try {
            return delegate.unpack(propertySpecs, uncompressedInput, readOrigin);
        } finally {
            IOUtils.closeQuietly(uncompressedInput);
        }
    }
}
