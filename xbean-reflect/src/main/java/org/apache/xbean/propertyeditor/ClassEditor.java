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
package org.apache.xbean.propertyeditor;

/**
 * A property editor for converting class names into class object instances
 *
 * @version $Rev: 6680 $
 */
public class ClassEditor extends AbstractConverter {
    public ClassEditor() {
        super(Class.class);
    }

    /**
     * Return a resolved class using the text value of the property as the name.
     * The class is loading using the current context class loader, using the resolution
     * rules defined by ClassLoading.java.
     *
     * @return a Class object created from the text value of the name.
     * @throws PropertyEditorException Unable to resolve the Class object from the name.
     */
    protected Object toObjectImpl(String text) {
        try {
            // load this using the current thread's context loader.
            return Class.forName(text, true, Thread.currentThread().getContextClassLoader());
        } catch (Exception e) {
            // not found exceptions are just turned into wrapped property exceptions.
            throw new PropertyEditorException("Unable to resolve class " + text, e);
        }
    }

    protected String toStringImpl(Object value) {
        Class type = (Class) value;
        String text = type.getSimpleName();
        return text;
    }
}
