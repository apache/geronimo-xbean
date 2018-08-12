/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.xbean.generator;

import java.io.File;

/**
 * A factory interface for generator type.
 *
 * Because generator might have a bit more complicated creation and use own logic to determine constructor arguments we
 * can not rely on simplest solution (no arg constructor).
 * Main responsibility of factory is mapping of given inputs such desired directory and log facade to plugin requirements.
 */
public interface GeneratorPluginFactory {

    /**
     * Creates new plugin preconfigured to output its results to given destination.
     *
     * @param destination Destination directory.
     * @param artifactSet Artifact set to publish/fetch contents generated in given execution cycle.
     * @param logFacade A logging bridge.
     * @return A plugin which should process mapping records.
     */
    GeneratorPlugin createPlugin(File destination, ArtifactSet artifactSet, LogFacade logFacade);

    /**
     * Returns position in generation chain for given plugin which is returned by this factory.
     *
     * This is an extension from 4.x/3.x APIs which allows to loosely couple generators which work on top of results
     * delivered by other generators.
     *
     * @return Position of generator. Recommended values are from 0 to 100 where 0 is earliest element while 100 is very
     * last. Values out of range should be also accepted.
     */
    int position();

    /**
     * Returns identifier of this plugin factory.
     *
     * Identifier is short text which allows to quickly identify plugin.
     * @return Plugin identifier.
     */
    String id();

    /**
     * Return name of created generator plugin which describes its role.
     *
     * It might be either wiki, schema or html. Any of these values is pulled from implemented plugins, however any value
     * is accepted.
     *
     * @return A human friendly name of generator (but not a description).
     */
    String name();

}
