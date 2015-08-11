/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package snowpine.pangu.dao;

import java.sql.SQLException;

/**
 *
 * @author Xuesong
 */
public class DAOWrapperException extends Exception{
    public DAOWrapperException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static void printSQLException(SQLException e) {
        // Unwraps the entire exception chain to unveil the real cause of the Exception.
        while (e != null) {
            System.err.println("\n----- SQLException -----");
            System.err.println("  SQL State:  " + e.getSQLState());
            System.err.println("  Error Code: " + e.getErrorCode());
            System.err.println("  Message:    " + e.getMessage());
            // for stack traces, refer to derby.log or uncomment this:
            // e.printStackTrace(System.err);
            e = e.getNextException();
        }
    }
}
