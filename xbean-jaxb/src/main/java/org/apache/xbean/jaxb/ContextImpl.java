package org.apache.xbean.jaxb;

import org.apache.xbean.jaxb.jndi.DefaultContext;

import javax.naming.Context;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

@XmlRootElement(name = "context")
public class ContextImpl {
    @XmlTransient
    public Map<String, Object> entries = new HashMap<String, Object>();

    // for JAXB
    @XmlElement(name = "entry")
    private ContextEntry[] getEntries() {
        ContextEntry[] r = new ContextEntry[entries.size()];
        int i = 0;
        for (Map.Entry<String, Object> e : entrySet()) {
            r[i++] = new ContextEntry(e.getKey(), e.getValue());
        }
        return r;
    }

    private void setEntries(ContextEntry[] v) {
        entries.clear();
        for (ContextEntry e : v)
            entries.put(e.getKey(), e.getValue());
    }
    
    @Override
    public String toString() {
        return "Context" + entries;
    }

    public Object get(String name) {
        return entries.get(name);
    }

    public void put(String key, Object value) {
        entrySet();
        entries.put(key, value);
    }

    public Set<Entry<String, Object>> entrySet() {
        return entries.entrySet();
    }

    // JAXB helper methods
    // -------------------------------------------------------------------------
    public void marshal(JAXBContext context, OutputStream os) throws Exception {
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(this, os);
    }

    public static ContextImpl load(JAXBContext context, InputStream in) throws Exception {
        return (ContextImpl) context.createUnmarshaller().unmarshal(in);
    }

    /**
     * Converts this context into a full JNDI context
     */
    public Context createJndiContext(Hashtable environment) {
        return new DefaultContext(environment, entries);
    }

    public void putAll(ContextImpl context) {
        entries.putAll(context.entries);
    }

}