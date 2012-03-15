package org.mwanzia;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.codehaus.jackson.map.util.StdDateFormat;
import org.mwanzia.SmallPropertyUtils.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.paranamer.BytecodeReadingParanamer;
import com.thoughtworks.paranamer.Paranamer;

/**
 * <p>
 * Defines a set of domain objects that are able to be accessed remotely by a
 * JavaScript client using mwanzia. To create a new Application, simply extend
 * this class and register the remotable classes by calling registerRemote().
 * Applications can use plugins by calling registerPlugin().
 * <p>
 * 
 * @author percy
 * 
 */
public abstract class Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    private String name;
    private boolean whitelistProperties;
    private Set<Class> remoteTypes = new HashSet<Class>();
    private List<Plugin> plugins = new ArrayList<Plugin>();

    /**
     * Construct a new Application identified by the given name. Amongst other
     * things, this defines the name of the variable under which the application
     * will be available in JavaScript.
     * 
     * @param name
     */
    protected Application(String name, boolean whitelistProperties) {
        this.name = name;
        this.whitelistProperties = whitelistProperties;
    }

    /**
     * Construct a new Application using its unqualified class name as the name.
     */
    protected Application(boolean whitelistProperties) {
        this.name = this.getClass().getSimpleName();
        this.whitelistProperties = whitelistProperties;
    }

    /**
     * Construct a new Application using its unqualified class name as the name.
     */
    protected Application() {
        this(true);
    }

    /**
     * Register a class that is eligible for remoting withing the context of
     * this Application.
     * 
     * @param remoteType
     */
    protected void registerRemote(Class remoteType) {
        this.remoteTypes.add(remoteType);
    }

    /**
     * Register a plugin to provide additional functionality to Mwanzia (only
     * for this Application). Remember to include any plugin-specific JavaScript
     * files on the client.
     * 
     * @param plugin
     */
    protected void registerPlugin(Plugin plugin) {
        this.plugins.add(plugin);
        for (Class remoteType : plugin.getRemoteTypes()) {
            this.registerRemote(remoteType);
        }
    }

    String getName() {
        return name;
    }

    /**
     * <p>
     * Handles a remote invocation, supplied in the form of JSON.
     * </p>
     * 
     * @param callString
     *            JSON structure defining the remote call (target, method,
     *            parameters, etc.)
     * @return
     * @throws Throwable
     */
    String invoke(String targetClassName, String methodName, String callString) throws Exception {
        Class targetClass = this.getClass().getClassLoader().loadClass(targetClassName);
        Method method = null;
        for (Method candidate : targetClass.getMethods()) {
            if (candidate.getName().equals(methodName)) {
                method = candidate;
                break;
            }
        }
        if (method == null)
            throw new MwanziaException(String.format("No method %1$s found on type %2$s",
                    methodName,
                    targetClass.getName()));
        if (!isRemotelyExecutable(method))
            throw new MwanziaException(String.format("Method %1$s on type %2$s is not remotely executable",
                    methodName,
                    targetClass.getName()));

        List<Interceptor> interceptors = new ArrayList<Interceptor>();
        for (Plugin plugin : plugins) {
            interceptors.add(plugin.buildInterceptor());
        }
        for (Interceptor interceptor : interceptors) {
            interceptor.beforeInvocation(targetClass, method);
        }
        Call call = JSON.deserialize(callString, Call.class);
        Object target = call.getTarget();
        List arguments = call.getArguments();

        final Iterator<Plugin> pluginIterator = plugins.iterator();
        Class[] argumentTypes = method.getParameterTypes();
        Object[] coercedArguments = new Object[arguments.size()];
        for (int i = 0; i < arguments.size(); i++) {
            Object argument = arguments.get(i);
            coercedArguments[i] = coerce(argument, argumentTypes[i]);
        }
        Map<String, Object> resultMap = new LinkedHashMap<String, Object>();
        resultMap.put("result", null);
        resultMap.put("exception", null);
        try {
            for (Interceptor interceptor : interceptors) {
                if (target != null)
                    target = interceptor.replaceTarget(target);
                coercedArguments = interceptor.prepareInvocation(target, method, coercedArguments);
            }
            Object result = method.invoke(target, coercedArguments);
            if (result != null) {
                for (Interceptor interceptor : interceptors) {
                    result = interceptor.replaceResult(result);
                }
            }
            for (Interceptor interceptor : interceptors) {
                interceptor.invocationSucceeded(target, result);
            }
            resultMap.put("result", result);
        } catch (Throwable exception) {
            if (exception instanceof InvocationTargetException) {
                exception = exception.getCause();
            }
            for (Interceptor interceptor : interceptors) {
                exception = interceptor.invocationFailed(exception);
            }
            LOGGER.info("Returning exception from {}", method, exception);
            resultMap.put("exception", exception);
        }
        return JSON.serialize(resultMap, whitelistProperties);
    }

    String coreJavaScript() throws Exception {
        final PrettyPrinter js = new PrettyPrinter();
        // Write out application class
        writeClass(js, this.getClass(), "Application", new Runnable() {
            @Override
            public void run() {
                js.write("init: function(name, remoteUrl, properties){").indent().newline();
                js.write(String.format("this._super('%1$s', name, remoteUrl, properties);", Application.this.getClass()
                        .getName())).outdent().newline();
                js.write("}");
            }
        });
        Set<Class> allClasses = collectAllClasses();
        Set<Class> types = new HashSet<Class>();
        for (Class clazz : allClasses) {
            final String className = clazz.getName();
            js.newline();
            String baseClassName = "RemoteObject";
            if (allClasses.contains(clazz.getSuperclass()))
                baseClassName = clazz.getSuperclass().getName();
            final Class finalClazz = clazz;
            writeClass(js, clazz, baseClassName, new Runnable() {
                @Override
                public void run() {
                    js.write("init: function(properties, className){").indent().newline();
                    js.write("if (!properties) properties = {};").newline();
                    js.write(String.format("properties._app = mwanzia._apps.%1$s;", name)).newline();
                    js.write(String.format("if (!className) className = '%1$s';", className)).newline();
                    if (finalClazz.isAnnotationPresent(Transferable.class)) {
                        js.write(String.format("properties.completelyTransferable = true;")).newline();
                    }
                    js.write("this._super(properties, className);").outdent().newline();
                    js.write("}");
                }
            });
            collectTypes(types, clazz);

        }
        writeTypeDescriptors(js, types);
        return js.toString();
    }

    String javaScriptInstance(String servletUrl) throws Exception {
        // Instantiate application (including properties)
        Map<Object, Object> properties = new LinkedHashMap<Object, Object>();
        for (Property descriptor : SmallPropertyUtils.getProperties(this.getClass()).values()) {
            properties.put(descriptor.name, descriptor.read(this));
        }
        return String.format("mwanzia._apps.%1$s = new mwanzia.%2$s('%3$s', '%4$s', %5$s);", this.name, this.getClass()
                .getName(), name, servletUrl, JSON.serialize(properties, whitelistProperties));
    }

    private void writeClass(PrettyPrinter js, Class clazz, String baseClassName, Runnable constructor) {
        js.write(String.format("mwanzia.addClass('%1$s', mwanzia.%2$s.extend({", clazz.getName(), baseClassName))
                .indent().newline();
        constructor.run();
        Set<String> unavailableMethodNames = new HashSet<String>();
        Set<Method> instanceMethods = new HashSet<Method>();
        Set<Method> staticMethods = new HashSet<Method>();
        // Collect methods
        for (Method method : clazz.getDeclaredMethods()) {
            if (isRemotelyExecutable(method)) {
                String methodName = method.getName();
                if (unavailableMethodNames.contains(methodName))
                    throw new MwanziaError(
                            String.format("Detected multiple methods named '%1$s' on %2$s. Overloading of remote methods is not currently supported.",
                                    methodName,
                                    clazz.getName()));
                unavailableMethodNames.add(methodName);
                if (Modifier.isStatic(method.getModifiers())) {
                    // Remember it for later
                    staticMethods.add(method);
                } else {
                    instanceMethods.add(method);
                }
            }
        }
        // Collect transferableProperties
        Set<String> transferableProperties = new HashSet<String>();
        for (Property descriptor : SmallPropertyUtils.getProperties(clazz).values()) {
            if (clazz.isAnnotationPresent(Transferable.class)
                    || (descriptor.isReadable() && descriptor.readMethod
                            .isAnnotationPresent(Transferable.class))
                    || (descriptor.isWriteable() && descriptor.writeMethod
                            .isAnnotationPresent(Transferable.class))) {
                transferableProperties.add(descriptor.name);
            }
        }
        writeInstanceMethods(js, clazz, instanceMethods);
        js.write(",").newline();
        writeTransferableProperties(js, clazz, transferableProperties);
        Paranamer paranamer = new BytecodeReadingParanamer();
        writeInstanceMethodParameterInfo(js, clazz, instanceMethods, paranamer);
        writeStaticMethods(js, clazz, staticMethods, paranamer);
        writeStaticMethodParameterInfo(js, clazz, staticMethods, paranamer);
        for (Plugin plugin : plugins) {
            plugin.postProcessClass(js, clazz, instanceMethods, staticMethods, transferableProperties);
        }
        js.newline();
    }

    private void writeInstanceMethods(PrettyPrinter js, Class clazz, Set<Method> instanceMethods) {
        for (Method method : instanceMethods) {
            js.write(",").newline();
            js.write(String.format("%1$s: function() { return this._remote('%2$s', arguments); }",
                    method.getName(),
                    method.getName()));
        }
    }

    private void writeTransferableProperties(PrettyPrinter js, Class clazz, Set<String> transferableProperties) {
        js.write("_transferableProperties: {");
        boolean first = true;
        for (String transferableProperty : transferableProperties) {
            if (!first)
                js.write(", ");
            js.write(String.format("'%1$s':''", transferableProperty));
            first = false;
        }
        js.write("}");
        for (Plugin plugin : plugins) {
            plugin.enhanceRemoteObject(js, clazz);
        }
        js.outdent().newline().write("}));").newline();
    }

    private void writeInstanceMethodParameterInfo(PrettyPrinter js, Class clazz, Set<Method> instanceMethods,
            Paranamer paranamer) {
        writeMethodParameterInfo(js,
                String.format("mwanzia.%1$s.prototype", clazz.getName()),
                instanceMethods,
                paranamer);
    }

    private void writeStaticMethodParameterInfo(PrettyPrinter js, Class clazz, Set<Method> staticMethods,
            Paranamer paranamer) {
        writeMethodParameterInfo(js, String.format("mwanzia.%1$s", clazz.getName()), staticMethods, paranamer);
    }

    private void writeMethodParameterInfo(PrettyPrinter js, String prefix, Set<Method> methods, Paranamer paranamer) {
        for (Method method : methods) {
            js.write(String.format("%1$s.%2$s.parameters = {", prefix, method.getName()));
            boolean firstParameter = true;
            String[] parameterNames = paranamer.lookupParameterNames(method);
            Class[] parameterTypes = method.getParameterTypes();
            js.indent();
            for (int i = 0; i < parameterNames.length; i++) {
                if (!firstParameter)
                    js.write(",");
                js.newline();
                js.write(String.format("%1$s: {type: '%2$s'}", parameterNames[i], parameterTypes[i].getName()));
                firstParameter = false;
            }
            js.outdent().newline().write("};").newline();
            js.write(String.format("%1$s.%2$s.parameterOrder = [", prefix, method.getName()));
            firstParameter = true;
            for (String parameterName : parameterNames) {
                if (!firstParameter)
                    js.write(", ");
                js.write(String.format("'%1$s'", parameterName));
                firstParameter = false;
            }
            js.write("];").newline();
        }
    }

    private void writeStaticMethods(PrettyPrinter js, Class clazz, Set<Method> staticMethods, Paranamer paranamer) {
        for (Method method : staticMethods) {
            js.write(String.format("mwanzia.%1$s.%2$s = mwanzia.delegate(null, %3$s\"return new mwanzia.AjaxInvocation(%4$s, '%5$s', null, '%6$s', arguments)\")",
                    clazz.getName(),
                    method.getName(),
                    buildParameterNames(paranamer, method),
                    name,
                    clazz.getName(),
                    method.getName())).newline();
        }
    }

    private void writeTypeDescriptors(PrettyPrinter js, Set<Class> types) {
        js.write("mwanzia.typedescriptors = {").indent().newline();
        boolean firstType = true;
        for (Class type : types) {
            if (!firstType)
                js.write(", ").newline();
            js.write(String.format("\"%1$s\": {", type.getName())).indent().newline();
            js.write(String.format("propertyTypes: {")).indent().newline();
            boolean firstPropertyType = true;
            for (Property descriptor : SmallPropertyUtils.getProperties(type).values()) {
                if (!firstPropertyType)
                    js.write(", ").newline();
                // exclude indexed properties
                if (descriptor.getPropertyType() != null) {
                    js.write(String.format("\"%1$s\": \"%2$s\"", descriptor.name, descriptor.getPropertyType()
                            .getName()));
                    firstPropertyType = false;
                }
            }
            js.outdent().newline().write("}");
            for (Plugin plugin : plugins) {
                plugin.addToTypeDescriptor(js, type);
            }
            js.outdent().newline().write("}");
            firstType = false;
        }
        js.outdent().newline().write("}").newline();
    }

    private String buildParameterNames(Paranamer paranamer, Method method) {
        StringBuilder parameterNames = new StringBuilder();
        String[] lookedUpParameterNames = paranamer.lookupParameterNames(method);
        for (int i = 0; i < lookedUpParameterNames.length; i++) {
            parameterNames.append("'");
            parameterNames.append(lookedUpParameterNames[i]);
            parameterNames.append("'");
            parameterNames.append(", ");
        }
        return parameterNames.toString();
    }

    private Set<Class> collectAllClasses() {
        List<List<Class>> hierarchies = new ArrayList<List<Class>>();
        for (Class clazz : remoteTypes) {
            hierarchies.add(buildClassHierarchy(clazz));
        }
        Set<Class> result = new TreeSet<Class>(new Comparator<Class>() {
            @Override
            public int compare(Class o1, Class o2) {
                if (o1 == o2)
                    return 0;
                else if (o2.isAssignableFrom(o1))
                    return 1;
                else if (o1.isAssignableFrom(o2))
                    return -1;
                else
                    return o1.getName().compareTo(o2.getName());
            }
        });
        for (List<Class> hierarchy : hierarchies) {
            result.addAll(hierarchy);
        }
        return result;
    }

    private List<Class> buildClassHierarchy(Class clazz) {
        List<Class> hierarchy = new ArrayList<Class>();
        Class current = clazz;
        while (current != null && !Object.class.equals(current)) {
            hierarchy.add(current);
            current = current.getSuperclass();
        }
        return hierarchy;
    }

    private static Object coerce(Object result, Class expectedType) {
        if (expectedType != null && result != null && !expectedType.isAssignableFrom(result.getClass())) {
            for (TypeCoercer<? extends Object, ? extends Object> coercer : TYPE_COERCERS) {
                if (coercer.inputType.equals(result.getClass()) && coercer.outputType.equals(expectedType)) {
                    return ((TypeCoercer<Object, Object>) coercer).coerce(result);
                }
            }
            if (!expectedType.isPrimitive())
                throw new MwanziaException(
                        String.format("Error while coercing argument. Object of type %1$s cannot be used in place of %2$s",
                                result.getClass().getName(),
                                expectedType.getName()));
        }
        return result;
    }

    protected static abstract class TypeCoercer<I, O> {
        public Class<I> inputType;
        public Class<O> outputType;

        public TypeCoercer(Class<I> inputType, Class<O> outputType) {
            this.inputType = inputType;
            this.outputType = outputType;
        }

        public abstract O coerce(I input);
    }

    protected static final List<TypeCoercer<? extends Object, ? extends Object>> TYPE_COERCERS = new ArrayList<TypeCoercer<? extends Object, ? extends Object>>();

    static {
        TYPE_COERCERS.add(new TypeCoercer<Integer, Long>(Integer.class, Long.class) {
            @Override
            public Long coerce(Integer input) {
                return new Long(input);
            }
        });
        TYPE_COERCERS.add(new TypeCoercer<String, Date>(String.class, Date.class) {
            @Override
            public Date coerce(String input) {
                try {
                    return input == null ? null : StdDateFormat.getBlueprintISO8601Format().parse(input);
                } catch (ParseException pe) {
                    throw new MwanziaException(String.format("Unable to parse date string '%1$s'", input), pe);
                }
            }
        });
        TYPE_COERCERS.add(new TypeCoercer<String, char[]>(String.class, char[].class) {
            @Override
            public char[] coerce(String input) {
                return input.toCharArray();
            }
        });
    }

    private Set<Class> collectTypes(Set<Class> types, Class type) {
        if (type == null || types.contains(type))
            return types;
        for (Property descriptor : SmallPropertyUtils.getProperties(type).values()) {
            LOGGER.debug("Collecting type descriptor for property {} on class {}", descriptor.name, type.getName());
            // This excludes indexed properties
            if (descriptor.getPropertyType() != null) {
                // Add the property
                types.add(descriptor.getPropertyType());
                // Recursively collect descriptors for the property type
                collectTypes(types, descriptor.getPropertyType());
            }
        }
        // Make sure we're also collecting descriptors for all remotable methods
        for (Method method : type.getMethods()) {
            if (isRemotelyExecutable(method)) {
                for (Class parameterType : method.getParameterTypes()) {
                    collectTypes(types, parameterType);
                }
            }
        }
        return types;
    }

    private boolean isRemotelyExecutable(Method method) {
        Remote annotation = method.getAnnotation(Remote.class);
        boolean remotelyExecutable = annotation != null;
        if (remotelyExecutable) {
            String[] allowedApplications = annotation.applications();
            if (allowedApplications != null && allowedApplications.length > 0) {
                boolean applicationFound = false;
                for (String allowedApplication : allowedApplications) {
                    if (allowedApplication.equals(this.name)) {
                        applicationFound = true;
                        break;
                    }
                }
                remotelyExecutable = applicationFound;
            }
        }
        return remotelyExecutable;
    }
}
