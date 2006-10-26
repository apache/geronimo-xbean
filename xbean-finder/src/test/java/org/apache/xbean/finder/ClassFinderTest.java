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

import java.util.List;
import java.util.Map;
import java.lang.annotation.Annotation;

import org.acme.foo.Holiday;
import org.acme.foo.Color;

/**
 * @version $Rev$ $Date$
 */
public class ClassFinderTest extends TestCase {

    public void testFindAnnotatedClasses() throws Exception {
        ClassFinder classFinder = new ClassFinder(Thread.currentThread().getContextClassLoader());
        List<Class> classes = classFinder.findAnnotatedClasses(Holiday.class);
        assertNotNull("classes", classes);
        assertEquals("classes.size", 3, classes.size());
    }

    public void testMapAnnotatedClasses() throws Exception {
        ClassFinder classFinder = new ClassFinder(Thread.currentThread().getContextClassLoader());
        Map<Class<? extends Annotation>, List<Class>> map = classFinder.mapAnnotatedClasses();
        List<Class> classes = map.get(Holiday.class);
        assertNotNull("classes", classes);
        assertEquals("classes.size", 3, classes.size());

        classes = map.get(Color.class);
        assertNotNull("classes", classes);
        assertEquals("classes.size", 8, classes.size());
    }
}
