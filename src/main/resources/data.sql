-- Datos iniciales (opcional)
INSERT INTO roles (name) VALUES
('ROLE_ADMIN'),
('ROLE_USER'),
('ROLE_MODERATOR');

-- Datos de prueba de usuarios
-- Contraseña para todos: 123456 (encriptada con BCrypt)
INSERT INTO users (username, email, password) VALUES
('admin', 'admin@carritoapp.com', '$2a$12$oKjH4iWyDhq.9E25pj0daOefgjmvTGncEeUJIpbfQZIIQHtzxQHqa'),
('johndoe', 'john@gmail.com', '$2a$12$oKjH4iWyDhq.9E25pj0daOefgjmvTGncEeUJIpbfQZIIQHtzxQHqa'),
('janedoe', 'jane@gmail.com', '$2a$12$oKjH4iWyDhq.9E25pj0daOefgjmvTGncEeUJIpbfQZIIQHtzxQHqa');

-- Asignar roles a usuarios
INSERT INTO users_roles (user_id, role_id) VALUES
(1, 1), -- admin tiene rol ADMIN
(1, 2), -- admin también tiene rol USER
(2, 2), -- johndoe tiene rol USER
(3, 2), -- janedoe tiene rol USER
(3, 3); -- janedoe también tiene rol MODERATOR
