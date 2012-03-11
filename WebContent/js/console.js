/*!
 * Mwanzia Web Framework
 * http://tbd.com
 *
 * Copyright 2010, Percy Wegmann
 * Dual licensed under the MIT or GPL Version 2 licenses.
 * http://tbd.com/license
 *
 */

/*
 * This file provides a stub for the console object on browsers
 * that don't support it natively (or via a plugin like Firebug).
 * 
 * Mwanzia uses console extensively, so you'll need to include
 * this file if you're using any of the mwanzia.*.js files.
 */
if (typeof(console) == "undefined") {
	console = {
        log: function(){
        },
        debug: function(){
        },
        info: function(){
        },
        warn: function(){
        },
        error: function(){
        }
    };
}

// If the available console object doesn't define a clear function, add one
if (typeof(console.clear) != "function") {
    console.clear = function(){
    };
}
