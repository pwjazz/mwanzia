package org.mwanzia.test;

import java.math.BigDecimal;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import org.codehaus.jackson.annotate.JsonProperty;
import org.mwanzia.Remote;
import org.mwanzia.Transferable;

@Entity
@Transferable
public class Transaction extends AbstractEntity {
    private static final long serialVersionUID = -6935565266599431825L;

    private Account account;
    private BigDecimal amount;
    private String memo;

    @Remote
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
    @JsonProperty
    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    @JsonProperty
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @JsonProperty
    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

}
