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

mwanzia.AjaxInvocation.prototype.validate = function(call) {
	if (!call) call = this._prepareCall();
	var targetFunction = this._identifyTargetFunction();
	var validationContext = new mwanzia.ValidationContext();
	for (var i=0; i<targetFunction.parameterOrder.length; i++) {
		var name = targetFunction.parameterOrder[i];
		var argument = call.arguments[i];
		var parameterInfo = targetFunction.parameters[name];
		var validations = parameterInfo.validations;
		console.debug("Validating parameter " + name + " with info", parameterInfo)
		if (typeof(validations) != "undefined") {
			validationContext.validate(name, argument, validations);
		}
	}
	if (validationContext.errors.length > 0)
		return validationContext.errors;
	else
		return null;
};

mwanzia.AjaxInvocation.prototype._doInvoke = function() {
	var call = this._prepareCall();
	var errors = this.validate(call);
	if (errors) {
		console.debug("Validation failed, throwing exception");
		var exception = new mwanzia.org.mwanzia.extras.validation.ValidationException({
			errors: errors
		});
		console.debug(exception);
		this._excepted(exception);
	} else {
		this._app._invokeRemote(call, this);
	}
	return this;
};

mwanzia.ValidationContext = Class.extend({
	init: function() {
		this.errors = [];
	},
	
	validate: function(fieldName, value, validations) {
		for (validationType in validations) {
			console.debug("Validating parameter " + fieldName + " for " + validationType);
			var validator = mwanzia._getValidator(validationType);
			var validationConfig = validations[validationType];
			console.debug(validationConfig);
			var errors = this.errors;
			function errorReporter(messageVariables) {
				console.debug("Validating parameter " + fieldName + " for " + validationType + " failed");
				if (!messageVariables) messageVariables = {};
				errors.push(new mwanzia.org.mwanzia.extras.validation.ValidationError({
					fieldName: fieldName,
					errorCode: validationConfig.errorCode,
					message: validationConfig.message, // todo: look this up from resource bundle
					messageVariables: messageVariables
				}));
			}
			validator(this, validationConfig, name, value, errorReporter);
		}
	}
});

mwanzia._getValidator = function(validationType) {
	var validator = mwanzia._validators[validationType];
	if (typeof(validator) == "undefined") {
		console.warn("No validator found for validation type " + validationType);
		validator = mwanzia._noopValidator;
	}
	return validator;
};

mwanzia._noopValidator = function() {};

mwanzia._validators = {
	Required: function(validationContext, config, fieldName, value, errorReporter) {
		if (typeof(value) == "undefined" || value == null || (typeof(value) == "string" && value.length == 0)) {
			errorReporter();
		}
	},
	
	NotNull: function(validationContext, config, fieldName, value, errorReporter) {
		if (typeof(value) == "undefined" || value == null) {
			errorReporter();
		}
	},
	
	NotEmpty: function(validationContext, config, fieldName, value, errorReporter) {
		if (typeof(value) == "string" && value.length == 0) {
			errorReporter();
		}
	},
	
	Length: function(validationContext, config, fieldName, value, errorReporter) {
		if (typeof(value) == "string" && value != null) {
			console.debug(value.length);
			if (config.min && value.length < config.min) {
				errorReporter();
			}
			if (config.max && value.length > config.max) {
				errorReporter();
			}
		}
	},
	
	AssertValid: function(validationContext, config, fieldName, value, errorReporter) {
		for (subField in config.targetValidations) {
			var subValue = value[subField];
			var compositeName = fieldName + "." + subField;
			var validations = config.targetValidations[subField];
			validationContext.validate(compositeName, subValue, validations);
		}
	}
};
