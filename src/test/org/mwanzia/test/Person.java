package org.mwanzia.test;

import javax.persistence.MappedSuperclass;

import org.mwanzia.JsonInclude;
import org.mwanzia.extras.validation.validators.Required;

@MappedSuperclass
public class Person extends AbstractEntity {
    private static final long serialVersionUID = -4199727665808793417L;

    @Required
    private String firstName;

    @Required
    private String lastName;

    public Person() {
    }

    public Person(String firstName, String lastName) {
        super();
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @JsonInclude
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @JsonInclude
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

}
