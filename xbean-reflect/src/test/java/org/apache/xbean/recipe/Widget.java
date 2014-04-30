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
package org.apache.xbean.recipe;

/**
 * @version $Rev$ $Date$
 */
public class Widget {

    private final String one;
    private final int two;
    private boolean three;
    private String four;
    private Class five;
    public String six;

    public Widget(String one, int two, boolean three, String four, Class five, String six) {
        this.five = five;
        this.four = four;
        this.one = one;
        this.six = six;
        this.three = three;
        this.two = two;
    }

    public Widget(String one, int two) {
        this.one = one;
        this.two = two;
    }

    public void setThree(boolean three) {
        this.three = three;
    }

    private void setFour(String four) {
        this.four = four;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Widget widget = (Widget) o;

        if (three != widget.three) return false;
        if (two != widget.two) return false;
        if (five != null ? !five.equals(widget.five) : widget.five != null) return false;
        if (four != null ? !four.equals(widget.four) : widget.four != null) return false;
        if (one != null ? !one.equals(widget.one) : widget.one != null) return false;
        if (six != null ? !six.equals(widget.six) : widget.six != null) return false;

        return true;
    }
}
