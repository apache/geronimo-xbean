/**
 * 
 * Copyright 2005 LogicBlaze, Inc. http://www.logicblaze.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 * 
 **/
package org.xbean.spring.task;

import org.codehaus.jam.JClass;

import java.io.*;

/**
 * 
 * @version $Revision: 1.1 $
 */
public class SchemaGenerator {

    private final JClass[] classes;
    private final File destFile;

    public SchemaGenerator(JClass[] classes, File destFile) {
        this.classes = classes;
        this.destFile = destFile;
    }

    public void generate() throws IOException {
        PrintWriter out = new PrintWriter(new FileWriter(destFile));
        try {
            generateFile(out);
        }
        finally {
            out.close();
        }

    }

    protected void generateFile(PrintWriter out) {
        for (int i = 0; i < classes.length; i++) {
            JClass type = classes[i];
            if (type.getAnnotation("") != null) {

            }
        }
    }

}
