/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package snowpine.pangu.dao;

/**
 *
 * @author Xuesong
 */
public interface UserDAO {
    public User findById(long id) throws DAOWrapperException;
    public User findByEmail(String email) throws DAOWrapperException;
}
