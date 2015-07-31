package revoluttest;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Api {

	public static final String SQL_GET_BALANCE = "select balance from users where user_id=?";
	public static final String SQL_UPDATE_BALANCE = "update users set balance=? where user_id=?";
	public static final String SQL_INSERT_TRANSACTION = "insert into transactions(transaction_time, from_user, to_user, amount) values(CURRENT_TIMESTAMP, ?, ?, ?)";
	public static final String SQL_GET_TRANSACTION = "select transaction_time, from_user, to_user, amount from transactions where transaction_id=?";
	public static final String SQL_GET_USER_TRANSACTIONS = 
			"select transaction_time, from_user, to_user, amount from transactions where (from_user=? or to_user=?) and "
			+ "DATE(transaction_time)>=? and DATE(transaction_time)<=?";
	
	private Response errorResponse(int httpCode, String msg) {
		return Response.status(httpCode).entity("{\"error\": \"" + msg + "\"}")
				.type(MediaType.APPLICATION_JSON).build();
	}

	@GET
	@Path("balance/{userid}")
	public BalanceBean balance(@PathParam("userid") long userId) {
		long balance;
		try (Connection conn = Main.ds.getConnection();
				PreparedStatement ps = conn.prepareStatement(SQL_GET_BALANCE);) {

			ps.setLong(1, userId);
			ResultSet rs = ps.executeQuery();
			if (rs != null && rs.next()) {
				balance = rs.getLong("balance");
			} else {
				// return not found: 404
				throw new NotFoundException(
						errorResponse(404, "user not found"));
			}
		} catch (SQLException sqle) {
			Main.printSQLException(sqle);
			// return server error: 500
			throw new InternalServerErrorException();
		}
		return new BalanceBean(balance);
	}
	
	@GET
	@Path("transaction/{transactionid}")
	public TransactionBean transaction(@PathParam("transactionid") long transactionId) {
		TransactionBean ret = null;
		try (Connection conn = Main.ds.getConnection();
				PreparedStatement ps = conn.prepareStatement(SQL_GET_TRANSACTION);) {

			ps.setLong(1, transactionId);
			ResultSet rs = ps.executeQuery();
			if (rs != null && rs.next()) {
				ret = new TransactionBean(rs.getTimestamp("transaction_time"), rs.getLong("from_user"),
						rs.getLong("to_user"), rs.getLong("amount"));
			} else {
				// return not found: 404
				throw new NotFoundException(
						errorResponse(404, "transaction not found"));
			}
		} catch (SQLException sqle) {
			Main.printSQLException(sqle);
			// return server error: 500
			throw new InternalServerErrorException();
		}
		return ret;
	}
	
	@GET
	@Path("transactions/{userid}")
	public List<TransactionBean> transactions(@PathParam("userid") long userId,
			@DefaultValue("1970-01-01") @QueryParam("startdate") Date startDate,
			@DefaultValue("2100-01-01") @QueryParam("enddate") Date endDate) {
		List<TransactionBean> ret = new ArrayList<>();
		try (Connection conn = Main.ds.getConnection();
				PreparedStatement ps = conn.prepareStatement(SQL_GET_USER_TRANSACTIONS);) {

			ps.setLong(1, userId);
			ps.setLong(2, userId);
			ps.setDate(3, startDate);
			ps.setDate(4, endDate);
			ResultSet rs = ps.executeQuery();
			while (rs != null && rs.next()) {
				ret.add(new TransactionBean(rs.getTimestamp("transaction_time"), rs.getLong("from_user"),
						rs.getLong("to_user"), rs.getLong("amount")));
			}
		} catch (SQLException sqle) {
			Main.printSQLException(sqle);
			// return server error: 500
			throw new InternalServerErrorException();
		}
		return ret;
	}

	@POST
	@Path("transfer")
	public TransactionIdBean transfer(TransferReqBean req) {
		long transactionId;

		Connection conn = null;
		PreparedStatement psGetBalance = null;
		PreparedStatement psUpdateBalance = null;
		PreparedStatement psInsertTransaction = null;

		// check req
		if (req.amount <= 0) {
			// return bad request: 400
			throw new BadRequestException(errorResponse(400,
					"amount must be greater than 0!"));
		}

		try {
			conn = Main.ds.getConnection();
			conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
			conn.setAutoCommit(false);

			psGetBalance = conn.prepareStatement(SQL_GET_BALANCE);
			psUpdateBalance = conn.prepareStatement(SQL_UPDATE_BALANCE);
			psInsertTransaction = conn.prepareStatement(SQL_INSERT_TRANSACTION,
					PreparedStatement.RETURN_GENERATED_KEYS);

			long fromBalance, toBalance;

			// check if users exist and user 'from' has enough balance
			psGetBalance.setLong(1, req.from);
			ResultSet rs = psGetBalance.executeQuery();
			if (rs != null && rs.next()) {
				fromBalance = rs.getLong("balance");
			} else {
				// return bad request: 400
				throw new BadRequestException(errorResponse(400, "user "
						+ req.from + " does not exist!"));
			}
			if (fromBalance < req.amount) {
				// return bad request: 400
				throw new BadRequestException(errorResponse(400, "user "
						+ req.from + " does not have enough money!"));
			}
			psGetBalance.setLong(1, req.to);
			rs = psGetBalance.executeQuery();
			if (rs != null && rs.next()) {
				toBalance = rs.getLong("balance");
			} else {
				// return bad request: 400
				throw new BadRequestException(errorResponse(400, "user "
						+ req.to + " does not exist!"));
			}

			// update balance
			psUpdateBalance.setLong(1, fromBalance - req.amount);
			psUpdateBalance.setLong(2, req.from);
			psUpdateBalance.executeUpdate();

			psUpdateBalance.setLong(1, toBalance + req.amount);
			psUpdateBalance.setLong(2, req.to);
			psUpdateBalance.executeUpdate();

			// insert transaction
			psInsertTransaction.setLong(1, req.from);
			psInsertTransaction.setLong(2, req.to);
			psInsertTransaction.setLong(3, req.amount);
			psInsertTransaction.executeUpdate();

			rs = psInsertTransaction.getGeneratedKeys();
			if (rs != null && rs.next()) {
				transactionId = rs.getLong(1);
			} else {
				System.err.println("\nERROR: can not get transaction id\n");
				// return server error: 500
				throw new InternalServerErrorException();
			}

			conn.commit();

		} catch (SQLException | WebApplicationException e) {

			if (conn != null) {
				try {
					conn.rollback();
				} catch (SQLException sqle) {
					Main.printSQLException(sqle);
				}
			}

			if (e instanceof SQLException) {
				Main.printSQLException((SQLException) e);
				// return server error: 500
				throw new InternalServerErrorException();
			} else {
				throw (WebApplicationException) e;
			}

		} finally {
			if (psGetBalance != null) {
				try {
					psGetBalance.close();
				} catch (SQLException sqle) {
					Main.printSQLException(sqle);
				}
			}
			if (psUpdateBalance != null) {
				try {
					psUpdateBalance.close();
				} catch (SQLException sqle) {
					Main.printSQLException(sqle);
				}
			}
			if (psInsertTransaction != null) {
				try {
					psInsertTransaction.close();
				} catch (SQLException sqle) {
					Main.printSQLException(sqle);
				}
			}
			if (conn != null) {
				try {
					conn.setAutoCommit(true);
					conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
					conn.close();
				} catch (SQLException sqle) {
					Main.printSQLException(sqle);
				}
			}
		}

		return new TransactionIdBean(transactionId);
	}

}
