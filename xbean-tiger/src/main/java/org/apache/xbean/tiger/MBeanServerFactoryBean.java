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
package org.apache.xbean.tiger;

import org.springframework.beans.factory.FactoryBean;

import javax.management.MBeanServer;

import java.lang.management.ManagementFactory;

/**
 * A Java 5 specific factory bean to access the platforms {@link MBeanServer}.
 * 
 * @org.apache.xbean.XBean element="platformMBeanServer" rootElement="true" 
 * description="Creates a reference to the Java 5 platform MBeanServer"
 *                  
 * @version $Revision$
 */
public class MBeanServerFactoryBean implements FactoryBean {

    public Object getObject() throws Exception {
        return ManagementFactory.getPlatformMBeanServer();
    }

    public Class getObjectType() {
        return MBeanServer.class;
    }

    public boolean isSingleton() {
        return true;
    }

}
