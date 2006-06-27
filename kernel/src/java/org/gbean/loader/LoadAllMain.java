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
package org.gbean.loader;

import org.gbean.kernel.Kernel;
import org.gbean.kernel.Main;

/**
 * @version $Revision$ $Date$
 */
public class LoadAllMain implements Main {
    public Kernel kernel;
    public Main next;

    public Kernel getKernel() {
        return kernel;
    }

    public void setKernel(Kernel kernel) {
        this.kernel = kernel;
    }

    public Main getNext() {
        return next;
    }

    public void setNext(Main next) {
        this.next = next;
    }

    public void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String location = args[i];
            LoaderUtil.load(kernel, location);
            LoaderUtil.verifyAllServicesRunning(kernel);
        }
        if (next != null) {
            next.main(args);
        }
    }
}
