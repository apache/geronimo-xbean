package org.xbean.jmx;

import java.util.List;
import javax.management.MBeanServer;

/**
 * $Rev$
 */
public class MBeanExporter
{
   private MBeanServer mbeanServer;
   private List mbeans;
   private List connectors;

   public void setMBeanServer(MBeanServer mbeanServer)
   {
      this.mbeanServer = mbeanServer;
   }

   public void setMBean(List mbeans)
   {
      this.mbeans = mbeans;
   }

   public void setConnector(List connectors)
   {
      this.connectors = connectors;
   }

   public void start()
   {
      System.out.println("start");
   }

   public void stop()
   {
      System.out.println("stop");
   }
}
