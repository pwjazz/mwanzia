package org.mwanzia.extras.validation;

import java.util.ArrayList;
import java.util.List;

import net.sf.oval.ConstraintViolation;

import org.codehaus.jackson.annotate.JsonProperty;

public class ValidationException extends RuntimeException {
    private static final long serialVersionUID = -7283646912755838847L;
    private List<ValidationError> errors = new ArrayList<ValidationError>();

    public ValidationException(List<ConstraintViolation> violations) {
        super(violations.get(0).getMessage());
        for (ConstraintViolation violation : violations) {
            errors.add(new ValidationError(violation));
        }
    }

    @JsonProperty
    public List<ValidationError> getErrors() {
        return errors;
    }

    public void setErrors(List<ValidationError> errors) {
        this.errors = errors;
    }

    @JsonProperty
    @Override
    public String getMessage() {
        return super.getMessage();
    }
}
