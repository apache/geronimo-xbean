package org.xbean.jmx;

import javax.management.MBeanServer;
import java.util.List;

/**
 * Exports services to an MBeanServer for management.
 * @org.xbean.XBean element="export"
 */
public class MBeanExporter {
    private MBeanServer mbeanServer;
    private List mbeans;
    private List connectors;

    /**
     * @org.xbean.Property alias="mbeanServer"
     */
    public MBeanServer getMBeanServer() {
        return mbeanServer;
    }

    public void setMBeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }

    /**
     * @org.xbean.Property alias="mbeans"
     */
    public List getMBeans() {
        return mbeans;
    }

    public void setMBeans(List mbeans) {
        this.mbeans = mbeans;
    }

    public List getConnectors() {
        return connectors;
    }

    public void setConnectors(List connectors) {
        this.connectors = connectors;
    }

    /**
     * @org.xbean.InitMethod
     */
    public void start() {
        System.out.println("start");
    }

    /**
     * @org.xbean.DestroyMethod
     */
    public void stop() {
        System.out.println("stop");
    }
}
