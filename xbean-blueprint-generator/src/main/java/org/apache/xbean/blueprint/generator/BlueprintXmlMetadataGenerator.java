package org.apache.xbean.blueprint.generator;

import org.apache.xbean.blueprint.namespace.BlueprintNamespaceDiscoverer;
import org.apache.xbean.generator.ArtifactSet;
import org.apache.xbean.generator.GeneratorPlugin;
import org.apache.xbean.generator.LogFacade;
import org.apache.xbean.generator.commons.XmlMetadataGenerator;
import org.codehaus.plexus.component.annotations.Component;

import java.io.File;

public class BlueprintXmlMetadataGenerator extends XmlMetadataGenerator {

    public static final String NAMESPACE_HANDLER = "org.apache.xbean.blueprint.context.v2.XBeanNamespaceHandler";

    public BlueprintXmlMetadataGenerator(File destination, ArtifactSet artifactSet, LogFacade logFacade) {
        super(NAMESPACE_HANDLER, new BlueprintNamespaceDiscoverer(), destination, artifactSet, logFacade);
    }

}
