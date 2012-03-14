/*!
 * Mwanzia Framework
 * http://code.google.com/p/mwanzia/
 *
 * Copyright (c) 2010 Percy Wegmann
 * Licensed under the MIT License
 * http://www.opensource.org/licenses/mit-license.php
 *
 * Mwanzia provides a framework for supporting transparent remoting from JavaScript to server-side Java objects.
 * 
 * Mwanzia has several external dependencies:
 * 
 * console.js - used as a logging stub if browser does not support logging
 * jquery - used for Ajax invocation (eventually we may support other frameworks as well)
 * json2.js - used to serialize and deserialize JSON from the server
 * date.format.js - used for date serialization and deserialization
 * typesystem.js - used to support the classical inheritance model used by many of
 * the objects in Mwanzia.
 * 
 */

// TODO: there shouldn't be references to mwanzia.Reference in here, since pass by reference semantics are for JPA only

/**
 * The mwanzia variable - all mwanzia types and functions are stored under this variable.
 * This includes class definitions for remote server-side objects.
 */
var mwanzia = {};

/**
 * Map of all apps
 */
mwanzia._apps = {};

/**
 * Configures Mwanzia to use default handlers for exceptions and/or errors
 * on calling remote methods. Exceptions are thrown by the server-side object,
 * whereas errors are lower-level problems usually having to do with the Ajax
 * transport.
 *
 * @param {Function} defaultExceptionHandler default handler for exceptions
 * @param {Function} defaultErrorHandler default handler for errors
 */
mwanzia.configure = function(defaultExceptionHandler, defaultErrorHandler){
    if (defaultExceptionHandler) {
        mwanzia._defaultHandlers.exception = defaultExceptionHandler;
    }
    if (defaultErrorHandler) {
        mwanzia._defaultHandlers.error = defaultErrorHandler;
    }
}

/**
 * Imports top level packages so that they can be referred to by name.
 * This includes all packages starting with com, org or net.
 * Also imports all applications by their name.
 */
function mwanziaImport() {
	var topLevelPackages = ["com", "org", "net"];
    for (var i=0; i<topLevelPackages.length; i++) {
    	var key = topLevelPackages[i];
    	var value = mwanzia[key];
    	if (typeof(value) != "undefined") {
	    	if (!this[key]) {
	    		console.debug("Importing " + key + " into current scope");
	            this[key] = mwanzia[key];
	    	} else {
	    		console.debug("Skipping import of " + key + " into current scope because it is already defined");
	    	}
    	}
    }
    for (var key in mwanzia._apps) {
    	if (!this[key]) {
    		console.debug("Importing app " + key + " into current scope");
            this[key] = mwanzia._apps[key];
    	} else {
    		console.debug("Skipping import of app " + key + " into current scope because it is already defined");
    	}
    }
}

/**
 * Imports the specified package into the current context, similar to an import statement
 * in Java. For example:
 *
 * mwanzia.importPackage(org.mwanzia.test)
 *
 * @param {Object} p
 */
mwanziaImportPackage = function(p){
	console.debug("Importing", p);
    for (var key in p) {
        if (!this[key]) 
            this[key] = p[key];
    }
}

/**
 * Utility function for adding the given array of argument names to
 * the specified function.
 *
 * @param {Function} fn
 * @param {Array} argumentNames
 */
mwanzia._addArgumentNames = function(fn, argumentNames){
    fn.argumentNames = argumentNames;
    return fn;
}

/**
 * Represents a remote Ajax call.  Clients can register three different callbacks:
 * 
 * success - called if the Ajax call succeeds
 * exception - called if the server-side method throws an exception
 * error - called if the Ajax transport experiences an error
 * 
 * An Ajax call is not actually processed until its go() method is called, at which point
 * it uses the configured Application to make the remote call and the asynchronously
 * triggers the correct callback based on the result.
 * 
 * @param {Object} app - the Application that will process this call
 * @param {String} targetClass - the class of the target server-side object
 * @param {Object} target - the target server-side object (null if calling a static method)
 * @param {String} methodName - the name of the server-side method being called
 * @param {Array} suppliedArgs - arguments to the method
 */
mwanzia.AjaxInvocation = Class.extend({
    init: function(app, targetClass, target, methodName, suppliedArgs){
        this._app = app;
        this._targetClass = targetClass;
        this._target = target;
        this._methodName = methodName;
        this._suppliedArgs = suppliedArgs;
        this._successCallback = new mwanzia.Future();
        this._errorCallback = new mwanzia.Future(mwanzia._defaultHandlers.error);
        this._exceptionCallback = new mwanzia.Future(mwanzia._defaultHandlers.exception);
        this._useNamedArgs = false;
    },
    
    useNamedArgs: function() {
    	this._useNamedArgs = true;
    	return this;
    },
    
    success: function(callback){
        this._successCallback.register(callback);
        return this;
    },
    
    error: function(callback){
        this._errorCallback.register(callback);
        return this;
    },
    
    exception: function(callback){
        this._exceptionCallback.register(callback);
        return this;
    },
    
    catchException: function(handlers, defaultHandler){
        this.exception(function(exception){
            var handled = false;
            for (var key in handlers) {
                if (key == exception['@class']) {
                    handled = true;
                    handlers[key](exception);
                    break;
                }
            }
            if (defaultHandler == null)
            	defaultHandler = mwanzia._defaultHandlers.exception;
            if (!handled) 
                defaultHandler(exception);
        });
        return this;
    },
    
    go: function(){
        return this._doInvoke();
    },
    
    _succeeded: function(result){
        this._successCallback.ready(result);
    },
    
    _errored: function(failure){
        this._errorCallback.ready(failure);
    },
    
    _excepted: function(exception){
        this._exceptionCallback.ready(exception);
    },
    
    _doInvoke: function(){
        this._app._invokeRemote(this._prepareCall(), this);
        return this;
    },
    
    _prepareCall: function(){
    	console.debug("Preparing call to remote method " + this._targetClass + "." + this._methodName, this._targetClass, this._target, this._suppliedArgs);
        var targetFunction = this._identifyTargetFunction();
        var args = this._prepareArgs(targetFunction);
        console.debug("args", args);
        console.debug("Building target data");
        var targetData = null;
        if (this._target) {
            for (var key in this._target) {
                if (this._target instanceof mwanzia.Reference || key == "@class" || key in this._target._transferableProperties) {
                    if (!targetData) 
                        targetData = {};
                    targetData[key] = this._target[key];
                }
            }
        }
        return {
            "@class": "org.mwanzia.Call",
            targetClass: this._targetClass,
            target: targetData,
            method: this._methodName,
            arguments: args
        }
    },
    
    _identifyTargetFunction: function() {
    	if (!this._target) {
            return mwanzia.getFromMap(mwanzia, this._targetClass + "." + this._methodName);
        }
        else {
            return mwanzia.getFromMap(mwanzia, this._targetClass + ".prototype." + this._methodName);
        }
    },
    
    _prepareArgs: function(targetFunction) {
    	console.debug("Collecting arguments");
        var args = [];
        var argsMap = this._buildArgsMap(targetFunction);
        for (var i in targetFunction.parameterOrder) {
        	var parameterName = targetFunction.parameterOrder[i];
        	args[i] = argsMap[parameterName];
        }
        return args;
    },
    
    _buildArgsMap: function(targetFunction) {
    	var argsMap = {};
    	// If the method requires more than 1 parameter and the parameters were passed as a map, assume call by name semantics
        console.debug("Using " + (this._useNamedArgs ? "named" : "positional") + " arguments");
    	for (var i in targetFunction.parameterOrder) {
        	var parameterName = targetFunction.parameterOrder[i];
        	var parameterInfo = targetFunction.parameters[parameterName];
            var argument = this._suppliedArgs[i];
            if (this._useNamedArgs) argument = this._suppliedArgs[0][parameterName];
            argsMap[parameterName] = this._buildArg(argument, parameterInfo);
        }
        return argsMap;
    },
    
    _buildArg: function(argument, parameterInfo) {
    	var parameterType = parameterInfo.type;
    	return mwanzia.typeConvert(argument, parameterType);
    }
});

/**
 * Base type for all classes whose functions are bindable, meaning that they are
 * implemented as delegates so that they can be passed around freely and invoked without
 * having to invoke them directly on this object.
 *
 * BindableClass accomplishes this by replacing all instances methods from its prototype with
 * Delegates on this.
 * 
 * BindableClass also makes all data from the data init parameter available directly under the
 * this object.
 *
 * @param {Object} data - initial data
 */
mwanzia.BindableClass = Class.extend({
    init: function(data){
        for (var key in data) {
            this[key] = data[key];
        }
        
        for (var key in this) {
            var value = this[key];
            if (typeof(value) == 'function') {
                this[key] = this._makeDelegate(value);
            }
        }
    },
    
    /**
     * Builds the actual delegate for a given function.  Can be overriden
     * by plugins if additional logic is needed on delegate.
     *
     * @param {Object} fn
     */
    _makeDelegate: function(fn){
        var thisObj = this;
        var result = function(){
            return fn.apply(thisObj, arguments);
        }
        for (var key in fn) {
            result[key] = fn[key]
        }
        return result;
    }
});

/**
 * Represents a remote server-side object with properties that can be access locally and
 * methods that can be invoked remotely.
 * 
 * All objects exported by the server derive from RemoteObject (perhaps transitively through
 * other classes).
 * 
 * @param {Object} properties
 * @param {Object} className
 */
mwanzia.RemoteObject = mwanzia.BindableClass.extend({
    init: function(properties, className){
        this["@class"] = className;
        for (property in properties) {
            this[property] = properties[property];
        }
        this._super();
    },
    
    _remote: function(name, suppliedArgs){
        return new mwanzia.AjaxInvocation(this._app, this['@class'], this, name, suppliedArgs);
    }
});

/**
 * Represents a future result. It is constructed with a callback that will be invoked
 * once the future result is ready (as indicated by someone calling the ready() method).
 *
 * @param {Function} callback to call once result is ready.  Accepts one argument, which
 * which is the result of this Future.
 */
mwanzia.Future = Class.extend({
    init: function(callback){
        this.result = null;
        this.resultReady = false;
        this.callback = callback;
    },
    
    ready: function(result){
        this.result = result;
        this.resultReady = true;
        if (this.callback) 
            this.callback(result);
    },
    
    /**
     * Registers a callback with this Future after it has already been constructed.
     * If the result is already ready, the callback will fire immediately.
     * 
     * @param {Function} callback
     */
    register: function(callback){
        this.callback = callback;
        if (this.resultReady) {
            this.callback(this.result);
        }
    }
});

/**
 * Represents a server-side Application.  All remote method invocation is dispatched via an Application.
 * 
 * @param {String} className - the java class name of this Application
 * @param {String} name - the logical name of this Application
 * @param {String} remoteUrl - the url at which this Application is reachable
 * @param {Object} properties - properties of the Application object
 */
mwanzia.Application = mwanzia.RemoteObject.extend({
    init: function(className, name, remoteUrl, properties){
        if (!properties) 
            properties = {};
        properties._app = this;
        properties._name = name;
        properties._remoteUrl = remoteUrl;
        this._super(properties, className);
    },
    
    _invokeRemote: function(call, result){
        console.debug("Making AJAX call");
        jQuery.ajax({
            url: this._remoteUrl,
            type: 'POST',
			dataType: "text",
            data: {
                application: this._name,
                call: mwanzia.stringify(call)
            },
            success: function(data, textStatus, xhr){
				data = JSON.parse(data, mwanzia.jsonReviver);
                if (data.exception) 
                    result._excepted(data.exception);
                else 
                    result._succeeded(data.result);
            },
            error: function(xhr, textStatus, error){
                result._errored(error);
            }
        });
        return result;
    }
});

/**
 * Regular expression for testing whether a String is an ISO8601 date.
 */
mwanzia.testDate = new RegExp("[0-9][0-9][0-9][0-9]\-[0-9][0-9]\-[0-9][0-9]T[0-9][0-9]:[0-9][0-9]:[0-9][0-9]\.[0-9][0-9][0-9][\+\-][0-9][0-9][0-9][0-9]");

mwanzia.stringify = function(obj) {
	return JSON.stringify(obj, function(key, value) {
		// Only serialize properties that don't begin with underscore (non-string keys are okay)
		if (typeof(key) != "string" || key.indexOf("_") != 0) return value;
		// Otherwise, don't return (i.e. return undefined), which causes JSON to exclude this key/value
	});
}

/**
 * A JSON2 reviver that replaces remote objects with actual instances of the right class
 * during deserialization.  Also handles converting date strings into actual dates.
 * 
 * @param {String} key - unused
 * @param {Object} target - the object that's potentially replaced
 */
mwanzia.jsonReviver = function(key, target) {
	var result = target;
    if (target != null) {
        var targetTypeName = target['@class'];
        
        if (targetTypeName) {
            var targetType = mwanzia.getFromMap(mwanzia, targetTypeName);
            if (targetType == null) 
                return target;
            result = new targetType(target);
        }
        else
	        if (typeof(target.match) != "undefined") {
	            // It's a string, see if we can convert to a date
	            if (mwanzia.testDate.exec(target) != null) {
	                result = new Date();
	                result.setISO8601(target);
	            }
	        }
    }
    return result;
}

/**
 * Converts plain JavaScript objects into a form ready for serialization to the server.
 * This is done based on the known types that are expected on the server.
 * 
 * For example, if the server expects a Long but we're passing the string "5", it will
 * be parsed into the number 5.
 */
mwanzia.typeConvert = function(value, targetType){
    console.debug("Converting object to type", value, targetType);
    if (value instanceof mwanzia.Application)
    	return null;
    if (value == null) 
        return null;
    if (value instanceof mwanzia.Reference) 
        return value;
    var conversionType = null;
    if (typeof(value) == "string") 
        conversionType = "string";
	if (typeof(value) == "number") {
		conversionType = "number";
		if (value != null) {
			var potentialRemoteObject = mwanzia.getFromMap(mwanzia, targetType);
			if (potentialRemoteObject != null) {
				// Convert number to remote object with ID
				return new mwanzia.Reference({"@class": targetType, id: value});
			}
		}
	}
	if (typeof(value) == "object") 
        conversionType = "object";
    console.debug("Conversion type", conversionType);
    if (conversionType == null) 
        return value;
    var conversion = mwanzia.typeConversions[conversionType][targetType];
    if (!conversion) 
        conversion = mwanzia.typeConversions[conversionType]['default'];
    var converted = conversion(value, targetType);
    console.debug("Converted", converted);
    return converted;
}

mwanzia.typeConverters = {
    integer: function(value){
        return value == null ? value : parseInt(value);
    },
    decimal: function(value){
        return value == null ? value : parseFloat(value);
    },
    noop: function(value){
        return value;
    },
    string: function(value){
        return value == null ? null : '' + value;
    }
}

mwanzia.typeConversions = {
    string: {
        "java.lang.Long": mwanzia.typeConverters.integer,
        "java.lang.Integer": mwanzia.typeConverters.integer,
        "java.lang.Byte": mwanzia.typeConverters.integer,
        "java.lang.Short": mwanzia.typeConverters.integer,
        "java.math.BigInteger": mwanzia.typeConverters.integer,
        "java.lang.Float": mwanzia.typeConverters.decimal,
        "java.lang.Double": mwanzia.typeConverters.decimal,
        "java.lang.BigDecimal": mwanzia.typeConverters.decimal,
        "default": mwanzia.typeConverters.noop
    },
	number: {
		"java.lang.String": function(number) {return number == null ? null : '' + number},
		"default": mwanzia.typeConverters.noop
	},
    object: {
        "default": function(value, targetType){
			if (value == null) 
                return null;
			var result = {};
            var typeDescriptor = mwanzia.typedescriptors[targetType];
            result["@class"] = targetType;
            for (var key in value) {
                if (key != null) {
                    console.debug(key);
					var childType = null;
					if (typeDescriptor) childType = typeDescriptor.propertyTypes[key];
                    result[key] = mwanzia.typeConvert(value[key], childType);
                }
            }
            return result;
        }
    }
}

/**
 * The default handlers for asynchronous exceptions and errors.
 *
 * @param {Object} exception
 */
mwanzia._defaultHandlers = {
    exception: function(exception){
        console.error("Unhandled exception", exception);
    },
    
    error: function(error){
        console.error("Unhandled error", error);
    }
}

/**
 * Adds a class to the Mwanzia system under the given name (which can include dot
 * notation, like org.mwanzia.test.TestApplication).
 *
 * @param {String} name (in dot notation)
 * @param {Object} clazz (the class definition)
 */
mwanzia.addClass = function(name, clazz){
	console.debug("Adding class " + name, clazz);
    mwanzia.setToMap(mwanzia, name, clazz);
};

/**
 * Constructs a delegate.  A delegate is like a function pointer that remembers the
 * "this" object of the original function.
 *
 * The parameters are the same as for the Function constructor,
 * except that before those arguments, you specify the object to be used as "this"
 * when calling the function.
 *
 * @param {Object} thisObj - object to use as "this" when invoking the delegate
 * @param {Object} argName1 - first argument name
 * @param {Object} argName2 - second argument name
 * @param {Object} etc - and so on
 * @param {Object} functionBody
 */
mwanzia.delegate = function(thisObj, argName1, argName2, etc, functionBody){
    var args = [arguments.length - 1];
    var argumentNames = [arguments.length - 2]
    for (var i = 1; i < arguments.length; i++) {
        args[i - 1] = arguments[i];
        if (i < arguments.length - 1) {
            argumentNames[i - 1] = arguments[i];
        }
    }
    var delegateFunction = Function.apply(null, args);
    var result = function(){
        return delegateFunction.apply(thisObj, arguments);
    };
    return mwanzia._addArgumentNames(result, argumentNames);
}

/**
 * Utility function for determining whether or not something is an array
 *
 * @param {Object} obj
 */
mwanzia.isArray = function(obj){
    return obj.constructor.toString().indexOf("Array") >= 0;
}

/**
 * Utility function for adding something to a nested map using dot notation.  Nested
 * maps (objects) will be created as needed.
 *
 * @param {Object} map
 * @param {Object} compositeKey
 * @param {Object} value
 */
mwanzia.setToMap = function(map, compositeKey, value){
	var current = map;
    var splitKey = compositeKey.split(".");
    for (var i in splitKey) {
        var key = splitKey[i];
        if (i == splitKey.length - 1) {
        	current[key] = value;
        }
        else {
            var childMap = current[key];
            if (!childMap) {
                childMap = {};
                current[key] = childMap;
            }
            current = childMap;
        }
    }
}

/**
 * Utility function for getting an item from a nested map using dot notation.
 *
 * For example, "org.mwanzia.test".
 *
 * @param {Object} map
 * @param {Object} compositeKey
 */
mwanzia.getFromMap = function(map, compositeKey){
    var current = map;
    var splitKey = compositeKey.split(".");
    for (var i in splitKey) {
        var key = splitKey[i];
        current = current[key];
        if (current == null) 
            return current;
    }
    return current;
}

/**
 * Adds a function to Date that populates it from an ISO8601 string (as sent by the server).
 * 
 * Courtesy of Dan at Dan's network - http://dansnetwork.com/2008/11/01/javascript-iso8601rfc3339-date-parser/
 * 
 * @param {Object} dString
 */
Date.prototype.setISO8601 = function(dString){
    var regexp = /(\d\d\d\d)(-)?(\d\d)(-)?(\d\d)(T)?(\d\d)(:)?(\d\d)(:)?(\d\d)(\.\d+)?(Z|([+-])(\d\d)(:)?(\d\d))/;
    if (dString.toString().match(new RegExp(regexp))) {
        var d = dString.match(new RegExp(regexp));
        var offset = 0;
        this.setUTCDate(1);
        this.setUTCFullYear(parseInt(d[1], 10));
        this.setUTCMonth(parseInt(d[3], 10) - 1);
        this.setUTCDate(parseInt(d[5], 10));
        this.setUTCHours(parseInt(d[7], 10));
        this.setUTCMinutes(parseInt(d[9], 10));
        this.setUTCSeconds(parseInt(d[11], 10));
        if (d[12]) 
            this.setUTCMilliseconds(parseFloat(d[12]) * 1000);
        else 
            this.setUTCMilliseconds(0);
        if (d[13] != 'Z') {
            offset = (d[15] * 60) + parseInt(d[17], 10);
            offset *= ((d[14] == '-') ? -1 : 1);
            this.setTime(this.getTime() - offset * 60 * 1000);
        }
    }
    else {
        this.setTime(Date.parse(dString));
    }
    return this;
};

/**
 * Defines a toJSON function on Date that serializes it to an ISO8601 formatted string.
 * This will get invoked automatically by the JSON2 processor when it stringifies the Date.
 */
(function(){
    function pad(n, digits){
        var result = '' + n;
        while (result.length < digits) 
            result = '0' + result;
        return result;
    }
    
    Date.prototype.toJSON = function(key){
        return isFinite(this.valueOf()) ? this.getUTCFullYear() + '-' +
        pad(this.getUTCMonth() + 1, 2) +
        '-' +
        pad(this.getUTCDate(), 2) +
        'T' +
        pad(this.getUTCHours(), 2) +
        ':' +
        pad(this.getUTCMinutes(), 2) +
        ':' +
        pad(this.getUTCSeconds(), 2) +
        '.' +
        pad(this.getUTCMilliseconds(), 3) +
        '+0000' : null;
    }
}());
