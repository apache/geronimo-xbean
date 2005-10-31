package org.xbean.jmx;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @org.xbean.XBean element="listen"
 */
public class EventAdapter {
    private Object source;
    private Class targetClass;
    private String addMethodName;
    private String removeMethodName;
    private Object listener;

    public void setSource(Object source) {
        this.source = source;
    }

    public void setTargetClass(Class targetClass) {
        this.targetClass = targetClass;
    }

    public void setAddMethodName(String addMethodName) {
        this.addMethodName = addMethodName;
    }

    public void setRemoveMethodName(String removeMethodName) {
        this.removeMethodName = removeMethodName;
    }

    /**
     * @org.xbean.InitMethod
     */
    public void start() {
        try {
            listener = targetClass.newInstance();
            Class[] interfaces = targetClass.getInterfaces();
            boolean found = false;
            for (int i = 0; i < interfaces.length; ++i) {
                try {
                    Method addMethod = source.getClass().getMethod(addMethodName, new Class[]{interfaces[i]});
                    addMethod.invoke(source, new Object[]{listener});
                    found = true;
                    break;
                } catch (NoSuchMethodException ignored) {
                }
            }
            if (!found) throw new JMXException("Could not find " + addMethodName + " in class " + targetClass.getName());
        } catch (InstantiationException x) {
            throw new JMXException(x);
        } catch (IllegalAccessException x) {
            throw new JMXException(x);
        } catch (InvocationTargetException x) {
            throw new JMXException(x.getCause());
        }
    }

    /**
     * @org.xbean.DestroyMethod
     */
    public void stop() {
        try {
            Class[] interfaces = targetClass.getInterfaces();
            boolean found = false;
            for (int i = 0; i < interfaces.length; ++i) {
                try {
                    Method removeMethod = source.getClass().getMethod(removeMethodName, new Class[]{interfaces[i]});
                    removeMethod.invoke(source, new Object[]{listener});
                    found = true;
                    break;
                } catch (NoSuchMethodException ignored) {
                }
            }
            if (!found) throw new JMXException("Could not find " + removeMethodName + " in class " + targetClass.getName());
        } catch (IllegalAccessException x) {
            throw new JMXException(x);
        } catch (InvocationTargetException x) {
            throw new JMXException(x.getCause());
        }
    }
}
