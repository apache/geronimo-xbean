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
package org.gbean.service;

import java.util.Set;
import java.util.Map;

/**
 * @version $Revision$ $Date$
 */
public interface ServiceFactory {
    Map getDependencies();

    void addDependency(String name, Set patterns);

    Object createService(ServiceContext serviceContext) throws Exception;

    void destroyService(ServiceContext serviceContext, Object service);

    boolean isEnabled();

    void setEnabled(boolean enabled);
}
