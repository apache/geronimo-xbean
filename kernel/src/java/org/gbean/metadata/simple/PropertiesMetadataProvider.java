/**
 *
 * Copyright 2005 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.gbean.metadata.simple;

import java.io.InputStream;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.gbean.kernel.ClassLoading;
import org.gbean.kernel.OperationSignature;
import org.gbean.kernel.ConstructorSignature;
import org.gbean.metadata.ClassMetadata;
import org.gbean.metadata.MetadataProvider;
import org.gbean.metadata.MethodMetadata;
import org.gbean.metadata.ParameterMetadata;
import org.gbean.metadata.ConstructorMetadata;

/**
 * @version $Revision$ $Date$
 */
public class PropertiesMetadataProvider implements MetadataProvider {
    private static final Log log = LogFactory.getLog(PropertiesMetadataProvider.class);

    public void addClassMetadata(ClassMetadata classMetadata) {
        try {
            Properties properties = loadProperties(classMetadata.getType());
            for (Iterator iterator = properties.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry entry = (Map.Entry) iterator.next();
                String propertyName = (String) entry.getKey();
                String propertyValue = (String) entry.getValue();
                processProperty(classMetadata, propertyName, propertyValue);
            }
        } catch (Exception e) {
            log.error("Error while loading properties based metadata for class " + classMetadata.getType().getName());
        }
    }

    private Properties loadProperties(Class type) throws IOException {
        InputStream in = null;
        try {
            Properties properties = new Properties();
            in = type.getClassLoader().getResourceAsStream(type.getName().replace('.', '/') + ".properties");
            if (in != null) {
                LineNumberReader lineReader = new LineNumberReader(new InputStreamReader(in));

                String line;
                while ((line = lineReader.readLine()) != null) {
                    // todo allow line continuations with trailing '\'

                    String name = line;
                    String value = "";

                    // break the line only at an equal sign
                    int equals = line.indexOf('=');
                    if (equals > 0) {
                        name = line.substring(0, equals);
                        if (equals < line.length()) {
                            value = line.substring(equals + 1);
                        }
                    }

                    // todo remove standard escapes such at \t \r \n and \\

                    name = name.trim();
                    value = value.trim();

                    if (name.length() > 0 && name.charAt(0) != '#' && name.charAt(0) != '!') {
                        properties.setProperty(name, value);
                    }
                }
            }
            return properties;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.error("Error while closing properties based metadata input stream for class "  + type.getName());
                }
            }
        }
    }

    private void processProperty(ClassMetadata classMetadata, String propertyName, String propertyValue) {
        // try to parse the property name as a method property
        if (parseMethodProperty(classMetadata, propertyName, propertyValue)) {
            return;
        }
        classMetadata.put(propertyName, propertyValue);
    }

    private boolean parseMethodProperty(ClassMetadata classMetadata, String propertyName, String propertyValue) {
        // if we don't have an open paren it is not a method property
        int openParen = propertyName.indexOf('(');
        if (openParen <= 0) {
            return false;
        }
        // if we don't have a close paren followed by a period after the open paren it is not a method property
        int closeParen = propertyName.indexOf(").", openParen);
        if (closeParen < 0) {
            return false;
        }

        // we must have some characters after the paren period
        if (propertyName.length() <= closeParen + 2) {
            return false;
        }

        // the method name is the characters before the openparen
        String methodName = propertyName.substring(0, openParen).trim();

        // parse the parameters
        List params = parseMethodParameters(classMetadata.getType().getClassLoader(), propertyName.substring(openParen+1, closeParen));
        if (params == null) {
            return false;
        }

        // the property name of the method metadata is the stuff after the ")."
        String methodPropertyName = propertyName.substring(closeParen + 2).trim();
        if (methodPropertyName.length() == 0) {
            return false;
        }

        // get the parameter index number if one is present
        int parameterIndex = -1;
        String parameterPropertyName = null;
        try {
            // if the property name does not include a period it is not metadata
            int period = methodPropertyName.indexOf('.');
            if (period > 0) {
                // we must have some characters after the period
                if (methodPropertyName.length() > period) {
                    String indexString = methodPropertyName.substring(0, period);
                    parameterIndex = Integer.parseInt(indexString);
                    parameterPropertyName = methodPropertyName.substring(period + 1);
                }
            }
        } catch (NumberFormatException e) {
        }


        // now that we know the method name and parameters lets try to get the method metadata for it
        String className = classMetadata.getType().getName();
        if (methodName.equals(className.substring(className.lastIndexOf(".") + 1))) {
            ConstructorSignature signature = new ConstructorSignature(params);
            ConstructorMetadata constructorMetadata = classMetadata.getConstructor(signature);
            if (constructorMetadata == null) {
                return false;
            }

            if (0 <= parameterIndex && parameterIndex < constructorMetadata.getSignature().getParameterTypes().size()) {
                // this is parameter metadata
                ParameterMetadata parameterMetadata = constructorMetadata.getParameter(parameterIndex);
                parameterMetadata.put(parameterPropertyName, propertyValue);
            } else {
                // this is constructor metadata
                constructorMetadata.put(methodPropertyName, propertyValue);
            }
        } else {
            OperationSignature signature = new OperationSignature(methodName, params);
            MethodMetadata methodMetadata = classMetadata.getMethod(signature);
            if (methodMetadata == null) {
                return false;
            }

            if (0 <= parameterIndex && parameterIndex < methodMetadata.getSignature().getParameterTypes().size()) {
                // this is parameter metadata
                ParameterMetadata parameterMetadata = methodMetadata.getParameter(parameterIndex);
                parameterMetadata.put(parameterPropertyName, propertyValue);
            } else {
                // this is constructor metadata
                methodMetadata.put(methodPropertyName, propertyValue);
            }
        }
        return true;
    }

    private List parseMethodParameters(ClassLoader classLoader, String paramsString) {
        try {
            List parameters = new LinkedList();
            for (StringTokenizer stringTokenizer = new StringTokenizer(paramsString, ", \t\n"); stringTokenizer.hasMoreTokens();) {
                String parameter = stringTokenizer.nextToken();
                Class parameterType = ClassLoading.loadClass(parameter, classLoader);
                parameters.add(parameterType.getName());
            }

            return parameters;
        } catch (ClassNotFoundException e) {
            log.error("Unable to load method parameter class"  + e);
            return null;
        }
    }
}
