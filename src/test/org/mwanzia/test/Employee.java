package org.mwanzia.test;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

import org.codehaus.jackson.annotate.JsonBackReference;
import org.codehaus.jackson.annotate.JsonProperty;

@Entity
public class Employee extends Person {
    private static final long serialVersionUID = -4217735931910449177L;

    private Company company;
    private Set<Branch> branches = new HashSet<Branch>();

    public Employee() {
        super();
    }

    public Employee(Company company, String firstName, String lastName) {
        super(firstName, lastName);
        this.company = company;
    }

    @ManyToOne
    @JsonProperty
    @JsonBackReference
    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    @ManyToMany
    @JsonProperty
    @JsonBackReference
    public Set<Branch> getBranches() {
        return branches;
    }

    public void setBranches(Set<Branch> branches) {
        this.branches = branches;
    }
}
