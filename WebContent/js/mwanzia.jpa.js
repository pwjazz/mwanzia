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

mwanzia.Reference = Class.extend({
    init: function(original){
        this["@class"] = "org.mwanzia.extras.jpa.Reference";
        this.targetClassName = original["@class"];
        this.id = original["id"];
    }
});


/**
 * Version of AjaxInvocation that performs lazy loading with getter methods
 * The first time that the getter is called, the value will be fetched remotely.
 * Subsequent calls to the getter will return the locally cached value, unless
 * clear() is called, which will clear any cached value.
 *
 * @param {Object} app
 * @param {Object} targetClass
 * @param {Object} target
 * @param {Object} methodName
 * @param {Object} suppliedArgs
 * @param {Object} propertyName
 */
mwanzia.LazyAjaxInvocation = mwanzia.AjaxInvocation.extend({
    init: function(app, targetClass, target, methodName, suppliedArgs, propertyName){
        this._propertyName = propertyName;
        this._originalTarget = target;
        if (!target.completelyTransferable) 
            target = target.byReference();
        this._super(app, targetClass, target, methodName, suppliedArgs);
    },
    
    success: function(callback){
        var originalTarget = this._originalTarget;
        var propertyName = this._propertyName;
        this._successCallback.register(function(value){
            console.debug("Initialized lazy property on ", originalTarget, propertyName);
            originalTarget[propertyName] = value;
            callback(value);
        });
        return this;
    },
    
    forceRemote: function(){
        this._originalTarget[this._propertyName] = null;
        return this;
    },
    
    go: function(){
        if (this._originalTarget[this._propertyName] != null) {
            console.debug("Using existing lazy property on ", this._originalTarget, this._propertyName);
            this._succeeded(this._originalTarget[this._propertyName]);
        }
        else {
            this._doInvoke();
        }
        return this;
    }
});

/**
 * RemoteObject is replaced with this version, which treats getter methods (methods beginning with "get" or "is")
 * as lazy loaded values.
 *
 * @param {Object} properties
 * @param {Object} className
 */
mwanzia.RemoteObject = mwanzia.RemoteObject.extend({
    init: function(properties, className){
        this._super(properties, className);
    },
    
    _remote: function(name, suppliedArgs){
        var propertyName = null;
        if (this[name].parameterOrder.length == 0) {
            if (name.indexOf("get") == 0) {
                propertyName = name.substring(3, 4).toLowerCase() + name.substring(4);
            }
            else 
                if (name.indexOf("is") == 0) {
                    propertyName = name.substring(2, 3).toLowerCase() + name.substring(3);
                }
            if (propertyName) {
                // It's a getter
                return new mwanzia.LazyAjaxInvocation(this._app, this['@class'], this, name, suppliedArgs, propertyName);
            }
        }
        return new mwanzia.AjaxInvocation(this._app, this['@class'], !this.completelyTransferable ? this.byReference() : this, name, suppliedArgs);
    },
    
    byReference: function(){
        return new mwanzia.Reference(this);
    },
    
    valueOf: function(){
        return this.id;
    }
});

mwanzia.Application.prototype._parseResponse = function(data) {
    // Use a custom reviver to handle references/cyclic references
    var originalJsonReviver = mwanzia.buildJsonReviver();
    var revivedObjectsByType = {};
    
    function jpaJsonReviver(key, target) {
        var result = target;
    	if (target != null) {
            var targetTypeName = target['@class'];
            if ("org.mwanzia.extras.jpa.Reference" == targetTypeName) {
                // This is a reference - replace it with an instanceof mwanzia.Reference
                return new mwanzia.Reference({"@class": target["targetClassName"], "id": target["id"]});
            } else {
                result = originalJsonReviver(key, target);
                if (result instanceof mwanzia.RemoteObject) {
                    var className = result["@class"];
                    var revivedObjectsById = revivedObjectsByType[className];
                    if (revivedObjectsById == null) {
                        revivedObjectsById = {};
                        revivedObjectsByType[className] = revivedObjectsById;
                    }
                    revivedObjectsById[result["id"]] = result;
                }
            }            
        }
    	return result;
    }

    function replaceReferences(object) {
        if (object == null) return;
        if (object instanceof mwanzia.Application) return;
        if (!(object instanceof Array) && typeof(object) != "object") return;
        for (var key in object) {
            var value = object[key];
            if (value instanceof mwanzia.Reference) {
                var targetTypeName = value.targetClassName;
                var revivedObjectsById = revivedObjectsByType[targetTypeName];
                if (revivedObjectsById != null) {
                    var revivedObject = revivedObjectsById[value.id];
                    if (revivedObject != null) {
                        object[key] = revivedObject;
                    }
                }
            } else {
                replaceReferences(value);
            }
        }
    }
    
    var result = JSON.parse(data, jpaJsonReviver);
    replaceReferences(result);
    return result;
};