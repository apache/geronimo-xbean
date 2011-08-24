/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.xbean.osgi.bundle.util;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.xbean.osgi.bundle.util.HeaderParser.HeaderElement;

/**
 * @version $Rev$ $Date$
 */
public class HeaderBuilder {

    private static final Pattern EXTENDED_PATTERN = Pattern.compile("[\\w_\\-\\.]+");

    public static String build(List<HeaderElement> headerElements) {
        if (headerElements == null || headerElements.size() == 0) {
            return "";
        }
        StringBuilder header = new StringBuilder();
        for (HeaderElement headerElement : headerElements) {
            String name = headerElement.getName();
            if (name == null || name.length() == 0) {
                throw new IllegalArgumentException("Invalid header name for the header elment " + headerElement);
            }
            if (header.length() > 0) {
                header.append(",");
            }
            header.append(name);
            for (Map.Entry<String, String> attribute : headerElement.getAttributes().entrySet()) {
                header.append(";").append(attribute.getKey()).append("=");
                if (EXTENDED_PATTERN.matcher(attribute.getValue()).matches()) {
                    header.append(attribute.getValue());
                } else {
                    header.append("\"").append(attribute.getValue()).append("\"");
                }
            }
            for (Map.Entry<String, String> directive : headerElement.getDirectives().entrySet()) {
                header.append(";").append(directive.getKey()).append(":=");
                if (EXTENDED_PATTERN.matcher(directive.getValue()).matches()) {
                    header.append(directive.getValue());
                } else {
                    header.append("\"").append(directive.getValue()).append("\"");
                }
            }
        }
        return header.toString();
    }
}
