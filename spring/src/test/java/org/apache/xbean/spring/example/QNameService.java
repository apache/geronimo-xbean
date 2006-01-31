/**
 * 
 * Copyright 2005-2006 The Apache Software Foundation or its licensors,  as applicable.
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
 * 
 **/
package org.apache.xbean.spring.example;

import java.util.List;

import javax.xml.namespace.QName;

/**
 * @org.apache.xbean.XBean element="qname-service"
 * @author gnodet
 */
public class QNameService {

    private QName[] services;
    private List list;

    public QName[] getServices() {
        return services;
    }

    public void setServices(QName[] services) {
        this.services = services;
    }

    public List getList() {
        return list;
    }

    public void setList(List list) {
        this.list = list;
    }


}
