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
package org.apache.xbean.blueprint.generator;

import org.apache.xbean.generator.ArtifactSet;
import org.apache.xbean.generator.GeneratorPlugin;
import org.apache.xbean.generator.GeneratorPluginFactory;
import org.apache.xbean.generator.LogFacade;
import org.codehaus.plexus.component.annotations.Component;

import java.io.File;

@Component(role = GeneratorPluginFactory.class, hint = BlueprintXsdGeneratorFactory.BLUEPRINT_SCHEMA_ID)
public class BlueprintXsdGeneratorFactory implements GeneratorPluginFactory {

    public static final String BLUEPRINT_SCHEMA_ID = "blueprint-schema";
    public static final int BLUEPRINT_XSD_POSITION = 20;

    public GeneratorPlugin createPlugin(File destination, ArtifactSet artifactSet, LogFacade logFacade) {
        return new BlueprintXsdGenerator(destination, artifactSet, logFacade);
    }

    public int position() {
        return BLUEPRINT_XSD_POSITION;
    }

    public String id() {
        return BLUEPRINT_SCHEMA_ID;
    }

    public String name() {
        return "Blueprint XML Schema";
    }
}
