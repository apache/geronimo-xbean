package org.xbean.jmx;

/**
 * $Rev$
 */
public class MBean
{
   private Object bean;
   private String objectName;
   private Class mbeanClass;
   private EventAdapter eventAdapter;

   public void setBean(Object bean)
   {
      this.bean = bean;
   }

   public void setObjectName(String objectName)
   {
      this.objectName = objectName;
   }

   public void setMBeanClass(Class mbeanClass)
   {
      this.mbeanClass = mbeanClass;
   }

   public void setEventAdapter(EventAdapter eventAdapter)
   {
      this.eventAdapter = eventAdapter;
   }
}
