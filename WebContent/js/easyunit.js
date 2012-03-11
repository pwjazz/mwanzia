/*!
 * EasyUnit JavaScript Testing Library
 * http://tbd.com
 *
 * Copyright 2010, Percy Wegmann
 * Dual licensed under the MIT or GPL Version 2 licenses.
 * http://tbd.com/license
 *
 */

/*
 * EasyUnit provides a mechanism for creating simple automated tests of JavaScript code,
 * inspired by JUnit but much simpler in scope.
 * 
 * To use, create a new page and link the easyunit.js and console.js files (EasyUnit
 * logs to the console).
 * 
 * Create a JavaScript object and assign to the global variable easyunit_tests.
 * 
 * The keys of this object are your test names and the values should be functions that
 * run your test.
 * 
 * EasyUnit provides the following assertions:
 * 
 * - assertEquals
 * - assertNotEquals
 * - assertTrue
 * - assertFalse
 * - fail
 * 
 * Your test results will be output at the end of your page as a table.
 * 
 * For example:
 * 
 * <script type="text/javascript" src="/js/console.js"></script>
 * <script type="text/javascript" src="/js/easyunit.js"></script>
 * 
 * <script type="text/javascript">
 * 
 * var easyunit_tests = {
 *  "Test Equality Assertions": function() {
 *      assertEquals("1 equals 1", 1, 1);
 *      assertNotEquals("1 equals 1", 1, 1);
 *  },
 *  
 *  "Test Truth Assertions": function() {
 *      assertTrue("5 is greater than 1", 5 > 1);
 *      assertFalse(1 is greater than 5", 1 > 5);
 *  }
 * }   
 * 
 * </script>
 */

function assertEquals(){
    if (arguments.length == 3) {
        var message = arguments[0];
        var expected = arguments[1];
        var actual = arguments[2];
    }
    else {
        var message = "Equal";
        var expected = arguments[0];
        var actual = arguments[1];
    }
    try {
        result(expected == actual, message, '', expected, actual);
    } 
    catch (exception) {
        console.error(exception);
        result(false, message, exception, expected, actual);
    }
}

function assertNotEquals(){
    if (arguments.length == 3) {
        var message = arguments[0];
        var expected = arguments[1];
        var actual = arguments[2];
    }
    else {
        var message = "Not equal";
        var expected = arguments[0];
        var actual = arguments[1];
    }
    try {
        result(expected != actual, message, '', expected, actual);
    } 
    catch (exception) {
        console.error(exception);
        result(false, message, exception, expected, actual);
    }
}

function assertTrue(){
    if (arguments.length == 2) {
        var message = arguments[0];
        var expected = true;
        var actual = arguments[1];
    }
    else {
        var message = "True";
        var expected = true;
        var actual = arguments[0];
    }
    try {
        result(expected == actual, message, '', expected, actual);
    } 
    catch (exception) {
        console.error(exception);
        result(false, message, exception, expected, actual);
    }
}

function assertFalse(){
    if (arguments.length == 2) {
        var message = arguments[0];
        var expected = false;
        var actual = arguments[1];
    }
    else {
        var message = "False";
        var expected = false;
        var actual = arguments[0];
    }
    try {
        result(expected == actual, message, '', expected, actual);
    } 
    catch (exception) {
        console.error(exception);
        result(false, message, exception, expected, actual);
    }
}

function assertNotEmpty(){
    if (arguments.length == 2) {
        var message = arguments[0];
        var expected = "not empty";
        var actual = arguments[1];
    }
    else {
        var message = "True";
        var expected = "not empty";
        var actual = arguments[0];
    }
    try {
        result(actual != null, message, '', expected, actual);
    } 
    catch (exception) {
        console.error(exception);
        result(false, message, exception, expected, actual);
    }
}

function assertEmpty(){
    if (arguments.length == 2) {
        var message = arguments[0];
        var expected = "empty";
        var actual = arguments[1];
    }
    else {
        var message = "False";
        var expected = "empty";
        var actual = arguments[0];
    }
    try {
        result(actual == null, message, '', expected, actual);
    } 
    catch (exception) {
        console.error(exception);
        result(false, message, exception, expected, actual);
    }
}

function fail(error, condition){
    if (!condition) 
        condition = '';
    result(false, condition, error, '', '');
}

function result(passed, condition, error, expected, actual){
    var row = $("<tr><td>" + (passed ? "PASS" : "FAIL") + "<td>" + currentTestFunction + "</td><td>" + condition + "</td><td>" + error + "</td><td>" + expected + "</td><td>" + actual + "</td></tr>");
    row.css("background-color", passed ? "lightgreen" : "red");
    if (!passed) {
        row.css("color", "white");
        //row.css("font-weight", "bold");
    }
    resultsTable.append(row);
}

var resultsTable = null;
var currentTestFunction = null;

$(document).ready(function(){
	if (typeof(easyunit_tests) == "undefined") {
		alert("No easyunit_tests defined, nothing to test");
	}
	else {
		mwanzia.configure(function(exception){
			console.error("Unexpected exception", exception);
			throw fail("Unexpected exception: " + mwanzia.stringify(exception));
		}, function(error){
			console.error("Unexpected error", error);
			throw fail("Unexpected error: " + error);
		});
		
		var doc = $(this);
		doc.append("<h1>Test Results</h1>");
		resultsTable = $("<table cellspacing='0' cellpadding='3'><tr><th>Pass/Fail</th><th>Test</th><th>Condition</th><th>Error</th><th>Expected</th><th>Actual</th></tr></table>");
		$("body").append(resultsTable);
		for (var functionName in easyunit_tests) {
			currentTestFunction = functionName;
			try {
				easyunit_tests[functionName]();
			} 
			catch (exception) {
				console.error(exception);
				fail(exception);
			}
		}
	}
});
