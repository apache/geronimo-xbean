package org.apache.xbean.jmx.assembler;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ObjectName;

import org.apache.xbean.jmx.MBeanInfoGenerator;
import org.livetribe.jmx.MBeanUtils;


/**
 * @version $Revision: $ $Date: $
 */
public class JavaBeanMBeanInfoGenerator implements MBeanInfoGenerator {
    public MBeanInfo createMBeanInfo(Object bean, ObjectName objectName) {
        MBeanAttributeInfo[] attributes = MBeanUtils.getMBeanAttributeInfo(bean);
        MBeanConstructorInfo[] constructors = MBeanUtils.getMBeanConstructorInfo(bean);
        MBeanOperationInfo[] operations = MBeanUtils.getMBeanOperationInfo(bean);

        return new MBeanInfo(bean.getClass().getName(), null, attributes, constructors, operations, null);
    }
}
