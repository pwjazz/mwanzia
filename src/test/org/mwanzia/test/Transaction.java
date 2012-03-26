package org.mwanzia.test;

import java.math.BigDecimal;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import org.mwanzia.JsonInclude;
import org.mwanzia.Remote;
import org.mwanzia.Transferable;
import org.mwanzia.extras.transactions.RequiresTransaction;

@Entity
@Transferable
public class Transaction extends AbstractEntity {
    private static final long serialVersionUID = -6935565266599431825L;

    private Account account;
    private BigDecimal amount;
    private String memo;

    @Remote
    @RequiresTransaction
    public Transaction submit() throws AccountClosedException {
        if (this.account.isClosed())
            throw new AccountClosedException(String.format("Account %1$s is already closed", account.getNumber()));
        this.account.getTransactions().add(this);
        return this;
    }

    public Transaction() {
    }

    public Transaction(Account account, BigDecimal amount, String memo) {
        super();
        this.account = account;
        this.amount = amount;
        this.memo = memo;
    }

    @ManyToOne
    @JsonInclude
    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    @JsonInclude
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @JsonInclude
    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

}
