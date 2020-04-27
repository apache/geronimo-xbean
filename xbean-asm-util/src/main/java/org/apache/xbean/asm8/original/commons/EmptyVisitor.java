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
package org.apache.xbean.asm8.original.commons;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.TypePath;

import static org.apache.xbean.asm8.original.commons.AsmConstants.ASM_VERSION;

public class EmptyVisitor extends ClassVisitor {
    protected final AnnotationVisitor av = new AnnotationVisitor(ASM_VERSION) {
        @Override
        public void visit(String name, Object value) {
            EmptyVisitor.this.visit(name, value);
        }

        @Override
        public void visitEnum(String name, String desc, String value) {
            EmptyVisitor.this.visitEnum(name, desc, value);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String desc) {
            return EmptyVisitor.this.visitAnnotation(name, desc);
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            return EmptyVisitor.this.visitArray(name);
        }

        @Override
        public void visitEnd() {
            EmptyVisitor.this.visitEnd();
        }
    };

    protected final FieldVisitor fv = new FieldVisitor(ASM_VERSION) {
        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return EmptyVisitor.this.visitAnnotation(desc, visible);
        }

        @Override
        public void visitAttribute(Attribute attribute) {
            EmptyVisitor.this.visitAttribute(attribute);
        }

        @Override
        public void visitEnd() {
            EmptyVisitor.this.visitEnd();
        }
    };
    protected final MethodVisitor mv = new MethodVisitor(ASM_VERSION) {
        @Override
        public AnnotationVisitor visitAnnotationDefault() {
            return EmptyVisitor.this.visitAnnotationDefault();
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return EmptyVisitor.this.visitAnnotation(desc, visible);
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
            return EmptyVisitor.this.visitMethodParameterAnnotation(parameter, desc, visible);
        }

        @Override
        public void visitAttribute(Attribute attribute) {
            EmptyVisitor.this.visitAttribute(attribute);
        }

        @Override
        public void visitCode() {
            EmptyVisitor.this.visitCode();
        }

        @Override
        public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
            EmptyVisitor.this.visitFrame(type, nLocal, local, nStack, stack);
        }

        @Override
        public void visitInsn(int opcode) {
            EmptyVisitor.this.visitInsn(opcode);
        }

        @Override
        public void visitJumpInsn(int i, Label label) {
            EmptyVisitor.this.visitJumpInsn(i, label);
        }

        @Override
        public void visitLabel(Label label) {
            EmptyVisitor.this.visitLabel(label);
        }

        @Override
        public void visitLdcInsn(Object cst) {
            EmptyVisitor.this.visitLdcInsn(cst);
        }

        @Override
        public void visitIincInsn(int var, int increment) {
            EmptyVisitor.this.visitIincInsn(var, increment);
        }

        @Override
        public void visitTableSwitchInsn(int i, int i2, Label label, Label... labels) {
            EmptyVisitor.this.visitTableSwitchInsn(i, i2, label, labels);
        }

        @Override
        public void visitLookupSwitchInsn(Label label, int[] ints, Label[] labels) {
            EmptyVisitor.this.visitLookupSwitchInsn(label, ints, labels);
        }

        @Override
        public void visitMultiANewArrayInsn(String desc, int dims) {
            EmptyVisitor.this.visitMultiANewArrayInsn(desc, dims);
        }

        @Override
        public void visitTryCatchBlock(Label label, Label label2, Label label3, String s) {
            EmptyVisitor.this.visitTryCatchBlock(label, label2, label3, s);
        }

        @Override
        public void visitLocalVariable(String s, String s2, String s3, Label label, Label label2, int i) {
            EmptyVisitor.this.visitLocalVariable(s, s2, s3, label, label2, i);
        }

        @Override
        public void visitLineNumber(int i, Label label) {
            EmptyVisitor.this.visitLineNumber(i, label);
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            EmptyVisitor.this.visitMaxs(maxStack, maxLocals);
        }

        @Override
        public void visitEnd() {
            EmptyVisitor.this.visitEnd();
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            EmptyVisitor.this.visitIntInsn(opcode, operand);
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            EmptyVisitor.this.visitVarInsn(opcode, var);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            EmptyVisitor.this.visitTypeInsn(opcode, type);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            EmptyVisitor.this.visitFieldInsn(opcode, owner, name, desc);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc) {
            EmptyVisitor.this.visitMethodInsn(opcode, owner, name, desc);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            EmptyVisitor.this.visitMethodInsn(opcode, owner, name, desc);
        }

        @Override
        public void visitInvokeDynamicInsn(String s, String s2, Handle handle, Object... objects) {
            EmptyVisitor.this.visitInvokeDynamicInsn(s, s2, handle, objects);
        }
    };

    public EmptyVisitor() {
        super(ASM_VERSION);
    }

    protected AnnotationVisitor visitAnnotationDefault() {
        return av;
    }

    protected AnnotationVisitor visitArray(String name) {
        return av;
    }

    protected AnnotationVisitor visitAnnotation(String name, String desc) {
        return av;
    }

    protected void visitEnum(String name, String desc, String value) {
        // no-op
    }

    protected void visit(String name, Object value) {
        // no-op
    }

    protected void visitVarInsn(int opcode, int var) {
        // no-op
    }

    protected void visitTypeInsn(int opcode, String type) {
        // no-op
    }

    protected void visitFieldInsn(int opcode, String owner, String name, String desc) {
        // no-op
    }

    protected void visitMethodInsn(int opcode, String owner, String name, String desc) {
        // no-op
    }

    protected void visitInvokeDynamicInsn(String s, String s2, Handle handle, Object[] objects) {
        // no-op
    }

    protected void visitIntInsn(int opcode, int operand) {
        // no-op
    }

    protected void visitJumpInsn(int i, Label label) {
        // no-op
    }

    protected void visitLabel(Label label) {
        // no-op
    }

    protected void visitLdcInsn(Object cst) {
        // no-op
    }

    protected void visitIincInsn(int var, int increment) {
        // no-op
    }

    protected void visitTableSwitchInsn(int i, int i2, Label label, Label[] labels) {
        // no-op
    }

    protected void visitLookupSwitchInsn(Label label, int[] ints, Label[] labels) {
        // no-op
    }

    protected void visitMultiANewArrayInsn(String desc, int dims) {
        // no-op
    }

    protected void visitTryCatchBlock(Label label, Label label2, Label label3, String s) {
        // no-op
    }

    protected void visitLocalVariable(String s, String s2, String s3, Label label, Label label2, int i) {
        // no-op
    }

    protected void visitLineNumber(int i, Label label) {
        // no-op
    }

    protected void visitMaxs(int maxStack, int maxLocals) {
        // no-op
    }

    protected void visitInsn(int opcode) {
        // no-op
    }

    protected void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
        // no-op
    }

    protected void visitCode() {
        // no-op
    }

    protected AnnotationVisitor visitMethodParameterAnnotation(int parameter, String desc, boolean visible) {
        return av;
    }

    protected AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
        return av;
    }

    @Override
    public void visit(int version, int access, String name, String signature,
                      String superName, String[] interfaces) {
        if (cv != null) {
            cv.visit(version, access, name, signature, superName, interfaces);
        }
    }

    @Override
    public void visitSource(String source, String debug) {
        if (cv != null) {
            cv.visitSource(source, debug);
        }
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
        if (cv != null) {
            cv.visitOuterClass(owner, name, desc);
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return av;
    }

    @Override
    public void visitAttribute(Attribute attr) {
        if (cv != null) {
            cv.visitAttribute(attr);
        }
    }

    @Override
    public void visitInnerClass(String name, String outerName,
                                String innerName, int access) {
        if (cv != null) {
            cv.visitInnerClass(name, outerName, innerName, access);
        }
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc,
                                   String signature, Object value) {
        return fv;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String signature, String[] exceptions) {
        return mv;
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        return av;
    }

    @Override
    public void visitEnd() {
        if (cv != null) {
            cv.visitEnd();
        }
    }

    public AnnotationVisitor annotationVisitor() {
        return av;
    }

    public FieldVisitor fieldVisitor() {
        return fv;
    }

    public MethodVisitor methodVisitor() {
        return mv;
    }
}
