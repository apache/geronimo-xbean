/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.xbean.finder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @version $Rev$ $Date$
 */
public class FilterList implements FilteredArchive.Filter {

    private final List<FilteredArchive.Filter> filters = new ArrayList<FilteredArchive.Filter>();

    public FilterList(FilteredArchive.Filter... filters) {
        this(Arrays.asList(filters));
    }

    public FilterList(Iterable<FilteredArchive.Filter> filters) {
        for (FilteredArchive.Filter filter : filters) {
            this.filters.add(filter);
        }
    }

    @Override
    public boolean accept(String className) {
        for (FilteredArchive.Filter filter : filters) {
            if (filter.accept(className)) return true;
        }

        return false;
    }

    public List<FilteredArchive.Filter> getFilters() {
        return filters;
    }
}
