package org.apache.xbean.spring.generator;

import org.apache.xbean.generator.ArtifactSet;
import org.apache.xbean.generator.GeneratorPlugin;
import org.apache.xbean.generator.GeneratorPluginFactory;
import org.apache.xbean.generator.LogFacade;
import org.codehaus.plexus.component.annotations.Component;

import java.io.File;

@Component(role = GeneratorPluginFactory.class, hint = SpringSchemasGeneratorFactory.SPRING_SCHEMAS_ID)
public class SpringSchemasGeneratorFactory implements GeneratorPluginFactory {

    public static final String SPRING_SCHEMAS_ID = "spring-schemas";
    public static final int SPRING_METAINF_SCHEMAS_POSITION = 90;

    public GeneratorPlugin createPlugin(File destination, ArtifactSet artifactSet, LogFacade logFacade) {
        return new SpringSchemasGenerator(destination, artifactSet, logFacade);
    }

    public int position() {
        return SPRING_METAINF_SCHEMAS_POSITION;
    }

    public String id() {
        return SPRING_SCHEMAS_ID;
    }

    public String name() {
        return "Spring META-INF/schemas";
    }
}
