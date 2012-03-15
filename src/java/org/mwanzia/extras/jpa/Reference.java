package org.mwanzia.extras.jpa;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.mwanzia.JSON;
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

    public Reference(Class clazz, Object id) {
        this(buildStub(clazz, id));
    }

    private static Map<String, Object> buildStub(Class clazz, Object id) {
        Map<String, Object> stub = new HashMap<String, Object>();
        stub.put("@class", clazz.getName());
        stub.put("@id", id);
        return stub;
    }

    public Object dereference() {
        try {
            Class targetClass = stub.getClass();
            // Find id
            Object id = SmallPropertyUtils.readProperty(stub, "id");
            // For entities, re-read the entity from the session
            EntityManager em = JPAPlugin.getCurrentEntityManager();
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
