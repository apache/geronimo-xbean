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
package org.apache.xbean.blueprint.generator;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.Path;

import java.io.File;
import java.util.Iterator;
import java.util.Set;
import java.util.List;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.Collections;
import java.util.StringTokenizer;
import java.beans.PropertyEditorManager;

/**
 * An Ant task for executing generating mapping metadata.
 *
 * @version $Revision$
 */
public class MappingGeneratorTask extends MatchingTask implements LogFacade {
    private String namespace;
    private Path srcDir;
    private String excludedClasses = null;
    private File destFile = new File("target/classes/schema.xsd");
    private String metaInfDir = "target/classes/";
    private String propertyEditorPaths = "org.apache.xbean.blueprint.context.impl";

    public File getDestFile() {
        return destFile;
    }

    public void setDestFile(File destFile) {
        this.destFile = destFile;
    }

    public String getMetaInfDir() {
        return metaInfDir;
    }

    public void setMetaInfDir(String metaInfDir) {
        this.metaInfDir = metaInfDir;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Path getSrcDir() {
        return srcDir;
    }

    public void setSrcDir(Path srcDir) {
        this.srcDir = srcDir;
    }

    public String getPropertyEditorPaths() {
        return propertyEditorPaths;
    }

    public void setPropertyEditorPaths(String propertyEditorPaths) {
        this.propertyEditorPaths = propertyEditorPaths;
    }

    public void execute() throws BuildException {
        if (namespace == null) {
            throw new BuildException("'namespace' must be specified");
        }
        if (srcDir == null) {
            throw new BuildException("'srcDir' must be specified");
        }
        if (destFile == null) {
            throw new BuildException("'destFile' must be specified");
        }

        if (propertyEditorPaths != null) {
            List editorSearchPath = new LinkedList(Arrays.asList(PropertyEditorManager.getEditorSearchPath()));
            StringTokenizer paths = new StringTokenizer(propertyEditorPaths, " ,");
            editorSearchPath.addAll(Collections.list(paths));
            PropertyEditorManager.setEditorSearchPath((String[]) editorSearchPath.toArray(new String[editorSearchPath.size()]));
        }

        ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            String[] excludedClasses = null;
            if (this.excludedClasses != null) {
                excludedClasses = this.excludedClasses.split(" *, *");
            }
            MappingLoader mappingLoader = new QdoxMappingLoader(namespace, getFiles(srcDir), excludedClasses);

            GeneratorPlugin[] plugins = new GeneratorPlugin[]{
                new XmlMetadataGenerator(metaInfDir, destFile),
                new DocumentationGenerator(destFile),
                new XsdGenerator(destFile)
            };

            // load the mappings
            Set namespaces = mappingLoader.loadNamespaces();
            if (namespaces.isEmpty()) {
                System.out.println("Warning: no namespaces found!");
            }

            // generate the files
            for (Iterator iterator = namespaces.iterator(); iterator.hasNext();) {
                NamespaceMapping namespaceMapping = (NamespaceMapping) iterator.next();
                for (int i = 0; i < plugins.length; i++) {
                    GeneratorPlugin plugin = plugins[i];
                    plugin.setLog(this);
                    plugin.generate(namespaceMapping);
                }
            }

            log("...done.");
        } catch (Exception e) {
            throw new BuildException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCL);
        }
    }

    private File[] getFiles(Path path) {
        if (path == null) {
            return null;
        }
        String[] paths = path.list();
        File[] files = new File[paths.length];
        for (int i = 0; i < files.length; i++) {
            files[i] = new File(paths[i]).getAbsoluteFile();
        }
        return files;
    }
}
