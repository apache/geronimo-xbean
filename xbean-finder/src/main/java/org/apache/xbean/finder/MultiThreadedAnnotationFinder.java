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

import org.apache.xbean.finder.archive.Archive;
import org.apache.xbean.finder.util.SingleLinkedList;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiThreadedAnnotationFinder extends AnnotationFinder {
    private ExecutorService executor = null;
    private CountDownLatch subclassesLatch = null;
    private CountDownLatch implementationsLatch = null;
    private final int threads;

    public MultiThreadedAnnotationFinder(final Archive archive, final boolean checkRuntimeAnnotation, final int threads) {
        super(archive, checkRuntimeAnnotation);
        this.threads = threads;
    }

    public MultiThreadedAnnotationFinder(final Archive archive, final int threads) {
        super(archive);
        this.threads = threads;
    }

    @Override
    public AnnotationFinder enableFindImplementations() {
        if (implementationsLatch == null) {
            implementationsLatch = scan(new FindImplementationsFactory());
        }
        return this;
    }

    @Override
    public AnnotationFinder enableFindSubclasses() {
        if (subclassesLatch == null) {
            subclassesLatch = scan(new FindSubclassesFactory());
        }
        return this;
    }

    private ExecutorService executor() {
        if (executor == null) {
            executor = Executors.newFixedThreadPool(threads, new DaemonThreadFactory());
        }
        return executor;
    }

    @Override
    public <T> List<Class<? extends T>> findSubclasses(final Class<T> clazz) {
        if (subclassesLatch == null) {
            enableFindSubclasses();
        }
        join(subclassesLatch);

        return super.findSubclasses(clazz);
    }

    @Override
    public <T> List<Class<? extends T>> findImplementations(final Class<T> clazz) {
        if (implementationsLatch == null) {
            enableFindImplementations();
        }
        join(implementationsLatch);
        return super.findImplementations(clazz);
    }

    private CountDownLatch scan(final ScanTaskFactory factory) {
        final ClassInfo[] classes = classInfos.values().toArray(new ClassInfo[classInfos.size()]);
        final ExecutorService es = executor();
        final CountDownLatch latch = new CountDownLatch(classes.length);
        for (final ClassInfo classInfo : classes) {
            es.submit(factory.next(classInfo, latch));
        }
        return latch;
    }

    private void join(final CountDownLatch latch) {
        try {
            latch.await(1, TimeUnit.HOURS);
        } catch (final InterruptedException e) {
            // no-op
        }
    }

    @Override
    protected Map<String, ClassInfo> newClassInfoMap() {
        return new ConcurrentHashMap<String, ClassInfo>();
    }

    @Override
    protected Map<String, List<Info>> newAnnotatedMap() {
        return new ConcurrentHashMap<String, List<Info>>();
    }

    @Override
    protected List<Info> initAnnotationInfos(String name) {
        List<Info> infos = annotated.get(name);
        if (infos == null) {
            infos = new SingleLinkedList<Info>();

            final List<Info> old = ((ConcurrentMap<String, List<Info>>) annotated).putIfAbsent(name, infos);
            if (old != null) {
                infos = old;
            }
        }
        return infos;
    }

    protected static class DaemonThreadFactory implements ThreadFactory {
        private final String name = "xbean-finder-" + hashCode();
        private final ThreadGroup group;
        private final AtomicInteger ids = new AtomicInteger(0);

        protected DaemonThreadFactory() {
            final SecurityManager securityManager = System.getSecurityManager();
            if (securityManager != null) {
                group = securityManager.getThreadGroup();
            } else {
                group = Thread.currentThread().getThreadGroup();
            }
        }

        // @Override
        public Thread newThread(final Runnable runnable) {
            final ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(AnnotationFinder.class.getClassLoader());
            try {
                final Thread thread = new Thread(group, runnable, name + " - " + ids.incrementAndGet());
                if (!thread.isDaemon()) {
                    thread.setDaemon(true);
                }
                if (thread.getPriority() != Thread.NORM_PRIORITY) {
                    thread.setPriority(Thread.NORM_PRIORITY);
                }
                return thread;
            } finally {
                Thread.currentThread().setContextClassLoader(loader);
            }
        }
    }

    protected static abstract class ScanTask implements Runnable {
        private final ClassInfo info;
        private final CountDownLatch latch;

        protected ScanTask(final ClassInfo info, final CountDownLatch latch) {
            this.info = info;
            this.latch = latch;
        }

        // @Override
        public void run() {
            try {
                doRun(info);
            } finally {
                latch.countDown();
            }
        }

        public abstract void doRun(final ClassInfo info);
    }

    protected static interface ScanTaskFactory {
        ScanTask next(final ClassInfo info, CountDownLatch latch);
    }

    protected class FindImplementationsFactory implements ScanTaskFactory {
        // @Override
        public ScanTask next(final ClassInfo info, final CountDownLatch latch) {
            return new FindImplementations(info, latch);
        }
    }

    protected class FindSubclassesFactory implements ScanTaskFactory {
        // @Override
        public ScanTask next(final ClassInfo info, final CountDownLatch latch) {
            return new FindSubclasses(info, latch);
        }
    }

    protected class FindImplementations extends ScanTask {
        public FindImplementations(final ClassInfo info, final CountDownLatch latch) {
            super(info, latch);
        }

        @Override
        public void doRun(final ClassInfo info) {
            linkInterfaces(info);
        }
    }

    protected class FindSubclasses extends ScanTask {
        public FindSubclasses(final ClassInfo info, final CountDownLatch latch) {
            super(info, latch);
        }

        @Override
        public void doRun(final ClassInfo info) {
            linkParent(info);
        }
    }
}
