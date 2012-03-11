package org.mwanzia.extras.validation;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import net.sf.oval.ConstraintViolation;
import net.sf.oval.configuration.annotation.Constraint;
import net.sf.oval.constraint.AssertValid;
import net.sf.oval.guard.Guard;

import org.apache.commons.beanutils.PropertyUtils;
import org.mwanzia.Interceptor;
import org.mwanzia.JSON;
import org.mwanzia.MwanziaError;
import org.mwanzia.Plugin;
import org.mwanzia.PrettyPrinter;

import com.thoughtworks.paranamer.BytecodeReadingParanamer;
import com.thoughtworks.paranamer.Paranamer;

public class ValidationPlugin extends Plugin {
    private static final List<String> EXCLUDED_ANNOTATION_PROPERTIES = Arrays.asList(new String[] { "appliesTo",
            "profiles", "when", "getClass", "hashCode", "annotationType", "toString" });

    private boolean skipValidation = false;

    public ValidationPlugin() {
        this(false);
    }

    public ValidationPlugin(boolean skipValidation) {
        this.skipValidation = skipValidation;
    }

    @Override
    public List<Class> getRemoteTypes() {
        return Arrays.asList(new Class[] { ValidationException.class, ValidationError.class });
    }

    @Override
    public Interceptor buildInterceptor() {
        return new Interceptor() {
            @Override
            public Object[] prepareInvocation(Object target, Method method, Object[] arguments) throws Exception {
                if (!skipValidation) {
                    List<ConstraintViolation> violations = new MwanziaGuard().validateMethodParameters(target,
                            method,
                            arguments);
                    if (violations.size() > 0) {
                        throw new ValidationException(violations);
                    }
                }
                return arguments;
            }
        };
    }

    @Override
    public void postProcessClass(PrettyPrinter js, Class clazz, Set<Method> instanceMethods, Set<Method> staticMethods,
            Set<String> transferableProperties) {
        Guard guard = new Guard();

        try {
            Paranamer paranamer = new BytecodeReadingParanamer();
            for (Method method : instanceMethods) {
                writePropertyValidations(js,
                        String.format("mwanzia.%1$s.prototype.%2$s.parameters", clazz.getName(), method.getName()),
                        method,
                        paranamer);
            }
            for (Method method : staticMethods) {
                writePropertyValidations(js,
                        String.format("mwanzia.%1$s.%2$s.parameters", clazz.getName(), method.getName()),
                        method,
                        paranamer);
            }
        } catch (Exception e) {
            throw new MwanziaError(String.format("Unable to configure validations for class %1$s: %2$s",
                    clazz.getName(),
                    e.getMessage()), e);
        }
    }

    private void writePropertyValidations(PrettyPrinter js, String prefix, Method method, Paranamer paranamer)
            throws Exception {
        Class[] parameterTypes = method.getParameterTypes();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        String[] parameterNames = paranamer.lookupParameterNames(method);
        for (int i = 0; i < parameterTypes.length; i++) {
            List<Annotation> validationAnnotations = collectValidationAnnotations(parameterAnnotations[i]);
            if (validationAnnotations.size() > 0) {
                js.write(String.format("%1$s.%2$s.validations = ", prefix, parameterNames[i]));
                writeValidations(js, validationAnnotations, parameterTypes[i]);
                js.write(";").newline();
            }
        }
    }

    private static void writeValidations(PrettyPrinter js, List<Annotation> validationAnnotations, Class targetType) {
        if (validationAnnotations.size() > 0) {
            js.write("{").indent().newline();
            boolean firstValidation = true;
            for (Annotation annotation : validationAnnotations) {
                if (!firstValidation) {
                    js.write(",").newline();
                }
                js.write(String.format("%1$s: {", annotation.annotationType().getSimpleName())).indent().newline();
                try {
                    boolean firstProperty = true;
                    for (Method method : annotation.getClass().getMethods()) {
                        if (method.getParameterTypes().length == 0 && !Void.TYPE.equals(method.getReturnType())) {
                            String annotationPropertyName = method.getName();
                            if (!EXCLUDED_ANNOTATION_PROPERTIES.contains(annotationPropertyName)) {
                                if (!firstProperty)
                                    js.write(",").newline();
                                js.write(String.format("%1$s: %2$s",
                                        annotationPropertyName,
                                        JSON.serialize(method.invoke(annotation), false)));
                                firstProperty = false;
                            }
                        }
                    }
                    if (annotation instanceof AssertValid) {
                        if (!firstProperty)
                            js.write(",").newline();
                        js.write("targetValidations: {").indent().newline();
                        boolean firstChildProperty = true;
                        for (PropertyDescriptor descriptor : PropertyUtils.getPropertyDescriptors(targetType)) {
                            // Exclude indexed properties
                            if (descriptor.getPropertyType() != null) {
                                List<Annotation> childValidationAnnotations = collectValidationAnnotations(descriptor
                                        .getReadMethod().getAnnotations());
                                if (childValidationAnnotations.size() > 0) {
                                    if (!firstChildProperty)
                                        js.write(",").newline();
                                    js.write(String.format("%1$s: ", descriptor.getName()));
                                    writeValidations(js, childValidationAnnotations, descriptor.getPropertyType());
                                    firstChildProperty = false;
                                }
                            }
                        }
                        for (Field field : targetType.getDeclaredFields()) {
                            List<Annotation> childValidationAnnotations = collectValidationAnnotations(field
                                    .getAnnotations());
                            if (childValidationAnnotations.size() > 0) {
                                if (!firstChildProperty)
                                    js.write(",").newline();
                                js.write(String.format("%1$s: ", field.getName()));
                                writeValidations(js,
                                        collectValidationAnnotations(field.getAnnotations()),
                                        field.getType());
                                firstChildProperty = false;
                            }
                        }
                        js.outdent().write("}");
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Unable to write validations: " + e.getMessage());
                }
                js.outdent().newline().write("}");
                firstValidation = false;
            }
            js.outdent().newline().write("}");
        }
    }

    private static List<Annotation> collectValidationAnnotations(Annotation[] annotations) {
        List<Annotation> validationAnnotations = new ArrayList<Annotation>();
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().isAnnotationPresent(Constraint.class)) {
                validationAnnotations.add(annotation);
            }
        }
        return validationAnnotations;
    }
}
