/* =====================================================================
 *
 * Copyright (c) 2003 David Blevins.  All rights reserved.
 *
 * =====================================================================
 */
package org.acme.foo;

/**
 * @version $Revision$ $Date$
 */
@java.lang.annotation.Target(value = {java.lang.annotation.ElementType.PACKAGE})
@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface Deployable {
}
