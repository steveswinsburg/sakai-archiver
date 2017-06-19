/*
 * YaleDirectoryException.java
 *
 * Created on April 5, 2006, 10:08 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.yale.its.amt.sakai.directory;

/**
 *
 * @author mikea
 */
public class YaleDirectoryException extends java.lang.Exception
{
    
    /**
     * Creates a new instance of <code>YaleDirectoryException</code> without detail message.
     */
    public YaleDirectoryException()
    {
    }
 
    /**
     * Constructs an instance of <code>YaleDirectoryException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public YaleDirectoryException(Throwable t)
    {
        super(t);
    }    
    
    
    /**
     * Constructs an instance of <code>YaleDirectoryException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public YaleDirectoryException(String msg)
    {
        super(msg);
    }
}
