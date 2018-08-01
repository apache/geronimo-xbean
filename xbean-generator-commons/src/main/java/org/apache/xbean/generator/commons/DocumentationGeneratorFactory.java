package org.apache.xbean.generator.commons;

import org.apache.xbean.generator.ArtifactSet;
import org.apache.xbean.generator.GeneratorPlugin;
import org.apache.xbean.generator.GeneratorPluginFactory;
import org.apache.xbean.generator.LogFacade;
import org.codehaus.plexus.component.annotations.Component;

import java.io.File;

@Component(role = GeneratorPluginFactory.class, hint = DocumentationGeneratorFactory.DOCUMENTATION_ID)
public class DocumentationGeneratorFactory implements GeneratorPluginFactory {

    public static final String DOCUMENTATION_ID = "documentation";
    public static final int DOCUMENTATION_POSITION = 100;

    public GeneratorPlugin createPlugin(File destination, ArtifactSet artifactSet, LogFacade logFacade) {
        return new DocumentationGenerator(destination, artifactSet, logFacade);
    }

    public int position() {
        return DOCUMENTATION_POSITION;
    }

    public String id() {
        return DOCUMENTATION_ID;
    }

    public String name() {
        return "Html documentation";
    }

}
