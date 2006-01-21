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

import java.util.Map;


/**
 * @org.apache.xbean.XBean element="favorite"
 * 
 * @author Dan Diephouse
 * @version $Id$
 */

// START SNIPPET: bean
public class FavoriteService {
    private Map favorites;

    /**
     * @org.apache.xbean.Map entryName="favorite-item" keyName="person"
     * @return
     */
    public Map getFavorites() {
        return favorites;
    }

    public void setFavorites(Map favorites) {
        this.favorites = favorites;
    }   
}
// END SNIPPET: bean

