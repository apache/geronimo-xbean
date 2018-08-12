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
package org.apache.xbean.model.type;

import org.apache.xbean.model.NestingType;
import org.apache.xbean.model.Type;

public class CollectionType implements NestingType {

    private final String name;
    private final Type nestedType;

    CollectionType(String name, Type nestedType) {
        this.name = name;
        this.nestedType = nestedType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isCollection() {
        return true;
    }

    public Type getNestedType() {
        return nestedType;
    }

    @Override
    public String toString() {
        return "CollectionType[" + name + "<" + nestedType + ">]";
    }

}
