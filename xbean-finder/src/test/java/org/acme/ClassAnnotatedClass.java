package org.acme;

import org.acme.bar.ClassAnnotation;

@ClassAnnotation
public class ClassAnnotatedClass {

    @ClassAnnotation
    private String green;

    private String red;

    @ClassAnnotation
    public ClassAnnotatedClass() {}

    public ClassAnnotatedClass(String red) {}

    @ClassAnnotation
    public void green() {}

    public void red() {}
}
