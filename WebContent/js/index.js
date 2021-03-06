// Configure Mwanzia with a default exception handler that just throws up an alert
function handleException(exception){
    alert("Unhandled exception: " + exception.message);
}

mwanzia.configure(handleException);

// Import everything in the org.jjro.test package
mwanzia.importPackage(org.jjro.test);

/**
 * The following few statements mix in UI functionality to some remote objects.
 * Unlike most MVC frameworks, Mwanzia doesn't have a concept of UI controls (or a UI controller).
 * Instead, UI-specific functionality can just be mixed in with the model, allowing it to act
 * as both model and controller.
 *
 * That said Mwanzia' model is itself UI agnostic, so it can be used with more traditional MVC frameworks
 * if you prefer.  I tend to like this hybrid approach with mixins because for most web applications,
 * a strict MVC approach is usually a little too heavy.
 *
 * WARNING - When using mixins, it is recommended that you avoid storing references from the remote object to DOM elements.
 * This is because the JJRO UI framework will sometimes store remote objects under a DOM element.  Storing
 * a DOM element under a remote object could thus lead to a circular reference between a native object (the DOM
 * element) and a JavaScript object (the remote object), which is a potential source of memory leaks.
 */
Person.mixin({
    /**
     * Combines the last name and first name
     */
    fullName: function(){
        return this.lastName + ", " + this.firstName;
    }
});

Company.mixin({
    /**
     * Shows the branches corresponding to this Company
     */
    showBranches: function(){
        $("#branches").jjro().list(this.branches);
    }
});

Branch.mixin({
    /**
     * Shows the accounts corresponding to this Branch.  Notice the use of the getAccounts()
     * method, which is an accessor for the lazy-loaded property accounts on the Branch.
     * The first time that we call getAccounts(), it will make a remote call to fetch the data.
     * Subsequent calls will use the locally cached data, unless and until someone calls getAccounts().clear(),
     * which invalidates the local information and causes the next call to getAccounts() to fetch
     * it remotely.  Since getAccounts() may return asynchronously, we always use the success(callback)
     * style of invocation.
     */
    showAccounts: function(){
        var accountsContainer = $("#accountsContainer");
        this.getAccounts().success(function(accounts){
            $("#accounts tbody").jjro().list(accounts);
            // Unblock when finished loading
            accountsContainer.show();
        }).go();
    },
    
    /**
     * Binds the New Account Form to the this Branch's openAccount method
     * and shows it.
     */
    showNewAccountForm: function(){
		$("#accountsView").hide();
        $('#newAccountForm').jjro().form(this.openAccount.form());
		$('#newAccountFormView').show().find("input[name='owner.firstName']").focus();
    }
});

/**
 * Gets the current company based on the selected option in select#companies.
 */
function getCurrentCompany(){
    return $("#companies option:selected").jjro().bound();
}

/**
 * Gets the current branch based on the selected option in select#branches.
 */
function getCurrentBranch(){
    return $("#branches option:selected").jjro().bound().value;
}

/**
 * Callback that fires after an account is added.
 * @param {Account} account
 */
function accountAdded(account){
    // Clear the lazy accounts property on the current branch (so that it's reloaded next time)
    getCurrentBranch().getAccounts().clear();
    // Show the accounts list
	$("#newAccountFormView").hide();
	$("#accountsView").show();
	// Set table row background-colors to transparent
	$("#accounts tbody tr").css("background-color", "transparent");
	// Add the new account to the table
    $("#accounts tbody").jjro().add(account, 0);
//	.css("opacity", "0").css("background-color", "#FFFFAA").animate({
//		"opacity": "1"
//	}, 1000);
}

// Custom form validation messages for the new customer form
// Note that for fields without a custom validation message, a default message will be generated by jquery.validate.js
var newAccountFormMessages = {
    "owner.firstName": "Please enter the owner's first name",
    "owner.address.postalCode": "Please enter a 5 digit zip code"
}

// Set up UI on document.ready
$(document).ready(function(){
	var companiesList = $("select#companies");
	companiesList.block();
    // Bind a list of all companies to the UI
    Company.list().success(function(companies){
        $("select#companies").jjro().list(companies);
		companiesList.unblock();
    }).go();
});

// Set up defaults for blockUI
$.blockUI.defaults.message = "<img src='img/ajax-loader.gif' />";
$.blockUI.defaults.css = {
    padding: 0,
    margin: 0,
    width: '100%',
    top: '0',
    left: '0',
    textAlign: 'center',
    verticalAlign: 'top',
    color: '#000',
    border: 'none',
    backgroundColor: 'transparent',
    cursor: 'wait'
};
$.blockUI.defaults.overlayCSS = {
    backgroundColor: '#FFFFFF'
};
