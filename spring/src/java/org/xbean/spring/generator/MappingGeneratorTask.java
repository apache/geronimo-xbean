package org.xbean.spring.generator;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

import java.io.File;
import java.util.Iterator;
import java.util.Set;

/**
 * An Ant task for executing Gram scripts, which are Groovy scripts executed on
 * the JAM context.
 *
 * @version $Revision$
 */
public class MappingGeneratorTask extends MatchingTask implements LogFacade {
    private String namespace;
    private Path srcDir = null;
    private Path toolpath = null;
    private Path classpath = null;
    private String includes = "**/*.java";
    private File destFile = new File("target/classes/activemq.xsd");
    private String metaInfDir = "target/classes/";

    public File getDestFile() {
        return destFile;
    }

    public void setDestFile(File scenariosFile) {
        this.destFile = scenariosFile;
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

    public void setSrcDir(Path srcDir) {
        this.srcDir = srcDir;
    }

    public void setToolpath(Path path) {
        if (toolpath == null) {
            toolpath = path;
        } else {
            toolpath.append(path);
        }
    }

    public void setToolpathRef(Reference r) {
        createToolpath().setRefid(r);
    }

    public Path createToolpath() {
        if (toolpath == null) {
            toolpath = new Path(getProject());
        }
        return toolpath.createPath();
    }

    public void setClasspath(Path path) {
        if (classpath == null) {
            classpath = path;
        } else {
            classpath.append(path);
        }
    }

    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    public Path createClasspath() {
        if (classpath == null) {
            classpath = new Path(getProject());
        }
        return classpath.createPath();
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

        try {
            MappingLoader mappingLoader = new JamMappingLoader(namespace,
                    getFiles(toolpath),
                    getFiles(classpath),
                    getFiles(srcDir),
                    includes);

            GeneratorPlugin[] plugins = new GeneratorPlugin[]{
                new XmlMetadataGenerator(this, metaInfDir),
                new DocumentationGenerator(this, destFile),
                new XsdGenerator(this, destFile)
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

            log("...done.");
        } catch (Exception e) {
            throw new BuildException(e);
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
