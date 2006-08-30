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
package org.apache.xbean.spring.example;

/**
 * Basic salad.
 * @org.apache.xbean.XBean
 * 
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */

// START SNIPPET: bean
public class SaladService {
    private final String dressing;
    private final String size;
    private final boolean crouton;

    public SaladService(String dressing, String size, boolean crouton) {
        this.dressing = dressing;
        this.size = size;
        this.crouton = crouton;
    }

    /**
     * Dressing What type of dressing do you want?
     */
    public String getDressing() {
        return dressing;
    }

    /**
     * What size do you want?
     */
    public String getSize() {
        return size;
    }

    /**
     * Do you want crutons on that?
     * @org.apache.xbean.Property alias="addCroutons"
     */
    public boolean isCrouton() {
        return crouton;
    }
}
// END SNIPPET: bean
