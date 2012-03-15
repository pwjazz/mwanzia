package org.mwanzia.extras.validation;

import java.io.Serializable;
import java.util.Map;

import net.sf.oval.ConstraintViolation;
import net.sf.oval.context.MethodParameterContext;
import net.sf.oval.context.OValContext;

import org.codehaus.jackson.annotate.JsonProperty;

public class ValidationError {
    private String fieldName;
    private String errorCode;
    private String message;
    private Map<String, ? extends Serializable> messageVariables;

    public ValidationError(ConstraintViolation violation) {
        this.fieldName = getFieldName(violation);
        this.message = violation.getMessage();
        this.errorCode = violation.getErrorCode();
        this.messageVariables = violation.getMessageVariables();
    }

    @JsonProperty
    public String getFieldName() {
        return fieldName;
    }

    @JsonProperty
    public String getMessage() {
        return message;
    }

    @JsonProperty
    public String getErrorCode() {
        return errorCode;
    }

    @JsonProperty
    public Map<String, ? extends Serializable> getMessageVariables() {
        return messageVariables;
    }

    public static String getFieldName(ConstraintViolation violation) {
        OValContext context = violation.getCheckDeclaringContext();
        return context instanceof MethodParameterContext ? ((MethodParameterContext) context).getParameterName() : null;
    }
}