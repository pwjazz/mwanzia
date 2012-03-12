# Mwanzia Overview

Mwanzia provides a mechanism for seamless remote binding from JavaScript to
server-side Java objects via an AJAX over HTTP transport. Mwanzia keeps the
semantics on the client as similar as possible to the server-side, and in
general aims to make programming on the client feel like you're programming on
the server.

Unlike [GWT](http://code.google.com/webtoolkit/), Mwanzia is not a UI framework.
Consequently, the server-side Java code does not need to contain any UI-related
logic or configuration. This makes Mwanzia suitable for binding directly from a
JavaScript UI to a domain model without needing to create any server-side
UI components.

A primary goal for Mwanzia is to avoid turning into a leaky abstraction.  In an
application that uses Mwanzia, you will have some objects that are exported via
Mwanzia and some that are not.  Basically, the Mwanzia objects behave just like
all your other objects, except that they support remoting which introduces the
following peculiarities:

+ JavaScript objects may include some asynchronous remote methods which have
  their own calling conventions that are explained below.
  
+ Some Java objects have one or more remotely accessible methods identified by
  the @Remote annotation.
  
+ Some Java objects can be sent to/from the client as JSON and you need to deal
  with the usual things like handling cyclic references, Hibernate lazy loading
  and so on.
  
In keeping with Mwanzia's "no leak" philosophy, the core of Mwanzia does
remoting and nothing else.  Mwanzia does provide a plugin mechanism that allows
it to be extended to support things like JPA, validation and other
application-specific functionality that may be related to remoting but isn't
strictly necessary.
  
If you want to dive right in, take a look at our [JavaScript Tests](https://github.com/pwjazz/mwanzia/blob/master/WebContent/core_tests.html) and the corresponding [Java Back-end Code](https://github.com/pwjazz/mwanzia/tree/master/src/test).

For a gentler introduction, read on.

## Somewhat Contrived Example

### Java Code

    package org.mwanzia.demo;
        
    /**
     * Part of the object model in our example system.
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

        // this annotation means that the ID will be transferred from client
        // to server when making calls to a specific instance of the Account        
        @Transferable 
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
    } 
    
### JavaScript Code
   
    // Import top level package names so that you can use them (e.g. org.mwanzia.demo)
    
    mwanziaImport();            
                
    // Import the org.mwanzia.demo package so that we can reference Account
    // directly, instead of using org.mwanzia.demo.Account
    
    mwanziaImportPackage(org.mwanzia.demo);
    
    var anAccount = null;
    
    Account.list().success(function(accountList) {
        
        // Get the third account
        anAccount = accountList[3];
        
        anAccount.close(function(updatedAccount) {
           // Update the local account
           anAccount = updatedAccount;
        }).go();
    }.go();
    
That's it - no messing with XHR objects, URLs, query parameters, ids or any of
the usual boilerplate!

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
    <script type="text/javascript" src="/js/mwanzia.core.js"></script>
    <!-- JPA Plugin (Optional) -->
    <script type="text/javascript" src="/js/mwanzia.jpa.js"></script>
    <!-- Validation Plugin (Optional) -->
    <script type="text/javascript" src="/js/mwanzia.validation.js"></script>
    <!-- The dynamically created server.js (generated by Mwanzia servlet) -->
    <script type="text/javascript" src="/server.js"></script>
    
+ *console.js* - Mwanzia logs trace and error information to the console.
                 Include console.js to provide compatibility with browsers that
                 don't have a console of their own.
                 
+ *jquery-1.4.2.min.js* - Mwanzia uses jQuery's ajax support.  Feel free to use
                           your own version of jQuery.
                           
+ *json2.js* - Mwanzia uses this library for parsing and serialization JSON
               instead of the browser's built-in JSON parser.
               
+ *typesystem.js* - Mwanzia uses a type system based on this
                    [John Resig Article](http://ejohn.org/blog/simple-javascript-inheritance/).
                    This type system allows the JavaScript versions of your
                    server-side objects to follow the same inheritance hierarchy
                    as their Java counterparts.
                    
+ *mwanzia.core.js* - The Mwanzia core client-side API.

+ *mwanzia.jpa.js* - (Optional) Client-side support for Mwanzia's JPA plugin.

+ *mwanzia.validation.js* - (Optional) Client-side support for Mwanzia's
                            validation plugin.                      

+ *server.js* - This is where the magic happens.  server.js is dynamically
                generated by the MwanziaServlet and sets up your client-side
                object model for all remotely accessible server-side classes.
    
### What Goes On Under the Covers

![Example Sequence Diagram](https://github.com/pwjazz/mwanzia/raw/master/docs/img/example_sequence_diagram.png)

1. When you include server.js on your page, it imports dynamically created
   JavaScript that defines the client-side version of your object model.  A look
   at this file will explain much about the magic that happens on the client-side.

2. When you call a remote method like list() in the browser, the JavaScript
   object dispatches this to the server via the MwanziaServlet.  In the case of
   a static method like list(), MwanziaServlet simply invokes the static method
   on the server and then returns the result to the client.
   
3. In addition to calling static methods, you may also call instance methods
   like close(). When you do this, MwanziaServlet will first instantiate an
   instance of the appropriate type, and then set all properties marked as
   @Transferable (in this case, the property "id").  Then it calls the method.
   
Note - JSON is handled on the server using [Jackson](http://jackson.codehaus.org/).

## Core Functionality

Mwanzia exports Java object models to JavaScript and makes certain methods
available for remote invocation.  In so doing, it attempts to match the client-
side semantics to the server-side as closely as possible.  The main difference
is that remote invocations are asynchronous, so there's some special syntax for
dealing with them.  That's it.

### Namespaces

Objects on the client are namespaced just like on the server.

##### Java

    package org.mwanzia.demo;
    
    public class ClassA {}
    
##### JavaScript
    // We need to import mwanzia first
    mwanziaImport();
    
    // Now we can reference ClassA fully qualified
    var theClass = org.mwanzia.demo.ClassA;
    
    // Or we can import the package and then reference it unqualified
    mwanziaImportPackage(org.mwanzia.demo);
    var theClass = ClassA;
    
### Constructors

Objects on the client are constructed as one would expect, using the new
operator.  Upon construction, one can pass any number of properties to the
constructor in an associative array.

Note - it is perfectly okay to include
properties on the client that are not defined on the server.

    var myObject = new ClassA({property1: "val", property2: 5});
    
### Remote Methods

Methods marked as @Remote are available from the client.  Both static and
instance methods can be marked as @Remote, and they are accessed from the client
using the same conventions as in Java.

##### Java

    public class ClassA {
        @Remote
        public static void remoteStaticMethod() {}
        
        @Remote
        public void remoteInstanceMethod() {}
        
        public static void nonRemoteStaticMethod() {}
        
        public static void nonRemoteInstanceMethod() {}
    }
    
##### JavaScript    

    // All these are true
    typeof(Account.remoteStaticMethod) == "function";
    
    typeof(new Account().remoteInstanceMethod) == "function";
    
    typeof(Account.nonRemoteStaticMethod) == "undefined";
    
    typeof(new Account().nonRemoteStaticMethod) == "undefined";

### Inheritance

Objects on the client follow the same inheritance hierarchy as objects on the
server.

##### Java

    public class Parent {
        @Remote
        public void parentMethod() {}
    }
    
    public class Child extends Parent {
        @Remote
        public void childMethod() {}
    }
    
##### JavaScript

    var aChild = new Child();
    
    // All of these are true
    
    aChild instanceof Child;
    
    aChild instanceof Parent;
    
    typeof(aChild.childMethod) == "function";
    
    typeof(aChild.parentMethod) == "function";
    
### Mixins

Client-side classes can be enhanced with additional properties and methods using
the mixin() static method.  mixin() can be called multiple times, and because it
modifies the Class's prototype, it can be called either before or after
instantiating an object of that type.

mixin() is syntactic sugar - you can accomplish the same thing as
mixin({??? : XXX}) using Class.prototype.??? = XXX, but Percy finds mixin()
easier to read, especially when mixing in lots of stuff.  mixin's syntax is also
consistent with John Resig's Class.extend(), making for a bit of symmetry.

##### Java

    public class Person {
        private String firstName;
        private String lastName;
        
        // Define getters and setters
    }

##### JavaScript

    Person.mixin({
        visible: true,
        
        getFullName: function() {
            return this.firstName + " " + this.lastName;
        }
    });
    
    var person = new Person({firstName: "Bob", lastName: "Smith"});
    
    // The below are true
    
    person.getFullName() == "Bob Smith";
    
    person.visible == true;
    
    // Mix in some more stuff
    
    Person.mixin({
        additionalProperty: 55
    });
    
    // The below is true
    
    person.additionalProperty == 55;
    
    // The below is still true
    
    person.visible == true;
    
### Remote Method Invocation

On the client-side, remote methods are actually factories for remote invocations.
They accept all of the same parameters as the server-sider method, plus a final
parameter for a callback function that handles the return value from the remote
function.

##### Java

    public class SampleClass {
    
        @Remote
        public static String speak(String text, boolean loud) {
            if (loud) text = text.toUpperCase();
            System.out.println(text);
            return loud ? "I spoke loudly" : "I spoke";
        }
    
    }
    
##### JavaScript

    var remoteInvocation = SampleClass.speak("Hello", true);
    
Since the call will be asynchronous, you'll typically want to register a
callback for receiving the return value.  This is done using the success()
method on the remote invocation.
    
Note - success(), like all methods on remote invocations, is chainable, meaning
that they all return a remoteInvocation, which you can then use immediately.

##### JavaScript

    var remoteInvocation = SampleClass.speak("Hello", true).success(function(returnValue) {
        // This is true
        returnValue == "I spoke loudly";
    });
    
When you're ready to make the call, use the method go() on the remote invocation.
Remote invocations are reusable, so you can call go() as many times as you like.

##### JavaScript
    
    var remoteInvocation = SampleClass.speak("Hello", true).success(function(returnValue) {
        // This is true
        returnValue == "I spoke loudly";
    }).go();
    
    // Do it again if you want
    remoteInvocation.go();
    
### Named Parameters

In addition to passing parameters positionally as in the above example, you
can also pass named parameters.  To do so, simply pass your parameters as an
associative array.

##### JavaScript

    var remoteInvocation = SampleClass.speak({text: "Hello", loud: true}).success(function(returnValue) {
        // This is true
        returnValue == "I spoke loudly";
    }).go();
    
### Exception Handling

Mwanzia allows clients to handle exceptions from remote methods using a similar
syntax as Java.  Exceptions are typed and polymorphic, for maximum compatibility
with the patterns you're accustomed to in Java.

To handle an exception, use the catchException() method on a remote invocation
and pass in an associative array of exception types and handler functions.

##### Java

    package org.mwanzia.demo;
    
    public class MyException extends Exception { 
        private long errorCode;
        
        public long getErrorCode() {
            return errorCode;
        }
        
        // Add constructors
    }

    public class SampleClass {
    
        @Remote
        public static void doStuff() throws MyException { ... }
    
    }
    
##### JavaScript

    var remoteInvocation = SampleClass.doStuff().success(function() {
        alert("All good");
    }).catchException({
        "org.mwanzia.demo.MyException": function(exception) {
            alert("Caught MyException with error code: " + exception.errorCode);
        },
        "java.lang.Exception": function(exception) {
            alert("Caught unexpected exception with message: " + exception.message);
        }
    });

### Passing Parameters

Remote methods can include parameters, including both primitive as well as
reference types.

+ Primitive types are handled as you would expect.
+ BigDecimals are treated as floating point numbers on the client.
+ Dates receive special handling.  They are serialized using an ISO8601 format
  and automatically converted to/from timezone-adjusted dates on the client.
+ Reference types are passed by value, which is to say that on the server, they
  reflect whatever data was passed in by the client.  Data can be nested to
  any depth, however if your input data contains cyclic graphs, you'll need to
  use the @JsonBackReference annotation to deal with this.
  
##### Java

    public class TextContainer {
        private String text;
        
        // Add getters and setters
    }
    
    public class Message {
        private TextContainer textContainer;
        private Date date;
        
        // Add getters and setters
    }
    
    public class Service {
        public void printMessage(Message message, int repetitions) {
            for (int i=0; i<repetitions; i++) {
                System.out.println(message.textContainer.text + " at " + date);
            }
        }
    }
    
##### JavaScript

    var message = new Message({text: new TextContainer({text: "Hello world"}),
                               date: new Date()});
    var service = new Service();
    
    // This will print the message 5 times on the server
    service.printMessage(message, 5).go();
    
For brevity, you can omit the types of the parameter and they will be inferred.

    service.printMessage({text: {text: "Hello World},
                          date: new Date()});
                          
### Passing Instance Data

When calling an instance of an object, you may want to set some or all of its
properties from the client and make these available on the server.  You do this
by applying the @Transferable annotation either to the individual properties or
to the whole class.

##### Java

    @Transferable
    public class Transaction {
        private BigDecimal amount;
        private String memo;
        
        // add getters and setters
        
        public String submit() {
            // Submit our transaction to the database
            // Using the amount and memo fields
            return referenceNumber;
        }
    }
    
##### JavaScript

    var transaction = new Transaction({ amount: 5.67,
                                        memo: "A small test transaction"});
                                        
    transaction.submit().success(function(referenceNumber) {
        alert(referenceNumber);
    }).go();                                    
    
### Security

Mwanzia takes various steps to prevent JavaScript clients from gaining access
to functionality and data that they ought not to.

#### Method Whitelisting

Only methods marked as @Remote are accessible from the client.

#### Property White- or Blacklisting

Your Mwanzia Application can use either whitelisting or blacklisting to
restrict client-side access to properties on your Java beans.  The default mode
is whitelisting.

+ *@JsonProperty* - add this to the getter method to whitelist the property

+ *@JsonIgnore* - add this to the getter method to blacklist the property

#### JPA Pass-by-Reference

When using the JPA Plugin, all non-primitive method parameters are passed
by reference unless marked with @ByValue.  This prevents the client from passing
in a persistent entity and inadvertently or maliciously modifying that
persistent entity.

#### Apache Shiro Plugin

Mwanzia includes a plugin that supports authentication and method-level
authorization using [Apache Shiro](http://shiro.apache.org/).

## Plugins

In order to support the use of Mwanzia in a variety of contexts, the core of
Mwanzia is extremely slim on features and focuses purely on remoting from
JavaScript to Java.  Mwanzia can be extended through the use of plugins, and
it includes a few plugins to support common usage patterns.

### JPA Support

Mwanzia is particularly well suited to binding from JavaScript to a persistent
server-side domain model.  In fact, this is the scenario for which Mwanzia
was originally authored.  The JPA plugin (currently implemented for Hibernate)
supports this pattern.

The JPA plugin provides several key features:

#### Managing Object Identity

Persistent entities are treated as references that are identified by a single
property named "id".  When a remote method is called on a persistent entity,
Mwanzia first loads that entity from the database and then invokes the method
on that peristent entity.

#### Pass by Reference

When calling a remote method that accepts a persistent entity as a parameter,
Mwanzia will by default pass that entity from the client by reference.  This
means that the entity is identified by its "id" property and the parameter
is loaded from the database before being passed to the method.

For example:

    public class Customer {
        @Id
        private Long id;
    }

    public class Account {
        @Id
        private Long id;
        
        private Set<Customer> customers = new HashSet<Customer>();
        
        public Account linkToCustomer(Customer customer) {
            this.customers.add(customer);
            return this;
        }
    }
    
On client:

    var account = // get the account from somewhere
    var customer = // get the customer from somewhere;
    
    account.linkToCustomer(customer, function(updatedAccount) {
        // handle the updatedAccount
    }).go();
    
In this example, when linkToCustomer is called, Mwanzia will look up the
Account by its id, look up the Customer by its id, and then pass that Customer
to that Account's linkToCustomer() method.

Since we're just passing the Customer by id, it's perfectly ok to just pass an
id on the client-side, like so:

    account.linkToCustomer(5, function(updatedAccount) {
        // handle the updatedAccount
    }).go();
    
Parameters can also be passed by value, if marked with the @ByValue annotation:

    public Account linkToCustomer(@ByValue Customer customer) {
            this.customers.add(customer);
            return this;
    }

On the client, you can then do something like this:

    var account = // get the account from somewhere
    var customer = new Customer({firstName: "Bob", lastName: "Smith"});
    
    account.linkToCustomer(customer, function(updatedAccount) {
        // handle the updatedAccount
    }).go();
    
#### Automatic Remote Registration

All persistent types are automatically exported to the client and eligible for
remote access.  Individual methods still need to be marked as @Remote to allow
remote invocation.

#### Optimistic Locking

The JPA plugin supports your application's use of optimistic locking as long
as the version field is called "version".

#### Configuration

TODO:

### Transaction Plugin

This plugin automatically starts and stops transactions on every remote invocation.
It works very nicely in coordination with the JPA plugin.

### Validation Plugin

This plugin supports declarative validation constraints using [OVal](http://oval.sourceforge.net/).

Constraint violations generate ValidationExceptions.  Built-in basic constraints
like required, length, etc can be validated both on the client and on the server.

Client-side validation failures throw exactly the same exception as server-side
failures, providing a unified programming model for validation.

### Shiro Plugin

Using [Apache Shiro](http://shiro.apache.org/), this plugin will the ability
to authenticate and authorize calls from the client.  It will support the
following Shiro annotations:

+ @RequiresAuthentication
+ @RequiresPermissions
+ @RequiresRoles
+ @RequiresUser

