package org.mwanzia.extras.jpa;

import java.io.Serializable;

import javax.persistence.EntityManager;

import org.mwanzia.JsonInclude;
import org.mwanzia.MwanziaException;
import org.mwanzia.SmallPropertyUtils;

/**
 * <p>
 * Represents a Reference to an Entity. Used to support call-by-reference
 * semantics from the client.
 * </p>
 * 
 * @author percy wegmann ( percy <at> karen and percy <dot> net )
 * 
 */
public class Reference {
    private String targetClassName;
    private Serializable id;

    public Reference() {
    }

    public Reference(Object original) throws Exception {
        this(original.getClass(), (Serializable) SmallPropertyUtils.readProperty(original, "id"));
    }

    public Reference(Class clazz, Serializable id) throws Exception {
        this.targetClassName = clazz.getName();
        this.id = id;
    }

    public Object dereference() {
        try {
            Class targetClass = getClass().getClassLoader().loadClass(targetClassName);
            // Re-read the entity from the session
            EntityManager em = JPAPlugin.getCurrentEntityManager();
            Object result = em.find(targetClass, id);
            return result;
        } catch (Exception e) {
            throw new MwanziaException(String.format("Unable to dereference object of type %1$s with id %2$s",
                    targetClassName,
                    id), e);
        }
    }

    @JsonInclude
    public String getTargetClassName() {
        return targetClassName;
    }

    public void setTargetClassName(String targetClassName) {
        this.targetClassName = targetClassName;
    }

    @JsonInclude
    public Serializable getId() {
        return id;
    }

    public void setId(Serializable id) {
        this.id = id;
    }
}
