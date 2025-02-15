/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.eclipse.aether.connector.basic;

import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithm;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * Calculates checksums for a downloaded file.
 */
final class ChecksumCalculator {

    static class Checksum {
        final ChecksumAlgorithmFactory checksumAlgorithmFactory;

        ChecksumAlgorithm algorithm;

        Exception error;

        Checksum(ChecksumAlgorithmFactory checksumAlgorithmFactory) {
            this.checksumAlgorithmFactory = requireNonNull(checksumAlgorithmFactory);
            this.algorithm = checksumAlgorithmFactory.getAlgorithm();
        }

        public void reset() {
            this.algorithm = checksumAlgorithmFactory.getAlgorithm();
            this.error = null;
        }

        public void update(ByteBuffer buffer) {
            this.algorithm.update(buffer);
        }

        public void error(Exception error) {
            this.error = error;
        }

        public Object get() {
            if (error != null) {
                return error;
            }
            return algorithm.checksum();
        }
    }

    private final List<Checksum> checksums;

    private final File targetFile;

    public static ChecksumCalculator newInstance(
            File targetFile, Collection<ChecksumAlgorithmFactory> checksumAlgorithmFactories) {
        if (checksumAlgorithmFactories == null || checksumAlgorithmFactories.isEmpty()) {
            return null;
        }
        return new ChecksumCalculator(targetFile, checksumAlgorithmFactories);
    }

    private ChecksumCalculator(File targetFile, Collection<ChecksumAlgorithmFactory> checksumAlgorithmFactories) {
        this.checksums = new ArrayList<>();
        Set<String> algos = new HashSet<>();
        for (ChecksumAlgorithmFactory checksumAlgorithmFactory : checksumAlgorithmFactories) {
            if (algos.add(checksumAlgorithmFactory.getName())) {
                this.checksums.add(new Checksum(checksumAlgorithmFactory));
            }
        }
        this.targetFile = targetFile;
    }

    public void init(long dataOffset) {
        for (Checksum checksum : checksums) {
            checksum.reset();
        }
        if (dataOffset <= 0L) {
            return;
        }

        try (InputStream in = new BufferedInputStream(Files.newInputStream(targetFile.toPath()))) {
            long total = 0;
            final byte[] buffer = new byte[1024 * 32];
            while (total < dataOffset) {
                int read = in.read(buffer);
                if (read < 0) {
                    throw new IOException(targetFile + " contains only " + total
                            + " bytes, cannot resume download from offset " + dataOffset);
                }
                total += read;
                if (total > dataOffset) {
                    read -= total - dataOffset;
                }
                update(ByteBuffer.wrap(buffer, 0, read));
            }
        } catch (IOException e) {
            for (Checksum checksum : checksums) {
                checksum.error(e);
            }
        }
    }

    public void update(ByteBuffer data) {
        for (Checksum checksum : checksums) {
            ((Buffer) data).mark();
            checksum.update(data);
            ((Buffer) data).reset();
        }
    }

    public Map<String, Object> get() {
        Map<String, Object> results = new HashMap<>();
        for (Checksum checksum : checksums) {
            results.put(checksum.checksumAlgorithmFactory.getName(), checksum.get());
        }
        return results;
    }
}
