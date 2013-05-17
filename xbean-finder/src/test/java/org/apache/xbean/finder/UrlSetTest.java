/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.xbean.finder;

import junit.framework.TestCase;

import java.io.File;
import java.net.URL;
import java.util.Locale;
import java.util.Properties;

/**
 * @version $Rev$ $Date$
 */
public class UrlSetTest extends TestCase {

    public void testAll() throws Exception {
        final URL[] originalUrls = new URL[]{
                new URL("file:/Users/dblevins/work/xbean/trunk/xbean-finder/target/classes/"),
                new URL("file:/Users/dblevins/work/xbean/trunk/xbean-finder/target/test-classes/"),
                new URL("jar:file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/.compatibility/14compatibility.jar!/"),
                new URL("jar:file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/charsets.jar!/"),
                new URL("jar:file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/classes.jar!/"),
                new URL("jar:file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/dt.jar!/"),
                new URL("jar:file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/jce.jar!/"),
                new URL("jar:file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/jconsole.jar!/"),
                new URL("jar:file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/jsse.jar!/"),
                new URL("jar:file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/laf.jar!/"),
                new URL("jar:file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/ui.jar!/"),
                new URL("jar:file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Home/lib/deploy.jar!/"),
                new URL("jar:file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Home/lib/ext/apple_provider.jar!/"),
                new URL("jar:file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Home/lib/ext/dnsns.jar!/"),
                new URL("jar:file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Home/lib/ext/localedata.jar!/"),
                new URL("jar:file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Home/lib/ext/sunjce_provider.jar!/"),
                new URL("jar:file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Home/lib/ext/sunpkcs11.jar!/"),
                new URL("jar:file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Home/lib/plugin.jar!/"),
                new URL("jar:file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Home/lib/sa-jdi.jar!/"),
                new URL("jar:file:/System/Library/Java/Extensions/CoreAudio.jar!/"),
                new URL("jar:file:/System/Library/Java/Extensions/MRJToolkit.jar!/"),
                new URL("jar:file:/System/Library/Java/Extensions/QTJSupport.jar!/"),
                new URL("jar:file:/System/Library/Java/Extensions/QTJava.zip!/"),
                new URL("jar:file:/System/Library/Java/Extensions/dns_sd.jar!/"),
                new URL("jar:file:/System/Library/Java/Extensions/j3daudio.jar!/"),
                new URL("jar:file:/System/Library/Java/Extensions/j3dcore.jar!/"),
                new URL("jar:file:/System/Library/Java/Extensions/j3dutils.jar!/"),
                new URL("jar:file:/System/Library/Java/Extensions/jai_codec.jar!/"),
                new URL("jar:file:/System/Library/Java/Extensions/jai_core.jar!/"),
                new URL("jar:file:/System/Library/Java/Extensions/mlibwrapper_jai.jar!/"),
                new URL("jar:file:/System/Library/Java/Extensions/vecmath.jar!/"),
                new URL("jar:file:/Users/dblevins/.m2/repository/junit/junit/3.8.1/junit-3.8.1.jar!/"),
        };
        UrlSet urlSet = new UrlSet(originalUrls);

        assertEquals("Urls.size()", 32, urlSet.getUrls().size());

        UrlSet homeSet = urlSet.matching(".*Home.*");
        assertEquals("HomeSet.getUrls().size()", 8, homeSet.getUrls().size());

//        homeSet = urlSet.relative(new File("/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Home"));
//        assertEquals("HomeSet.getUrls().size()", 8, homeSet.getUrls().size());

        UrlSet urlSet2 = urlSet.exclude(homeSet);
        assertEquals("Urls.size()", 24, urlSet2.getUrls().size());

        UrlSet xbeanSet = urlSet.matching(".*xbean.*");
        assertEquals("XbeanSet.getUrls().size()", 2, xbeanSet.getUrls().size());

        UrlSet junitSet = urlSet.matching(".*junit.*");
        assertEquals("JunitSet.getUrls().size()", 1, junitSet.getUrls().size());

        UrlSet mergedSet = homeSet.include(xbeanSet);
        assertEquals("MergedSet.getUrls().size()", 10, mergedSet.getUrls().size());

        mergedSet.include(junitSet);
        assertEquals("MergedSet.getUrls().size()", 10, mergedSet.getUrls().size());

        UrlSet mergedSet2 = mergedSet.include(junitSet);
        assertEquals("MergedSet2.getUrls().size()", 11, mergedSet2.getUrls().size());

        UrlSet filteredSet = urlSet.exclude(".*System/Library.*");
        assertEquals("FilteredSet.getUrls().size()", 3, filteredSet.getUrls().size());

        filteredSet.exclude(junitSet);
        assertEquals("FilteredSet.getUrls().size()", 3, filteredSet.getUrls().size());

        UrlSet filteredSet2 = filteredSet.exclude(junitSet);
        assertEquals("FilteredSet2.getUrls().size()", 2, filteredSet2.getUrls().size());
    }


    public void testOsxJdk16Filtering() throws Exception {
        String osName = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);

        String urlPrefix = osName.contains("windows") ?
                "file:/" + new File("/System").getAbsolutePath().substring(0, 2) // C: could be D: or any other letter
                : "file:";

        final URL[] urls = {
                new URL(urlPrefix + "/Applications/IntelliJ%20IDEA%2011.app/lib/idea_rt.jar"),
                new URL(urlPrefix + "/Applications/IntelliJ%20IDEA%2011.app/plugins/junit/lib/junit-rt.jar"),
                new URL(urlPrefix + "/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Classes/charsets.jar"),
                new URL(urlPrefix + "/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Classes/classes.jar"),
                new URL(urlPrefix + "/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Classes/jsse.jar"),
                new URL(urlPrefix + "/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Classes/ui.jar"),
                new URL(urlPrefix + "/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home/lib/dt.jar"),
                new URL(urlPrefix + "/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home/lib/ext/apple_provider.jar"),
                new URL(urlPrefix + "/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home/lib/ext/dnsns.jar"),
                new URL(urlPrefix + "/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home/lib/ext/localedata.jar"),
                new URL(urlPrefix + "/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home/lib/ext/sunjce_provider.jar"),
                new URL(urlPrefix + "/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home/lib/ext/sunpkcs11.jar"),
                new URL(urlPrefix + "/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home/lib/jce.jar"),
                new URL(urlPrefix + "/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home/lib/jconsole.jar"),
                new URL(urlPrefix + "/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home/lib/management-agent.jar"),
                new URL(urlPrefix + "/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home/lib/sa-jdi.jar"),
                new URL(urlPrefix + "/System/Library/Java/Support/CoreDeploy.bundle/Contents/Resources/Java/deploy.jar"),
                new URL(urlPrefix + "/System/Library/Java/Support/Deploy.bundle/Contents/Resources/Java/javaws.jar"),
                new URL(urlPrefix + "/Users/dblevins/.m2/repository/asm/asm-commons/3.2/asm-commons-3.2.jar"),
                new URL(urlPrefix + "/Users/dblevins/.m2/repository/asm/asm-tree/3.2/asm-tree-3.2.jar"),
                new URL(urlPrefix + "/Users/dblevins/.m2/repository/asm/asm/3.2/asm-3.2.jar"),
                new URL(urlPrefix + "/Users/dblevins/.m2/repository/junit/junit/4.8.2/junit-4.8.2.jar"),
                new URL(urlPrefix + "/Users/dblevins/.m2/repository/org/osgi/org.osgi.core/4.3.1/org.osgi.core-4.3.1.jar"),
                new URL(urlPrefix + "/Users/dblevins/.m2/repository/org/slf4j/slf4j-api/1.5.11/slf4j-api-1.5.11.jar"),
                new URL(urlPrefix + "/Users/dblevins/work/xbean/trunk/xbean-bundleutils/target/classes/"),
                new URL(urlPrefix + "/Users/dblevins/work/xbean/trunk/xbean-finder/target/classes/"),
                new URL(urlPrefix + "/Users/dblevins/work/xbean/trunk/xbean-finder/target/test-classes/"),
        };


        System.setProperty("java.endorsed.dirs", "/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home/lib/endorsed");
        System.setProperty("java.ext.dirs",
                           "/Library/Java/Extensions" + File.pathSeparator
                           + "/System/Library/Java/Extensions" + File.pathSeparator
                           + "/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home/lib/ext");
        System.setProperty("java.home", "/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home");
        System.setProperty("os.name", "Mac OS X");

        final UrlSet urlSet = new UrlSet(urls);
        assertEquals(urls.length, urlSet.size());
        assertEquals(27, urlSet.excludeJavaEndorsedDirs().size());
        assertEquals(22, urlSet.excludeJavaExtDirs().size());
        assertEquals(13, urlSet.excludeJavaHome().size());
        assertEquals(11, urlSet.excludeJvm().size());
    }

    private final Properties properties = new Properties();

    protected void setUp() throws java.lang.Exception {
        swap(System.getProperties(), properties);
    }

    protected void tearDown() throws java.lang.Exception {
        swap(properties, System.getProperties());
    }

    private static void swap(Properties from, Properties to) {
        to.clear();
        to.putAll(from);
    }
}
