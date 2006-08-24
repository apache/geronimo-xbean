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
package org.apache.xbean.server.spring.jmx;

import javax.management.MBeanServer;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * A FactoryBean that uses the xbean.tiger MBeanServerFactoryBean if it can be loaded or, uses 
 * the spring MBeanServerFactoryBean otherwise.
 * 
 * @version $Revision$
 */
public class MBeanServerFactoryBean implements FactoryBean {

    public Object getObject() throws Exception {
        
        ClassLoader classLoader = MBeanServerFactoryBean.class.getClassLoader();
        try {
            
            // Try to use the xbean tiger version first...
            FactoryBean fb = (FactoryBean) classLoader.loadClass("org.apache.xbean.tiger.MBeanServerFactoryBean").newInstance();
            return fb.getObject();
            
        } catch (Throwable e) {
            
            // Fallback to using the spring factory bean then 
            FactoryBean fb = (FactoryBean) classLoader.loadClass("org.springframework.jmx.support.MBeanServerFactoryBean").newInstance();
            ((InitializingBean)fb).afterPropertiesSet();
            return fb.getObject();
            
        }
    }

    public Class getObjectType() {
        return MBeanServer.class;
    }

    public boolean isSingleton() {
        return true;
    }

}
