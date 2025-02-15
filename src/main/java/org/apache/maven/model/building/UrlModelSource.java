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
package org.apache.maven.model.building;

import org.apache.maven.building.UrlSource;

import java.net.URL;

/**
 * Wraps an ordinary {@link URL} as a model source.
 *
 * @author Benjamin Bentmann
 *
 * @deprecated instead use {@link UrlSource}
 */
@Deprecated
public class UrlModelSource extends UrlSource implements ModelSource {
    /**
     * Creates a new model source backed by the specified URL.
     *
     * @param pomUrl The POM file, must not be {@code null}.
     */
    public UrlModelSource(URL pomUrl) {
        super(pomUrl);
    }
}
