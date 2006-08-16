/**
 * 
 * Copyright 2005-2006 The Apache Software Foundation or its licensors,  as applicable.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 * 
 **/
package org.apache.xbean.maven;

import java.beans.PropertyEditorManager;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.BuildException;
import org.apache.xbean.spring.generator.DocumentationGenerator;
import org.apache.xbean.spring.generator.GeneratorPlugin;
import org.apache.xbean.spring.generator.LogFacade;
import org.apache.xbean.spring.generator.MappingLoader;
import org.apache.xbean.spring.generator.NamespaceMapping;
import org.apache.xbean.spring.generator.QdoxMappingLoader;
import org.apache.xbean.spring.generator.XmlMetadataGenerator;
import org.apache.xbean.spring.generator.XsdGenerator;

/**
 * @author <a href="gnodet@apache.org">Guillaume Nodet</a>
 * @version $Id: GenerateApplicationXmlMojo.java 314956 2005-10-12 16:27:15Z brett $
 * @goal mapping
 * @description Creates xbean mapping file
 * @phase generate-sources
 */
public class XBeanMojo extends AbstractMojo implements LogFacade {

    /**
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;
    
	/**
     * @parameter
     * @required
	 */
    private String namespace;
    
    /**
     * @parameter expression="${basedir}/src/main/java"
     * @required
     */
    private File srcDir;
    
    /**
     * @parameter
     */
    private String excludedClasses;

    /**
     * @parameter expression="${basedir}/target/xbean/"
     * @required
     */
    private File outputDir;
    
    /**
     * @parameter
     */
    private File schema;
    
    /**
     * @parameter expression="org.apache.xbean.spring.context.impl"
     */
    private String propertyEditorPaths;


    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().debug( " ======= XBeanMojo settings =======" );
        getLog().debug( "namespace[" + namespace + "]" );
        getLog().debug( "srcDir[" + srcDir + "]" );
        getLog().debug( "schema[" + schema + "]" );
        getLog().debug( "excludedClasses[" + excludedClasses + "]" );
        getLog().debug( "outputDir[" + outputDir + "]" );
        getLog().debug( "propertyEditorPaths[" + propertyEditorPaths + "]" );

        if (schema == null) {
            schema = new File(outputDir, project.getArtifactId() + ".xsd");
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
            schema.getParentFile().mkdirs();

            String[] excludedClasses = null;
            if (this.excludedClasses != null) {
                excludedClasses = this.excludedClasses.split(" *, *");
            }
            MappingLoader mappingLoader = new QdoxMappingLoader(namespace, new File[] { srcDir }, excludedClasses);
            GeneratorPlugin[] plugins = new GeneratorPlugin[]{
                new XmlMetadataGenerator(this, outputDir.getAbsolutePath()),
                new DocumentationGenerator(this, schema),
                new XsdGenerator(this, schema)
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
                    plugin.generate(namespaceMapping);
                }
            }

            Resource res = new Resource();
            res.setDirectory(outputDir.toString());
            project.addResource(res);

            log("...done.");
        } catch (Exception e) {
            throw new BuildException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCL);
        }
    }

	public void log(String message) {
		getLog().info(message);
	}

	public void log(String message, int level) {
		getLog().info(message);
	}
}
