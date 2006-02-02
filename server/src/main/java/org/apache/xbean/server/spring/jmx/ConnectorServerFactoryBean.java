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
package org.apache.xbean.server.spring.jmx;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.management.JMException;

import java.io.IOException;

/**
 * A helper class which logs a useful startup message.
 * 
 * @version $Revision$
 */
public class ConnectorServerFactoryBean extends org.springframework.jmx.support.ConnectorServerFactoryBean {

    private static final Log log = LogFactory.getLog(ConnectorServerFactoryBean.class);

    private String serviceUrl;

    public void setServiceUrl(String serviceUrl) {
        super.setServiceUrl(serviceUrl);
        this.serviceUrl = serviceUrl;
    }

    public void afterPropertiesSet() throws JMException, IOException {
        super.afterPropertiesSet();

        log.info("Started remote JMX connector. Point your JMX console to: " + serviceUrl);
    }

}
