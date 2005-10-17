/*
 * © Copyright 2004 Hewlett-Packard
 */
package org.xbean.jmx;

/**
 * $Rev$
 */
public class JMXException extends RuntimeException
{
   public JMXException()
   {
   }

   public JMXException(String message)
   {
      super(message);
   }

   public JMXException(String message, Throwable cause)
   {
      super(message, cause);
   }

   public JMXException(Throwable cause)
   {
      super(cause);
   }
}
