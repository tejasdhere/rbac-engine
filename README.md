# Dynamic RBAC Authorization Engine

A production-grade, dynamic Role-Based Access Control (RBAC) authorization engine built with Spring Boot. Roles, permissions, and user-role-permission assignments are fully persisted in a relational database and evaluated dynamically at runtime on every request.

---

## 1. Overview

Traditional Spring Security applications hardcode role checks directly in source code using annotations like `@Secured("ROLE_ADMIN")`, `@PreAuthorize("hasRole('ADMIN')")`, or route-level checks like `hasAuthority(...)`. This tightly couples authorization logic to static compilation artifacts: modifying permissions, adding custom roles, or adjusting access requires code changes, reviews, and redeployment.

This engine decouples authorization completely from code:
- **No Hardcoded Role Names**: Roles (e.g., `ADMIN`, `USER`) and permissions are database rows.
- **Runtime Dynamic Authorization**: Permissions are checked dynamically using Spring Security's `PermissionEvaluator` via `@PreAuthorize("hasPermission(null, 'PERMISSION_NAME')")`.
- **Immediate Effect**: Granting or revoking permissions in the database takes effect on the next HTTP request without server restarts.
- **Clean Architecture & Separation of Concerns**: Controllers remain thin and security-annotated; services manage domain rules and transactions; security checks are isolated to a single dedicated evaluator.

---

## 2. Tech Stack

- **Java**: 21 (LTS)
- **Framework**: Spring Boot 4.0.0
- **Security**: Spring Security 7
- **Persistence**: Spring Data JPA, Hibernate 7
- **Database**: H2 Database Engine running in **FILE mode** (`jdbc:h2:file:./data/rbacdb`)
- **Validation**: Jakarta Validation (`jakarta.validation.*`)
- **Build & Dependency Management**: Apache Maven (`./mvnw` / `.\mvnw`)
- **Boilerplate Reduction**: Project Lombok
- **Testing**: JUnit 5, Mockito, Spring Security Test, MockMvc
- **Test Coverage**: JaCoCo Maven Plugin (0.8.12)
- **Static Analysis & Quality**: SonarQube via `sonar-maven-plugin` (5.0.0.4389)

---

## 3. Project Structure

Base package: `com.project.rbac_engine`

```
com.project.rbac_engine
├── RbacEngineApplication.java            # Spring Boot application entry point
│
├── config
│   └── DataSeeder.java                   # Idempotent bootstrap database seeder
│
├── controller
│   ├── PermissionController.java         # POST /permissions
│   ├── RoleController.java               # POST /roles, POST /roles/{roleId}/permissions/{permissionId}
│   ├── SecureDataController.java         # GET /secure-data
│   └── UserRoleController.java           # POST /users/{userId}/roles/{roleId}
│
├── dto
│   ├── PermissionRequest.java            # Request body for permission creation
│   ├── PermissionResponse.java           # Response body for permission creation
│   ├── RoleRequest.java                  # Request body for role creation
│   └── RoleResponse.java                 # Response body for role creation
│
├── entity
│   ├── Permission.java                   # permissions table
│   ├── Role.java                         # roles table
│   ├── RolePermission.java               # role_permissions join table (unique role_id + permission_id)
│   ├── User.java                         # users table
│   └── UserRole.java                     # user_roles join table (unique user_id + role_id)
│
├── exception
│   ├── DuplicateResourceException.java   # RuntimeException mapped to HTTP 409
│   ├── ErrorResponse.java                # Uniform error JSON record
│   ├── GlobalExceptionHandler.java       # Centralized @RestControllerAdvice exception handler
│   └── ResourceNotFoundException.java    # RuntimeException mapped to HTTP 404
│
├── repository
│   ├── PermissionRepository.java         # JPQL query to load user permissions dynamically
│   ├── RolePermissionRepository.java     # Role-Permission mapping repository
│   ├── RoleRepository.java               # Role entity repository
│   ├── UserRepository.java               # User entity repository
│   └── UserRoleRepository.java           # User-Role mapping repository
│
├── security
│   ├── CustomPermissionEvaluator.java    # PermissionEvaluator checking runtime DB permissions
│   ├── CustomUserDetailsService.java     # UserDetailsService returning user with EMPTY authorities
│   ├── MethodSecurityConfig.java         # @EnableMethodSecurity & static MethodSecurityExpressionHandler
│   └── SecurityConfig.java               # Stateless HTTP Basic filter chain (authentication-only)
│
└── service
    ├── PermissionService.java            # Permission business logic
    ├── RoleService.java                  # Role creation and role-permission assignment
    └── UserRoleService.java              # User-role assignment
```

---

## 4. Setup and Run

### Prerequisites
- JDK 21 installed and on your system `PATH`
- Git

### Build the Application
```bash
# Windows
.\mvnw clean package

# Linux / macOS
./mvnw clean package
```

### Run the Application
```bash
# Windows
.\mvnw spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

The application starts on `http://localhost:8080`.

### Database & Storage Details
- **Persistent Storage**: Data is stored on disk in the `./data/` directory (`./data/rbacdb.mv.db`). The database survives application restarts.
- **H2 Console**: Accessible during development at `http://localhost:8080/h2-console`
  - **JDBC URL**: `jdbc:h2:file:./data/rbacdb`
  - **User**: `sa`
  - **Password**: *(leave blank)*

### Bootstrap Default Accounts
On initial startup, `DataSeeder` seeds bootstrap accounts with BCrypt-hashed passwords:

| Username | Password | Assigned Role | Assigned Permissions |
|---|---|---|---|
| `admin` | `admin123` | `ADMIN` | `ROLE_CREATE`, `PERMISSION_CREATE`, `ROLE_PERMISSION_ASSIGN`, `USER_ROLE_ASSIGN` |
| `user` | `user123` | `USER` | `SECURE_DATA_READ` |

---

## 5. Authorization Flow

Every incoming HTTP request undergoes a clear two-stage verification process:

```
[ Client Request ]
       │
       ▼
[ SecurityFilterChain ] (SecurityConfig)
       │
       ├──> Authentication (HTTP Basic credentials: username + password)
       │    └── CustomUserDetailsService queries DB by username
       │    └── Password verified using BCryptPasswordEncoder
       │    └── Authentication principal stored in SecurityContext (EMPTY authorities)
       │
       ▼
[ DispatcherServlet & Spring MVC ]
       │
       ▼
[ Controller Method with @PreAuthorize("hasPermission(null, 'XYZ')") ]
       │
       ▼
[ MethodSecurityInterceptor ]
       │
       ▼
[ CustomPermissionEvaluator.hasPermission(...) ]
       │
       ├── 1. Validates Authentication is authenticated and principal exists
       ├── 2. Queries PermissionRepository.findPermissionNamesByUserId(user.getId())
       │      (traverses users -> user_roles -> roles -> role_permissions -> permissions in DB)
       └── 3. Checks if list contains requested permission string
              ├── TRUE  ─> Controller method executes (200 / 201)
              └── FALSE ─> Throws AccessDeniedException ─> GlobalExceptionHandler returns 403 Forbidden
```

---

## 6. Permission Evaluation Logic

Authorization checks do not inspect role names or static authority strings. Instead, `PermissionRepository` executes a dynamic SQL query across the relational mapping tables:

```java
@Query("""
        SELECT DISTINCT p.name
          FROM UserRole ur
          JOIN RolePermission rp ON rp.role = ur.role
          JOIN Permission p      ON p = rp.permission
         WHERE ur.user.id = :userId
        """)
List<String> findPermissionNamesByUserId(@Param("userId") Long userId);
```

When `CustomPermissionEvaluator.hasPermission(Authentication auth, Object targetDomainObject, Object permission)` executes:
1. It verifies `auth.isAuthenticated() == true`.
2. It verifies `permission instanceof String`.
3. It resolves the authenticated `User` from the database.
4. It calls `findPermissionNamesByUserId(user.getId())`.
5. It returns `grantedPermissions.contains(permissionName)`.

Because permissions are loaded directly from the database on each method invocation, any administrative change (such as granting a new permission to a role) takes effect immediately for active users.

---

## 7. How PermissionEvaluator is Registered

Method security is activated in `MethodSecurityConfig`:

```java
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {

    @Bean
    public static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            CustomPermissionEvaluator customPermissionEvaluator) {

        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(customPermissionEvaluator);
        return handler;
    }
}
```

### Why a `static @Bean` Method?
Spring Security's authorization proxy infrastructure initializes `MethodSecurityExpressionHandler` very early during container startup. Declaring the `@Bean` method as `static` allows Spring to instantiate the expression handler and inject `CustomPermissionEvaluator` without prematurely instantiating the enclosing `@Configuration` class, preventing cyclic bean dependency issues.

---

## 8. Why Hardcoded Roles are Avoided

1. **Zero Tight Coupling**: Hardcoding `@PreAuthorize("hasRole('ADMIN')")` freezes roles in code. If business requirements introduce a `MANAGER` or `AUDITOR` role with a subset of administrative powers, code must be modified, recompiled, and redeployed.
2. **Fine-Grained Principle of Least Privilege**: Roles are simply labeled aggregations of permissions. Business logic checks granular capabilities (`ROLE_CREATE`, `USER_ROLE_ASSIGN`), not broad job titles.
3. **Runtime Dynamism**: Dynamic permissions allow administrators to create roles, define custom permission bundles, and adjust access on a running system without downtime.
4. **Empty Spring Security Authorities**: In `CustomUserDetailsService`, `UserDetails.getAuthorities()` intentionally returns an empty list. All authorization is funneled through `CustomPermissionEvaluator`, eliminating conflicting or competing authorization paths.

---

## 9. Permissions & Endpoints Matrix

| HTTP Method | Endpoint | Required Permission | Description |
|---|---|---|---|
| `POST` | `/roles` | `ROLE_CREATE` | Create a new role |
| `POST` | `/permissions` | `PERMISSION_CREATE` | Create a new permission |
| `POST` | `/roles/{roleId}/permissions/{permissionId}` | `ROLE_PERMISSION_ASSIGN` | Map a permission to a role |
| `POST` | `/users/{userId}/roles/{roleId}` | `USER_ROLE_ASSIGN` | Map a role to a user |
| `GET` | `/secure-data` | `SECURE_DATA_READ` | Access protected business data |

---

## 10. API Request Flow & cURL Examples

### 1. Create a Role (`POST /roles`)
Requires `ROLE_CREATE`.

```bash
curl -i -u admin:admin123 -X POST http://localhost:8080/roles \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"COMPLIANCE_OFFICER\"}"
```
**Expected Response (201 Created):**
```json
{
  "id": 3,
  "name": "COMPLIANCE_OFFICER"
}
```

### 2. Create a Permission (`POST /permissions`)
Requires `PERMISSION_CREATE`.

```bash
curl -i -u admin:admin123 -X POST http://localhost:8080/permissions \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"AUDIT_LOG_EXPORT\"}"
```
**Expected Response (201 Created):**
```json
{
  "id": 6,
  "name": "AUDIT_LOG_EXPORT"
}
```

### 3. Assign Permission to Role (`POST /roles/{roleId}/permissions/{permissionId}`)
Requires `ROLE_PERMISSION_ASSIGN`.

```bash
curl -i -u admin:admin123 -X POST http://localhost:8080/roles/3/permissions/6
```
**Expected Response (201 Created):**
```json
{
  "message": "Permission assigned to role successfully"
}
```

### 4. Assign Role to User (`POST /users/{userId}/roles/{roleId}`)
Requires `USER_ROLE_ASSIGN`.

```bash
curl -i -u admin:admin123 -X POST http://localhost:8080/users/2/roles/3
```
**Expected Response (201 Created):**
```json
{
  "message": "Role assigned to user successfully"
}
```

### 5. Access Secure Data (`GET /secure-data`)
Requires `SECURE_DATA_READ`.

```bash
curl -i -u user:user123 http://localhost:8080/secure-data
```
**Expected Response (200 OK):**
```json
{
  "message": "Access granted to secure data"
}
```

---

### Error Scenario Examples

#### 400 Bad Request (Validation Error)
```bash
curl -i -u admin:admin123 -X POST http://localhost:8080/roles \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"\"}"
```
```json
{
  "timestamp": "2026-11-02T15:37:22.464322900Z",
  "status": 400,
  "error": "Bad Request",
  "message": "name: Role name must not be blank",
  "path": "/roles"
}
```

#### 401 Unauthorized (Missing Credentials)
```bash
curl -i http://localhost:8080/secure-data
```
```json
{
  "timestamp": "2026-11-02T15:38:41.219945Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/secure-data"
}
```

#### 403 Forbidden (Insufficient Permissions)
`user` attempts to create a role (lacks `ROLE_CREATE`):
```bash
curl -i -u user:user123 -X POST http://localhost:8080/roles \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"AUDITOR\"}"
```
```json
{
  "timestamp": "2026-11-02T15:38:28.754465100Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied",
  "path": "/roles"
}
```

#### 404 Not Found (Entity Does Not Exist)
```bash
curl -i -u admin:admin123 -X POST http://localhost:8080/users/999/roles/1
```
```json
{
  "timestamp": "2026-11-02T15:38:11.534634Z",
  "status": 404,
  "error": "Not Found",
  "message": "User not found with id: 999",
  "path": "/users/999/roles/1"
}
```

#### 409 Conflict (Duplicate Resource / Mapping)
```bash
curl -i -u admin:admin123 -X POST http://localhost:8080/roles \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"ADMIN\"}"
```
```json
{
  "timestamp": "2026-11-02T15:37:44.649283900Z",
  "status": 409,
  "error": "Conflict",
  "message": "Role already exists with name: ADMIN",
  "path": "/roles"
}
```

---

## 11. Error Response Format

All application errors return a uniform JSON format:

```json
{
  "timestamp": "2026-11-02T15:37:22.464322900Z",
  "status": 400,
  "error": "Bad Request",
  "message": "name: Role name must not be blank",
  "path": "/roles"
}
```

| Field | Type | Description |
|---|---|---|
| `timestamp` | `Instant` (ISO-8601) | Exact timestamp of error occurrence |
| `status` | `int` | HTTP status code (400, 401, 403, 404, 409, 500) |
| `error` | `String` | HTTP error reason phrase |
| `message` | `String` | Actionable, user-friendly error description (internal details sanitized) |
| `path` | `String` | Request URI that triggered the failure |

---

## 12. Testing & Quality Assurance

The test suite consists of **57 automated tests** spanning unit, web slice, and integration tests with **100% line coverage** across business and security components.

```bash
# Execute test suite and generate JaCoCo report
.\mvnw clean verify
```

### Test Summary
- **Total Tests Run**: 57
- **Failures**: 0
- **Errors**: 0
- **Skipped**: 0
- **JaCoCo Line Coverage**: **99.6%** (100% on all controllers, services, repositories, evaluators, and exception handlers)

### Coverage Report Location
The HTML coverage report is automatically generated at:
```
target/site/jacoco/index.html
```

---

## 13. SonarQube Code Quality

The project is preconfigured with the `sonar-maven-plugin` in `pom.xml`.

### SonarQube Scan Command
```bash
# Windows
.\mvnw clean verify sonar:sonar -Dsonar.token=YOUR_TOKEN

# Linux / macOS
./mvnw clean verify sonar:sonar -Dsonar.token=YOUR_TOKEN
```
*(Replace `YOUR_TOKEN` with your SonarQube authentication token; never commit credentials into version control).*

### Quality Gate Metrics
- **Quality Gate**: **Passed**
- **Line Coverage**: **99.6%**
- **Duplicated Lines**: **0.0%**
- **Reliability Rating**: **A**
- **Security Rating**: **A**
- **Maintainability Rating**: **A**

![SonarQube Dashboard](docs/sonar-dashboard.png)

---

## 14. Assumptions and Design Decisions

1. **Seeded Bootstrap Administrator**:
   To bootstrap an empty database, an initial administrator user (`admin` / `admin123`) and basic roles/permissions must exist. This seed data is inserted dynamically via `DataSeeder` on startup into the database file, not hardcoded into Java security logic.
2. **Roles as Database Records**:
   `ADMIN` and `USER` are strictly rows in the `roles` table. In code, methods are guarded exclusively by specific permission names (e.g., `ROLE_CREATE`), never by role names.
3. **Persistent H2 File Mode**:
   H2 is configured in file mode (`jdbc:h2:file:./data/rbacdb`) to persist state across application restarts while maintaining a zero-dependency local setup. Tests run in an isolated in-memory H2 database (`jdbc:h2:mem:testdb`) configured via `src/test/resources/application.yml` so production database files are never mutated during automated test runs.
4. **Stateless HTTP Basic Authentication**:
   The engine is designed as a stateless REST API with session creation disabled (`SessionCreationPolicy.STATELESS`). Credentials accompany every request.
5. **No Intermediate Caching**:
   Permission lookups query the database directly on every request (`PermissionRepository.findPermissionNamesByUserId`). This guarantees real-time consistency: permission changes made by administrators take effect immediately without requiring cache eviction or synchronization logic.
6. **Sonar Security Note — CSRF Disabled**:
   SonarQube flags CSRF disabled (`csrf.disable()`) as a security review item. In this application, this is an intentional, documented design decision: the API is entirely stateless, relies on HTTP Basic headers rather than browser session cookies, and has no UI or browser-based session cookies, making CSRF protection inapplicable.

