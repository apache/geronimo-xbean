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
package org.xbean.spring.context;

import java.util.Map;

import org.springframework.context.support.AbstractXmlApplicationContext;
import org.xbean.spring.example.BeerService;
import org.xbean.spring.example.FavoriteService;

/**
 * @author James Strachan
 * @version $Id$
 * @since 1.0
 */
public class FavoriteUsingSpringTest extends SpringTestSupport {
    
    public void testFavs() throws Exception {
        FavoriteService fs = (FavoriteService) getBean("favoriteService");
  
        Map favorites = fs.getFavorites();
        assertNotNull(favorites);
        assertEquals(1, favorites.size());
        
        assertEquals("Grey Goose", favorites.get("Dan"));
    }

    protected AbstractXmlApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("org/xbean/spring/context/favorite-normal.xml");
    }
}
