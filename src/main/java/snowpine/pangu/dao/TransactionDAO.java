/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package snowpine.pangu.dao;

import java.sql.Date;
import java.util.List;

/**
 *
 * @author Xuesong
 */
public interface TransactionDAO {
    public Transaction findById(long id) throws DAOWrapperException;
    public List<Transaction> findByUser(long userId, Date startDate, Date endDate) throws DAOWrapperException;
    public long newTransaction(long fromUser, long toUser, long amount) throws DAODataException, DAOWrapperException;
}
