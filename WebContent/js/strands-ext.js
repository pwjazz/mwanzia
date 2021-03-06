/**
Strands Extra Functions - JavaScript Cooperative Threading and Coroutine support
Copyright (C) 2007 Xucia Incorporation
Author - Kris Zyp - kriszyp@xucia.com
 */
		/**
		 * This function will can be used to synchronize access to a critical section of code. If there is other code with a 
		 * lock on the lock object, than the code will be executed once the lock is released
		 * @param lockObject	This is the object to use lock. Two competing threads can use a single lock object to safeguard a critical section
		 * @param func		This is function to execute while locking out other execution of this function
		 * @param notExclusive	Setting this to true will cause the function to be executed even if other code is using the lock object. The lock will be incremented so that synchronize will still block for calls that don't use this parameter
		 */
strands.synchronize = function(lockObject,func,notExclusive) {
			return function() { // $_lock is the number of functions currently executing
										// $_wfs are the functions that are currently resulting to access this code
				with(_frm(this,arguments,[],[])) {
					if (_cp == 0) {
						if (lockObject.$_lock && !notExclusive)  {
							var future = new Future();
							push(lockObject.$_wfs,future.fulfill);
							return future.result();
						}
						else {
							if (!lockObject.$_wfs)
								lockObject.$_wfs = [];		
							if (lockObject.$_lock)
								lockObject.$_lock++;
							else {
								lockObject.$_lock = 1;			
							}
						}
					}
					_cp=1;
					try {
						var retValue = func.apply(this,arguments);			
						return retValue;
					}
					finally {
						if (retValue == _S) return _s();// if it is suspending we must keep it locked, so this a clever way to avoid the next code
							lockObject.$_lock--;
							if (lockObject.$_lock < 0) // Assertion
								throw new Error("Lock count less than zero");
							if (lockObject.$_wfs.length > 0)
								setTimeout(function() {			
									synchronize(lockObject,lockObject.$_wfs.pop());
								},1);			
					}
				}
			}
		}
	/**
	 * This can be used by idempotent functions that are written in native code that make calls to compiled code
	 */
strands.rerunLater = function() {
			// If a rerunnable function calls a strands function in needs to call this after a narrative frame suspends. This will make it so there will not be a reference to this frame and it will not be required to be executed
			if (suspendedFrame)
				delete suspendedFrame._r.func;
			return Suspend;
		}
