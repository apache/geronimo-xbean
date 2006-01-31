package org.apache.xbean.spring.example;

import java.util.List;

import javax.xml.namespace.QName;

/**
 * @org.apache.xbean.XBean element="qname-service"
 * @author gnodet
 */
public class QNameService {

    private QName[] services;
    private List list;

    public QName[] getServices() {
        return services;
    }

    public void setServices(QName[] services) {
        this.services = services;
    }

    public List getList() {
        return list;
    }

    public void setList(List list) {
        this.list = list;
    }


}
