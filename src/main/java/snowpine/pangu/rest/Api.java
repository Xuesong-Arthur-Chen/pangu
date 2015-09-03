package snowpine.pangu.rest;

import java.sql.Date;
import java.util.List;
import javax.inject.Singleton;

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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import snowpine.pangu.dao.DAODataException;
import snowpine.pangu.dao.DAOObjs;
import snowpine.pangu.dao.DAOWrapperException;
import snowpine.pangu.dao.Transaction;

@Singleton
@Path("api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Api {

    public static Response errorResponse(int httpCode, String msg) {
        return Response.status(httpCode).entity("{\"error\": \"" + msg + "\"}")
                .type(MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("balance/{userid}")
    public BalanceRes balance(@PathParam("userid") long userId) {
        // check req
        if (userId <= 0) {
            throw new BadRequestException(errorResponse(400, "invalid user id"));
        }

        long balance;
        try {
            balance = DAOObjs.userDAO.getBalanceById(userId);
        } catch(DAODataException daode) {
            throw new BadRequestException(errorResponse(400, daode.getMessage()));
        } catch (DAOWrapperException daoe) {
            throw new InternalServerErrorException();
        }

        return new BalanceRes(balance);
    }

    @GET
    @Path("transaction/{transactionid}")
    public Transaction transaction(
            @PathParam("transactionid") long transactionId) {
        // check req
        if (transactionId <= 0) {
            throw new BadRequestException(errorResponse(400, "invalid transaction id"));
        }

        Transaction transaction = null;
        try {
            transaction = DAOObjs.transactionDAO.findById(transactionId);
        } catch (DAOWrapperException daoe) {
            throw new InternalServerErrorException();
        }

        if (transaction == null) {
            throw new NotFoundException(errorResponse(404, "transaction not found"));
        }

        return transaction;
    }

    @GET
    @Path("transactions/{userid}")
    public List<Transaction> transactions(
            @PathParam("userid") long userId,
            @DefaultValue("1970-01-01") @QueryParam("startdate") Date startDate,
            @DefaultValue("2100-01-01") @QueryParam("enddate") Date endDate) {
        // check req
        if (userId <= 0) {
            throw new BadRequestException(errorResponse(400, "invalid user id"));
        }

        List<Transaction> ret = null;
        try {
            ret = DAOObjs.transactionDAO.findByUser(userId, startDate, endDate);
        } catch(DAOWrapperException daoe) {
            throw new InternalServerErrorException();
        }
        
        if(ret == null) {
            throw new NotFoundException(errorResponse(404, "user not found"));
        }
        
        return ret;
    }

    @POST
    @Path("transfer")
    public TransferRes transfer(TransferReq req) {
        // check req
        if (req.getAmount() <= 0) {
            throw new BadRequestException(errorResponse(400,
                    "amount must be greater than 0!"));
        }
        if (req.getFrom() <= 0 || req.getTo() <= 0) {
            throw new BadRequestException(errorResponse(400, "invalid user id"));
        }
        if (req.getFrom() == req.getTo()) {
            throw new BadRequestException(errorResponse(400,
                    "sender and receiver must be different users!"));
        }

        long transactionId;
        try {
            transactionId = DAOObjs.transactionDAO.newTransaction(req.getFrom(), 
                    req.getTo(), req.getAmount());
        } catch(DAODataException daode) {
            throw new BadRequestException(errorResponse(400, daode.getMessage()));
        } catch(DAOWrapperException daowe) {
            throw new InternalServerErrorException();
        }

        return new TransferRes(transactionId);
    }
}
