package org.apache.xbean.classpath;

/**
 * @version $Revision$ $Date$
 */

import junit.framework.TestCase;

public class SystemClassPathTest extends TestCase {

    public void testAddJarToPath() throws Exception {
        SystemClassPath systemClassPath = new SystemClassPath();

        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();

        try {
            systemClassLoader.loadClass("groovy.lang.GroovyShell");
            fail("Class already exists");
        } catch (ClassNotFoundException e) {
            // this should fail
        }


//        URL groovyJar = new URL("http://www.ibiblio.org/maven/groovy/jars/groovy-SNAPSHOT.jar");
//        systemClassPath.addJarToPath(groovyJar);
//
//        try {
//            systemClassLoader.loadClass("groovy.lang.GroovyShell");
//        } catch (ClassNotFoundException e) {
//            // this should fail pass
//            fail("Class already exists");
//        }
    }
}