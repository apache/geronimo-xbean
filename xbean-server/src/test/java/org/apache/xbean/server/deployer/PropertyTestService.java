/**
 *
 * Copyright 2005-2006 The Apache Software Foundation
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
 */
package org.apache.xbean.server.deployer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * A simple test service used to validate that the property expansion works properly
 * 
 * @version $Revision$
 */
public class PropertyTestService implements InitializingBean {

    private static final Log log = LogFactory.getLog(PropertyTestService.class);
    
    private String baseDir;
    private String currentDir;
    
    public void afterPropertiesSet() throws Exception {
        assertValidProperty("baseDir", baseDir);
        assertValidProperty("currentDir", currentDir);
        if (!currentDir.endsWith("testcase")) {
            throw new IllegalArgumentException("The current directory should end with testcase but was: " + currentDir);
        }
    }

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public String getCurrentDir() {
        return currentDir;
    }

    public void setCurrentDir(String currentDir) {
        this.currentDir = currentDir;
    }


    protected void assertValidProperty(String name, String value) {
        log.info("Configured with " + name + ": " + value);
        
        if (value == null) {
            throw new IllegalArgumentException("No " + name + " property specified!");
        }
        if (value.length() == 0) {
            throw new IllegalArgumentException("Blank " + name + " property specified!");
        }
        if (value.startsWith("$")) {
            throw new IllegalArgumentException("The " + name + " property has not been expanded properly!: Its value is: " + value);
        }
    }

}
