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


package org.apache.xbean.finder;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.EmptyVisitor;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

/**
 * @version $Rev$ $Date$
 */
public abstract class AbstractFinder {
    private final Map<String, List<Info>> annotated = new HashMap<String, List<Info>>();
    protected final List<ClassInfo> classInfos = new ArrayList<ClassInfo>();
    private final List<String> classesNotLoaded = new ArrayList<String>();
    private final int ASM_FLAGS = ClassReader.SKIP_CODE + ClassReader.SKIP_DEBUG + ClassReader.SKIP_FRAMES;

    protected abstract URL getResource(String className);

    protected abstract Class<?> loadClass(String fixedName) throws ClassNotFoundException;

    public boolean isAnnotationPresent(Class<? extends Annotation> annotation) {
        List<Info> infos = annotated.get(annotation.getName());
        return infos != null && !infos.isEmpty();
    }

    /**
     * Returns a list of classes that could not be loaded in last invoked findAnnotated* method.
     * <p/>
     * The list will only contain entries of classes whose byte code matched the requirements
     * of last invoked find* method, but were unable to be loaded and included in the results.
     * <p/>
     * The list returned is unmodifiable.  Once obtained, the returned list will be a live view of the
     * results from the last findAnnotated* method call.
     * <p/>
     * This method is not thread safe.
     * @return an unmodifiable live view of classes that could not be loaded in previous findAnnotated* call.
     */
    public List<String> getClassesNotLoaded() {
        return Collections.unmodifiableList(classesNotLoaded);
    }

    public List<Package> findAnnotatedPackages(Class<? extends Annotation> annotation) {
        classesNotLoaded.clear();
        List<Package> packages = new ArrayList<Package>();
        List<Info> infos = getAnnotationInfos(annotation.getName());
        for (Info info : infos) {
            if (info instanceof PackageInfo) {
                PackageInfo packageInfo = (PackageInfo) info;
                try {
                    Package pkg = packageInfo.get();
                    // double check via proper reflection
                    if (pkg.isAnnotationPresent(annotation)) {
                        packages.add(pkg);
                    }
                } catch (ClassNotFoundException e) {
                    classesNotLoaded.add(packageInfo.getName());
                }
            }
        }
        return packages;
    }

    public List<Class> findAnnotatedClasses(Class<? extends Annotation> annotation) {
        classesNotLoaded.clear();
        List<Class> classes = new ArrayList<Class>();
        List<Info> infos = getAnnotationInfos(annotation.getName());
        for (Info info : infos) {
            if (info instanceof ClassInfo) {
                ClassInfo classInfo = (ClassInfo) info;
                try {
                    Class clazz = classInfo.get();
                    // double check via proper reflection
                    if (clazz.isAnnotationPresent(annotation)) {
                        classes.add(clazz);
                    }
                } catch (ClassNotFoundException e) {
                    classesNotLoaded.add(classInfo.getName());
                }
            }
        }
        return classes;
    }

    /**
     * Naive implementation - works extremelly slow O(n^3)
     *
     * @param annotation
     * @return list of directly or indirectly (inherited) annotated classes
     */
    public List<Class> findInheritedAnnotatedClasses(Class<? extends Annotation> annotation) {
        classesNotLoaded.clear();
        List<Class> classes = new ArrayList<Class>();
        List<Info> infos = getAnnotationInfos(annotation.getName());
        for (Info info : infos) {
            try {
            	if(info instanceof ClassInfo){
                   classes.add(((ClassInfo) info).get());
            	}
            } catch (ClassNotFoundException cnfe) {
                // TODO: ignored, but a log message would be appropriate
            }
        }
        boolean annClassFound;
        List<ClassInfo> tempClassInfos = new ArrayList<ClassInfo>(classInfos);
        do {
            annClassFound = false;
            for (int pos = 0; pos < tempClassInfos.size(); pos++) {
                ClassInfo classInfo = tempClassInfos.get(pos);
                try {
                    // check whether any superclass is annotated
                    String superType = classInfo.getSuperType();
                    for (Class clazz : classes) {
                        if (superType.equals(clazz.getName())) {
                            classes.add(classInfo.get());
                            tempClassInfos.remove(pos);
                            annClassFound = true;
                            break;
                        }
                    }
                    // check whether any interface is annotated
                    List<String> interfces = classInfo.getInterfaces();
                    for (String interfce: interfces) {
                        for (Class clazz : classes) {
                            if (interfce.replaceFirst("<.*>","").equals(clazz.getName())) {
                                classes.add(classInfo.get());
                                tempClassInfos.remove(pos);
                                annClassFound = true;
                                break;
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    classesNotLoaded.add(classInfo.getName());
                } catch (NoClassDefFoundError e) {
                    classesNotLoaded.add(classInfo.getName());
                }
            }
        } while (annClassFound);
        return classes;
    }

    public List<Method> findAnnotatedMethods(Class<? extends Annotation> annotation) {
        classesNotLoaded.clear();
        List<ClassInfo> seen = new ArrayList<ClassInfo>();
        List<Method> methods = new ArrayList<Method>();
        List<Info> infos = getAnnotationInfos(annotation.getName());
        for (Info info : infos) {
            if (info instanceof MethodInfo && !info.getName().equals("<init>")) {
                MethodInfo methodInfo = (MethodInfo) info;
                ClassInfo classInfo = methodInfo.getDeclaringClass();

                if (seen.contains(classInfo)) continue;

                seen.add(classInfo);

                try {
                    Class clazz = classInfo.get();
                    for (Method method : clazz.getDeclaredMethods()) {
                        if (method.isAnnotationPresent(annotation)) {
                            methods.add(method);
                        }
                    }
                } catch (ClassNotFoundException e) {
                    classesNotLoaded.add(classInfo.getName());
                }
            }
        }
        return methods;
    }

    public List<Constructor> findAnnotatedConstructors(Class<? extends Annotation> annotation) {
        classesNotLoaded.clear();
        List<ClassInfo> seen = new ArrayList<ClassInfo>();
        List<Constructor> constructors = new ArrayList<Constructor>();
        List<Info> infos = getAnnotationInfos(annotation.getName());
        for (Info info : infos) {
            if (info instanceof MethodInfo && info.getName().equals("<init>")) {
                MethodInfo methodInfo = (MethodInfo) info;
                ClassInfo classInfo = methodInfo.getDeclaringClass();

                if (seen.contains(classInfo)) continue;

                seen.add(classInfo);

                try {
                    Class clazz = classInfo.get();
                    for (Constructor constructor : clazz.getConstructors()) {
                        if (constructor.isAnnotationPresent(annotation)) {
                            constructors.add(constructor);
                        }
                    }
                } catch (ClassNotFoundException e) {
                    classesNotLoaded.add(classInfo.getName());
                }
            }
        }
        return constructors;
    }

    public List<Field> findAnnotatedFields(Class<? extends Annotation> annotation) {
        classesNotLoaded.clear();
        List<ClassInfo> seen = new ArrayList<ClassInfo>();
        List<Field> fields = new ArrayList<Field>();
        List<Info> infos = getAnnotationInfos(annotation.getName());
        for (Info info : infos) {
            if (info instanceof FieldInfo) {
                FieldInfo fieldInfo = (FieldInfo) info;
                ClassInfo classInfo = fieldInfo.getDeclaringClass();

                if (seen.contains(classInfo)) continue;

                seen.add(classInfo);

                try {
                    Class clazz = classInfo.get();
                    for (Field field : clazz.getDeclaredFields()) {
                        if (field.isAnnotationPresent(annotation)) {
                            fields.add(field);
                        }
                    }
                } catch (ClassNotFoundException e) {
                    classesNotLoaded.add(classInfo.getName());
                }
            }
        }
        return fields;
    }

    public List<Class> findClassesInPackage(String packageName, boolean recursive) {
        classesNotLoaded.clear();
        List<Class> classes = new ArrayList<Class>();
        for (ClassInfo classInfo : classInfos) {
            try {
                if (recursive && classInfo.getPackageName().startsWith(packageName)){
                    classes.add(classInfo.get());
                } else if (classInfo.getPackageName().equals(packageName)){
                    classes.add(classInfo.get());
                }
            } catch (ClassNotFoundException e) {
                classesNotLoaded.add(classInfo.getName());
            }
        }
        return classes;
    }

    protected List<Info> getAnnotationInfos(String name) {
        List<Info> infos = annotated.get(name);
        if (infos == null) {
            infos = new ArrayList<Info>();
            annotated.put(name, infos);
        }
        return infos;
    }

    protected void readClassDef(InputStream in) throws IOException {
        ClassReader classReader = new ClassReader(in);
        classReader.accept(new InfoBuildingVisitor(), ASM_FLAGS);
    }

    public class Annotatable {
        private final List<AnnotationInfo> annotations = new ArrayList<AnnotationInfo>();

        public Annotatable(AnnotatedElement element) {
            for (Annotation annotation : element.getAnnotations()) {
                annotations.add(new AnnotationInfo(annotation.annotationType().getName()));
            }
        }

        public Annotatable() {
        }

        public List<AnnotationInfo> getAnnotations() {
            return annotations;
        }

    }

    public static interface Info {
        String getName();

        List<AnnotationInfo> getAnnotations();
    }

    public class PackageInfo extends Annotatable implements Info {
        private final String name;
        private final ClassInfo info;
        private final Package pkg;

        public PackageInfo(Package pkg){
            super(pkg);
            this.pkg = pkg;
            this.name = pkg.getName();
            this.info = null;
        }

        public PackageInfo(String name) {
            info = new ClassInfo(name, null);
            this.name = name;
            this.pkg = null;
        }

        public String getName() {
            return name;
        }

        public Package get() throws ClassNotFoundException {
            return (pkg != null)?pkg:info.get().getPackage();
        }
    }

    public class ClassInfo extends Annotatable implements Info {
        private String name;
        private final List<MethodInfo> methods = new ArrayList<MethodInfo>();
        private final List<MethodInfo> constructors = new ArrayList<MethodInfo>();
        private String superType;
        private final List<String> interfaces = new ArrayList<String>();
        private final List<FieldInfo> fields = new ArrayList<FieldInfo>();
        private Class<?> clazz;
        private ClassNotFoundException notFound;

        public ClassInfo(Class clazz) {
            super(clazz);
            this.clazz = clazz;
            this.name = clazz.getName();
            Class superclass = clazz.getSuperclass();
            this.superType = superclass != null ? superclass.getName(): null;
        }

        public ClassInfo(String name, String superType) {
            this.name = name;
            this.superType = superType;
        }

        public String getPackageName(){
        	  return name.substring(0,name.lastIndexOf("."));
        }

        public List<MethodInfo> getConstructors() {
            return constructors;
        }

        public List<String> getInterfaces() {
            return interfaces;
        }

        public List<FieldInfo> getFields() {
            return fields;
        }

        public List<MethodInfo> getMethods() {
            return methods;
        }

        public String getName() {
            return name;
        }

        public String getSuperType() {
            return superType;
        }

        public Class get() throws ClassNotFoundException {
            if (clazz != null) return clazz;
            if (notFound != null) throw notFound;
            try {
                String fixedName = name.replaceFirst("<.*>", "");
                this.clazz = loadClass(fixedName);
                return clazz;
            } catch (ClassNotFoundException notFound) {
                classesNotLoaded.add(name);
                this.notFound = notFound;
                throw notFound;
            }
        }


        public String toString() {
            return name;
        }
    }

    public class MethodInfo extends Annotatable implements Info {
        private final ClassInfo declaringClass;
        private final String returnType;
        private final String name;
        private final List<List<AnnotationInfo>> parameterAnnotations = new ArrayList<List<AnnotationInfo>>();

        public MethodInfo(ClassInfo info, Constructor constructor){
            super(constructor);
            this.declaringClass = info;
            this.name = "<init>";
            this.returnType = Void.TYPE.getName();
        }

        public MethodInfo(ClassInfo info, Method method){
            super(method);
            this.declaringClass = info;
            this.name = method.getName();
            this.returnType = method.getReturnType().getName();
        }

        public MethodInfo(ClassInfo declarignClass, String name, String returnType) {
            this.declaringClass = declarignClass;
            this.name = name;
            this.returnType = returnType;
        }

        public List<List<AnnotationInfo>> getParameterAnnotations() {
            return parameterAnnotations;
        }

        public List<AnnotationInfo> getParameterAnnotations(int index) {
            if (index >= parameterAnnotations.size()) {
                for (int i = parameterAnnotations.size(); i <= index; i++) {
                    List<AnnotationInfo> annotationInfos = new ArrayList<AnnotationInfo>();
                    parameterAnnotations.add(i, annotationInfos);
                }
            }
            return parameterAnnotations.get(index);
        }

        public String getName() {
            return name;
        }

        public ClassInfo getDeclaringClass() {
            return declaringClass;
        }

        public String getReturnType() {
            return returnType;
        }

        public String toString() {
            return declaringClass + "@" + name;
        }
    }

    public class FieldInfo extends Annotatable implements Info {
        private final String name;
        private final String type;
        private final ClassInfo declaringClass;

        public FieldInfo(ClassInfo info, Field field){
            super(field);
            this.declaringClass = info;
            this.name = field.getName();
            this.type = field.getType().getName();
        }

        public FieldInfo(ClassInfo declaringClass, String name, String type) {
            this.declaringClass = declaringClass;
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public ClassInfo getDeclaringClass() {
            return declaringClass;
        }

        public String getType() {
            return type;
        }

        public String toString() {
            return declaringClass + "#" + name;
        }
    }

    public class AnnotationInfo extends Annotatable implements Info {
        private final String name;

        public AnnotationInfo(Annotation annotation){
            this(annotation.getClass().getName());
        }

        public AnnotationInfo(Class<? extends Annotation> annotation) {
            this.name = annotation.getName().intern();
        }

        public AnnotationInfo(String name) {
            name = name.replaceAll("^L|;$", "");
            name = name.replace('/', '.');
            this.name = name.intern();
        }

        public String getName() {
            return name;
        }

        public String toString() {
            return name;
        }
    }

    public class InfoBuildingVisitor extends EmptyVisitor {
        private Info info;

        public InfoBuildingVisitor() {
        }

        public InfoBuildingVisitor(Info info) {
            this.info = info;
        }

        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            if (name.endsWith("package-info")) {
                info = new PackageInfo(javaName(name));
            } else {
                ClassInfo classInfo = new ClassInfo(javaName(name), javaName(superName));

                if (signature == null) {
                    for (String interfce : interfaces) {
                        classInfo.getInterfaces().add(javaName(interfce));
                    }
                } else {
                    // the class uses generics
                    new SignatureReader(signature).accept(new GenericAwareInfoBuildingVisitor(GenericAwareInfoBuildingVisitor.TYPE.CLASS, classInfo));
                }
                info = classInfo;
                classInfos.add(classInfo);
            }
        }

        private String javaName(String name) {
            return (name == null)? null:name.replace('/', '.');
        }

        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            AnnotationInfo annotationInfo = new AnnotationInfo(desc);
            info.getAnnotations().add(annotationInfo);
            getAnnotationInfos(annotationInfo.getName()).add(info);
            return new InfoBuildingVisitor(annotationInfo);
        }

        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            ClassInfo classInfo = ((ClassInfo) info);
            FieldInfo fieldInfo = new FieldInfo(classInfo, name, desc);
            classInfo.getFields().add(fieldInfo);
            return new InfoBuildingVisitor(fieldInfo);
        }

        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            ClassInfo classInfo = ((ClassInfo) info);
            MethodInfo methodInfo = new MethodInfo(classInfo, name, desc);
            classInfo.getMethods().add(methodInfo);
            return new InfoBuildingVisitor(methodInfo);
        }

        public AnnotationVisitor visitParameterAnnotation(int param, String desc, boolean visible) {
            MethodInfo methodInfo = ((MethodInfo) info);
            List<AnnotationInfo> annotationInfos = methodInfo.getParameterAnnotations(param);
            AnnotationInfo annotationInfo = new AnnotationInfo(desc);
            annotationInfos.add(annotationInfo);
            return new InfoBuildingVisitor(annotationInfo);
        }
    }

    public static class GenericAwareInfoBuildingVisitor implements SignatureVisitor {

        public enum TYPE {
            CLASS
        }

        public enum STATE {
            BEGIN, END, SUPERCLASS, INTERFACE, FORMAL_TYPE_PARAM
        }

        private Info info;
        private GenericAwareInfoBuildingVisitor.TYPE type;
        private GenericAwareInfoBuildingVisitor.STATE state;

        private static boolean debug = false;

        public GenericAwareInfoBuildingVisitor() {
        }

        public GenericAwareInfoBuildingVisitor(GenericAwareInfoBuildingVisitor.TYPE type, Info info) {
            this.type = type;
            this.info = info;
            this.state = GenericAwareInfoBuildingVisitor.STATE.BEGIN;
        }

        public void visitFormalTypeParameter(String s) {
            if (debug) System.out.println(" s=" + s);
            switch (state) {
                case BEGIN:
                    ((ClassInfo) info).name += "<" + s;
            }
            state = GenericAwareInfoBuildingVisitor.STATE.FORMAL_TYPE_PARAM;
        }

        public SignatureVisitor visitClassBound() {
            if (debug) System.out.println(" visitClassBound()");
            return this;
        }

        public SignatureVisitor visitInterfaceBound() {
            if (debug) System.out.println(" visitInterfaceBound()");
            return this;
        }

        public SignatureVisitor visitSuperclass() {
            if (debug) System.out.println(" visitSuperclass()");
            state = GenericAwareInfoBuildingVisitor.STATE.SUPERCLASS;
            return this;
        }

        public SignatureVisitor visitInterface() {
            if (debug) System.out.println(" visitInterface()");
            ((ClassInfo) info).getInterfaces().add("");
            state = GenericAwareInfoBuildingVisitor.STATE.INTERFACE;
            return this;
        }

        public SignatureVisitor visitParameterType() {
            if (debug) System.out.println(" visitParameterType()");
            return this;
        }

        public SignatureVisitor visitReturnType() {
            if (debug) System.out.println(" visitReturnType()");
            return this;
        }

        public SignatureVisitor visitExceptionType() {
            if (debug) System.out.println(" visitExceptionType()");
            return this;
        }

        public void visitBaseType(char c) {
            if (debug) System.out.println(" visitBaseType(" + c + ")");
        }

        public void visitTypeVariable(String s) {
            if (debug) System.out.println(" visitTypeVariable(" + s + ")");
        }

        public SignatureVisitor visitArrayType() {
            if (debug) System.out.println(" visitArrayType()");
            return this;
        }

        public void visitClassType(String s) {
            if (debug) System.out.println(" visitClassType(" + s + ")");
            switch (state) {
                case INTERFACE:
                    List<String> interfces = ((ClassInfo) info).getInterfaces();
                    int idx = interfces.size() - 1;
                    String interfce = interfces.get(idx);
                    if (interfce.length() == 0) {
                        interfce = javaName(s);
                    } else {
                        interfce += javaName(s);
                    }
                    interfces.set(idx, interfce);
                    break;
                case SUPERCLASS:
                    if (!s.equals("java/lang/Object")) {
                        ((ClassInfo) info).superType = javaName(s);
                    }
            }
        }

        public void visitInnerClassType(String s) {
            if (debug) System.out.println(" visitInnerClassType(" + s + ")");
        }

        public void visitTypeArgument() {
            if (debug) System.out.println(" visitTypeArgument()");
            switch (state) {
                case INTERFACE:
                    List<String> interfces = ((ClassInfo) info).getInterfaces();
                    int idx = interfces.size() - 1;
                    String interfce = interfces.get(idx);
                    interfce += "<";
                    interfces.set(idx, interfce);
            }
        }

        public SignatureVisitor visitTypeArgument(char c) {
            if (debug) System.out.println(" visitTypeArgument(" + c + ")");
            switch (state) {
                case INTERFACE:
                    List<String> interfces = ((ClassInfo) info).getInterfaces();
                    int idx = interfces.size() - 1;
                    String interfce = interfces.get(idx);
                    interfce += "<";
                    interfces.set(idx, interfce);
            }
            return this;
        }

        public void visitEnd() {
            if (debug) System.out.println(" visitEnd()");
            switch (state) {
                case INTERFACE:
                    List<String> interfces = ((ClassInfo) info).getInterfaces();
                    int idx = interfces.size() - 1;
                    String interfce = interfces.get(idx);
                    interfce += ">";
                    interfces.set(idx, interfce);
                    break;
                case FORMAL_TYPE_PARAM:
                    String name = ((ClassInfo) info).name;
                    if (name.contains("<")) {
                        ((ClassInfo) info).name += ">";
                    }
            }
            state = GenericAwareInfoBuildingVisitor.STATE.END;
        }

        private String javaName(String name) {
            return (name == null)? null:name.replace('/', '.');
        }

    }
}
