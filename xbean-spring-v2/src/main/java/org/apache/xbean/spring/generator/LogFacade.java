package org.apache.xbean.spring.generator;

public interface LogFacade {

    void log(String message);
    
    void log(String message, int level);
}
