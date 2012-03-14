package org.mwanzia.extras.jpa;

import java.io.IOException;
import java.util.Map;

import javax.persistence.EntityManager;

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
     * A stub of the referenced object containing only the @class and id
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
            // Find id
            Object id = PropertyUtils.getProperty(stub, "id");
            // For entities, re-read the entity from the session
            EntityManager em = AbstractJPAPlugin.getCurrentEntityManager();
            Object result = em.find(targetClass, id);
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
