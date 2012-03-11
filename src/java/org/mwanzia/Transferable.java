package org.mwanzia;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Marks a class or individual property (getter method) as transferable.
 * Transferable properties are sent back to the server when invoking remote
 * methods on the object. When applied to a class, all of that class's
 * properties (including inherited) are transferable.
 * <p>
 * 
 * <p>
 * This is useful (for example) for identifying instances of the remote object
 * by making it's ID property Transferable.
 * </p>
 * 
 * @author percy
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.TYPE, ElementType.METHOD })
public @interface Transferable {

}
