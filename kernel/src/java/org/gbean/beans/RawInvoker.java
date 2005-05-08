/**
 *
 * Copyright 2004 The Apache Software Foundation
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
package org.gbean.beans;

import java.util.Map;

import org.apache.geronimo.kernel.proxy.DeadProxyException;

/**
 * The raw invoker provides a raw (fast) access invoke operations on a GBean.
 *
 * @version $Rev: 106610 $ $Date: 2004-11-25 13:40:42 -0800 (Thu, 25 Nov 2004) $
 */
public final class RawInvoker {
    private final String objectName;
    private GBeanInstance gbeanInstance;

    public RawInvoker(GBeanInstance gbeanInstance) {
        objectName = gbeanInstance.getObjectName();
        this.gbeanInstance = gbeanInstance;
    }

    void destroy() {
        this.gbeanInstance = null;
    }

    private synchronized GBeanInstance getGBeanInstance() {
        if (gbeanInstance == null) {
            throw new DeadProxyException("Invalid proxy to " + objectName);
        }
        return gbeanInstance;
    }

    public Map getOperationIndex() {
        return getGBeanInstance().getOperationIndex();
    }

    public Object invoke(final int index, final Object[] args) throws Exception {
        return getGBeanInstance().invoke(index, args);
    }
}
