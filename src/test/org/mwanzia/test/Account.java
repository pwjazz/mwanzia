package org.mwanzia.test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.apache.shiro.authz.annotation.RequiresRoles;
import org.mwanzia.JsonInclude;
import org.mwanzia.Remote;
import org.mwanzia.extras.transactions.RequiresTransaction;

@Entity
public class Account extends AbstractEntity {
    private static final long serialVersionUID = 7736561423786566601L;

    private Customer owner;
    private Branch branch;
    private UUID number;
    private Date dateOpened;
    private Date dateClosed;
    private Set<Transaction> transactions = new HashSet<Transaction>();

    public Account() {
    }

    public Account(Customer owner, Branch branch, UUID number, Date dateOpened) {
        super();
        this.owner = owner;
        this.branch = branch;
        this.number = number;
        this.dateOpened = dateOpened;
    }

    // The below intentionally has an extra comma and extra whitespace
    // to make sure Mwanzia can handle it
    @RequiresRoles("corporate, , toomuchpowerforanyone")
    @Remote
    public Account setHugeBalance() {
        return this;
    }

    @RequiresRoles("corporate")
    @Remote
    @RequiresTransaction
    public Account close() throws AccountClosedException {
        if (this.isClosed())
            throw new AccountClosedException(String.format("This account was already closed on " + this.dateClosed));
        this.dateClosed = new Date();
        JPA.getInstance().getEntityManager().persist(this);
        return this;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonInclude
    @Remote
    public Customer getOwner() {
        return owner;
    }

    public void setOwner(Customer owner) {
        this.owner = owner;
    }

    @ManyToOne
    @JsonInclude
    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    @JsonInclude
    public UUID getNumber() {
        return number;
    }

    public void setNumber(UUID number) {
        this.number = number;
    }

    @JsonInclude
    public Date getDateOpened() {
        return dateOpened;
    }

    public void setDateOpened(Date dateOpened) {
        this.dateOpened = dateOpened;
    }

    @JsonInclude
    public Date getDateClosed() {
        return dateClosed;
    }

    public void setDateClosed(Date dateClosed) {
        this.dateClosed = dateClosed;
    }

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    public Set<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(Set<Transaction> transactions) {
        this.transactions = transactions;
    }

    @JsonInclude
    public BigDecimal getBalance() {
        BigDecimal balance = new BigDecimal("0.00");
        for (Transaction transaction : transactions) {
            balance = balance.add(transaction.getAmount());
        }
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        // do nothing
    }

    @Transient
    @JsonInclude
    public boolean isClosed() {
        return dateClosed != null && dateClosed.before(new Date());
    }

    public void setClosed(boolean closed) {
        // only included for symmetry
    }
}
