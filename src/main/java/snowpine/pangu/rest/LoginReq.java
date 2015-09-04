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
public class LoginReq {
    
    private long userId;
    private String password;

    public LoginReq() {
    }   

    public LoginReq(long userId, String password) {
        this.userId = userId;
        this.password = password;
    }   

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

}
