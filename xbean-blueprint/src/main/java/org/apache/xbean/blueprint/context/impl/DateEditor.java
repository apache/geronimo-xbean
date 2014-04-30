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
package org.apache.xbean.blueprint.context.impl;

import java.beans.PropertyEditorSupport;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

/**
 * Editor for <code>java.util.Date</code>
 */
public class DateEditor extends PropertyEditorSupport {

    private DateFormat dateFormat = DateFormat.getInstance();
    
	public void setAsText(String text) throws IllegalArgumentException {
		try {
            setValue(dateFormat.parse(text));
		}
        catch (ParseException e) {
            throw new IllegalArgumentException(
                    "Could not convert Date for " + text + ": " + e.getMessage());
        }
	}

	public String getAsText() {
        Date value = (Date) getValue();
		return (value != null ? dateFormat.format(value) : "");
	}

}
