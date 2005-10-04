package org.xbean.spring.task;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.codehaus.jam.JClass;
import org.codehaus.jam.JamService;
import org.codehaus.jam.JamServiceFactory;
import org.codehaus.jam.JamServiceParams;

import java.io.File;

/**
 * An Ant task for executing Gram scripts, which are Groovy scripts executed on
 * the JAM context.
 * 
 * @version $Revision: 1.2 $
 */
public class SchemaGenerateTask extends MatchingTask {

    private Path srcDir = null;
    private Path mToolpath = null;
    private Path mClasspath = null;
    private String mIncludes = "**/*.java";
    private File destFile = new File("target/classes/activemq.xsd");

    public File getDestFile() {
        return destFile;
    }

    public void setDestFile(File scenariosFile) {
        this.destFile = scenariosFile;
    }

    public void setSrcDir(Path srcDir) {
        this.srcDir = srcDir;
    }

    public void setToolpath(Path path) {
        if (mToolpath == null) {
            mToolpath = path;
        }
        else {
            mToolpath.append(path);
        }
    }

    public void setToolpathRef(Reference r) {
        createToolpath().setRefid(r);
    }

    public Path createToolpath() {
        if (mToolpath == null) {
            mToolpath = new Path(getProject());
        }
        return mToolpath.createPath();
    }

    public void setClasspath(Path path) {
        if (mClasspath == null) {
            mClasspath = path;
        }
        else {
            mClasspath.append(path);
        }
    }

    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    public Path createClasspath() {
        if (mClasspath == null) {
            mClasspath = new Path(getProject());
        }
        return mClasspath.createPath();
    }

    public void execute() throws BuildException {
        if (srcDir == null) {
            throw new BuildException("'srcDir' must be specified");
        }
        if (destFile == null) {
            throw new BuildException("'destFile' must be specified");
        }
        JamServiceFactory jamServiceFactory = JamServiceFactory.getInstance();
        JamServiceParams serviceParams = jamServiceFactory.createServiceParams();
        if (mToolpath != null) {
            File[] tcp = path2files(mToolpath);
            for (int i = 0; i < tcp.length; i++) {
                serviceParams.addToolClasspath(tcp[i]);
            }
        }
        if (mClasspath != null) {
            File[] cp = path2files(mClasspath);
            for (int i = 0; i < cp.length; i++) {
                serviceParams.addClasspath(cp[i]);
            }
        }

        try {
            serviceParams.includeSourcePattern(path2files(srcDir), mIncludes);
            JamService jam = jamServiceFactory.createService(serviceParams);
            JClass[] classes = jam.getAllClasses();
            SchemaGenerator generator = new SchemaGenerator(classes, destFile);
            generator.generate();

            log("...done.");
        }
        catch (Exception e) {
            throw new BuildException(e);
        }
    }

    protected File[] path2files(Path path) {
        String[] list = path.list();
        File[] out = new File[list.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = new File(list[i]).getAbsoluteFile();
        }
        return out;
    }
}
