package org.mwanzia.test;

import java.util.Date;
import java.util.Map;

import javax.persistence.EntityTransaction;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mwanzia.Application;
import org.mwanzia.JSON;

public class TestApplicationTest {
    @BeforeClass
    public static void setupOnce() throws Exception {
        JPA.initialize("demo");
    }

    @Test
    public void test() throws Exception {
        Application application = new TestApplication();
        EntityTransaction transaction = JPA.getInstance().getEntityManager().getTransaction();
        transaction.begin();
        Company company = Company.create("My Test Company");
        Employee manager = company.hire("Sally", "Manager");
        Branch austinBranch = company.newBranch(manager, "Austin", new Address("2110 Slaughter Lane", null, "Austin",
                State.TX, "78748"));
        Account account = austinBranch.openAccount(new Customer("Percy", "Wegmann", "123-45-6701", 18, new Address(
                "Undisclosed", "Line 2", "Austin", State.TX, "23456")), new Date[] { new Date(), new Date() });
        Employee employee = company.hire("Second", "Employee");
        JPA.getInstance().getEntityManager().persist(company);
        JPA.getInstance().getEntityManager().close();
        transaction.commit();
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(JSON.toJson(company, true));
        System.out.println(json);
        Company deserialized = JSON.fromJson(mapper.readValue(json, Map.class), Company.class);
        Assert.assertEquals("Wrong company name", company.getName(), deserialized.getName());
        Assert.assertEquals("Wrong number of branches", company.getBranches().size(), deserialized.getBranches().size());
        Assert.assertEquals("Wrong number of company employees", company.getEmployees().size(), deserialized
                .getEmployees().size());
        Assert.assertEquals("Wrong branch name", company.getBranches().get("Austin").getName(), deserialized
                .getBranches().get("Austin").getName());
        Assert.assertEquals("Wrong number of accounts",
                company.getBranches().get("Austin").getAccounts().size(),
                deserialized.getBranches().get("Austin").getAccounts().size());
        Assert.assertEquals("Wrong account", account, deserialized.getBranches().get("Austin").getAccounts().iterator()
                .next());
        Customer deserializedOwner = JSON.fromJson(mapper.readValue(mapper.writeValueAsString(JSON.toJson(account
                .getOwner(), false)), Map.class), Customer.class);
        Assert.assertEquals("Wrong state", account.getOwner().getAddress().getState(), deserializedOwner.getAddress()
                .getState());
    }
}
