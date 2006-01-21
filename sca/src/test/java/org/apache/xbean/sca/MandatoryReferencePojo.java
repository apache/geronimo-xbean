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
package org.apache.xbean.sca;

import org.osoa.sca.annotations.Reference;

/**
 * 
 * @version $Revision$
 */
public class MandatoryReferencePojo {

    private String cheese;
    private boolean beer;

    public String getCheese() {
        return cheese;
    }

    @Reference(required = true)
    public void setCheese(String cheese) {
        this.cheese = cheese;
    }

    public boolean isBeer() {
        return beer;
    }

    @Reference(required = false)
    public void setBeer(boolean beer) {
        this.beer = beer;
    }

}
