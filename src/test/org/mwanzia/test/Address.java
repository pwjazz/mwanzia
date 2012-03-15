package org.mwanzia.test;

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import net.sf.oval.constraint.Assert;
import net.sf.oval.constraint.Length;

import org.codehaus.jackson.annotate.JsonProperty;
import org.mwanzia.extras.validation.validators.Required;

@Embeddable
public class Address {
    @Required
    private String line1;

    private String line2;

    @Required
    private String city;

    @Required
    @Length(min = 2, max = 2)
    private State state;

    @Required
    @Assert(expr = "_value.length == 5", lang = "javascript")
    private String postalCode;

    public Address() {
    }

    public Address(String line1, String line2, String city, State state, String postalCode) {
        super();
        this.line1 = line1;
        this.line2 = line2;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
    }

    @JsonProperty
    public String getLine1() {
        return line1;
    }

    public void setLine1(String line1) {
        this.line1 = line1;
    }

    @JsonProperty
    public String getLine2() {
        return line2;
    }

    public void setLine2(String line2) {
        this.line2 = line2;
    }

    @JsonProperty
    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    @JsonProperty
    @Enumerated(EnumType.STRING)
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @JsonProperty
    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

}
