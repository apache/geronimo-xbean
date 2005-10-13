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
package org.xbean.kernel;

/**
 * The immutable unique name of a service.  A proper implementation of ServiceName must have a correct implementation of
 * equals and hashCode.  A ServiceName should have one constructor that takes a single String and the toString method
 * should return a String that can be used in the String constructor.  This means the following code should work:
 * <p><blockquote><pre>
 * Constructor constructor = serviceName.getClass().getConstructor(new Class[] {String.class});
 * ServiceName name = constructor.newInstance(new Object[] {serviceName.toString()});
 * </pre></blockquote>
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public interface ServiceName {
    /**
     * A service name must properly implement hashCode.  For example,
     * <p><blockquote><pre>
     * public int hashCode() {
     *     int result = 17;
     *     result = 37 * result + integer;
     *     result = 37 * result + (object == null ? 0 : object.hashCode());
     *     return result;
     * }
     * </pre></blockquote>
     *
     * @return the hash code
     */
    int hashCode();

    /**
     * A service name must property implement equals.  For example,
     * <p><blockquote><pre>
     * public boolean equals(Object obj) {
     *     if (!(obj instanceof MyServiceName)) {
     *         return false;
     *     }
     *     MyServiceName name = (MyServiceName) obj;
     *     return integer == name.integer &&
     *             (object == null ? name.object == null : object.equals(name.object));
     * }
     * </pre></blockquote>
     *
     * @param object some object
     * @return true if the object is equivalent to this service name; false otherwise
     */
    boolean equals(Object object);

    /**
     * A service name should return a string from toString that can be used in a String constructor.
     *
     * @return the connonical form of this name
     */
    String toString();
}
