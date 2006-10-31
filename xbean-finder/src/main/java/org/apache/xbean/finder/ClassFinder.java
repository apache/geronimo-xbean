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
package org.apache.xbean.finder;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.EmptyVisitor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * ClassFinder searches the classpath of the specified classloader for
 * packages, classes, constructors, methods, or fields with specific annotations.
 *
 * For security reasons ASM is used to find the annotations.  Classes are not
 * loaded unless they match the requirements of a called findAnnotated* method.
 * Once loaded, these classes are cached.
 *
 * The getClassesNotLoaded() method can be used immediately after any find*
 * method to get a list of classes which matched the find requirements (i.e.
 * contained the annotation), but were unable to be loaded.
 *
 * @author David Blevins
 * @version $Rev$ $Date$
 */
public class ClassFinder {
    private final Map<String, List<Info>> annotated = new HashMap();

    private final ClassLoader classLoader;
    private final List<String> classesNotLoaded = new ArrayList();

    /**
     * Creates a ClassFinder that will search the urls in the specified classloader
     * excluding the urls in the classloader's parent.
     *
     * To include the parent classloader, use:
     *
     *    new ClassFinder(classLoader, false);
     *
     * To exclude the parent's parent, use:
     *
     *    new ClassFinder(classLoader, classLoader.getParent().getParent());
     *
     * @param classLoader
     * @throws Exception
     */
    public ClassFinder(ClassLoader classLoader) throws Exception {
        this(classLoader, true);
    }

    /**
     * Creates a ClassFinder that will search the urls in the specified classloader.
     *
     * @param classLoader
     * @param excludeParent
     * @throws Exception
     */
    public ClassFinder(ClassLoader classLoader, boolean excludeParent) throws Exception {
        this(classLoader, getUrls(classLoader, excludeParent));
    }

    /**
     * Creates a ClassFinder that will search the urls in the specified classloader excluding
     * the urls in the 'exclude' classloader.
     *
     * @param classLoader
     * @param exclude
     * @throws Exception
     */
    public ClassFinder(ClassLoader classLoader, ClassLoader exclude) throws Exception {
        this(classLoader, getUrls(classLoader, exclude));
    }

    public ClassFinder(ClassLoader classLoader, URL url) {
        this(classLoader, Arrays.asList(new URL[]{url}));
    }

    public ClassFinder(ClassLoader classLoader, Collection<URL> urls) {
        this.classLoader = classLoader;

        List<String> classNames = new ArrayList();
        for (URL location : urls) {
            try {
                if (location.getProtocol().equals("jar")) {
                    classNames.addAll(jar(location));
                } else if (location.getProtocol().equals("file")) {
                    classNames.addAll(file(location));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (String className : classNames) {
            readClassDef(className);
        }

    }

    /**
     * Returns a list of classes that could not be loaded in last invoked findAnnotated* method.
     * <p/>
     * The list will only contain entries of classes whose byte code matched the requirements
     * of last invoked find* method, but were unable to be loaded and included in the results.
     * <p/>
     * The list returned is unmodifiable and the results of this method will change
     * after each invocation of a findAnnotated* method.
     * <p/>
     * This method is not thread safe.
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

    private static Collection<URL> getUrls(ClassLoader classLoader, boolean excludeParent) throws IOException {
        return getUrls(classLoader, excludeParent? classLoader.getParent() : null);
    }

    private static Collection<URL> getUrls(ClassLoader classLoader, ClassLoader excludeParent) throws IOException {
        Map<String, URL> urls = toMap(classLoader.getResources("META-INF"));

        if (excludeParent != null) {
            Map<String, URL> parentUrls = toMap(excludeParent.getResources("META-INF"));
            for (String url : parentUrls.keySet()) {
                urls.remove(url);
            }
        }

        return urls.values();
    }

    private static Map<String, URL> toMap(Enumeration<URL> enumeration) {
        Map<String, URL> urls = new HashMap();
        while (enumeration.hasMoreElements()) {
            URL url = enumeration.nextElement();
            urls.put(url.toExternalForm(), url);
        }
        return urls;
    }

    private List<String> file(URL location) {
        List<String> classNames = new ArrayList();
        File dir = new File(location.getPath());
        if (dir.getName().equals("META-INF")) {
            dir = dir.getParentFile(); // Scrape "META-INF" off
        }
        if (dir.isDirectory()) {
            scanDir(dir, classNames, "");
        }
        return classNames;
    }

    private void scanDir(File dir, List<String> classNames, String packageName) {
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                scanDir(file, classNames, packageName + file.getName() + ".");
            } else if (file.getName().endsWith(".class")) {
                String name = file.getName();
                name = name.replaceFirst(".class$", "");
                classNames.add(packageName + name);
            }
        }
    }

    private List<String> jar(URL location) throws IOException {
        List<String> classNames = new ArrayList();

        String jarPath = location.getFile().replaceFirst("./META-INF", "");
        URL url = new URL(jarPath);
        InputStream in = url.openStream();
        JarInputStream jarStream = new JarInputStream(in);

        JarEntry entry;
        while ((entry = jarStream.getNextJarEntry()) != null) {
            if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                continue;
            }
            String className = entry.getName();
            className = className.replaceFirst(".class$", "");
            className = className.replace('/', '.');
            classNames.add(className);
        }

        return classNames;
    }

    public static class Annotatable {
        private final List<AnnotationInfo> annotations = new ArrayList();

        public List<AnnotationInfo> getAnnotations() {
            return annotations;
        }
    }

    public static interface Info {
        String getName();

        List<AnnotationInfo> getAnnotations();
    }

    public class PackageInfo extends Annotatable implements Info {
        private final ClassInfo info;

        public PackageInfo(String name) {
            info = new ClassInfo(name, null);
        }

        public String getName() {
            return info.getName();
        }

        public Package get() throws ClassNotFoundException {
            return info.get().getPackage();
        }
    }

    public class ClassInfo extends Annotatable implements Info {
        private final String name;
        private final List<MethodInfo> methods;
        private final List<MethodInfo> constructors;
        private final String superType;
        private final List<String> interfaces;
        private final List<FieldInfo> fields;
        private Class<?> clazz;
        private ClassNotFoundException notFound;

        public ClassInfo(String name, String superType) {
            this.name = name;
            this.superType = superType;
            this.methods = new ArrayList();
            this.constructors = new ArrayList();
            this.interfaces = new ArrayList();
            this.fields = new ArrayList();
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
                this.clazz = classLoader.loadClass(name);
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
        private final List<List<AnnotationInfo>> parameterAnnotations;

        public MethodInfo(ClassInfo declarignClass, String name, String returnType) {
            this.declaringClass = declarignClass;
            this.name = name;
            this.returnType = returnType;
            this.parameterAnnotations = new ArrayList();
        }

        public List<List<AnnotationInfo>> getParameterAnnotations() {
            return parameterAnnotations;
        }

        public List<AnnotationInfo> getParameterAnnotations(int index) {
            if (index >= parameterAnnotations.size()) {
                List<AnnotationInfo> annotationInfos = new ArrayList<AnnotationInfo>();
                parameterAnnotations.add(index, annotationInfos);
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
            return name.toString();
        }
    }

    private List<Info> getAnnotationInfos(String name) {
        List<Info> infos = annotated.get(name);
        if (infos == null) {
            infos = new ArrayList();
            annotated.put(name, infos);
        }
        return infos;
    }

    private void readClassDef(String className) {
        if (!className.endsWith(".class")) {
            className = className.replace('.', '/') + ".class";
        }
        ClassReader classReader = null;
        try {
            URL resource = classLoader.getResource(className);
            classReader = new ClassReader(resource.openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

//        classReader.accept(new ASMifierClassVisitor(new PrintWriter(System.out)), true);
        classReader.accept(new InfoBuildingVisitor(), true);
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

                for (String interfce : interfaces) {
                    classInfo.getInterfaces().add(javaName(interfce));
                }
                info = classInfo;
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
}