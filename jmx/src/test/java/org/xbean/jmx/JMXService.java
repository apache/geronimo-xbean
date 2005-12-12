/**
 *
 * Copyright 2005 (C) The original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.xbean.jmx;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;


/**
 * @version $Revision: $ $Date: $
 */
public class JMXService {
    private List listeners = new ArrayList();
    private int attr1;
    private Integer attr2;
    private boolean dog;

    public JMXService() {
    }

    public JMXService(int attr1, Integer attr2, boolean dog) {
        this.attr1 = attr1;
        this.attr2 = attr2;
        this.dog = dog;
    }

    public int getAttr1() {
        return attr1;
    }

    public void setAttr1(int attr1) {
        this.attr1 = attr1;
    }

    public Integer getAttr2() {
        return attr2;
    }

    public void setAttr2(Integer attr2) {
        this.attr2 = attr2;
    }

    public boolean isDog() {
        return dog;
    }

    public void setDog(boolean dog) {
        this.dog = dog;
    }

    public String getFoo() {
        return "BAR";
    }

    public void setBar(String bar) {
        if (!"FOO".equals(bar)) throw new IllegalArgumentException("Only FOO is accepted");
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
