package org.apache.xbean.spring.generator;


import org.apache.xbean.generator.ArtifactSet;
import org.apache.xbean.generator.GeneratorPlugin;
import org.apache.xbean.generator.GeneratorPluginFactory;
import org.apache.xbean.generator.LogFacade;
import org.codehaus.plexus.component.annotations.Component;

import java.io.File;

@Component(role = GeneratorPluginFactory.class, hint = SpringMetadataGeneratorFactory.SPRING_METADATA_ID)
public class SpringMetadataGeneratorFactory implements GeneratorPluginFactory {

    public static final String SPRING_METADATA_ID = "spring-metadata";
    public static final int METADATA_POSITION = 10;

    public GeneratorPlugin createPlugin(File destination, ArtifactSet artifactSet, LogFacade logFacade) {
        return new SpringXmlMetadataGenerator(destination, artifactSet, logFacade);
    }

    public int position() {
        return METADATA_POSITION;
    }

    public String id() {
        return SPRING_METADATA_ID;
    }

    public String name() {
        return "Spring XBean metadata";
    }
}
