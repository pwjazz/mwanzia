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
 * jquery - used for Ajax invocation (eventually we may support other frameworks as well)
 * json2.js - used to serialize and deserialize JSON from the server
 * date.format.js - used for date serialization and deserialization
 * typesystem.js - used to support the classical inheritance model used by many of
 * the objects in Mwanzia.
 * 
 */

mwanzia.Form = mwanzia.BindableClass.extend({
    init: function(submitCallback, initialData){
        console.debug("Initialing form with data", initialData);
        if (!submitCallback) 
            throw "submitCallback is required";
        if (submitCallback["argumentNames"] == null) 
            throw "submitCallback must include an argumentNames list";
        if (submitCallback["argumentTypes"] == null) 
            throw "submitCallback must include an argumentTypes list";
        this.submitCallback = submitCallback;
        this.data = initialData == null ? {} : initialData;
        this.validationRules = submitCallback.argumentValidators;
        if (!this.validationRules) 
            this.validationRules = {};
        this.was_submitted = false;
    },
    
    getData: function(key){
        console.debug("Getting value for " + key);
        return mwanzia.getFromMap(this.data, key);
    },
    
    setData: function(key, value){
        console.log("Setting " + key);
        mwanzia.setToMap(this.data, key, value);
    },
    
    submit: function(callbacks){
        if (this.was_submitted) {
            console.debug("Skipping submission of already submitted form");
        }
        else {
            console.debug("Submitting form");
            this.was_submitted = true;
            var args = {};
            for (var i in this.submitCallback.argumentNames) {
                var argumentName = this.submitCallback.argumentNames[i];
                var argumentType = this.submitCallback.argumentTypes[i];
                var argumentValue = mwanzia.getFromMap(this.data, argumentName);
                args[argumentName] = argumentValue;
            }
            console.debug("Form arguments: ", args);
            var successCallback = callbacks["success"];
            var exceptionCallback = callbacks["exception"];
            var errorCallback = callbacks["error"];
            if (!successCallback) 
                successCallback = function(response){
                    console.debug("Form submitted successfully with response:", response);
                }
            if (!exceptionCallback) 
                exceptionCallback = function(exception){
                    console.warn("Unhandled exception on submitting form:", exception);
                }
            try {
                console.debug("Constructing form arguments array");
                var argsArray = [];
                for (var i in this.submitCallback.argumentNames) {
                    var argumentName = this.submitCallback.argumentNames[i];
                    var argumentType = this.submitCallback.argumentTypes[i];
                    console.debug(argumentName, argumentType);
                    argsArray[i] = mwanzia.typeConvert(args[argumentName], argumentType);
                }
                console.debug("argsArray", argsArray);
                var response = this.submitCallback.apply(null, argsArray);
                if (response instanceof mwanzia.AjaxInvocation) {
                    response.success(successCallback);
                    response.exception(function(exception){
                        this.was_submitted = false;
                        exceptionCallback(exception);
                    });
                    if (errorCallback) 
                        response.error(errorCallback);
                    response.go();
                }
                else {
                    successCallback(response);
                }
            } 
            catch (exception) {
                exceptionCallback(exception);
				this.was_submitted = false;
            }
        }
    }
});

mwanzia.functionToForm = function(initialValues){
    console.debug("Creating form from: " + this);
    return new mwanzia.Form(this, initialValues);
}
