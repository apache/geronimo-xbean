package org.apache.xbean.generator.commons;

import org.apache.xbean.generator.ArtifactSet;
import org.apache.xbean.generator.GeneratorPlugin;
import org.apache.xbean.generator.GeneratorPluginFactory;
import org.apache.xbean.generator.LogFacade;
import org.codehaus.plexus.component.annotations.Component;

import java.io.File;

@Component(role = GeneratorPluginFactory.class, hint = WikiGeneratorFactory.WIKI_ID)
public class WikiGeneratorFactory implements GeneratorPluginFactory {

    public static final String WIKI_ID = "wiki";
    public static final int WIKI_POSITION = 80;

    public GeneratorPlugin createPlugin(File destination, ArtifactSet artifactSet, LogFacade logFacade) {
        return new WikiDocumentationGenerator(destination, artifactSet, logFacade);
    }

    public int position() {
        return WIKI_POSITION;
    }

    public String id() {
        return WIKI_ID;
    }

    public String name() {
        return "wiki";
    }

}
