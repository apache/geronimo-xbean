/**
 *
 * Copyright 2006 The Apache Software Foundation
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
package org.apache.xbean.naming.context;

import javax.naming.Name;

/**
 * @version $Rev$ $Date$
 */
public interface ContextAccess {
    ContextAccess MODIFIABLE = new ContextAccess() {
        public boolean isModifiable(Name name) {
            return true;
        }
    };

    ContextAccess UNMODIFIABLE = new ContextAccess() {
        public boolean isModifiable(Name name) {
            return false;
        }
    };

    boolean isModifiable(Name name);
}
