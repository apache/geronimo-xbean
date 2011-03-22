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

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

/**
 * @version $Rev$ $Date$
 */
public class RegexFilteredArchive implements Archive {

    private final Pattern include;
    private final Pattern exclude;

    private final Archive archive;

    public RegexFilteredArchive(Archive archive, String include, String exclude) {
        this.archive = archive;
        this.include = (".*".equals(include)) ? null : Pattern.compile(include);
        this.exclude = ("".equals(exclude)) ? null : Pattern.compile(exclude);
    }

    public RegexFilteredArchive(Archive archive, Pattern include, Pattern exclude) {
        this.archive = archive;
        this.include = include;
        this.exclude = exclude;
    }

    @Override
    public InputStream getBytecode(String className) throws IOException, ClassNotFoundException {
        return archive.getBytecode(className);
    }

    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return archive.loadClass(className);
    }

    @Override
    public Iterator<String> iterator() {
        return new Filter(archive.iterator());
    }

    private final class Filter implements Iterator<String> {
        private final Iterator<String> it;

        private String next;

        private Filter(Iterator<String> it) {
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            if (next != null) return true;
            if (!it.hasNext()) return false;
            seek();
            return hasNext();
        }

        @Override
        public String next() {
            if (!hasNext()) throw new NoSuchElementException();

            String s = next;
            next = null;

            return s;
        }

        @Override
        public void remove() {
            it.remove();
        }

        private void seek() {
            while (next == null && it.hasNext()) {
                next = it.next();

                if (include != null && include.matcher(next).matches()) break;
                if (exclude != null && exclude.matcher(next).matches()) next = null;

            }
        }

    }
}
