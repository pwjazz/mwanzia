package org.mwanzia.test;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import net.sf.oval.constraint.AssertValid;

import org.mwanzia.JsonInclude;
import org.mwanzia.Remote;
import org.mwanzia.extras.jpa.ByValue;
import org.mwanzia.extras.transactions.RequiresTransaction;
import org.mwanzia.extras.validation.validators.Required;

@Entity
public class Branch extends AbstractEntity {
    private static final long serialVersionUID = 1205854667091787314L;

    private Company company;
    private Employee manager;
    private String name;
    private Address address;
    private Set<Employee> employees = new HashSet<Employee>();
    private Set<Account> accounts = new HashSet<Account>();

    @Remote
    @RequiresTransaction
    public Account openAccount(@Required @AssertValid @ByValue Customer owner, Date[] unusedParameter) {
        if (owner.getId() == null)
            JPA.getInstance().getEntityManager().persist(owner);
        Account account = new Account(owner, this, UUID.randomUUID(), new Date());
        this.accounts.add(account);
        return account;
    }

    public Branch() {
    }

    public Branch(Company company, Employee manager, String name, Address address) {
        super();
        this.company = company;
        this.manager = manager;
        this.name = name;
        this.address = address;
    }

    @JsonInclude
    @ManyToOne
    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    @JsonInclude
    @ManyToOne
    public Employee getManager() {
        return manager;
    }

    public void setManager(Employee manager) {
        this.manager = manager;
    }

    @JsonInclude
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Embedded
    @JsonInclude
    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    @ManyToMany
    @JsonInclude
    public Set<Employee> getEmployees() {
        return employees;
    }

    public void setEmployees(Set<Employee> employees) {
        this.employees = employees;
    }

    @Remote
    @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL)
    @JsonInclude
    public Set<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(Set<Account> accounts) {
        this.accounts = accounts;
    }

}
