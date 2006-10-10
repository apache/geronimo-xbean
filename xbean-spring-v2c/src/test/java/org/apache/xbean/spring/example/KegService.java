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

//START SNIPPET: java
/**
 * @org.apache.xbean.XBean element="keg" 
 * 
 * Used to verify that property PropertyEditors work correctly.
 * 
 * @author chirino
 */
public class KegService {
	
    private long remaining;

	/**
	 * Gets the amount of beer remaining in the keg (in ml)
	 * 
	 * @param remaining
	 */
	public long getRemaining() {
		return remaining;
	}

	/**
	 * Sets the amount of beer remaining in the keg (in ml)
	 * 
     * @org.apache.xbean.Property propertyEditor="org.apache.xbean.spring.example.MilliLittersPropertyEditor"
	 * @param remaining
	 */
	public void setRemaining(long remaining) {
		this.remaining = remaining;
	}
	
	public long dispense( long amount ) {
		this.remaining -= amount;
		return this.remaining;
	}

}
// END SNIPPET: java

