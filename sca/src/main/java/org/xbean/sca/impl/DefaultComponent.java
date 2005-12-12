/**
 * 
 * Copyright 2005 LogicBlaze, Inc. http://www.logicblaze.com
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
package org.xbean.sca.impl;

import org.osoa.sca.model.*;

/**
 * 
 * @version $Revision$
 */
public abstract class DefaultComponent implements Component {

    private String name;
    private Implementation implementation;
    private PropertyValues propertyValues;
    private ReferenceValues referenceValues;
/*    
    private Sequence any;
    private Sequence anyAttribute;
*/    

    public Implementation getImplementation() {
        return implementation;
    }

    public void setImplementation(Implementation implementation) {
        this.implementation = implementation;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PropertyValues getPropertyValues() {
        return propertyValues;
    }

    public void setPropertyValues(PropertyValues propertyValues) {
        this.propertyValues = propertyValues;
    }

    public ReferenceValues getReferenceValues() {
        return referenceValues;
    }

    public void setReferenceValues(ReferenceValues referenceValues) {
        this.referenceValues = referenceValues;
    }

/*
    public Sequence getAny() {
        return any;
    }

    public void setAny(Sequence any) {
        this.any = any;
    }

    public Sequence getAnyAttribute() {
        return anyAttribute;
    }

    public void setAnyAttribute(Sequence anyAttribute) {
        this.anyAttribute = anyAttribute;
    }
*/
    
}
