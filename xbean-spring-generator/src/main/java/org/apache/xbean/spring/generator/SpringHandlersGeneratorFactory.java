package org.apache.xbean.spring.generator;

import org.apache.xbean.generator.ArtifactSet;
import org.apache.xbean.generator.GeneratorPlugin;
import org.apache.xbean.generator.GeneratorPluginFactory;
import org.apache.xbean.generator.LogFacade;
import org.codehaus.plexus.component.annotations.Component;

import java.io.File;

@Component(role = GeneratorPluginFactory.class, hint = SpringHandlersGeneratorFactory.SPRING_HANDLERS_ID)
public class SpringHandlersGeneratorFactory implements GeneratorPluginFactory {

    public static final String SPRING_HANDLERS_ID = "spring-handlers";
    public static final int SPRING_METAINF_HANDLERS_POSITION = 91;

    public GeneratorPlugin createPlugin(File destination, ArtifactSet artifactSet, LogFacade logFacade) {
        return new SpringHandlersGenerator(SpringXmlMetadataGenerator.NAMESPACE_HANDLER, destination, artifactSet, logFacade);
    }

    public int position() {
        return SPRING_METAINF_HANDLERS_POSITION;
    }

    public String id() {
        return SPRING_HANDLERS_ID;
    }

    public String name() {
        return "Spring META-INF/handlers";
    }
}
