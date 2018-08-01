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
package org.apache.xbean.spring.generator;

import org.apache.xbean.generator.ArtifactSet;
import org.apache.xbean.generator.GeneratorPlugin;
import org.apache.xbean.generator.LogFacade;
import org.apache.xbean.generator.commons.XmlMetadataGenerator;
import org.apache.xbean.namespace.NamespaceDiscoverer;
import org.apache.xbean.spring.namespace.SpringNamespaceDiscoverer;
import org.codehaus.plexus.component.annotations.Component;

import java.io.File;


/**
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
@Component(role = GeneratorPlugin.class)
public class SpringXmlMetadataGenerator extends XmlMetadataGenerator {

    public static final String NAMESPACE_HANDLER = "org.apache.xbean.spring.context.v2.XBeanNamespaceHandler";

    public SpringXmlMetadataGenerator(File destination, ArtifactSet artifactSet, LogFacade logFacade) {
    //String namespaceHandler, NamespaceDiscoverer namespaceDiscoverer, String metaInfDir, LogFacade logFacade) {
        super(NAMESPACE_HANDLER, new SpringNamespaceDiscoverer(), destination, artifactSet, logFacade);
    }
}
