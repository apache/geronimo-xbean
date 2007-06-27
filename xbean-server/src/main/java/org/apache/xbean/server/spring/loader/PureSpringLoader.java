/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.xbean.server.spring.loader;

import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * A derivation of {@link SpringLoader} which does not use the XBean versions of the Spring ApplicationContext classes
 * and which so enforces XML validation
 *
 * @version $Revision: 1.1 $
 */
public class PureSpringLoader extends SpringLoader {
    
    @Override
    protected AbstractXmlApplicationContext createXmlApplicationContext(String configLocation) {
        return new FileSystemXmlApplicationContext(new String[]{configLocation}, false);
    }
}
