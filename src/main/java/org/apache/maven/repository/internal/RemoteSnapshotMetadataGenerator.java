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
package org.apache.maven.repository.internal;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.impl.MetadataGenerator;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.util.ConfigUtils;

import java.util.*;

/**
 * Maven remote GAV level metadata generator.
 * <p>
 * Remote snapshot metadata converts artifact on-the-fly to use timestamped snapshot version, and enlist it accordingly.
 */
class RemoteSnapshotMetadataGenerator implements MetadataGenerator {

    private final Map<Object, RemoteSnapshotMetadata> snapshots;

    private final boolean legacyFormat;

    private final Date timestamp;

    RemoteSnapshotMetadataGenerator(RepositorySystemSession session, DeployRequest request) {
        legacyFormat = ConfigUtils.getBoolean(session, false, "maven.metadata.legacy");

        timestamp = (Date) ConfigUtils.getObject(session, new Date(), "maven.startTime");

        snapshots = new LinkedHashMap<>();

        /*
         * NOTE: This should be considered a quirk to support interop with Maven's legacy ArtifactDeployer which
         * processes one artifact at a time and hence cannot associate the artifacts from the same project to use the
         * same timestamp+buildno for the snapshot versions. Allowing the caller to pass in metadata from a previous
         * deployment allows to re-establish the association between the artifacts of the same project.
         */
        for (Metadata metadata : request.getMetadata()) {
            if (metadata instanceof RemoteSnapshotMetadata) {
                RemoteSnapshotMetadata snapshotMetadata = (RemoteSnapshotMetadata) metadata;
                snapshots.put(snapshotMetadata.getKey(), snapshotMetadata);
            }
        }
    }

    public Collection<? extends Metadata> prepare(Collection<? extends Artifact> artifacts) {
        for (Artifact artifact : artifacts) {
            if (artifact.isSnapshot()) {
                Object key = RemoteSnapshotMetadata.getKey(artifact);
                RemoteSnapshotMetadata snapshotMetadata = snapshots.get(key);
                if (snapshotMetadata == null) {
                    snapshotMetadata = new RemoteSnapshotMetadata(artifact, legacyFormat, timestamp);
                    snapshots.put(key, snapshotMetadata);
                }
                snapshotMetadata.bind(artifact);
            }
        }

        return snapshots.values();
    }

    public Artifact transformArtifact(Artifact artifact) {
        if (artifact.isSnapshot() && artifact.getVersion().equals(artifact.getBaseVersion())) {
            Object key = RemoteSnapshotMetadata.getKey(artifact);
            RemoteSnapshotMetadata snapshotMetadata = snapshots.get(key);
            if (snapshotMetadata != null) {
                artifact = artifact.setVersion(snapshotMetadata.getExpandedVersion(artifact));
            }
        }

        return artifact;
    }

    public Collection<? extends Metadata> finish(Collection<? extends Artifact> artifacts) {
        return Collections.emptyList();
    }
}
