/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.xbean.blueprint.cm;

import java.util.Map;

import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.MapContext;
import org.apache.commons.jexl2.UnifiedJEXL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Rev$ $Date$
 */
public class JexlExpressionParser {
    private static final Logger log = LoggerFactory.getLogger(JexlExpressionParser.class);

    private final JexlEngine engine;
    private final UnifiedJEXL jexl;
    protected final JexlContext context;

    public JexlExpressionParser(final Map<String, Object> vars) {
        if (vars == null) {
            throw new IllegalArgumentException("vars");
        }
        engine = new JexlEngine();
        jexl = new UnifiedJEXL(engine);
        context = new MapContext(vars);

        log.trace("Using variables: {}", vars);
    }

//    private FlatResolver resolver = new FlatResolver(true);
//
//    protected Expression createExpression(final String expression) throws Exception {
//        // assert expression != null;
//
//        Expression expr = engine.createExpression(expression);
//        expr.addPreResolver(resolver);
//
//        return expr;
//    }

    public Object evaluate(final String expression) throws Exception {
        if (expression == null) {
            throw new IllegalArgumentException("expression");
        }

        log.trace("Evaluating expression: {}", expression);
        return jexl.parse(expression).evaluate(context);
//        Expression expr = createExpression(expression);
//        Object obj = expr.evaluate(context);
//        log.trace("Result: {}", obj);
//
//        return obj;
    }

//    public String parse(final String input) {
//        if (input == null) {
//            throw new IllegalArgumentException("input");
//        }
//
//        log.trace("Parsing input: {}", input);
//
//        StringBuilder buff = new StringBuilder();
//
//        int cur = 0;
//        int prefixLoc;
//        int suffixLoc;
//
//        while (cur < input.length()) {
//            prefixLoc = input.indexOf("${", cur);
//
//            if (prefixLoc < 0) {
//                break;
//            }
//
//            suffixLoc = findBlockEnd(prefixLoc + 2, input);
//            if (suffixLoc < 0) {
//                throw new RuntimeException("Missing '}': " + input);
//            }
//
//            String expr = input.substring(prefixLoc + 2, suffixLoc);
//            buff.append(input.substring(cur, prefixLoc));
//
//            try {
//                buff.append(evaluate(expr));
//            }
//            catch (Exception e) {
//                throw new RuntimeException("Failed to evaluate: " + expr, e);
//            }
//
//            cur = suffixLoc + 1;
//        }
//
//        buff.append(input.substring(cur));
//
//        log.trace("Parsed result: {}", buff);
//
//        return buff.toString();
//    }

//    private int findBlockEnd(int pos, String input) {
//        int nested = 0;
//        while (pos < input.length()) {
//            char ch = input.charAt(pos);
//            if (ch == '{') {
//                nested++;
//            } else if (ch == '}') {
//                if (nested == 0) {
//                    return pos;
//                } else {
//                    nested--;
//                }
//            }
//            pos++;
//        }
//        return -1;
//    }

//    public String parse(final String input, final boolean trim) {
//        String output = parse(input);
//        if (trim && output != null) {
//            output = output.trim();
//        }
//
//        return output;
//    }
}
