package org.mwanzia.extras.validation;

import java.util.Arrays;

import net.sf.oval.exception.ConstraintsViolatedException;
import net.sf.oval.exception.ExceptionTranslator;
import net.sf.oval.exception.OValException;

public class MwanziaOValExceptionTranslator implements ExceptionTranslator {
    @Override
    public RuntimeException translateException(OValException e) {
        if (e instanceof ConstraintsViolatedException) {
            ConstraintsViolatedException cve = (ConstraintsViolatedException) e;
            return new ValidationException(Arrays.asList(cve.getConstraintViolations()));
        } else {
            return new RuntimeException("Unexpected OVal exception: " + e.getMessage());
        }
    }
}
