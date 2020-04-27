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

import org.apache.xbean.asm8.original.commons.EmptyVisitor;
import org.apache.xbean.finder.archive.Archive;
import org.apache.xbean.finder.util.Classes;
import org.apache.xbean.finder.util.SingleLinkedList;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ClassFinder searches the classpath of the specified classloader for
 * packages, classes, constructors, methods, or fields with specific annotations.
 * <p/>
 * For security reasons ASM is used to find the annotations.  Classes are not
 * loaded unless they match the requirements of a called findAnnotated* method.
 * Once loaded, these classes are cached.
 *
 * @version $Rev$ $Date$
 */
public class AnnotationFinder implements IAnnotationFinder {
    private static final int ASM_FLAGS = ClassReader.SKIP_CODE + ClassReader.SKIP_DEBUG + ClassReader.SKIP_FRAMES;

    // this flag is just a backdoor to allow workaround in case we impact an application, if we aresafe for 2-3 versions
    // let remove it
    //
    // main issue which can happen is a parent class we dont want to scan appears,
    // xbean.finder.prevent-lazy-linking= true will prevent it, see readClassDef(Class)
    private static final boolean ALLOW_LAZY_LINKING = !Boolean.getBoolean("xbean.finder.prevent-lazy-linking");

    private final Set<Class<? extends Annotation>> metaroots = new HashSet<Class<? extends Annotation>>();

    protected final Map<String, List<Info>> annotated = newAnnotatedMap();

    protected final Map<String, ClassInfo> classInfos = newClassInfoMap();
    protected final Map<String, ClassInfo> originalInfos = newClassInfoMap();
    private final List<String> classesNotLoaded = new LinkedList<String>();
    private final Archive archive;
    private final boolean checkRuntimeAnnotation;
    private volatile boolean linking;

    private AnnotationFinder(AnnotationFinder parent, Iterable<String> classNames) {
        this.archive = new SubArchive(classNames);
        this.checkRuntimeAnnotation = parent.checkRuntimeAnnotation;
        this.metaroots.addAll(parent.metaroots);

        for (Class<? extends Annotation> metaroot : metaroots) {
            final ClassInfo info = parent.classInfos.get(metaroot.getName());
            if (info == null) continue;
            readClassDef(info);
        }
        for (String name : classNames) {
            final ClassInfo info = parent.classInfos.get(name);
            if (info == null) continue;
            readClassDef(info);
        }

        resolveAnnotations(parent, new LinkedList<String>());
        for (ClassInfo classInfo : classInfos.values()) {
            if (isMetaRoot(classInfo)) {
                try {
                    metaroots.add((Class<? extends Annotation>) classInfo.get());
                } catch (ClassNotFoundException e) {
                    classesNotLoaded.add(classInfo.getName());
                }
            }
        }

        for (Class<? extends Annotation> metaroot : metaroots) {
            List<Info> infoList = annotated.get(metaroot.getName());
            for (Info info : infoList) {
                final String className = info.getName() + "$$";
                final ClassInfo i = parent.classInfos.get(className);
                if (i == null) continue;
                readClassDef(i);
            }
        }
    }

    protected Map<String, List<Info>> newAnnotatedMap() {
        return new HashMap<String, List<Info>>();
    }

    protected Map<String, ClassInfo> newClassInfoMap() {
        return new HashMap<String, ClassInfo>();
    }

    protected boolean cleanOnNaked() {
        return false;
    }

    protected boolean isTracked(final String annotationType) {
        return true;
    }

    /**
     *
     * @param archive
     * @param checkRuntimeAnnotation Has no effect on findMetaAnnotated* methods
     */
    public AnnotationFinder(Archive archive, boolean checkRuntimeAnnotation) {
        this.archive = archive;
        this.checkRuntimeAnnotation = checkRuntimeAnnotation;

        for (Archive.Entry entry : archive) {
            final String className = entry.getName();
            try {
                readClassDef(entry.getName(), entry.getBytecode());
            } catch (NoClassDefFoundError e) {
                throw new NoClassDefFoundError("Could not fully load class: " + className + "\n due to:" + e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // keep track of what was originally from the archives
        originalInfos.putAll(classInfos);
    }

    public AnnotationFinder(Archive archive) {
        this(archive, true);
    }

    public boolean hasMetaAnnotations() {
        return metaroots.size() > 0;
    }

    private void readClassDef(ClassInfo info) {
        classInfos.put(info.getName(), info);
        index(info);
        index(info.constructors);
        for (MethodInfo ctor : info.constructors) {
            index(ctor.parameters);
        }
        index(info.methods);
        for (MethodInfo method : info.methods) {
            index(method.parameters);
        }
        index(info.fields);
    }

    private void resolveAnnotations(AnnotationFinder parent, List<String> scanned) {
        // Get a list of the annotations that exist before we start
        final List<String> annotations = new ArrayList<String>(annotated.keySet());

        for (String annotation : annotations) {
            if (scanned.contains(annotation)) continue;
            final ClassInfo info = parent.classInfos.get(annotation);
            if (info == null) continue;
            readClassDef(info);
        }

        // If the "annotated" list has grown, then we must scan those
        if (annotated.keySet().size() != annotations.size()) {
            resolveAnnotations(parent, annotations);
        }
    }


    private void index(List<? extends Info> infos) {
        for (Info i : infos) {
            index(i);
        }
    }

    private void index(Info i) {
        for (AnnotationInfo annotationInfo : i.getAnnotations()) {
            index(annotationInfo, i);
        }
    }

    public List<String> getAnnotatedClassNames() {
        return new ArrayList<String>(originalInfos.keySet());
    }

    public Archive getArchive() {
        return archive;
    }

    /**
     * The link() method must be called to successfully use the findSubclasses and findImplementations methods
     *
     * @return
     * @throws java.io.IOException
     */
    public AnnotationFinder link() {

        enableFindSubclasses();

        enableFindImplementations();

        enableMetaAnnotations();

        return this;
    }

    public AnnotationFinder enableMetaAnnotations() {
        // diff new and old lists
        resolveAnnotations(new LinkedList<String>());

        linkMetaAnnotations();

        return this;
    }

    public AnnotationFinder enableFindImplementations() {
        for (ClassInfo classInfo : classInfos.values().toArray(new ClassInfo[classInfos.size()])) {

            linkInterfaces(classInfo);

        }
        return this;
    }

    public AnnotationFinder enableFindSubclasses() {
        final boolean originalLinking = linking;
        linking = ALLOW_LAZY_LINKING;
        for (ClassInfo classInfo : classInfos.values().toArray(new ClassInfo[classInfos.size()])) {

            linkParent(classInfo);
        }
        linking = originalLinking;
        return this;
    }

    /**
     * Used to support meta annotations
     * <p/>
     * Once the list of classes has been read from the Archive, we
     * iterate over all the annotations that are used by those classes
     * and recursively resolve any annotations those annotations use.
     *
     * @param scanned
     * @throws ClassNotFoundException
     * @throws IOException
     */
    private void resolveAnnotations(List<String> scanned) {
        // Get a list of the annotations that exist before we start
        final List<String> annotations = new ArrayList<String>(annotated.keySet());

        for (String annotation : annotations) {
            if (scanned.contains(annotation)) continue;
            readClassDef(annotation);
        }

        // If the "annotated" list has grown, then we must scan those
        if (annotated.keySet().size() != annotations.size()) {
            resolveAnnotations(annotations);
        }


//        for (ClassInfo classInfo : classInfos.values()) {
//            for (AnnotationInfo annotationInfo : classInfo.getAnnotations()) {
//                for (AnnotationInfo info : annotationInfo.getAnnotations()) {
//                    final String annotation = info.getName();
//
//                    if (hasName(annotation, "Metaroot") && !scanned.contains(annotation)) {
//                        readClassDef(annotation);
//                    }
//                }
//            }
//        }
    }

    private void linkMetaAnnotations() {
        for (ClassInfo classInfo : classInfos.values().toArray(new ClassInfo[classInfos.size()])) {
            if (isMetaRoot(classInfo)) {
                try {
                    metaroots.add((Class<? extends Annotation>) classInfo.get());
                } catch (ClassNotFoundException e) {
                    classesNotLoaded.add(classInfo.getName());
                }
            }
        }

        for (Class<? extends Annotation> metaroot : metaroots) {
            List<Info> infoList = annotated.get(metaroot.getName());
            for (Info info : infoList) {
                readClassDef(info.getName() + "$$");
            }
        }
    }

    private boolean isMetaRoot(ClassInfo classInfo) {
        if (!classInfo.isAnnotation()) return false;

        if (classInfo.getName().equals("javax.annotation.Metatype")) return true;
        if (isSelfAnnotated(classInfo, "Metatype")) return true;
        if (isSelfAnnotated(classInfo, "Metaroot")) return false;

        for (AnnotationInfo annotationInfo : classInfo.getAnnotations()) {
            final ClassInfo annotation = classInfos.get(annotationInfo.getName());
            if (annotation == null) return false;
            if (annotation.getName().equals("javax.annotation.Metaroot")) return true;
            if (isSelfAnnotated(annotation, "Metaroot")) return true;
        }

        return false;
    }

    private boolean isSelfAnnotated(ClassInfo classInfo, String metatype) {
        if (!classInfo.isAnnotation()) return false;

        final String name = classInfo.getName();
        if (!hasName(name, metatype)) return false;

        for (AnnotationInfo info : classInfo.getAnnotations()) {
            if (info.getName().equals(name)) return true;
        }

        return true;
    }

    private boolean hasName(String className, String simpleName) {
        return className.equals(simpleName) || className.endsWith("." + simpleName) || className.endsWith("$" + simpleName);
    }

    protected void linkParent(ClassInfo classInfo) {
        if (classInfo.superType == null) return;
        if (isJvm(classInfo.superType)) return;

        ClassInfo parentInfo = classInfo.superclassInfo;

        if (parentInfo == null) {

            parentInfo = classInfos.get(classInfo.superType);

            if (parentInfo == null) {
                // best scanning we can do, try it first
                readClassDef(classInfo.superType);

                parentInfo = classInfos.get(classInfo.superType);

                if (parentInfo == null) {
                    // parentInfo == null means readClassDef fails so clean up error and retry
                    classesNotLoaded.remove(classInfo.superType);

                    try {
                        if (classInfo.get() != null) { // call get() to ensure clazz got a change to be loaded
                            readClassDef(((Class<?>) classInfo.clazz).getSuperclass());
                            parentInfo = classInfos.get(classInfo.superType);
                        }
                    } catch (final ClassNotFoundException e) {
                        // no-op
                    } catch (final Throwable e) {
                        // no-op
                    }
                }

                if (parentInfo == null) return;

                linkParent(parentInfo);
            }

            classInfo.superclassInfo = parentInfo;
        }

        synchronized (parentInfo.subclassInfos) {
            if (!parentInfo.subclassInfos.contains(classInfo)) {
                parentInfo.subclassInfos.add(classInfo);
            }
        }
    }

    /*
    protected boolean isJvm(final String superType) {
        // TODO: can't we simply do startsWith("java")?
        return superType.startsWith("java.lang.")
                || superType.startsWith("java.beans.")
                || superType.startsWith("java.util.")
                || superType.startsWith("java.io.")
                || superType.startsWith("java.text.")
                || superType.startsWith("java.net.")
                || superType.startsWith("java.sql.")
                || superType.startsWith("java.security.")
                || superType.startsWith("java.awt.")
                || superType.startsWith("javax.swing.");
    }
    */

    protected boolean isJvm(final String name) {
        return name.startsWith("java.");
    }

    protected void linkInterfaces(ClassInfo classInfo) {
        final List<ClassInfo> infos = new LinkedList<ClassInfo>();

        if (classInfo.clazz != null) {
            final Class<?>[] interfaces = classInfo.clazz.getInterfaces();

            for (Class<?> clazz : interfaces) {
                ClassInfo interfaceInfo = classInfos.get(clazz.getName());

                if (interfaceInfo == null) {
                    readClassDef(clazz);
                }

                interfaceInfo = classInfos.get(clazz.getName());

                if (interfaceInfo != null) {
                    infos.add(interfaceInfo);
                }
            }
        } else {
            for (final String className : classInfo.interfaces) {
                if (isJvm(className)) {
                    continue;
                }
                ClassInfo interfaceInfo = classInfos.get(className);

                if (interfaceInfo == null) {
                    readClassDef(className);
                }

                interfaceInfo = classInfos.get(className);

                if (interfaceInfo != null) {
                    infos.add(interfaceInfo);
                }
            }
        }

        for (ClassInfo info : infos) {
            linkInterfaces(info);
        }
    }

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
     *
     * @return an unmodifiable live view of classes that could not be loaded in previous findAnnotated* call.
     */
    public List<String> getClassesNotLoaded() {
        return Collections.unmodifiableList(classesNotLoaded);
    }

    public List<Package> findAnnotatedPackages(Class<? extends Annotation> annotation) {
        classesNotLoaded.clear();
        List<Package> packages = new LinkedList<Package>();
        List<Info> infos = getAnnotationInfos(annotation.getName());
        for (Info info : infos) {
            if (info instanceof PackageInfo) {
                PackageInfo packageInfo = (PackageInfo) info;
                try {
                    Package pkg = packageInfo.get();
                    // double check via proper reflection
                    if (!checkRuntimeAnnotation || pkg.isAnnotationPresent(annotation)) {
                        packages.add(pkg);
                    }
                } catch (ClassNotFoundException e) {
                    classesNotLoaded.add(packageInfo.getName());
                }
            }
        }
        return packages;
    }

    public List<Class<?>> findAnnotatedClasses(Class<? extends Annotation> annotation) {
        classesNotLoaded.clear();
        List<Class<?>> classes = new LinkedList<Class<?>>();
        List<Info> infos = getAnnotationInfos(annotation.getName());
        for (Info info : infos) {
            if (info instanceof ClassInfo) {
                ClassInfo classInfo = (ClassInfo) info;
                try {
                    Class clazz = classInfo.get();
                    // double check via proper reflection
                    if (!checkRuntimeAnnotation || clazz.isAnnotationPresent(annotation)) {
                        classes.add(clazz);
                    }
                } catch (ClassNotFoundException e) {
                    classesNotLoaded.add(classInfo.getName());
                }
            }
        }
        return classes;
    }

    public List<Annotated<Class<?>>> findMetaAnnotatedClasses(Class<? extends Annotation> annotation) {
        classesNotLoaded.clear();
        Set<Class<?>> classes = findMetaAnnotatedClasses(annotation, new HashSet<Class<?>>());

        List<Annotated<Class<?>>> list = new LinkedList<Annotated<Class<?>>>();

        for (Class<?> clazz : classes) {
            if (Annotation.class.isAssignableFrom(clazz) && isMetaAnnotation((Class<? extends Annotation>) clazz)) continue;
            list.add(new MetaAnnotatedClass(clazz));
        }

        return list;
    }

    private static boolean isMetaAnnotation(Class<? extends Annotation> clazz) {
        for (Annotation annotation : clazz.getDeclaredAnnotations()) {
            if (isMetatypeAnnotation(annotation.annotationType())) return true;
        }

        return false;
    }

    private static boolean isMetatypeAnnotation(Class<? extends Annotation> type) {
        if (isSelfAnnotated(type, "Metatype")) return true;

        for (Annotation annotation : type.getAnnotations()) {
            if (isSelfAnnotated(annotation.annotationType(), "Metaroot")) return true;
        }

        return false;
    }

    private static boolean isSelfAnnotated(Class<? extends Annotation> type, String name) {
        return type.isAnnotationPresent(type) && type.getSimpleName().equals(name) && validTarget(type);
    }

    private static boolean validTarget(Class<? extends Annotation> type) {
        final Target target = type.getAnnotation(Target.class);

        if (target == null) return false;

        final ElementType[] targets = target.value();

        return targets.length == 1 && targets[0] == ElementType.ANNOTATION_TYPE;
    }


    private Set<Class<?>> findMetaAnnotatedClasses(Class<? extends Annotation> annotation, Set<Class<?>> classes) {
        List<Info> infos = getAnnotationInfos(annotation.getName());
        for (Info info : infos) {
            if (info instanceof ClassInfo) {
                ClassInfo classInfo = (ClassInfo) info;
                try {
                    Class clazz = classInfo.get();

                    if (classes.contains(clazz)) continue;

                    // double check via proper reflection
                    if (clazz.isAnnotationPresent(annotation)) {
                        classes.add(clazz);
                    }

                    String meta = info.getMetaAnnotationName();
                    if (meta != null) {
                        classes.addAll(findMetaAnnotatedClasses((Class<? extends Annotation>) clazz, classes));
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
    public List<Class<?>> findInheritedAnnotatedClasses(Class<? extends Annotation> annotation) {
        classesNotLoaded.clear();
        List<Class<?>> classes = new LinkedList<Class<?>>();
        List<Info> infos = getAnnotationInfos(annotation.getName());
        for (Info info : infos) {
            try {
                if (info instanceof ClassInfo) {
                    classes.add(((ClassInfo) info).get());
                }
            } catch (ClassNotFoundException cnfe) {
                // TODO: ignored, but a log message would be appropriate
            }
        }
        boolean annClassFound;
        List<ClassInfo> tempClassInfos = new ArrayList<ClassInfo>(classInfos.values());
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
                    for (String interfce : interfces) {
                        for (Class clazz : classes) {
                            if (interfce.replaceFirst("<.*>", "").equals(clazz.getName())) {
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
        List<ClassInfo> seen = new LinkedList<ClassInfo>();
        List<Method> methods = new LinkedList<Method>();
        List<Info> infos = getAnnotationInfos(annotation.getName());
        for (Info info : infos) {
            if (info instanceof MethodInfo && !info.getName().equals("<init>")) {
                final MethodInfo methodInfo = (MethodInfo) info;

                if (checkRuntimeAnnotation) {
                    final ClassInfo classInfo = methodInfo.getDeclaringClass();

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
                    } catch (ClassCircularityError cce) {
                        classesNotLoaded.add(classInfo.getName());
                    }
                } else {
                    try {
                        final Method method = (Method) methodInfo.get();
                        methods.add(method);
                    } catch (ClassNotFoundException e) {
                        classesNotLoaded.add(methodInfo.getDeclaringClass().getName());
                    }
                }
            }
        }
        return methods;
    }

    public List<Parameter<Method>> findAnnotatedMethodParameters(Class<? extends Annotation> annotation) {
        classesNotLoaded.clear();

        final Set<ClassInfo> seen = checkRuntimeAnnotation ? new HashSet<ClassInfo>() : null;
        final List<Parameter<Method>> result = new LinkedList<Parameter<Method>>();
        for (Info info : getAnnotationInfos(annotation.getName())) {
            if (!(info instanceof ParameterInfo)) {
                continue;
            }
            final ParameterInfo parameterInfo = (ParameterInfo) info;
            if ("<init>".equals(parameterInfo.getDeclaringMethod().getName())) {
                continue;
            }
            final ClassInfo classInfo = parameterInfo.getDeclaringMethod().getDeclaringClass();

            if (checkRuntimeAnnotation) {
                if (!seen.add(classInfo)) {
                    continue;
                }
                try {
                    Class<?> clazz = classInfo.get();
                    for (Method method : clazz.getDeclaredMethods()) {
                        for (Annotation[] annotations : method.getParameterAnnotations()) {
                            for (int i = 0; i < annotations.length; i++) {
                                if (annotations[i].annotationType().equals(annotation)) {
                                    result.add(Parameter.declaredBy(method, i));
                                }
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    classesNotLoaded.add(classInfo.getName());
                }
            } else {
                try {
                    @SuppressWarnings("unchecked")
                    final Parameter<Method> parameter = (Parameter<Method>) parameterInfo.get();
                    result.add(parameter);
                } catch (ClassNotFoundException e) {
                    classesNotLoaded.add(parameterInfo.getDeclaringMethod().getDeclaringClass().getName());
                }
            }
        }
        return result;
    }

    public List<Annotated<Method>> findMetaAnnotatedMethods(Class<? extends Annotation> annotation) {
        classesNotLoaded.clear();

        Set<Method> methods = findMetaAnnotatedMethods(annotation, new HashSet<Method>(), new HashSet<String>());

        List<Annotated<Method>> targets = new LinkedList<Annotated<Method>>();

        for (Method method : methods) {
            targets.add(new MetaAnnotatedMethod(method));
        }

        return targets;
    }

    private Set<Method> findMetaAnnotatedMethods(Class<? extends Annotation> annotation, Set<Method> methods, Set<String> seen) {
        List<Info> infos = getAnnotationInfos(annotation.getName());

        for (Info info : infos) {

            String meta = info.getMetaAnnotationName();
            if (meta != null) {
                if (meta.equals(annotation.getName())) continue;
                if (!seen.add(meta)) continue;


                ClassInfo metaInfo = classInfos.get(meta);

                Class<?> clazz;
                try {
                    clazz = metaInfo.get();
                } catch (ClassNotFoundException e) {
                    classesNotLoaded.add(metaInfo.getName());
                    continue;
                }

                findMetaAnnotatedMethods((Class<? extends Annotation>) clazz, methods, seen);

            } else if (info instanceof MethodInfo && !((MethodInfo) info).isConstructor()) {

                MethodInfo methodInfo = (MethodInfo) info;

                ClassInfo classInfo = methodInfo.getDeclaringClass();

                try {
                    Class clazz = classInfo.get();
                    for (Method method : clazz.getDeclaredMethods()) {
                        if (method.isAnnotationPresent(annotation)) {
                            methods.add(method);
                        }
                    }
                } catch (ClassNotFoundException e) {
                    classesNotLoaded.add(classInfo.getName());
                } catch (NoClassDefFoundError ncdfe) {
                    classesNotLoaded.add(classInfo.getName());
                }
            }
        }

        return methods;
    }

    public List<Annotated<Field>> findMetaAnnotatedFields(Class<? extends Annotation> annotation) {
        classesNotLoaded.clear();

        Set<Field> fields = findMetaAnnotatedFields(annotation, new HashSet<Field>(), new HashSet<String>());

        List<Annotated<Field>> targets = new LinkedList<Annotated<Field>>();

        for (Field field : fields) {
            targets.add(new MetaAnnotatedField(field));
        }

        return targets;
    }

    private Set<Field> findMetaAnnotatedFields(Class<? extends Annotation> annotation, Set<Field> fields, Set<String> seen) {
        List<Info> infos = getAnnotationInfos(annotation.getName());

        for (Info info : infos) {

            String meta = info.getMetaAnnotationName();
            if (meta != null) {
                if (meta.equals(annotation.getName())) continue;
                if (!seen.add(meta)) continue;


                ClassInfo metaInfo = classInfos.get(meta);

                Class<?> clazz;
                try {
                    clazz = metaInfo.get();
                } catch (ClassNotFoundException e) {
                    classesNotLoaded.add(metaInfo.getName());
                    continue;
                }

                findMetaAnnotatedFields((Class<? extends Annotation>) clazz, fields, seen);

            } else if (info instanceof FieldInfo) {

                FieldInfo fieldInfo = (FieldInfo) info;

                ClassInfo classInfo = fieldInfo.getDeclaringClass();

                try {
                    Class clazz = classInfo.get();
                    for (Field field : clazz.getDeclaredFields()) {
                        if (field.isAnnotationPresent(annotation)) {
                            fields.add(field);
                        }
                    }
                } catch (ClassNotFoundException e) {
                    classesNotLoaded.add(classInfo.getName());
                } catch (NoClassDefFoundError ncdfe) {
                    classesNotLoaded.add(classInfo.getName());
                }
            }
        }

        return fields;
    }

    public List<Constructor> findAnnotatedConstructors(Class<? extends Annotation> annotation) {
        classesNotLoaded.clear();
        List<ClassInfo> seen = new LinkedList<ClassInfo>();
        List<Constructor> constructors = new LinkedList<Constructor>();
        List<Info> infos = getAnnotationInfos(annotation.getName());
        for (Info info : infos) {
            if (info instanceof MethodInfo && info.getName().equals("<init>")) {
                MethodInfo methodInfo = (MethodInfo) info;

                if (checkRuntimeAnnotation) {
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
                    } catch (NoClassDefFoundError ncdfe) {
                        classesNotLoaded.add(classInfo.getName());
                    }
                } else {
                    try {
                        constructors.add((Constructor) methodInfo.get());
                    } catch (ClassNotFoundException e) {
                        classesNotLoaded.add(methodInfo.getDeclaringClass().getName());
                    }
                }
            }
        }
        return constructors;
    }

    public List<Parameter<Constructor<?>>> findAnnotatedConstructorParameters(Class<? extends Annotation> annotation) {
        classesNotLoaded.clear();

        final Set<ClassInfo> seen = checkRuntimeAnnotation ? new HashSet<ClassInfo>() : null;
        final List<Parameter<Constructor<?>>> result = new LinkedList<Parameter<Constructor<?>>>();
        for (Info info : getAnnotationInfos(annotation.getName())) {
            if (!(info instanceof ParameterInfo)) {
                continue;
            }
            final ParameterInfo parameterInfo = (ParameterInfo) info;
            if (!"<init>".equals(parameterInfo.getDeclaringMethod().getName())) {
                continue;
            }
            final ClassInfo classInfo = parameterInfo.getDeclaringMethod().getDeclaringClass();

            if (checkRuntimeAnnotation) {
                if (!seen.add(classInfo)) {
                    continue;
                }
                try {
                    Class<?> clazz = classInfo.get();
                    for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
                        for (Annotation[] annotations : ctor.getParameterAnnotations()) {
                            for (int i = 0; i < annotations.length; i++) {
                                if (annotations[i].annotationType().equals(annotation)) {
                                    @SuppressWarnings({ "rawtypes", "unchecked" })
                                    final Parameter<Constructor<?>> parameter = Parameter.declaredBy((Constructor) ctor, i);
                                    result.add(parameter);
                                }
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    classesNotLoaded.add(classInfo.getName());
                }
            } else {
                try {
                    @SuppressWarnings("unchecked")
                    final Parameter<Constructor<?>> parameter = (Parameter<Constructor<?>>) parameterInfo.get();
                    result.add(parameter);
                } catch (ClassNotFoundException e) {
                    classesNotLoaded.add(parameterInfo.getDeclaringMethod().getDeclaringClass().getName());
                }
            }
        }
        return result;
    }

    public List<Field> findAnnotatedFields(Class<? extends Annotation> annotation) {
        classesNotLoaded.clear();
        List<ClassInfo> seen = new LinkedList<ClassInfo>();
        List<Field> fields = new LinkedList<Field>();
        List<Info> infos = getAnnotationInfos(annotation.getName());
        for (Info info : infos) {
            if (info instanceof FieldInfo) {
                FieldInfo fieldInfo = (FieldInfo) info;

                if (checkRuntimeAnnotation) {
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
                    } catch (NoClassDefFoundError ncdfe) {
                        classesNotLoaded.add(classInfo.getName());
                    }
                } else {
                    try {
                        fields.add((Field) fieldInfo.get());
                    } catch (ClassNotFoundException e) {
                        classesNotLoaded.add(fieldInfo.getDeclaringClass().getName());
                    }
                }
            }
        }
        return fields;
    }

    public List<Class<?>> findClassesInPackage(String packageName, boolean recursive) {
        classesNotLoaded.clear();
        List<Class<?>> classes = new LinkedList<Class<?>>();
        for (ClassInfo classInfo : classInfos.values()) {
            try {
                if (recursive && classInfo.getPackageName().startsWith(packageName)) {
                    classes.add(classInfo.get());
                } else if (classInfo.getPackageName().equals(packageName)) {
                    classes.add(classInfo.get());
                }
            } catch (ClassNotFoundException e) {
                classesNotLoaded.add(classInfo.getName());
            }
        }
        return classes;
    }

    public <T> List<Class<? extends T>> findSubclasses(Class<T> clazz) {
        if (clazz == null) throw new NullPointerException("class cannot be null");

        classesNotLoaded.clear();

        final ClassInfo classInfo = classInfos.get(clazz.getName());

        List<Class<? extends T>> found = new LinkedList<Class<? extends T>>();

        if (classInfo == null) return found;

        findSubclasses(classInfo, found, clazz);

        return found;
    }

    private <T> void findSubclasses(ClassInfo classInfo, List<Class<? extends T>> found, Class<T> clazz) {

        for (ClassInfo subclassInfo : classInfo.subclassInfos) {

            try {
                found.add(subclassInfo.get().asSubclass(clazz));
            } catch (ClassNotFoundException e) {
                classesNotLoaded.add(subclassInfo.getName());
            }

            findSubclasses(subclassInfo, found, clazz);
        }
    }

    private <T> List<Class<? extends T>> _findSubclasses(Class<T> clazz) {
        if (clazz == null) throw new NullPointerException("class cannot be null");

        List<Class<? extends T>> classes = new LinkedList<Class<? extends T>>();


        for (ClassInfo classInfo : classInfos.values()) {

            try {

                final String name = clazz.getName();
                if (name.equals(classInfo.superType)) {

                    if (clazz.isAssignableFrom(classInfo.get())) {
                        final Class<? extends T> asSubclass = classInfo.get().asSubclass(clazz);
                        classes.add(asSubclass);
                        classes.addAll(_findSubclasses(asSubclass));
                    }
                }

            } catch (ClassNotFoundException e) {
                classesNotLoaded.add(classInfo.getName());
            }

        }

        return classes;
    }

    public <T> List<Class<? extends T>> findImplementations(Class<T> clazz) {
        if (clazz == null) throw new NullPointerException("class cannot be null");
        if (!clazz.isInterface()) new IllegalArgumentException("class must be an interface");
        classesNotLoaded.clear();

        final String interfaceName = clazz.getName();

        // Collect all interfaces extending the main interface (recursively)
        // Collect all implementations of interfaces
        // i.e. all *directly* implementing classes
        final List<ClassInfo> infos = collectImplementations(interfaceName);

        // Collect all subclasses of implementations
        final List<Class<? extends T>> classes = new LinkedList<Class<? extends T>>();
        for (ClassInfo info : infos) {
            try {
                final Class<? extends T> impl = (Class<? extends T>) info.get();

                if (!classes.contains(impl) && clazz.isAssignableFrom(impl)) {
                    classes.add(impl);

                    // Optimization: Don't need to call this method if parent class was already searched


                    final List<Class<? extends T>> c = _findSubclasses((Class<T>) impl);
                    for (final Class<? extends T> cl : c) {
                        if (!classes.contains(cl)) {
                            classes.add(cl);
                        }
                    }
                }

            } catch (final ClassNotFoundException e) {
                classesNotLoaded.add(info.getName());
            }
        }
        return classes;
    }

    private List<ClassInfo> collectImplementations(String interfaceName) {
        final List<ClassInfo> infos = new LinkedList<ClassInfo>();

        for (ClassInfo classInfo : classInfos.values()) {

            if (classInfo.interfaces.contains(interfaceName)) {

                infos.add(classInfo);

                try {

                    final Class clazz = classInfo.get();

                    if (clazz.isInterface() && !clazz.isAnnotation()) {

                        infos.addAll(collectImplementations(classInfo.name));

                    }

                } catch (ClassNotFoundException ignore) {
                    // we'll deal with this later
                }
            }
        }
        return infos;
    }

    protected List<Info> getAnnotationInfos(String name) {
        final List<Info> infos = annotated.get(name);
        if (infos != null) return infos;
        return Collections.EMPTY_LIST;
    }

    protected List<Info> initAnnotationInfos(String name) {
        List<Info> infos = annotated.get(name);
        if (infos == null) {
            infos = new SingleLinkedList<Info>();
            annotated.put(name, infos);
        }
        return infos;
    }

    protected void readClassDef(final String className) {
        if (classInfos.containsKey(className)) return;
        try {
            readClassDef(className, archive.getBytecode(className));

        } catch (Exception e) {
            if (className.endsWith("$$")) return;
            classesNotLoaded.add(className);
        }
    }

    protected void readClassDef(final String className, InputStream in) throws IOException {
        try {
            ClassReader classReader = new ClassReader(in);
            classReader.accept(new InfoBuildingVisitor(), ASM_FLAGS);

        } catch (final Exception e) {
            throw new RuntimeException("Unable to read class definition for " + className, e);

        } finally {
            in.close();
        }
    }

    protected void readClassDef(Class clazz) {
        List<Info> infos = new LinkedList<Info>();

        Package aPackage = clazz.getPackage();
        if (aPackage != null) {
            final PackageInfo info = new PackageInfo(aPackage);
            for (AnnotationInfo annotation : info.getAnnotations()) {
                List<Info> annotationInfos = initAnnotationInfos(annotation.getName());
                if (!annotationInfos.contains(info)) {
                    annotationInfos.add(info);
                }
            }
        }

        ClassInfo classInfo = new ClassInfo(clazz);
        infos.add(classInfo);
        for (Method method : clazz.getDeclaredMethods()) {
            MethodInfo methodInfo = new MethodInfo(classInfo, method);
            if (linking) {
                classInfo.methods.add(methodInfo);
            }
            infos.add(methodInfo);
            for (Annotation[] annotations : method.getParameterAnnotations()) {
                for (int i = 0; i < annotations.length; i++) {
                    infos.add(new ParameterInfo(methodInfo, i));
                }
            }
        }

        for (Constructor<?> constructor : clazz.getConstructors()) {
            MethodInfo methodInfo = new MethodInfo(classInfo, constructor);
            if (linking) {
                classInfo.methods.add(methodInfo);
            }
            infos.add(methodInfo);
            for (Annotation[] annotations : constructor.getParameterAnnotations()) {
                for (int i = 0; i < annotations.length; i++) {
                    infos.add(new ParameterInfo(methodInfo, i));
                }
            }
        }

        for (Field field : clazz.getDeclaredFields()) {
            final FieldInfo fieldInfo = new FieldInfo(classInfo, field);
            if (linking) {
                classInfo.fields.add(fieldInfo);
            }
            infos.add(fieldInfo);
        }

        for (Info info : infos) {
            for (AnnotationInfo annotation : info.getAnnotations()) {
                List<Info> annotationInfos = initAnnotationInfos(annotation.getName());
                annotationInfos.add(info);
            }
        }

        if (linking) {
            classInfos.put(classInfo.name, classInfo);
        }
    }

    public AnnotationFinder select(Class<?>... clazz) {
        String[] names = new String[clazz.length];
        int i = 0;
        for (Class<?> name : clazz) {
            names[i++] = name.getName();
        }

        return new AnnotationFinder(this, Arrays.asList(names));
    }

    public AnnotationFinder select(String... clazz) {
        return new AnnotationFinder(this, Arrays.asList(clazz));
    }

    public AnnotationFinder select(Iterable<String> clazz) {
        return new AnnotationFinder(this, clazz);
    }

    public class SubArchive implements Archive {
        private List<Entry> classes = new LinkedList<Entry>();

        public SubArchive(String... classes) {
            for (String name : classes) {
                this.classes.add(new E(name));
            }
        }

        public SubArchive(Iterable<String> classes) {
            for (String name : classes) {
                this.classes.add(new E(name));
            }
        }

        public InputStream getBytecode(String className) throws IOException, ClassNotFoundException {
            return archive.getBytecode(className);
        }

        public Class<?> loadClass(String className) throws ClassNotFoundException {
            return archive.loadClass(className);
        }

        public Iterator<Entry> iterator() {
            return classes.iterator();
        }

        public class E implements Entry {
            private final String name;

            public E(String name) {
                this.name = name;
            }

            public String getName() {
                return name;
            }

            public InputStream getBytecode() throws IOException {
                return new ByteArrayInputStream(new byte[0]);
            }
        }
    }

    public class Annotatable {
        private final List<AnnotationInfo> annotations = new LinkedList<AnnotationInfo>();

        public Annotatable(AnnotatedElement element) {
            for (Annotation annotation : getAnnotations(element)) {
                annotations.add(new AnnotationInfo(Type.getType(annotation.annotationType()).getDescriptor()));
            }
        }

        public Annotatable() {
        }

        public Annotation[] getDeclaredAnnotations() {
            return new Annotation[0];
        }

        public List<AnnotationInfo> getAnnotations() {
            return annotations;
        }

        public String getMetaAnnotationName() {
            return null;
        }

        /**
         * Utility method to get around some errors caused by
         * interactions between the Equinox class loaders and
         * the OpenJPA transformation process.  There is a window
         * where the OpenJPA transformation process can cause
         * an annotation being processed to get defined in a
         * classloader during the actual defineClass call for
         * that very class (e.g., recursively).  This results in
         * a LinkageError exception.  If we see one of these,
         * retry the request.  Since the annotation will be
         * defined on the second pass, this should succeed.  If
         * we get a second exception, then it's likely some
         * other problem.
         *
         * @param element The AnnotatedElement we need information for.
         * @return An array of the Annotations defined on the element.
         */
        private Annotation[] getAnnotations(AnnotatedElement element) {
            try {
                return element.getAnnotations();
            } catch (LinkageError e) {
                return element.getAnnotations();
            }
        }

    }

    public static interface Info {

        String getMetaAnnotationName();

        String getName();

        List<AnnotationInfo> getAnnotations();

        Annotation[] getDeclaredAnnotations();
    }

    public class PackageInfo extends Annotatable implements Info {
        private final String name;
        private final ClassInfo info;
        private final Package pkg;

        public PackageInfo(Package pkg) {
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
            return (pkg != null) ? pkg : info.get().getPackage();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PackageInfo that = (PackageInfo) o;

            if (name != null ? !name.equals(that.name) : that.name != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return name != null ? name.hashCode() : 0;
        }
    }

    public class ClassInfo extends Annotatable implements Info {
        private String name;
        private final List<MethodInfo> methods = new SingleLinkedList<MethodInfo>();
        private final List<MethodInfo> constructors = new SingleLinkedList<MethodInfo>();
        private String superType;
        private ClassInfo superclassInfo;
        private final List<ClassInfo> subclassInfos = new SingleLinkedList<ClassInfo>();
        private final List<String> interfaces = new SingleLinkedList<String>();
        private final List<FieldInfo> fields = new SingleLinkedList<FieldInfo>();
        private Class<?> clazz;


        public ClassInfo(Class clazz) {
            super(clazz);
            this.clazz = clazz;
            this.name = clazz.getName();
            final Class superclass = clazz.getSuperclass();
            this.superType = superclass != null ? superclass.getName() : null;
            for (Class intrface : clazz.getInterfaces()) {
                this.interfaces.add(intrface.getName());
            }
        }

        public ClassInfo(final String name, final String superType) {
            this.name = name;
            this.superType = superType;
        }

        @Override
        public String getMetaAnnotationName() {
            for (AnnotationInfo info : getAnnotations()) {
                for (Class<? extends Annotation> metaroot : metaroots) {
                    if (info.getName().equals(metaroot.getName())) return name;
                }
            }

            if (name.endsWith("$$")) {
                ClassInfo info = classInfos.get(name.substring(0, name.length() - 2));
                if (info != null) {
                    return info.getMetaAnnotationName();
                }
            }

            return null;
        }

        public String getPackageName() {
            return name.indexOf(".") > 0 ? name.substring(0, name.lastIndexOf(".")) : "";
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

        public boolean isAnnotation() {
            return "java.lang.Object".equals(superType) && interfaces.size() == 1 && "java.lang.annotation.Annotation".equals(interfaces.get(0));
        }

        public Class<?> get() throws ClassNotFoundException {
            if (clazz != null) return clazz;
            try {
                String fixedName = name.replaceFirst("<.*>", "");
                this.clazz = archive.loadClass(fixedName);
                return clazz;
            } catch (ClassNotFoundException notFound) {
                classesNotLoaded.add(name);
                throw notFound;
            }
        }

        public String toString() {
            return name;
        }
    }

    public class MethodInfo extends Annotatable implements Info {
        private final ClassInfo declaringClass;
        private final String descriptor;
        private final String name;
        private final List<List<AnnotationInfo>> parameterAnnotations = new LinkedList<List<AnnotationInfo>>();
        private final List<ParameterInfo> parameters = new SingleLinkedList<ParameterInfo>();
        private Member method;

        public MethodInfo(ClassInfo info, Constructor constructor) {
            super(constructor);
            this.declaringClass = info;
            this.name = "<init>";
            this.descriptor = Type.getConstructorDescriptor(constructor);
        }

        public MethodInfo(ClassInfo info, Method method) {
            super(method);
            this.declaringClass = info;
            this.name = method.getName();
            this.descriptor = Type.getMethodDescriptor(method);
            this.method = method;
        }

        public MethodInfo(ClassInfo declarignClass, String name, String descriptor) {
            this.declaringClass = declarignClass;
            this.name = name;
            this.descriptor = descriptor;
        }

        public String getDescriptor() {
            return descriptor;
        }

        @Override
        public String getMetaAnnotationName() {
            return declaringClass.getMetaAnnotationName();
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            super.getDeclaredAnnotations();
            try {
                return ((AnnotatedElement) get()).getDeclaredAnnotations();
            } catch (ClassNotFoundException e) {
                return super.getDeclaredAnnotations();
            }
        }

        public boolean isConstructor() {
            return getName().equals("<init>");
        }

        public List<List<AnnotationInfo>> getParameterAnnotations() {
            return parameterAnnotations;
        }

        public List<AnnotationInfo> getParameterAnnotations(int index) {
            if (index >= parameterAnnotations.size()) {
                for (int i = parameterAnnotations.size(); i <= index; i++) {
                    List<AnnotationInfo> annotationInfos = new LinkedList<AnnotationInfo>();
                    parameterAnnotations.add(i, annotationInfos);
                }
            }
            return parameterAnnotations.get(index);
        }

        public List<ParameterInfo> getParameters() {
            return parameters;
        }

        public String getName() {
            return name;
        }

        public ClassInfo getDeclaringClass() {
            return declaringClass;
        }

        public String toString() {
            return declaringClass + "@" + name;
        }

        public Member get() throws ClassNotFoundException {
            if (method == null) {
                method = toMethod();
            }

            return method;
        }

        private Member toMethod() throws ClassNotFoundException {
            org.objectweb.asm.commons.Method method = new org.objectweb.asm.commons.Method(name, descriptor);

            Class<?> clazz = this.declaringClass.get();
            List<Class> parameterTypes = new LinkedList<Class>();

            for (Type type : method.getArgumentTypes()) {
                String paramType = type.getClassName();
                try {
                    parameterTypes.add(Classes.forName(paramType, clazz.getClassLoader()));
                } catch (ClassNotFoundException cnfe) {
                    throw new IllegalStateException("Parameter class could not be loaded for type " + paramType, cnfe);
                }
            }

            Class[] parameters = parameterTypes.toArray(new Class[parameterTypes.size()]);

            IllegalStateException noSuchMethod = null;
            while (clazz != null) {
                try {
                    if (name.equals("<init>")) {
                        return clazz.getDeclaredConstructor(parameters);
                    } else {
                        return clazz.getDeclaredMethod(name, parameters);
                    }
                } catch (NoSuchMethodException e) {
                    if (noSuchMethod == null) {
                        noSuchMethod = new IllegalStateException("Callback method does not exist: " + clazz.getName() + "." + name, e);
                    }
                    clazz = clazz.getSuperclass();
                }
            }

            throw noSuchMethod;
        }

    }

    public class ParameterInfo extends Annotatable implements Info {
        private final MethodInfo declaringMethod;
        private final int index;
        private final List<AnnotationInfo> annotations = new LinkedList<AnnotationInfo>();
        private Parameter<?> parameter;

        public ParameterInfo(MethodInfo parent, int index) {
            super();
            this.declaringMethod = parent;
            this.index = index;
        }

        public ParameterInfo(MethodInfo parent, Parameter<?> parameter) {
            super(parameter);
            this.declaringMethod = parent;
            this.index = parameter.getIndex();
            this.parameter = parameter;
        }

        public String getName() {
            return Integer.toString(index);
        }

        public Parameter<?> get() throws ClassNotFoundException {
            if (parameter == null) {
                Member member = declaringMethod.get();
                if (member instanceof Method) {
                    parameter = Parameter.declaredBy((Method) member, index);
                } else if (member instanceof Constructor<?>) {
                    parameter = Parameter.declaredBy((Constructor<?>) member, index);

                }
            }
            return parameter;
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            try {
                return get().getDeclaredAnnotations();
            } catch (ClassNotFoundException e) {
                return super.getDeclaredAnnotations();
            }
        }

        public MethodInfo getDeclaringMethod() {
            return declaringMethod;
        }

        @Override
        public String toString() {
            return String.format("%s(arg%s)", declaringMethod, index);
        }
    }

    public class FieldInfo extends Annotatable implements Info {
        private final String name;
        private final String type;
        private final ClassInfo declaringClass;
        private Field field;

        public FieldInfo(ClassInfo info, Field field) {
            super(field);
            this.declaringClass = info;
            this.name = field.getName();
            this.type = Type.getType(field.getType()).getDescriptor();
            this.field = field;
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

        public String getType() { // if this method starts to be used internally move this to constructors and just return type
            final Type t = Type.getType(type);
            if (t.getClassName() == null) {
                return t.getDescriptor();
            }
            return t.getClassName();
        }

        public String toString() {
            return declaringClass + "#" + name;
        }

        @Override
        public String getMetaAnnotationName() {
            return declaringClass.getMetaAnnotationName();
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            super.getDeclaredAnnotations();
            try {
                return ((AnnotatedElement) get()).getDeclaredAnnotations();
            } catch (ClassNotFoundException e) {
                return super.getDeclaredAnnotations();
            }
        }

        public Member get() throws ClassNotFoundException {
            if (field == null) {
                field = toField();
            }

            return field;
        }

        private Field toField() throws ClassNotFoundException {

            Class<?> clazz = this.declaringClass.get();

            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException(name, e);
            }

        }
    }

    public class AnnotationInfo extends Annotatable implements Info {
        private final String name;

        public AnnotationInfo(Annotation annotation) {
            this(Type.getType(annotation.annotationType()).getDescriptor());
        }

        public AnnotationInfo(Class<? extends Annotation> annotation) {
            this.name = annotation.getName().intern();
        }

        public AnnotationInfo(String name) {
            final Type type = Type.getType(name);
            name = type.getClassName();
            if (name == null) {
                name = type.getDescriptor(); // name was already a class name
            }
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public String toString() {
            return name;
        }
    }

    private void index(AnnotationInfo annotationInfo, Info info) {
        initAnnotationInfos(annotationInfo.getName()).add(info);
    }

    public class InfoBuildingVisitor extends EmptyVisitor {
        private Info info;

        public InfoBuildingVisitor() {
        }

        public InfoBuildingVisitor(Info info) {
            this.info = info;
        }

        public Info getInfo() {
            return info;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            if (name.endsWith("package-info")) {
                info = new PackageInfo(javaName(name));
            } else {

                ClassInfo classInfo = new ClassInfo(javaName(name), javaName(superName));

//                if (signature == null) {
                for (final String interfce : interfaces) {
                    classInfo.interfaces.add(javaName(interfce));
                }
//                } else {
//                    // the class uses generics
//                    new SignatureReader(signature).accept(new GenericAwareInfoBuildingVisitor(GenericAwareInfoBuildingVisitor.TYPE.CLASS, classInfo));
//                }
                info = classInfo;
                classInfos.put(classInfo.getName(), classInfo);
            }
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            if (cleanOnNaked()) {
                if (ClassInfo.class.isInstance(info) && isNaked(ClassInfo.class.cast(info))) {
                    classInfos.remove(info.getName());
                } else if (PackageInfo.class.isInstance(info) && isNaked(PackageInfo.class.cast(info))) {
                    classInfos.remove(info.getName());
                }
            }
        }

        private boolean isNaked(final PackageInfo info) {
            return info.getAnnotations().isEmpty();
        }

        private boolean isNaked(final ClassInfo info) {
            if (!info.getAnnotations().isEmpty()) {
                return false;
            }
            for (final FieldInfo fieldInfo : info.getFields()) {
                if (!fieldInfo.getAnnotations().isEmpty()) {
                    return false;
                }
            }
            for (final MethodInfo methodInfo : info.getMethods()) {
                if (!methodInfo.getAnnotations().isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        private String javaName(String name) {
            return (name == null) ? null : name.replace('/', '.');
        }

        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            super.visitInnerClass(name, outerName, innerName, access);
        }

        @Override
        public void visitAttribute(Attribute attribute) {
            super.visitAttribute(attribute);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            if (isTracked(desc)) {
                AnnotationInfo annotationInfo = new AnnotationInfo(desc);
                info.getAnnotations().add(annotationInfo);
                index(annotationInfo, info);
                return new InfoBuildingVisitor(annotationInfo).annotationVisitor();
            }
            return super.visitAnnotation(desc, visible);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            ClassInfo classInfo = ((ClassInfo) info);
            FieldInfo fieldInfo = new FieldInfo(classInfo, name, desc);
            classInfo.getFields().add(fieldInfo);
            return new InfoBuildingVisitor(fieldInfo).fieldVisitor();
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            ClassInfo classInfo = ((ClassInfo) info);
            MethodInfo methodInfo = new MethodInfo(classInfo, name, desc);

            classInfo.getMethods().add(methodInfo);
            return new InfoBuildingVisitor(methodInfo).methodVisitor();
        }


        @Override
        public AnnotationVisitor visitMethodParameterAnnotation(int param, String desc, boolean visible) {
            if (isTracked(desc)) {
                MethodInfo methodInfo = ((MethodInfo) info);
                List<AnnotationInfo> annotationInfos = methodInfo.getParameterAnnotations(param);
                AnnotationInfo annotationInfo = new AnnotationInfo(desc);
                annotationInfos.add(annotationInfo);

                ParameterInfo parameterInfo = new ParameterInfo(methodInfo, param);
                methodInfo.getParameters().add(parameterInfo);
                index(annotationInfo, parameterInfo);
                return new InfoBuildingVisitor(annotationInfo).annotationVisitor();
            }
            return super.visitMethodParameterAnnotation(param, desc, visible);
        }
    }
}