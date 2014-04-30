/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.xbean.blueprint.cm;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.aries.blueprint.compendium.cm.CmPropertyPlaceholder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Rev$ $Date$
 */
public class JexlPropertyPlaceholder extends CmPropertyPlaceholder {

    private static final Logger LOGGER = LoggerFactory.getLogger(JexlPropertyPlaceholder.class);

    private transient JexlExpressionParser parser;

    @Override
    protected String processString(String str) {
        LOGGER.debug("Processing {} from configuration with pid {}", str, getPersistentId());
        JexlExpressionParser parser = getParser();
        try {
            return parser.evaluate(str).toString();
        } catch (Exception e) {
            LOGGER.info("Could not evaluate expressions {}  for {}", str, getPersistentId());
            LOGGER.info("Exception:", e);
        }
        return str;
    }

    protected synchronized JexlExpressionParser getParser() {
        if (parser == null) {
//            try {
                parser = new JexlExpressionParser(toMap());
//            } catch (IOException e) {
                // ignore
//            }
        }
        return parser;
    }

    private Map<String, Object> toMap() {
        return new ConfigMap();
//        Map<String, Object> map = new HashMap<String, Object>();
//        if (config != null) {
//            Dictionary<String, Object> properties = config.getProperties();
//            for (Enumeration<String> e = properties.keys(); e.hasMoreElements(); ) {
//                String key = e.nextElement();
//                Object value = properties.get(key);
//                map.put(key, value);
//            }
//        }
//        return map;
    }

    private class ConfigMap implements Map<String, Object> {

        public int size() {
            return 0;
        }

        public boolean isEmpty() {
            return false;
        }

        public boolean containsKey(Object o) {
            return getProperty((String) o) != null;
        }

        public boolean containsValue(Object o) {
            return false;
        }

        public Object get(Object o) {
            return getProperty((String) o);
        }

        public Object put(String s, Object o) {
            return null;
        }

        public Object remove(Object o) {
            return null;
        }

        public void putAll(Map<? extends String, ? extends Object> map) {
        }

        public void clear() {
        }

        public Set<String> keySet() {
            return null;
        }

        public Collection<Object> values() {
            return null;
        }

        public Set<Entry<String, Object>> entrySet() {
            return null;
        }
    }
}
