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
package org.gbean.spring;

import java.util.Properties;

/**
 * @version $Revision$ $Date$
 */
public class HelloMessage implements MyLifecycle {
    private String prefix;
    private String suffix;
    private Properties properties;

    public HelloMessage() {
    }

    public HelloMessage(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
    }

    public HelloMessage(String prefix, String suffix, Properties properties) {
        this.prefix = prefix;
        this.suffix = suffix;
        this.properties = properties;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String message(String message) {
        return prefix + message + suffix;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public void begin() {
    }

    public void end() {
    }
}
