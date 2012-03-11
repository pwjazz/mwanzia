package org.mwanzia.extras.jpa;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Version;

import org.apache.commons.beanutils.PropertyUtils;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.mwanzia.JSON;
import org.mwanzia.MwanziaException;

/**
 * <p>
 * Represents a Reference to an Entity. Used to support call-by-reference
 * semantics from the client.
 * </p>
 * 
 * @author percy wegmann ( percy <at> karen and percy <dot> net )
 * 
 */
@JsonDeserialize(using = Reference.ReferenceDeserializer.class)
public class Reference {
    /**
     * A stub of the referenced object containing only the @class, id and
     * version
     */
    private Object stub;

    public Reference() {
    }

    public Reference(Object stub) {
        super();
        this.stub = stub;
    }

    public Object dereference() {
        try {
            Class targetClass = stub.getClass();
            // Find id and version
            Object id = null;
            Object version = null;
            String versionPropertyName = null;
            // Find a more efficient way to identify ID and Version properties
            // (and ideally not limited to annotations)
            for (PropertyDescriptor descriptor : PropertyUtils.getPropertyDescriptors(targetClass)) {
                if ((descriptor.getReadMethod() != null && descriptor.getReadMethod().isAnnotationPresent(Id.class))
                        || (descriptor.getWriteMethod() != null && descriptor.getWriteMethod()
                                .isAnnotationPresent(Id.class))) {
                    id = descriptor.getReadMethod().invoke(stub);
                } else if ((descriptor.getReadMethod() != null && descriptor.getReadMethod()
                        .isAnnotationPresent(Version.class))
                        || (descriptor.getWriteMethod() != null && descriptor.getWriteMethod()
                                .isAnnotationPresent(Version.class))) {
                    versionPropertyName = descriptor.getName();
                    version = descriptor.getReadMethod().invoke(stub);
                }
            }
            // For entities, re-read the entity from the session
            EntityManager em = JPAPlugin.getCurrentEntityManager();
            Object result = em.find(targetClass, id);
            if (versionPropertyName != null && version != null && ((Long) version) != 0L) {
                // Set the version if available
                PropertyUtils.setProperty(result, versionPropertyName, version);
            }
            return result;
        } catch (Exception e) {
            throw new MwanziaException(String.format("Unable to dereference object %1$s", stub), e);
        }
    }

    public Object getStub() {
        return stub;
    }

    public void setStub(Object stub) {
        this.stub = stub;
    }

    public static class ReferenceDeserializer extends JsonDeserializer<Object> {
        @Override
        public Object deserialize(JsonParser parser, DeserializationContext context) throws IOException,
                JsonProcessingException {
            Map<Object, Object> referenceMap = JSON.deserialize(parser, Map.class);
            Reference reference = new Reference(referenceMap.get("stub"));
            return reference.dereference();
        }
    }
}
