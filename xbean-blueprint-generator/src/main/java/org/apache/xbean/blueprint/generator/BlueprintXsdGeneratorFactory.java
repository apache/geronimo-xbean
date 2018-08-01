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
