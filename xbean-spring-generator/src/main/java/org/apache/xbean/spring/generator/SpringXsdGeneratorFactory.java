package org.apache.xbean.spring.generator;


import org.apache.xbean.generator.ArtifactSet;
import org.apache.xbean.generator.GeneratorPlugin;
import org.apache.xbean.generator.GeneratorPluginFactory;
import org.apache.xbean.generator.LogFacade;
import org.codehaus.plexus.component.annotations.Component;

import java.io.File;

@Component(role = GeneratorPluginFactory.class, hint = SpringXsdGeneratorFactory.SPRING_SCHEMA_ID)
public class SpringXsdGeneratorFactory implements GeneratorPluginFactory {

    public static final String SPRING_SCHEMA_ID = "spring-xml-schema";
    public static final int SPRING_XSD_POSITION = 20;

    public GeneratorPlugin createPlugin(File destination, ArtifactSet artifactSet, LogFacade logFacade) {
        return new SpringXsdGenerator(destination, artifactSet, logFacade);
    }

    public int position() {
        return SPRING_XSD_POSITION;
    }

    public String id() {
        return SPRING_SCHEMA_ID;
    }

    public String name() {
        return "Spring XML Schema";
    }
}
