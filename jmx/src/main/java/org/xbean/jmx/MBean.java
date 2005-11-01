package org.xbean.jmx;

/**
 * @org.xbean.XBean element="mbean"
 */
public class MBean {
    private Object bean;
    private String objectName;
    private Class mbeanClass;
    private EventAdapter eventAdapter;

    public Object getBean() {
        return bean;
    }

    public void setBean(Object bean) {
        this.bean = bean;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    /**
     * @org.xbean.Property alias="mbean"
     */
    public Class getMBeanClass() {
        return mbeanClass;
    }

    public void setMBeanClass(Class mbeanClass) {
        this.mbeanClass = mbeanClass;
    }

    public EventAdapter getEventAdapter() {
        return eventAdapter;
    }

    public void setEventAdapter(EventAdapter eventAdapter) {
        this.eventAdapter = eventAdapter;
    }
}
