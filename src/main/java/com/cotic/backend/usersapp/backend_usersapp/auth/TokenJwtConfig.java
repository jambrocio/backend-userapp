package com.cotic.backend.usersapp.backend_usersapp.auth;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Jwts;

public class TokenJwtConfig {

    //public final static String SECRET_KEY = "algun_token_con_alguna_frase_secreta";
    public final static SecretKey SECRET_KEY = Jwts.SIG.HS256.key().build();
    public final static String PREFIX_TOKEN = "Bearer ";
    public final static String HEADER_AUTHORIZATION = "Authorization";

}
