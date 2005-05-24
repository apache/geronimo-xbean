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
package org.gbean.metadata.simple;

import java.util.Collection;
import java.util.Iterator;

import org.gbean.metadata.ClassMetadata;
import org.gbean.metadata.MetadataManager;
import org.gbean.metadata.MetadataProvider;

/**
 * @version $Revision$ $Date$
 */
public class SimpleMetadataManager implements MetadataManager {
    private final Collection metadataProviders;

    public SimpleMetadataManager(Collection metadataProviders) {
        this.metadataProviders = metadataProviders;
    }

    public ClassMetadata getClassMetadata(Class type) {
        ClassMetadata classMetadata = new SimpleClassMetadata(type);
        for (Iterator iterator = metadataProviders.iterator(); iterator.hasNext();) {
            MetadataProvider metadataProvider = (MetadataProvider) iterator.next();
            metadataProvider.addClassMetadata(classMetadata);
        }
        return classMetadata;
    }
}