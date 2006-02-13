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
    
    private String dir;
    
    public void afterPropertiesSet() throws Exception {
        log.info("Configured with dir: " + dir);
        
        if (dir == null) {
            throw new IllegalArgumentException("No dir property specified!");
        }
        if (dir.length() == 0) {
            throw new IllegalArgumentException("Blank dir property specified!");
        }
        if (dir.startsWith("$")) {
            throw new IllegalArgumentException("The dir property has not been expanded properly!: Its value is: " + dir);
        }
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    
}
