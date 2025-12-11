package com.cotic.backend.usersapp.backend_usersapp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import com.cotic.backend.usersapp.backend_usersapp.repositories.UserRepository;

import javax.sql.DataSource;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DataSource dataSource;

    @Autowired
    public DataInitializer(UserRepository userRepository, DataSource dataSource) {
        this.userRepository = userRepository;
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            // Solo ejecuta el script si no hay usuarios
            ScriptUtils.executeSqlScript(dataSource.getConnection(), new ClassPathResource("data.sql"));
        }
    }
}
