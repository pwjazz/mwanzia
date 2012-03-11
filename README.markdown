# Mwanzia Overview

Mwanzia provides a mechanism for seamless remote binding from JavaScript to
server-side Java objects via an HTTP transport. Mwanzia keeps the semantics on
the client as similar as possible to the server-side, and in general aims to
make programming on the client feel like you're programming on the server.

Unlike [GWT](http://code.google.com/webtoolkit/), Mwanzia is not a UI framework.
Consequently, the server-side Java code does not need to contain any UI-related
logic or configuration. This makes Mwanzia suitable for binding directly from a
JavaScript UI to a domain model without needing to create any server-side
UI components.

## Example

### Java Code

    package org.mwanzia.demo;
        
    /**
     * Part of the domain model in our example system.
     */
    public class Account {
        private Long id;
        private String name;
        private Date closedDate;
        
        /**
         * The @Remote annotation indicates that this method is callable
         * from JavaScript.
         */
        @Remote
        public static List<Account> list() {
           // query for your accounts as necessary
        }
        
        @Remote
        public Account close() {
           this.closedDate = new Date();
           // persist, audit log, whatever else you need to do
           return this;
        }
        
        // Market the ID property as @Transferable.  When a call is made
        // on the client to an instance of Account, that Account will be
        // passed to the server with all @Transferable properties set to
        // their values from the client.  In our example, we identify the
        // Account by id, so we make this transferable. 
        @Transferable
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
    } 
    
### JavaScript Code
   
    // Import top level package names so that you can use them
    // (e.g. org.mwanzia.demo)
    
    mwanziaImport();            
                
    // Import the org.mwanzia.demo package so that we can reference Account
    // directly, instead of using org.mwanzia.demo.Account
    
    mwanziaImportPackage(org.mwanzia.demo);
    
    var theAccount = null;
    
    // Call the list() method with a success callback
    // we have to use callbacks because remote methods are all invoked
    // asynchronously
    
    // Note that list() is a static method on the Java class, and the same
    // semantics apply in the JavaScript version.
    
    Account.list().success(function(accountList) {
        
        // Get the third account
        theAccount = accountList[3];
        
        // Account.close() is an instance method, so we call it on a
        // specific instance of an Account.
        theAccount.close(function(updatedAccount) {
           
           // Update the account shown in the UI
           theAccount = updatedAccount;
        }).go();
    }.go(); // don't forget to call go() to execute the method
    
    <p>That's it - no messing with XHR objects, URLs, query parameters, ids
        or any of the usual boilerplate!</p>

## Wait a minute, where's the plumbing?!

Okay, we cheated a little bit in our example, you do also have to set up some
plumbing to get this all to work, but it's pretty minimal. You'll need to:

1. Create an Application class and register your remote objects
2. Configure the Mwanzia Servlet in your web.xml
3. Import some JavaScript on the client

### Application Class

In order to use Mwanzia, you create a sub-class of org.mwanzia.Application.
Inside the constructor, you register classes for remote access using the
method registerRemote().

    package org.mwanzia.demo;
    
    public class DemoApplication extends org.mwanzia.Application {
        public DemoApplication() {
            super();
            // Register Account as a remoteable class in this Application
            registerRemote(Account.class);
        }
    }

### web.xml

    <servlet>
        <servlet-name>Mwanzia</servlet-name>
        <servlet-class>org.mwanzia.MwanziaServlet</servlet-class>
        <init-param>
            <!-- TODO - add support for multiple applications -->
            <param-name>application</param-name>
            <param-value>org.mwanzia.test.TestApplication</param-value>
        </init-param>
        <load-on-startup />
    </servlet>

    <servlet-mapping>
        <servlet-name>Mwanzia</servlet-name>
        <url-pattern>/server.js</url-pattern>
    </servlet-mapping>

### Script Imports

Mwanzia has a number of JavaScript dependencies.

    <!-- Required JavaScript Libraries -->
    <script type="text/javascript" src="/js/console.js"></script>
    <script type="text/javascript" src="/js/jquery-1.4.2.min.js"></script>
    <script type="text/javascript" src="/js/json2.js"></script>
    <script type="text/javascript" src="/js/typesystem.js"></script>
    <script type="text/javascript" src="/js/date.format.js"></script>
    <script type="text/javascript" src="/js/mwanzia.core.js"></script>
    <!-- JPA Plugin (Optional) -->
    <script type="text/javascript" src="/js/mwanzia.jpa.js"></script>
    <!-- Validation Plugin (Optional) -->
    <script type="text/javascript" src="/js/mwanzia.validation.js"></script>
    <!-- The dynamically created server.js (generated by Mwanzia servlet) -->
    <script type="text/javascript" src="/server.js"></script>  
    
### What Goes On Under the Covers

![Example Sequence Diagram](http://www.websequencediagrams.com/cgi-bin/cdraw?lz=VXNlci0-QnJvd3Nlcjogb3BlbiBwYWdlCgAMBy0-V2ViIFNlcnZlcjogL2xpc3RfYWNjb3VudHMuaHRtbAoAFgoAPQtzdGF0aWMgSFRNTABACk13YW56aWFTZXJ2bGV0OiAvanMvcwBVBS5qcwoAEA4AgQkLSmF2YVNjcmlwdCBkb21haW4gbW9kZWwAgRgKQQCBCgYgKEpTKTogbGlzdCgpCgAJDAAYCwCBRAYAFBMAEgcAPhAAgWQIIGFzIEpTT04ASQ8AgjAJABcRAII3CQCCUwliaW5kAEYKaW50byBET00KAIJzD2NsaWNrIGNsb3MAgnkLAIFaCDUAgV0HABoFAIFaCwASBgAbDQCBYAkAGRMAgVsRAFAIdXBkYXRlAIEdCQBPEQCEHwkAGRAAgVcXAEEPAIFsCQ&s=modern-blue)