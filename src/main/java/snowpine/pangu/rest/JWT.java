/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package snowpine.pangu.rest;

import javax.crypto.spec.SecretKeySpec;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.lang.JoseException;

/**
 *
 * @author Xuesong
 */
public class JWT {

    private static final byte[] keyBytes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 
                                            16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};
    private static final SecretKeySpec key = new SecretKeySpec(keyBytes, "HmacSHA256");
    private static final String keyId = "AuthKey";
    private static final String issuer = "pangu";
    private static final String audience = "ApiClient";

    public static final String generateJwt(JwtClaims claims) throws JoseException {
        claims.setIssuer(issuer);
        claims.setAudience(audience);
        //jws
        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        jws.setKey(key);
        jws.setKeyIdHeaderValue(keyId);
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA256);

        return jws.getCompactSerialization();        
    }

    public static final JwtClaims verifyJwt(String jwt) {
        JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                .setRequireExpirationTime()
                .setAllowedClockSkewInSeconds(30)
                .setRequireSubject()
                .setExpectedIssuer(true, issuer)
                .setExpectedAudience(true, audience)
                .setVerificationKey(key)
                .build();
        JwtClaims jwtClaims = null;
        try {
            jwtClaims = jwtConsumer.processToClaims(jwt);
        } catch (InvalidJwtException e) {
            System.err.println("Invalid JWT! " + e);
        }
        
        return jwtClaims;
    }
}
