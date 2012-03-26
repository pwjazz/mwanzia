# Mwanzia Overview

WARNING - Mwanzia is still under active development and should be considered
alpha quality

Mwanzia makes your Java objects available from JavaScript so that you can access
their data and invoke remote functions with no boilerplate code.

##### Java Code

    package org.mwanzia.demo;
        
    public class Account {
        private Long id;
        private String name;
        private Date closedDate;
        
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

        @Transferable 
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
    } 
    
##### JavaScript Code
   
    mwanziaImport();            
    mwanziaImportPackage(org.mwanzia.demo);
    
    Account.list().success(function(accountList) {
        var thirdAccountInList = accountList[3];
        
        thirdAccountInList.close(function(updatedAccount) {
           // Update the local account
           thirdAccountInList = updatedAccount;
        }).go();
    }.go();
    
That's it - no messing with XHR objects, URLs, query parameters, ids or any of
the usual boilerplate!

## Why Choose Mwanzia?

+ You like to write your UI in JavaScript and your back-end in Java, without
  having to [write UI code in Java](http://code.google.com/webtoolkit/overview.html)
  
+ You think it would be cool to call your Java methods using
  [named parameters](http://en.wikipedia.org/wiki/Named_parameters)
  
+ You want to deploy to a servlet container, like
  [Tomcat](http://tomcat.apache.org/),
  [Jetty](http://jetty.codehaus.org/jetty/), or
  [AppEngine](http://code.google.com/appengine/docs/java/overview.html)
  
+ Mwanzia works in a [shared nothing architecture](http://en.wikipedia.org/wiki/Shared_nothing_architecture)

+ Mwanzia plays nice with JPA, in particular Hibernate and DataNucleus on AppEngine

+ Mwanzia is an [API](http://en.wikipedia.org/wiki/Api), not a
  [framework](http://en.wikipedia.org/wiki/Web_application_framework)
  
+ Mwanzia has minimal external dependencies to keep you out of jar hell

+ It's modular - use existing plugins and/or add your own to achieve tight
  integration with your app
  
+ Mwanzia is licensed under the liberal
  [MIT License](http://en.wikipedia.org/wiki/Mit_license) so that you can use it
  in open and closed-source projects
  
## Less Leak Philosophy

Mwanzia does its best to stay out of the developer's way.  Because Mwanzia is
fundamentally a JSON over AJAX mechanism, developers do need to be aware of a
handful of unavoidable peculiarities.

+ Remote methods on JavaScript objects are asynchronous, so they have special
  calling conventions.
  
+ Methods on Java objects that have been marked as @Remote are remotely callable
  from JavaScript and should be treated with the same respect as any API.
  
+ Any Java objects that are sent to/from the client as JSON require the
  developer to manage the usual things like handling cyclic references,
  Hibernate lazy loading and so on.  The Hibernate JPA Plugin automaticaly
  handles cyclic references using pass-by-reference semantaics, so for many
  scenarios developers don't need to worry about it.

+ Depending on which plugins are used, there may be other peculiarities.

In our opinion, Mwanzia solves many remoting-related problems without 
introducing any significant new ones.

## A Look Under the Covers of Our Example

![Example Sequence Diagram](https://github.com/pwjazz/mwanzia/raw/master/docs/img/example_sequence_diagram.png)

1. When you include server.js on your page, it imports dynamically created
   JavaScript that defines the client-side version of your object model.  A look
   at this file will explain much about the magic that happens on the client-side.
   This file is also useful for auditing what has been exposed from your system
   via Mwanzia.

2. When you call a remote method like list() in the browser, the JavaScript
   object dispatches this to the server via the MwanziaServlet.  In the case of
   a static method like list(), MwanziaServlet simply invokes the static method
   on the server and then returns the result to the client.
   
3. In addition to calling static methods, you may also call instance methods
   like close(). When you do this, MwanziaServlet will first instantiate an
   instance of the appropriate type, and then set all properties marked as
   @Transferable (in this case, the property "id").  Then it calls the method.
   
## In-Browser Unit Tests

If you want to dive right in to the gory details, take a look at our live
[JavaScript Tests](http://ec2-23-20-152-26.compute-1.amazonaws.com:8080/mwanzia/core_tests.html).

This is a full featured Mwanzia application demonstrating use of the JPA,
validation and Shiro authentication/authorization plugins.  Here is the corresponding
[Java Back-end Code](https://github.com/pwjazz/mwanzia/tree/master/src/test).

For a gentler introduction, read on.

## Basic Setup

To use Mwanzia, you'll need to:

1. Add server-side jar dependencies to build path
2. Create an Application class and register your remote objects
3. Configure the Mwanzia Servlet in your web.xml
4. Import some JavaScript on the client

### Server-Side Dependencies

<table>
    <tr>
        <th>Required By</th>
        <th>Dependency</th>
        <th>Jar</th>
    </tr>
    <tr>
        <td>Core</td>
        <td>Mwanzia</td>
        <td><a href="https://github.com/pwjazz/mwanzia/raw/master/dist/mwanzia-0.1.1.jar">mwanzia-0.1.1.jar</a></td>
    </tr>
    <tr>
        <td>Core</td>
        <td><a href="http://paranamer.codehaus.org/">Paranamer</a></td>
        <td><a href="https://github.com/pwjazz/mwanzia/raw/master/WebContent/WEB-INF/lib/paranamer-2.2.1.jar">paranamer-2.2.1.jar</a></td>
    </tr>
    <tr>
        <td>Core</td>
        <td><a href="http://www.slf4j.org/">SLF4J</a></td>
        <td><a href="https://github.com/pwjazz/mwanzia/raw/master/WebContent/WEB-INF/lib/slf4j-api-1.6.1.jar">slf4j-api-1.6.1.jar</a></td>
    </tr>
    <tr>
        <td>JPA Plugin</td>
        <td>JPA 2.0 API</td>
        <td><a href="https://github.com/pwjazz/mwanzia/raw/master/WebContent/WEB-INF/lib/hibernate-jpa-2.0-api-1.0.0.Final.jar">hibernate-jpa-2.0-api-1.0.0.Final.jar</a></td>
    </tr>
    <tr>
        <td>Validation Plugin</td>
        <td><a href="http://oval.sourceforge.net">OVal</a></td>
        <td><a href="https://github.com/pwjazz/mwanzia/raw/master/WebContent/WEB-INF/lib/oval-1.61.jar">oval-1.61.jar</a></td>
    </tr>
    <tr>
        <td>Validation Plugin (if using JavaScript Expressions)</td>
        <td><a href="http://www.mozilla.org/rhino/">Rhino</a></td>
        <td><a href="https://github.com/pwjazz/mwanzia/raw/master/WebContent/WEB-INF/lib/js.jar">js.jar</a></td>
    </tr>
    <tr>
        <td>Shiro Plugin</td>
        <td><a href="http://shiro.apache.org/">Apache Shiro</a></td>
        <td><a href="https://github.com/pwjazz/mwanzia/raw/master/WebContent/WEB-INF/lib/shiro-all-1.0.0-incubating.jar">shiro-all-1.0.0-incubating.jar</a></td>
    </tr>
    <tr>
        <td>Shiro Plugin</td>
        <td><a href="http://commons.apache.org/beanutils/">Commons BeanUtils</a></td>
        <td><a href="https://github.com/pwjazz/mwanzia/raw/master/WebContent/WEB-INF/lib/commons-beanutils-core-1.8.3.jar">commons-beanutils-core-1.8.3.jar</a></td>
    </tr>
    <tr>
        <td>Shiro Plugin</td>
        <td><a href="http://commons.apache.org/collections/">Commons Collections</a></td>
        <td><a href="https://github.com/pwjazz/mwanzia/raw/master/WebContent/WEB-INF/lib/commons-collections-3.1.jar">commons-collections-3.1.jar</a></td>
    </tr>
    <tr>
        <td>Shiro Plugin</td>
        <td><a href="http://commons.apache.org/logging/">Commons Logging</a></td>
        <td><a href="https://github.com/pwjazz/mwanzia/raw/master/WebContent/WEB-INF/lib/commons-logging-1.1.1.jar">commons-logging-1.1.1.jar</a></td>
    </tr>
</table>

### Application Class

In order to use Mwanzia, you create a sub-class of org.mwanzia.Application.
Inside the constructor, you register classes for remote access using the
method registerRemote().

You also need to implement hooks into whatever JSON parser you're using.
In our case, we use Jackson by just subclassing the built-in JacksonApplication.

If you use your a different JSON parser, just make sure that it is capable of
representing objects as maps.  Most JSON parsers should work fine.  

    package org.mwanzia.demo;
    
    public class DemoApplication extends org.mwanzia.extras.jackson.JacksonApplication {
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

### Client-Side Dependencies

Mwanzia has a number of JavaScript dependencies.  You can get these from the
[test application](https://github.com/pwjazz/mwanzia/tree/master/WebContent/js).

    <!-- Required JavaScript Libraries -->
    <script type="text/javascript" src="js/console.js"></script>
    <script type="text/javascript" src="js/jquery-1.7.1.min.js"></script>
    <script type="text/javascript" src="js/json2.js"></script>
    <script type="text/javascript" src="js/typesystem.js"></script>
    <script type="text/javascript" src="js/mwanzia.core.js"></script>
    <!-- JPA Plugin (Optional) -->
    <script type="text/javascript" src="js/mwanzia.jpa.js"></script>
    <!-- Validation Plugin (Optional) -->
    <script type="text/javascript" src="js/mwanzia.validation.js"></script>
    <!-- The dynamically created server.js (generated by Mwanzia servlet) -->
    <script type="text/javascript" src="server.js"></script>
    
+ *console.js* - Mwanzia logs trace and error information to the console.
                 Include console.js to provide compatibility with browsers that
                 don't have a console of their own.
                 
+ *jquery-1.7.1.min.js* - Mwanzia uses jQuery's ajax support.  Feel free to use
                          any version of jQuery as of 1.4 (may even work with
                          earlier versions).
                           
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
                
                For performance reasons, this file is usually only generated
                once and then cached in memory.  You can use the following system
                property to have the file regenerate automatically during
                development.  -Dmwanzia.mode=dev                
    
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
    
### JavaBean Properties

JavaBean properties, both instance and static, are available on the client as
JavaScript properties.  Derived properties are also available.

Properties are only available if the object was read from the server.

##### Java

    public class MyClass {
        private static String staticProp = "A";
        private String instanceProp = "B";
        
        public static String getStaticProp() {
            return staticProp;
        }
        
        public String getInstanceProp() {
            return instanceProp;
        }
        
        public String getDerivedProp() {
            return "C";
        }
    }
    
##### JavaScript

    var obj = // read instance of MyClass from server
    
    // All of the following are true
    
    MyClass.staticProp == "A";
    
    obj.instanceProp == "B";
    
    obj.derivedProp == "C";
    
    // All of the following are also true
    
    typeof(new MyClass().instanceProp) == "undefined";
    
    typeof(new MyClass().derivedProp) == "undefined";
    
### Cyclic References

With Mwanzia, use the annotation @JsonExclude to deal with pruning your
object graph on return to the client.  When dealing with JPA entities you don't 
need to worry about this (see JPA Plugin section below).

##### Java Code

    public class Owner {
        
        public Owned getOwned() {};
        
        public static Owner find() {};
    }
    
    public class Owned {
        
        @JsonExclude
        public Owner getOwner() {};
        
    }
    
##### JavaScript Code

    Owner.find().success(function(foundOwner) {
        // All of these are true
        foundOwner.owned != null
        
        foundOwner.owned.owner == null;
    }).go();
    
### Application Config

On the client, your Application class is availabe under its own name.  Any
properties that you defined on your application are available in the client.
This is handy for static configuration data.

Note - these properties are not static.  The client gets an instance of your
Application.

##### Java

    package org.mwanzia.demo;
    
    public class TestApplication {
    
        public String[] getStates() {
            return new String[] {"AL", "AK", ... };
        }
        
    }

##### JavaScript

    importMwanzia();
    
    // The below is true
    
    TestApplication.states.length > 0
    
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
    
### Exception and Error Handling

Mwanzia allows clients to handle exceptions from remote methods using a similar
syntax as Java.  Exceptions are typed and polymorphic, for maximum compatibility
with the patterns you're accustomed to in Java.

To handle an exception, use the catchException() method on a remote invocation
and pass in an associative array of exception types and handler functions.
catchException also accepts a default handler that will handle any exception
that wasn't specifically handled.

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
        }
    }, function(exception) {
        // This is the default handler in case exception didn't match a specific
        // type
        alert("Caught unexpected exception with message: " + exception.message);
    });
    
#### Default Handlers

configure() allows you to register default handlers for exceptions (throw from
server) and errors (problems arising on JavaScript side, for example network
down).

##### JavaScript

    mwanzia.configure(function(exception){
        console.error("Unexpected exception", exception);
        throw fail("Unexpected exception: " + mwanzia.stringify(exception));
    }, function(error){
        console.error("Unexpected error", error);
        throw fail("Unexpected error: " + error);
    });

### Passing Parameters

Remote methods can include parameters, including both primitive as well as
reference types.

+ Primitive types are handled as you would expect.
+ BigDecimals are treated as floating point numbers on the client.
+ Dates receive special handling.  They are serialized using an ISO8601 format
  and automatically converted to/from timezone-adjusted dates on the client.
+ JavaScript arrays convert to/from Collections and Arrays on the server
  (depending on your method signature)
+ Java Maps convert to/from JavaScript associative arrays
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

+ *@JsonInclude* - add this to the getter method to whitelist the property

+ *@JsonExclude* - add this to the getter method to blacklist the property

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

note - requires JPA2, Hibernate or DataNucleus on AppEngine

Mwanzia is particularly well suited to binding from JavaScript to a persistent
server-side domain model.  In fact, this is the scenario for which Mwanzia
was originally authored.  The JPA plugin supports this patterns.

The JPA plugin is framework agnostic - you provide the hook for obtaining an
EntityManager from your own environment.  The only requirement is that you use
only 1 EntityManager per thread.

For vanilla JPA2, use JPA2Plugin.

For Hibernate on JPA1, use HibernateJPA1Plugin.

For Hibernate on JPA2, use HibernateJPA2Plugin.

For App Engine, using AppEngineJPAPlugin.
 
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

#### Handling Cyclic References

In the commonly used bi-directional mapping, the object graph ends up with
cyclic references.  JSON has no way of representing these, but Mwanzia adds its
own mechanism for handling them.  When serializing an object graph to the
client, Mwanzia automatically replaces cyclicly referenced objects with a
placeholder Reference.  On the client-side, these place-holder references are
replaced by the actual object so that the graph is reconstituted as it appeared
on the server.

##### Java Code

    public class Child {
        private Long id;
        private Parent parent;
        
        @ManyToOne
        public Parent getParent() {
            return parent;
        }    
    }
    
    public class Parent {
        private Long id;
        private Set<Child> children;
        
        @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch=FetchType.EAGER)
        public Set<Child> getChildren() {
            return children;
        }
        
        @Remote
        public static Parent findParentSomehow() { ... }
    }
    
##### JavaScript

    Parent.findParentSomehow(function(parent) {
        for (var i=0; i<parent.children.length; i++) {
            var child = parent.children[i];
            // The below is true
            if (child.parent == parent) {
                // This will be true
            }
        }
    }).go();


#### Automatic Handling for Lazy Loading (Hibernate Only)

Lazy loaded single and multi-ended associations are handled transparently by
Mwanzia.  On the client, objects appear with nothing but their id.  Multi-ended
associations appear as arrays of objects with only ids.

Thus, you can still check for null/not null, and you have an id to use when
calling back to the server.

##### Java

    public class Related {
    
    }
    
    public class Owner {
        private Related related;
        
        @ManyToOne
        @Remote
        public Related getRelated() {
            return related;
        }
    }
    
##### JavaScript

    var owner = // get owner somehow;
    
    var related = null;
    
    owner.getRelated().success(function(_related) {
        // This first time makes a remote call
        related = _related;
    });
    
    owner.getRelated().success(function(_related) {
        // This time we're getting a cached value
        related = _related;
    });
    
    // We can also access the property directly now
    related = owner.related;
    
    // Now we clear the cached value
    owner.getRelated().clear();
    
#### Remote Lazy Loading (Hibernate Only)

To handle lazy-loaded associations, the plugin supports remote lazy loading.
After reading a property once, Mwanzia will use a locally cached value.

To clear locally cached values and force a remote fetch, call forceRemote() on
the remote invocation before you continue.  forceRemote() is chainable.

Just mark your getter method with @Remote to enable this.

##### Java

    public class Related {
    
    }
    
    public class Owner {
        private Related related;
        
        @ManyToOne(fetch = FetchType.LAZY)
        @Remote
        public Related getRelated() {
            return related;
        }
    }
    
##### JavaScript

    var owner = // get owner somehow;
    
    var related = null;
    
    // The first time, force a remote load
    owner.getRelated().forceRemote().success(function(_related) {
        // This first time makes a remote call
        related = _related;
    });
    
    owner.getRelated().success(function(_related) {
        // This time we're getting a cached value
        related = _related;
    });
    
    // We can also access the property directly now
    related = owner.related;
    
### Transaction Plugin

This plugin automatically starts and stops transactions.  You can enable
transactions for classes or methods using the @RequiresTransaction annotation.

The plugin knows nothing about specific transaction frameworks, it just requires
that you implement some abstract methods to plug into your platform.  Here's an
example from a JPA application:

    public TestApplication() {
        super();
        // Register a plugin for doing transaction management
        registerPlugin(new TransactionPlugin<EntityTransaction>() {
            @Override
            protected EntityTransaction beginTransaction() {
                JPA.getInstance().clear();
                EntityTransaction transaction = JPA.getInstance().getEntityManager().getTransaction();
                transaction.begin();
                return transaction;
            }

            @Override
            protected void commit(EntityTransaction transaction) throws Exception {
                transaction.commit();
            }

            @Override
            protected void rollback(EntityTransaction transaction) throws Exception {
                transaction.rollback();
            }
        });
        registerPlugin(new HibernateJPA2Plugin() {
            @Override
            protected EntityManager getEntityManager() {
                return JPA.getInstance().getEntityManager();
            }
        });
        registerPlugin(new ValidationPlugin());
        registerPlugin(new ShiroPlugin(this));
    }

### Validation Plugin

This plugin supports declarative validation constraints using [OVal](http://oval.sourceforge.net/).

The plugin provides a validate() method on all remote invocations on the client,
allowing your JavaScript application to validate a call before submitting it.
The validate() method is also automatically invoked before submitting the call
to the server.

Even though constraints are only validated on method calls, the can be defined
both on entities and on methods.

Constraint violations generate ValidationExceptions.  ValidationException
includes a list of ValidationErrors that provide details about what fields
failed to validate and why.

Built-in basic constraints like required, length, etc can be validated both on
the client and on the server, whereas more complicated constraints may only be
checked on the server-side.

Client-side validation failures throw exactly the same exception as server-side
failures, providing a unified programming model for validation.

If you include Rhino on your classpath, you can use OVal's Assert
with JavaScript.  The validation plugin will run this same statement on the
client when you validate there.

##### Java

    public class Address {
        public void save(@Required Date when) {
            // save this address
        }
    
        @Transferable
        @Assert(expr = "_value.length == 5", lang = "javascript")
        private String postalCode;
    
        // Because this is a Java expression, it can only be checked on the server
        @Assert(expr = "_value == true", lang = "java")
        private boolean aBoolean;
    }
        
##### JavaScript

    var address = new Address({postalCode: "123456", aBoolean: false});
    
    try {
        address.save(new Date()).validate();
    } catch (error) {
        // This should be true
        error instanceof org.mwanzia.extras.ValidationException;
    }
    
    // Fix the postalCode but omit the date
    address.postalCode = "12345";
    // This time, we won't call validate ourselves and just submit
    address.save().catchException({
        "org.mwanzia.extras.ValidationException": function() {
            // we should end up here
            // this call never actually went to the server
        }
    });
    
    // Now include a date, but aBoolean is bad
    address.save(new Date()).catchException({
        "org.mwanzia.extras.ValidationException": function() {
            // we should end up here
            // this call went to the server, but we get the exact same
            // type of exception
        }
    });
    
### Shiro Plugin

Using [Apache Shiro](http://shiro.apache.org/), this plugin provides the ability
to authenticate and authorize calls from the client.  It supports the following
Shiro annotations:

+ @RequiresAuthentication
+ @RequiresRoles

In the future, we will add support for @RequiresUser and @RequiresPermissions