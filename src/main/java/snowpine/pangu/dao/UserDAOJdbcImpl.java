/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package snowpine.pangu.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

/**
 *
 * @author Xuesong
 */
public class UserDAOJdbcImpl implements UserDAO {

    private static final String SQL_GET_USER = "select * from users where user_id=?";

    private final DataSource dataSource;

    public UserDAOJdbcImpl(DataSource ds) {
        this.dataSource = ds;
    }

    private User getUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("user_id"));
        user.setBalance(rs.getLong("balance"));

        return user;
    }

    @Override
    public User findById(long id) throws DAOWrapperException {
        User user = null;

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(SQL_GET_USER);) {

            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs != null && rs.next()) {
                user = getUser(rs);
            }

        } catch (SQLException sqle) {
            DAOWrapperException.printSQLException(sqle);
            throw new DAOWrapperException("SQLException", sqle);
        }

        return user;
    }
}
