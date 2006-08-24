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
package org.apache.xbean.jmx;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;


/**
 * @version $Revision: $ $Date: $
 */
public class JMXService {
    private List listeners = new ArrayList();
    private int intAttr;
    private Integer integerAttr;
    private boolean booleanAttr;

    public JMXService() {
    }

    public JMXService(int intAttr, Integer integerAttr, boolean booleanAttr) {
        this.intAttr = intAttr;
        this.integerAttr = integerAttr;
        this.booleanAttr = booleanAttr;
    }

    public int getIntAttr() {
        return intAttr;
    }

    public void setIntAttr(int intAttr) {
        this.intAttr = intAttr;
    }

    public Integer getIntegerAttr() {
        return integerAttr;
    }

    public void setIntegerAttr(Integer integerAttr) {
        this.integerAttr = integerAttr;
    }

    public boolean isBooleanAttr() {
        return booleanAttr;
    }

    public void setBooleanAttr(boolean booleanAttr) {
        this.booleanAttr = booleanAttr;
    }

    public String getReadOnly() {
        return "BAR";
    }

    public void setWriteOnly(String value) {
        if (!"FOO".equals(value)) throw new IllegalArgumentException("Only FOO is accepted");
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        listeners.add(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        listeners.remove(listener);
    }

    public void firePropertyChange() {
        Long oldValue = new Long(System.currentTimeMillis());
        Long newValue = new Long(oldValue.longValue() - 1L);
        PropertyChangeEvent event = new PropertyChangeEvent(this, "property", oldValue, newValue);
        for (int i = 0; i < listeners.size(); i++) {
            PropertyChangeListener listener = (PropertyChangeListener) listeners.get(i);
            listener.propertyChange(event);
        }
    }

    public List getPropertyChangeListeners() {
        return listeners;
    }
}
