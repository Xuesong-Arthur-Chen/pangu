/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package snowpine.pangu.rest;

/**
 *
 * @author Xuesong
 */
public class BalanceRes {

    public BalanceRes() {
    }

    public BalanceRes(long balance) {
        this.balance = balance;
    }
    
    private long balance;

    public long getBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        this.balance = balance;
    }

}
