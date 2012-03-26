package org.mwanzia.test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;

import net.sf.oval.constraint.AssertValid;
import net.sf.oval.guard.Guarded;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.mwanzia.JsonInclude;
import org.mwanzia.Remote;
import org.mwanzia.extras.transactions.RequiresTransaction;
import org.mwanzia.extras.validation.validators.Required;

@Entity
@Guarded
public class Company extends AbstractEntity {
    private static final long serialVersionUID = -7040545055875794979L;

    private String name;
    private Map<String, Branch> branches = new HashMap<String, Branch>();
    private Set<Employee> employees = new HashSet<Employee>();

    @Remote
    public static List<Company> list() {
        return JPA.getInstance().getEntityManager().createQuery("from Company order by name").getResultList();
    }

    @Remote
    @RequiresTransaction
    @RequiresAuthentication
    public static Company create(@Required String name) {
        Company company = new Company(name);
        JPA.getInstance().getEntityManager().persist(company);
        return company;
    }

    @Remote
    @RequiresTransaction
    public static void deleteAll() {
        // TODO: right now this doesn't delete anything because the foreign key
        // constraints are tricky
        JPA.getInstance().getEntityManager().createQuery("delete from Account");
        JPA.getInstance().getEntityManager().createQuery("delete from Customer");
        JPA.getInstance().getEntityManager().createQuery("delete from Employee");
        JPA.getInstance().getEntityManager().createQuery("delete from Branch");
        JPA.getInstance().getEntityManager().createQuery("delete from Company");
    }

    @Remote
    @RequiresTransaction
    public Employee hire(@Required String firstName, String lastName) {
        Employee employee = new Employee(this, firstName, lastName);
        this.employees.add(employee);
        return employee;
    }

    @Remote
    @RequiresTransaction
    public Branch newBranch(Employee manager, String name, @AssertValid Address address) {
        Branch branch = new Branch(this, manager, name, address);
        this.branches.put(name, branch);
        return branch;
    }

    public Company() {
    }

    public Company(String name) {
        super();
        this.name = name;
    }

    @JsonInclude
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL)
    @JsonInclude
    public Set<Employee> getEmployees() {
        return employees;
    }

    public void setEmployees(Set<Employee> employees) {
        this.employees = employees;
    }

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL)
    @MapKey(name = "name")
    @JsonInclude
    public Map<String, Branch> getBranches() {
        return branches;
    }

    public void setBranches(Map<String, Branch> branches) {
        this.branches = branches;
    }

}
