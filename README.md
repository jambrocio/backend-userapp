# backend-usersapp

Aplicación Spring Boot (Java 17) para gestión de usuarios — proyecto de ejemplo.

Resumen rápido
- Spring Boot 3.4.11
- Java 17
- JPA (MySQL)
- Seguridad: Spring Security + JWT (jjwt 0.12.x)
- Documentación OpenAPI / Swagger: springdoc-openapi-starter-webmvc-ui

Contenido relevante
- `src/main/java/.../auth/TokenJwtConfig.java` — configuración de secret, prefijo y header
- `src/main/java/.../auth/SpringSecurityConfig.java` — reglas de seguridad (Swagger y `/users/validate-token` permitidos públicamente)
- `src/main/java/.../controllers/UserController.java` — endpoints REST (incluye `POST /users/validate-token`)
- `src/main/java/.../services/UserServiceImpl.java` — lógica de negocio y validación/parseo del JWT
- `src/main/resources/application.properties` — configuración de BD, puerto y rutas de springdoc
- `data.sql` — script de datos para precarga
- `src/main/java/.../DataInitializer.java` — ejecuta `data.sql` solo si la tabla `users` está vacía

Cómo ejecutar (Windows / PowerShell)
1. Asegúrate de tener JDK 17 instalado y `JAVA_HOME` configurado.
2. Configura la conexión a la base de datos en `src/main/resources/application.properties` (usuario, contraseña y URL).
3. Ejecuta (desde la raíz del proyecto):

```powershell
# descarga dependencias y compila (saltea tests)
.\mvnw -DskipTests package

# o ejecutar desde el wrapper
.\mvnw spring-boot:run
```

Endpoints principales
- Swagger UI (interfaz):
  - http://localhost:8082/swagger-ui-custom.html
  - También disponible: `/swagger-ui/index.html` y `/swagger-ui/`
- OpenAPI JSON:
  - http://localhost:8082/api-docs
- Usuarios (ejemplos):
  - GET /users — lista pública
  - GET /users/{id} — requiere rol USER o ADMIN
  - POST /users — requiere ADMIN
  - POST /users/validate-token — endpoint público para validar un JWT

Validate-token
- Ruta: POST /users/validate-token
- Body JSON: `{ "token": "Bearer eyJ..." }` (acepta con o sin prefijo `Bearer `)
- Respuesta JSON de ejemplo (válido):

```json
{
  "valid": true,
  "username": "juan",
  "roles": ["ROLE_USER"]
}
```

Si el token no es válido:

```json
{ "valid": false }
```

Carga de datos inicial (data.sql)
- Para evitar duplicados, el proyecto incluye `DataInitializer` que ejecuta `data.sql` solo cuando la tabla `users` está vacía (conteo == 0).
- Cambia esto si prefieres otra condición (por ejemplo, comprobar roles o tablas adicionales).

Seguridad y notas JWT
- La clave/secreto está definido en `TokenJwtConfig` usando `Jwts.SIG.HS256.key().build()` (llave auto-generada en código).
- En producción deberías almacenar/inyectar la clave en un secreto seguro (Key Vault, envvar, ficheros protegidos) y no usar una clave generada en tiempo de ejecución.