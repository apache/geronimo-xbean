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
package org.apache.xbean.spring.context.v2c;

import java.beans.PropertyEditor;
import org.springframework.beans.factory.FactoryBean;

public class PropertyEditorFactory implements FactoryBean {

	PropertyEditor propertyEditor;
	String value;
	Class type = Object.class;
	
	public Object getObject() throws Exception {
		propertyEditor.setAsText(value);
    	return propertyEditor.getValue();		
	}

	public Class getObjectType() {
		return type;
	}

	public boolean isSingleton() {
		return true;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public PropertyEditor getPropertyEditor() {
		return propertyEditor;
	}

	public void setPropertyEditor(PropertyEditor propertyEditor) {
		this.propertyEditor = propertyEditor;
	}

	public Class getType() {
		return type;
	}

	public void setType(Class type) {
		this.type = type;
	}

}
