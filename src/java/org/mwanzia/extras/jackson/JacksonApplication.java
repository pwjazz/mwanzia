package org.mwanzia.extras.jackson;

import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.mwanzia.Application;

/**
 * An application that uses Jackson to process JSON.
 * 
 * @author percy wegmann ( percy <at> karen and percy <dot> net )
 * 
 */
public abstract class JacksonApplication extends Application {
    private final ObjectMapper mapper = new ObjectMapper();

    public JacksonApplication() {
        super();
    }

    public JacksonApplication(boolean whitelistProperties) {
        super(whitelistProperties);
    }

    public JacksonApplication(String name, boolean whitelistProperties) {
        super(name, whitelistProperties);
    }

    @Override
    public String serializeToJson(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Unable to serialize json: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> parseJson(String json) {
        try {
            return (Map<String, Object>) mapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Unable to parse json: " + e.getMessage(), e);
        }
    }

}
