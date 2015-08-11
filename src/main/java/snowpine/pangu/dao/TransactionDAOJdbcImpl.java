/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package snowpine.pangu.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

/**
 *
 * @author Xuesong
 */
public class TransactionDAOJdbcImpl implements TransactionDAO {

    public static final String SQL_LOCK_BALANCE = "update users set balance=balance "
            + "where user_id=?";
    public static final String SQL_GET_BALANCE_FOR_UPDATE = "select balance from users "
            + "where user_id=? for update";
    public static final String SQL_UPDATE_BALANCE = "update users set balance=? where user_id=?";
    public static final String SQL_INSERT_TRANSACTION = "insert into transactions"
            + "(transaction_time, from_user, to_user, amount) values(CURRENT_TIMESTAMP, ?, ?, ?)";

    public static final String SQL_GET_TRANSACTION = "select * from transactions where transaction_id=?";

    public static final String SQL_GET_USER_TRANSACTIONS = "select * from transactions "
            + "where (from_user=? or to_user=?) and "
            + "DATE(transaction_time)>=? and DATE(transaction_time)<=?";

    private final DataSource dataSource;
    private final UserDAO userDAO;

    public TransactionDAOJdbcImpl(DataSource dataSource, UserDAO userDAO) {
        this.dataSource = dataSource;
        this.userDAO = userDAO;
    }

    private Transaction getTransaction(ResultSet rs) throws SQLException {
        Transaction transaction = new Transaction();
        transaction.setId(rs.getLong("transaction_id"));
        transaction.setTime(rs.getTimestamp("transaction_time"));
        transaction.setFromUser(rs.getLong("from_user"));
        transaction.setToUser(rs.getLong("to_user"));
        transaction.setAmount(rs.getLong("amount"));

        return transaction;
    }

    @Override
    public Transaction findById(long id) throws DAOWrapperException {
        Transaction transaction = null;

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(SQL_GET_TRANSACTION);) {

            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs != null && rs.next()) {
                transaction = getTransaction(rs);
            }

        } catch (SQLException sqle) {
            DAOWrapperException.printSQLException(sqle);
            throw new DAOWrapperException("SQLException", sqle);
        }

        return transaction;
    }

    @Override
    public List<Transaction> findByUser(long userId, Date startDate, Date endDate)
            throws DAOWrapperException {

        User user = userDAO.findById(userId);
        if (user == null) {
            return null;
        }

        List<Transaction> ret = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
                PreparedStatement psGetTransactions = conn
                .prepareStatement(SQL_GET_USER_TRANSACTIONS);) {

            psGetTransactions.setLong(1, userId);
            psGetTransactions.setLong(2, userId);
            psGetTransactions.setDate(3, startDate);
            psGetTransactions.setDate(4, endDate);
            ResultSet rs = psGetTransactions.executeQuery();
            while (rs != null && rs.next()) {
                ret.add(getTransaction(rs));
            }
        } catch (SQLException sqle) {
            DAOWrapperException.printSQLException(sqle);
            throw new DAOWrapperException("SQLException", sqle);
        }

        return ret;
    }

    private void updateBalance(Connection conn, long userId, long amount)
            throws DAODataException, SQLException {

        try (PreparedStatement psLockBalance = conn
                .prepareStatement(SQL_LOCK_BALANCE);
                PreparedStatement psGetBalance = conn
                .prepareStatement(SQL_GET_BALANCE_FOR_UPDATE);
                PreparedStatement psUpdateBalance = conn
                .prepareStatement(SQL_UPDATE_BALANCE);) {

            long balance;

            psLockBalance.setLong(1, userId);
            psLockBalance.executeUpdate();

            psGetBalance.setLong(1, userId);
            ResultSet rs = psGetBalance.executeQuery();
            if (rs != null && rs.next()) {
                balance = rs.getLong("balance");
            } else {
                throw new DAODataException("user " + userId + " does not exist!");
            }

            balance += amount;
            // check if user has enough balance
            if (balance < 0) {
                throw new DAODataException("user " + userId + " does not have enough money!");
            }

            psUpdateBalance.setLong(1, balance);
            psUpdateBalance.setLong(2, userId);
            psUpdateBalance.executeUpdate();
        }
    }

    private long insertTransaction(Connection conn, long fromUser, long toUser, long amount)
            throws SQLException, DAOWrapperException {
        try (PreparedStatement psInsertTransaction = conn
                .prepareStatement(SQL_INSERT_TRANSACTION,
                        PreparedStatement.RETURN_GENERATED_KEYS);) {

            psInsertTransaction.setLong(1, fromUser);
            psInsertTransaction.setLong(2, toUser);
            psInsertTransaction.setLong(3, amount);
            psInsertTransaction.executeUpdate();

            ResultSet rs = psInsertTransaction.getGeneratedKeys();
            if (rs != null && rs.next()) {
                return rs.getLong(1);
            } else {
                String msg = "can not get transaction id";
                System.err.println(msg);
                throw new DAOWrapperException(msg, null);
            }
        }
    }

    @Override
    public long newTransaction(long fromUser, long toUser, long amount) 
            throws DAODataException, DAOWrapperException {
        long transactionId;

        try (Connection conn = dataSource.getConnection();) {
            try {
                conn.setAutoCommit(false);

                // avoid database deadlock
                if (fromUser < toUser) {
                    updateBalance(conn, fromUser, -amount);
                    updateBalance(conn, toUser, amount);
                } else {
                    updateBalance(conn, toUser, amount);
                    updateBalance(conn, fromUser, -amount);
                }
                transactionId = insertTransaction(conn, fromUser, toUser, amount);

                conn.commit();
            } catch (SQLException | DAODataException | DAOWrapperException e) {
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException sqle) {
                        DAOWrapperException.printSQLException(sqle);
                    }
                }

                if (e instanceof SQLException) {
                    SQLException sqle = (SQLException) e;
                    DAOWrapperException.printSQLException(sqle);
                    throw new DAOWrapperException("SQLException", sqle);
                } else {
                    throw e;
                }
            }
        } catch (SQLException sqle) {
            DAOWrapperException.printSQLException(sqle);
            throw new DAOWrapperException("SQLException", sqle);
        }

        return transactionId;
    }

}
