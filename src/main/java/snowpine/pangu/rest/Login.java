/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package snowpine.pangu.rest;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.inject.Singleton;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.lang.JoseException;
import snowpine.pangu.dao.DAOObjs;
import snowpine.pangu.dao.DAOWrapperException;
import snowpine.pangu.dao.User;

/**
 *
 * @author Xuesong
 */
@Singleton
@Path("login")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Login {
    @POST
    public LoginRes login(LoginReq req) {
        try {
        // check req
        if(req.getPassword() == null) {
            throw new BadRequestException(Api.errorResponse(400, "invalid email or password"));
        }

        User user = null;
        try {
            user = DAOObjs.userDAO.findById(req.getUserId());
        } catch (DAOWrapperException daoe) {
            throw new InternalServerErrorException();
        }

        if (user == null) {
            throw new BadRequestException(Api.errorResponse(400, "invalid email or password"));
        }

        Base64.Decoder dec = Base64.getDecoder();
        
        PBEKeySpec ks = new PBEKeySpec(req.getPassword().toCharArray(), dec.decode(user.getSalt()), 1000, 16);
        SecretKeyFactory kf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] passhash = kf.generateSecret(ks).getEncoded();
        if(!Arrays.equals(passhash, dec.decode(user.getPasshash()))) {
            throw new BadRequestException(Api.errorResponse(400, "invalid email or password"));
        }
        
        JwtClaims claims = new JwtClaims();
        claims.setSubject(String.valueOf(user.getId()));
        claims.setExpirationTimeMinutesInTheFuture(15);
        String token = JWT.generateJwt(claims);
        
        LoginRes res = new LoginRes(token);
        
        return res;
        } catch(NoSuchAlgorithmException | InvalidKeySpecException | JoseException e) {
            System.err.println(e);
            throw new InternalServerErrorException();
        }
    }
}
