package com.cotic.backend.usersapp.backend_usersapp.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cotic.backend.usersapp.backend_usersapp.models.IUser;
import com.cotic.backend.usersapp.backend_usersapp.models.dto.UserDto;
import com.cotic.backend.usersapp.backend_usersapp.models.dto.mapper.DtoMapperUser;
import com.cotic.backend.usersapp.backend_usersapp.models.entities.Role;
import com.cotic.backend.usersapp.backend_usersapp.models.entities.User;
import com.cotic.backend.usersapp.backend_usersapp.models.request.UserRequest;
import com.cotic.backend.usersapp.backend_usersapp.repositories.RoleRepository;
import com.cotic.backend.usersapp.backend_usersapp.auth.TokenJwtConfig;
import com.cotic.backend.usersapp.backend_usersapp.auth.SimpleGrantedAuthorityJsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SecurityException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Arrays;
import com.cotic.backend.usersapp.backend_usersapp.repositories.UserRepository;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository repo;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository repo, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> findAll() {
        List<User> users = (List<User>) repo.findAll();
        return users
                .stream()
                .map(u -> DtoMapperUser.builder().setUser(u).build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDto> findById(Long id) {

        return repo.findById(id).map(u -> DtoMapperUser.builder().setUser(u).build());

    }

    @Override
    @Transactional
    public UserDto save(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRoles(getRoles(user));

        return DtoMapperUser.builder().setUser(repo.save(user)).build();
    }

    @Override
    @Transactional
    public void remove(Long id) {
        repo.deleteById(id);
    }

    @Override
    @Transactional
    public Optional<UserDto> update(UserRequest user, Long id) {
        Optional<User> o = repo.findById(id);
        User userOptional = null;
        if (o.isPresent()) {
            User userdb = o.orElseThrow();
            userdb.setRoles(getRoles(user));
            userdb.setUsername(user.getUsername());
            userdb.setEmail(user.getEmail());
            userOptional = repo.save(userdb);
        }
        return Optional.ofNullable(DtoMapperUser.builder().setUser(userOptional).build());
    }

    /**
     * Valida un token JWT usando la clave secreta configurada.
     * 
     * @param token el token JWT (puede incluir o no el prefijo 'Bearer ')
     * @return true si el token es válido, false si es inválido o expirado
     */
    public boolean isTokenValid(String token) {
        if (token == null || token.isBlank())
            return false;
        String rawToken = token.startsWith(TokenJwtConfig.PREFIX_TOKEN)
                ? token.substring(TokenJwtConfig.PREFIX_TOKEN.length())
                : token;
        try {
            Jwts.parser()
                    .verifyWith(TokenJwtConfig.SECRET_KEY)
                    .build()
                    .parseSignedClaims(rawToken);
            return true;
        } catch (SecurityException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            // Token inválido
            return false;
        } catch (ExpiredJwtException e) {
            // Token expirado
            return false;
        }
    }

    /**
     * Valida y extrae información del token JWT.
     * 
     * @param token token (con o sin prefijo Bearer )
     * @return TokenValidationResult con valid, username y roles (nombres)
     */
    public TokenValidationResult validateTokenDetails(String token) {
        TokenValidationResult result = new TokenValidationResult();
        if (token == null || token.isBlank()) {
            result.setValid(false);
            return result;
        }
        String rawToken = token.startsWith(TokenJwtConfig.PREFIX_TOKEN)
                ? token.substring(TokenJwtConfig.PREFIX_TOKEN.length())
                : token;
        try {
            Claims claims = Jwts.parser().verifyWith(TokenJwtConfig.SECRET_KEY).build().parseSignedClaims(rawToken)
                    .getPayload();
            String username = claims.getSubject();
            Object authoritiesClaims = claims.get("authorities");

            List<String> roles = Arrays
                    .asList(new ObjectMapper()
                            .addMixIn(SimpleGrantedAuthority.class, SimpleGrantedAuthorityJsonCreator.class)
                            .readValue(authoritiesClaims.toString().getBytes(), SimpleGrantedAuthority[].class))
                    .stream()
                    .map(SimpleGrantedAuthority::getAuthority)
                    .toList();

            result.setValid(true);
            result.setUsername(username);
            result.setRoles(roles);
            return result;
        } catch (JwtException | IllegalArgumentException | java.io.IOException e) {
            result.setValid(false);
            return result;
        }
    }

    private List<Role> getRoles(IUser user) {
        Optional<Role> ou = roleRepository.findByName("ROLE_USER");

        List<Role> roles = new ArrayList<>();
        if (ou.isPresent()) {
            roles.add(ou.orElseThrow());
        }

        if (user.isAdmin()) {
            Optional<Role> oa = roleRepository.findByName("ROLE_ADMIN");
            if (oa.isPresent()) {
                roles.add(oa.orElseThrow());
            }
        }

        return roles;
    }

}
