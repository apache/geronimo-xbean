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

/**
 * First, all Include directives are evaluated; at least one must match, or the className is rejected.
 * Next, all Exclude directives are evaluated. If any matches, the className is rejected.
 * Last, any classNames which do not match an Include or a Exclude directive are denied by default.
 */
public class IncludeExcludeFilter implements FilteredArchive.Filter {

    private FilteredArchive.Filter include;
    private FilteredArchive.Filter exclude;

    public IncludeExcludeFilter(FilteredArchive.Filter include, FilteredArchive.Filter exclude) {
        this.include = include;
        this.exclude = exclude;
    }

    @Override
    public boolean accept(String className) {
        if (include.accept(className)) return !exclude.accept(className);
        return false;
    }
}
