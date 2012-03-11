package org.mwanzia.extras.validation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.sf.oval.ConstraintViolation;
import net.sf.oval.exception.ValidationFailedException;
import net.sf.oval.guard.Guard;
import net.sf.oval.guard.ParameterNameResolverParanamerImpl;

public class MwanziaGuard extends Guard {
    public MwanziaGuard() {
        super();
        setExceptionTranslator(new MwanziaOValExceptionTranslator());
        parameterNameResolver.setDelegate(new ParameterNameResolverParanamerImpl());
    }

    public List<ConstraintViolation> validateMethodParameters(Object validatedObject, Method method, Object[] args)
            throws ValidationFailedException {
        List<ConstraintViolation> violations = new ArrayList<ConstraintViolation>();
        super.validateMethodParameters(validatedObject, method, args, violations);
        return violations;
    }

}
