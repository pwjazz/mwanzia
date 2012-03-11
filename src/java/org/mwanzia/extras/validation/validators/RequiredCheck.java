package org.mwanzia.extras.validation.validators;

import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.OValContext;

public class RequiredCheck extends AbstractAnnotationCheck<Required> {
    public boolean isSatisfied(Object validatedObject, Object valueToValidate, OValContext context, Validator validator) {
        if (valueToValidate == null)
            return false;
        if (valueToValidate instanceof String && ((String) valueToValidate).trim().length() == 0)
            return false;
        return true;
    }

}
