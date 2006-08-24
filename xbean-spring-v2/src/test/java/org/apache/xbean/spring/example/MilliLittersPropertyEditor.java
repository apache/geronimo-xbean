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
package org.apache.xbean.spring.example;

import java.beans.PropertyEditorSupport;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//START SNIPPET: java
/**
 * 
 * Used to verify that per property PropertyEditors work correctly.
 * 
 * @author chirino
 */
public class MilliLittersPropertyEditor extends PropertyEditorSupport {

	public void setAsText(String text) throws IllegalArgumentException {

		Pattern p = Pattern.compile("^(\\d+)\\s*(l(iter)?)?$", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(text);
		if( m.matches() ) {
			setValue(new Long(Long.parseLong(m.group(1))*1000));
			return;
		}
		
		p = Pattern.compile("^(\\d+)\\s*(ml)?$", Pattern.CASE_INSENSITIVE);
		m = p.matcher(text);
		if( m.matches() ) {
			setValue(new Long(Long.parseLong(m.group(1))));
			return;
		}

		p = Pattern.compile("^(\\d+)\\s*pints?$", Pattern.CASE_INSENSITIVE);
	    m = p.matcher(text);
		if( m.matches() ) {
			long pints = Long.parseLong(m.group(1));
			setValue(new Long( (long)(pints * 1750) ));
			return;
		}
		
		throw new IllegalArgumentException("Could convert not to long (in ml) for "+ text);		
	}

	public String getAsText() {
		Long value = (Long) getValue();
		return (value != null ? value.toString() : "");
	}
}
//END SNIPPET: java
