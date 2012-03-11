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

(function($){
	var MwanziaPlugin = function(selected) {
	   	this.listFrom = function(){
            var target = selected;
            return function(bound){
                target.list(bound);
            }
        }
        
		this.list = function(bound){
            return selected.mwanzia().bind(bound, "list");
        }
		
        this.form = function(bound){
            return selected.mwanzia().bind(bound, "form");
        }
		
        this.bind = function(bound, command){
            return selected.each(function(){
                var node = $(this);
                node.mwanzia().bound(bound);
                // Unbind standard events
                node.unbind("mwanzia_setting");
                node.unbind("mwanzia_set");
                node.unbind("mwanzia_submitted");
                // Bind standard events
                var setting = node.attr("mwanzia:onsetting");
                var set = node.attr("mwanzia:onset");
                var submitted = node.attr("mwanzia:onsubmitted");
                if (setting) 
                    node.bind("mwanzia_setting", function(event, invocation){
                        mwanzia.delegate(node, "invocation", setting)(invocation);
                    });
                if (set) 
                    node.bind("mwanzia_set", function(event, result){
                        mwanzia.delegate(node, "result", set)(result);
                    });
                if (submitted) 
                    node.bind("mwanzia_submitted", function(event, result){
                        mwanzia.delegate(node, "result", submitted)(result);
                    });
                // Figure out command and handle first
                if (!command) {
                    if (node.attr("mwanzia:set")) {
                        command = "set";
                    }
                    else 
                        if (node.attr("mwanzia:list")) {
                            command = "list";
                        }
                        else 
                            if (node.attr("mwanzia:push")) {
                                command = "push";
                            }
                            else 
                                if (node.attr("mwanzia:form")) {
                                    command = "form";
                                }
                }
                var needToBindChildren = true;
                if ("set" == command) {
                    // Set the inner HTML
                    var expression = node.attr("mwanzia:set");
                    extract(node, bound, function(result){
                        console.debug("Binding expression, from, to ", expression, result, node);
                        node.mwanzia().set(result);
                        node.html(result);
                    }, expression);
                    needToBindChildren = false;
                }
                else 
                    if ("list" == command) {
                        // Iterate over a list of items using the first child as a template
                        var expression = node.attr("mwanzia:list");
                        extract(node, bound, function(result){
                            node.mwanzia().set(result);
                            var template = node.mwanzia().data("listTemplate");
                            if (!template) {
                                template = node.children().first();
                                node.mwanzia().data("listTemplate", template);
                            }
                            node.children().remove();
                            if (result != null) {
                                if (!isArray(result)) {
                                    // Convert map to list
                                    var list = [];
                                    for (var key in result) {
                                        list.push({
                                            key: key,
                                            value: result[key]
                                        });
                                    }
                                    result = list;
                                }
                                for (var i in result) {
									var item = template.clone();
					                item.mwanzia().bind(result[i]);
					                node.append(item);
                                }
                            }
                        }, expression);
                        needToBindChildren = false;
                    }
                    else 
                        if ("form" == command) {
                            var expression = node.attr("mwanzia:form");
                            extract(node, bound, function(form){
                                if (!(form instanceof mwanzia.Form)) {
                                    console.error(form + " is not a mwanzia.Form.  Only Forms can be bound to an HTML form element using mwanzia:form");
                                }
                                else {
                                    console.debug("Binding form to UI");
                                    node.mwanzia().set(form);
                                    node.find(":input").each(function(){
                                        var input = $(this);
                                        if (input.attr("type") != "submit" && input.attr("type") != "button") 
                                            input.val("");
                                        input.val(form.getData(input.attr("name")));
                                    });
                                    console.debug("Setting form to validate with rules:", form.validationRules);
                                    var submitHandler = function(){
                                        node.trigger("mwanzia_submitting");
                                        node.find(":input").each(function(){
                                            var input = $(this);
                                            form.setData(input.attr("name"), input.val());
                                        });
                                        form.submit({
                                            success: function(result){
                                                node.trigger("mwanzia_submitted", result);
                                            },
                                            exception: function(exception){
                                                alert("Problem submitting form: " + exception);
                                            }
                                        });
                                    }
                                    var validatorOptions = {
                                        rules: form.validationRules,
                                        onsubmit: false
                                    };
                                    // Unbind submit handlers
                                    node.unbind("submit");
                                    var messagesExpression = node.attr("mwanzia:messages");
                                    if (!messagesExpression) 
                                        node.submit(function(){
                                            if (node.validate(validatorOptions).form()) {
                                                submitHandler();
                                            }
                                            return false;
                                        });
                                    else 
                                        extract(node, bound, function(customMessages){
                                            console.log("Using custom validation messages:", customMessages);
                                            validatorOptions.messages = customMessages;
                                            node.submit(function(){
                                                if (node.validate(validatorOptions).form()) {
                                                    submitHandler();
                                                }
                                                return false;
                                            });
                                        }, messagesExpression);
                                }
                            }, expression);
                        }
                        else 
                            if ("push" == command) {
                                var expression = node.attr("mwanzia:push");
                                // Bind the children
                                extract(node, bound, function(result){
                                    node.mwanzia().set(result);
                                    node.children().mwanzia().bind(result);
                                }, expression);
                                needToBindChildren = false;
                            }
                // Now handle attributes
                var attributes = node.mapAttributes();
                for (var key in attributes) {
                    var attributeValue = attributes[key];
                    if (key.indexOf("mwanzia:") == 0) {
                        var jbKey = key.substring(5).toLowerCase();
                        if (jbKey in
                        {
                            "onsetting": "",
                            "onset": "",
                            "onsubmitted": "",
                            "set": "",
                            "list": "",
                            "push": "",
                            "form": "",
                            "messages": ""
                        }) 
                            continue;
                        if (jbKey.indexOf("on") == 0) {
                            // Register event handler
                            event(node, bound, jbKey.substring(2), attributeValue);
                        }
                        else {
                            // Bind attribute
                            extract(node, bound, function(result){
                                node.attr(jbKey, result);
                            }, attributeValue);
                        }
                    }
                }
                if (needToBindChildren) {
                    node.children().mwanzia().bind(bound);
                }
            });
        }
		
        this.rebind = function(){
            return selected.mwanzia().bind(selected.mwanzia().bound());
        }
		
        this.add = function(newItem, position){
            console.debug("Adding to", selected);
			var originalItems = selected.mwanzia().bound();
			if (position != null) {
				originalItems.splice(position, 0, newItem);
			} else {
				originalItems.push(newItem);
			}
			selected.mwanzia().list(originalItems);
			return selected;
        }
		
		this.data = function(key, value) {
            var target = selected.first();
            var result = target.data("mwanzia_data");
            if (result == null) {
                result = {};
                target.data("mwanzia_data", result);
            }
            if (value) {
                result[key] = value;
                console.debug("Set mwanzia data on", target, key, value, result);
                return value;
            } else {
                var returnValue = result[key];
                console.debug("Got mwanzia data from", target, key, returnValue);
                return returnValue;
            }
        }
		
        this.bound = function(value) {
            return selected.data("bound", value);
        }
		
        this.set = function(value) {
            return selected.data("set", value);
        }
	}
	
    $.fn.mwanzia = function() {
        return new MwanziaPlugin(this)
    };
    
	function extract(node, bound, callback, expression){
        if (!expression) {
            callback(bound);
        }
        else {
            var extractor = null;
            try {
                extractor = mwanzia.delegate(node, "bound", "return " + expression);
            } 
            catch (exception) {
                console.log("Error evaluating expression   " + expression + "   : " + exception);
            }
            if (extractor) {
                var result = null;
                try {
                    result = extractor(bound);
                } 
                catch (exception) {
                    console.log("Error extracting value using expression " + expression + " :" + exception);
                }
                if (result instanceof mwanzia.AjaxInvocation) {
                    node.trigger("mwanzia_binding", [result]);
                    result.success(function(result){
                        callback(result);
                        node.trigger("mwanzia_bound", [result]);
                    }).go();
                }
                else {
                    callback(result);
                    node.trigger("mwanzia_bound", [result]);
                }
            }
        }
    }
    
    function event(node, bound, event, expression){
        var handler = null;
        try {
            handler = mwanzia.delegate(node, "bound", "event", "rebind", expression);
        }
        catch (exception) {
            console.log("Error evaluating expression   " + expression + "   : " + exception);
        }
        if (handler) {
            node.unbind(event);
            node.bind(event, function(e){
                handler(bound, e, function(target){
                    if (!target) 
                        target = node;
                    return function(result){
                        target.mwanzia().bind(result);
                    }
                });
            });
        }
    }
})(jQuery);
