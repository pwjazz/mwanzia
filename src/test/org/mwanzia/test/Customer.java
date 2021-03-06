package org.mwanzia.test;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

import net.sf.oval.constraint.AssertValid;
import net.sf.oval.constraint.Min;

import org.mwanzia.JsonInclude;
import org.mwanzia.Remote;
import org.mwanzia.extras.validation.validators.Required;

@Entity
public class Customer extends Person {
    private static final long serialVersionUID = 5460946911455976898L;

    @Required
    private String ssn; 

    @Required
    @Min(18)
    private long age;

    @Required
    @AssertValid
    private Address address;
    
    
    private Set<Account> accounts = new HashSet<Account>();

    public Customer() {
        super();
    }

    public Customer(String firstName, String lastName, String ssn, long age, Address address) {
        super(firstName, lastName);
        this.ssn = ssn.replaceAll("[^0-9]", "");
        this.age = age;
        this.address = address;
    }

    public String getSsn() {
        return ssn;
    }
    
    public void setSsn(String ssn) {
        this.ssn = ssn;
    }
    
    @JsonInclude
    public long getAge() {
        return age;
    }

    public void setAge(long age) {
        this.age = age;
    }

    @Embedded
    @JsonInclude
    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    @Remote
    @OneToMany(mappedBy = "owner")
    public Set<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(Set<Account> accounts) {
        this.accounts = accounts;
    }

}
