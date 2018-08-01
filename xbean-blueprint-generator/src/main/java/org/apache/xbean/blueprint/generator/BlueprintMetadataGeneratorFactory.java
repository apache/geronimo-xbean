package org.apache.xbean.blueprint.generator;


import org.apache.xbean.generator.ArtifactSet;
import org.apache.xbean.generator.GeneratorPlugin;
import org.apache.xbean.generator.GeneratorPluginFactory;
import org.apache.xbean.generator.LogFacade;
import org.codehaus.plexus.component.annotations.Component;

import java.io.File;

@Component(role = GeneratorPluginFactory.class, hint = BlueprintMetadataGeneratorFactory.BLUEPRINT_METADATA_ID)
public class BlueprintMetadataGeneratorFactory implements GeneratorPluginFactory {

    public static final String BLUEPRINT_METADATA_ID = "blueprint-metadata";
    public static final int METADATA_POSITION = 10;

    public GeneratorPlugin createPlugin(File destination, ArtifactSet artifactSet, LogFacade logFacade) {
        return new BlueprintXmlMetadataGenerator(destination, artifactSet, logFacade);
    }

    public int position() {
        return METADATA_POSITION;
    }

    public String id() {
        return BLUEPRINT_METADATA_ID;
    }

    public String name() {
        return "Blueprint XBean metadata";
    }

}
