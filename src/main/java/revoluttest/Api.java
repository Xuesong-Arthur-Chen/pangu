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
	public static final String SQL_GET_USER_TRANSACTIONS = "select transaction_time, from_user, to_user, amount from transactions where (from_user=? or to_user=?) and "
			+ "DATE(transaction_time)>=? and DATE(transaction_time)<=?";

	private Response errorResponse(int httpCode, String msg) {
		return Response.status(httpCode).entity("{\"error\": \"" + msg + "\"}")
				.type(MediaType.APPLICATION_JSON).build();
	}

	@GET
	@Path("balance/{userid}")
	public BalanceBean balance(@PathParam("userid") long userId) {
		// check req
		if (userId <= 0) {
			// return bad request: 400
			throw new BadRequestException(errorResponse(400, "invalid user id"));
		}

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
	public TransactionBean transaction(
			@PathParam("transactionid") long transactionId) {
		// check req
		if (transactionId <= 0) {
			// return bad request: 400
			throw new BadRequestException(errorResponse(400,
					"invalid transaction id"));
		}

		TransactionBean ret = null;
		try (Connection conn = Main.ds.getConnection();
				PreparedStatement ps = conn
						.prepareStatement(SQL_GET_TRANSACTION);) {

			ps.setLong(1, transactionId);
			ResultSet rs = ps.executeQuery();
			if (rs != null && rs.next()) {
				ret = new TransactionBean(rs.getTimestamp("transaction_time"),
						rs.getLong("from_user"), rs.getLong("to_user"),
						rs.getLong("amount"));
			} else {
				// return not found: 404
				throw new NotFoundException(errorResponse(404,
						"transaction not found"));
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
	public List<TransactionBean> transactions(
			@PathParam("userid") long userId,
			@DefaultValue("1970-01-01") @QueryParam("startdate") Date startDate,
			@DefaultValue("2100-01-01") @QueryParam("enddate") Date endDate) {
		// check req
		if (userId <= 0) {
			// return bad request: 400
			throw new BadRequestException(errorResponse(400, "invalid user id"));
		}

		List<TransactionBean> ret = new ArrayList<>();
		try (Connection conn = Main.ds.getConnection();
				PreparedStatement psCheckUser = conn
						.prepareStatement(SQL_GET_BALANCE);
				PreparedStatement psGetTransactions = conn
						.prepareStatement(SQL_GET_USER_TRANSACTIONS);) {

			psCheckUser.setLong(1, userId);
			ResultSet rs = psCheckUser.executeQuery();
			if (rs == null || !rs.next()) {
				// return not found: 404
				throw new NotFoundException(
						errorResponse(404, "user not found"));
			}

			psGetTransactions.setLong(1, userId);
			psGetTransactions.setLong(2, userId);
			psGetTransactions.setDate(3, startDate);
			psGetTransactions.setDate(4, endDate);
			rs = psGetTransactions.executeQuery();
			while (rs != null && rs.next()) {
				ret.add(new TransactionBean(
						rs.getTimestamp("transaction_time"), rs
								.getLong("from_user"), rs.getLong("to_user"),
						rs.getLong("amount")));
			}
		} catch (SQLException sqle) {
			Main.printSQLException(sqle);
			// return server error: 500
			throw new InternalServerErrorException();
		}
		return ret;
	}

	private void updateBalance(PreparedStatement psGetBalance,
			PreparedStatement psUpdateBalance, long userId, long amount)
			throws SQLException {
		long balance;
		psGetBalance.setLong(1, userId);
		ResultSet rs = psGetBalance.executeQuery();
		if (rs != null && rs.next()) {
			balance = rs.getLong("balance");
		} else {
			// return bad request: 400
			throw new BadRequestException(errorResponse(400, "user " + userId
					+ " does not exist!"));
		}
		
		balance += amount;
		// check if user has enough balance
		if (balance < 0) {
			// return bad request: 400
			throw new BadRequestException(errorResponse(400, "user " + userId
					+ " does not have enough money!"));
		}
		
		psUpdateBalance.setLong(1, balance);
		psUpdateBalance.setLong(2, userId);
		psUpdateBalance.executeUpdate();
	}

	@POST
	@Path("transfer")
	public TransactionIdBean transfer(TransferReqBean req) {
		// check req
		if (req.getAmount() <= 0) {
			// return bad request: 400
			throw new BadRequestException(errorResponse(400,
					"amount must be greater than 0!"));
		}
		if (req.getFrom() <= 0 || req.getTo() <= 0) {
			// return bad request: 400
			throw new BadRequestException(errorResponse(400, "invalid user id"));
		}
		if (req.getFrom() == req.getTo()) {
			// return bad request: 400
			throw new BadRequestException(errorResponse(400,
					"sender and receiver must be different users!"));
		}

		long transactionId;

		Connection conn = null;
		PreparedStatement psGetBalance = null;
		PreparedStatement psUpdateBalance = null;
		PreparedStatement psInsertTransaction = null;

		try {
			conn = Main.ds.getConnection();
			conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
			conn.setAutoCommit(false);

			psGetBalance = conn.prepareStatement(SQL_GET_BALANCE);
			psUpdateBalance = conn.prepareStatement(SQL_UPDATE_BALANCE);
			psInsertTransaction = conn.prepareStatement(SQL_INSERT_TRANSACTION,
					PreparedStatement.RETURN_GENERATED_KEYS);

			//avoid database deadlock
			if (req.getFrom() < req.getTo()) {
				updateBalance(psGetBalance, psUpdateBalance, req.getFrom(), -req.getAmount());
				updateBalance(psGetBalance, psUpdateBalance, req.getTo(), req.getAmount());
			} else {				
				updateBalance(psGetBalance, psUpdateBalance, req.getTo(), req.getAmount());
				updateBalance(psGetBalance, psUpdateBalance, req.getFrom(), -req.getAmount());
			}			

			// insert transaction
			psInsertTransaction.setLong(1, req.getFrom());
			psInsertTransaction.setLong(2, req.getTo());
			psInsertTransaction.setLong(3, req.getAmount());
			psInsertTransaction.executeUpdate();

			ResultSet rs = psInsertTransaction.getGeneratedKeys();
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
				} catch (SQLException sqle) {
					Main.printSQLException(sqle);
				}
				// return conn to connection pool
				try {
					conn.close();
				} catch (SQLException sqle) {
					Main.printSQLException(sqle);
				}
			}
		}

		return new TransactionIdBean(transactionId);
	}
}
