# Runtime RBAC Verification & Dynamic Proof

Verification conducted against the running application (`http://localhost:8080`) backed by persistent H2 file database (`./data/rbacdb`).

---

## Part 1: Core Authorization Checks

### 1. Admin creates a role -> Expect 201 Created

**Command:**
```powershell
curl.exe -i -u admin:admin123 -X POST http://localhost:8080/roles -H "Content-Type: application/json" -d '{\"name\":\"TEST_ROLE_ADMIN\"}'
```

**Output:**
```http
HTTP/1.1 201 
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: SAMEORIGIN
Content-Type: application/json
Transfer-Encoding: chunked
Date: Mon, 02 Nov 2026 19:09:11 GMT

{"id":35,"name":"TEST_ROLE_ADMIN"}
```

---

### 2. User tries to create a role -> Expect 403 Forbidden

**Command:**
```powershell
curl.exe -i -u user:user123 -X POST http://localhost:8080/roles -H "Content-Type: application/json" -d '{\"name\":\"TEST_ROLE_USER_DENIED\"}'
```

**Output:**
```http
HTTP/1.1 403 
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: SAMEORIGIN
Content-Type: application/json
Transfer-Encoding: chunked
Date: Mon, 02 Nov 2026 19:09:24 GMT

{"timestamp":"2026-11-02T19:09:24.198418600Z","status":403,"error":"Forbidden","message":"Access denied","path":"/roles"}
```

---

### 3. User reads secure data -> Expect 200 OK

**Command:**
```powershell
curl.exe -i -u user:user123 http://localhost:8080/secure-data
```

**Output:**
```http
HTTP/1.1 200 
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: SAMEORIGIN
Content-Type: application/json
Transfer-Encoding: chunked
Date: Mon, 02 Nov 2026 19:09:49 GMT

{"message":"Access granted to secure data"}
```

---

### 4. No credentials reads secure data -> Expect 401 Unauthorized

**Command:**
```powershell
curl.exe -i http://localhost:8080/secure-data
```

**Output:**
```http
HTTP/1.1 401 
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: SAMEORIGIN
Content-Type: application/json;charset=UTF-8
Content-Length: 172
Date: Mon, 02 Nov 2026 19:10:04 GMT

{"timestamp":"2026-11-02T19:10:04.297918800Z","status":401,"error":"Unauthorized","message":"Full authentication is required to access this resource","path":"/secure-data"}
```

---

## Part 2: Dynamic RBAC Proof (Runtime Permission Mutation)

This sequence proves that authorization rules update dynamically at runtime directly from the database without server restart.

### Step 1: Standard `user` attempts role creation -> Denied (403 Forbidden)

**Command:**
```powershell
curl.exe -i -u user:user123 -X POST http://localhost:8080/roles -H "Content-Type: application/json" -d '{\"name\":\"ROLE_DYNAMIC_1\"}'
```

**Output:**
```http
HTTP/1.1 403 
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: SAMEORIGIN
Content-Type: application/json
Transfer-Encoding: chunked
Date: Mon, 02 Nov 2026 19:10:15 GMT

{"timestamp":"2026-11-02T19:10:15.241655900Z","status":403,"error":"Forbidden","message":"Access denied","path":"/roles"}
```

---

### Step 2: `admin` dynamically assigns `ROLE_CREATE` (permissionId=1) to `USER` role (roleId=2) -> Success (201 Created)

**Command:**
```powershell
curl.exe -i -u admin:admin123 -X POST http://localhost:8080/roles/2/permissions/1
```

**Output:**
```http
HTTP/1.1 201 
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: SAMEORIGIN
Content-Type: application/json
Transfer-Encoding: chunked
Date: Mon, 02 Nov 2026 19:10:26 GMT

{"message":"Permission assigned to role successfully"}
```

---

### Step 3: Exact same `user` attempts role creation again -> Allowed (201 Created)

**Command:**
```powershell
curl.exe -i -u user:user123 -X POST http://localhost:8080/roles -H "Content-Type: application/json" -d '{\"name\":\"ROLE_DYNAMIC_2\"}'
```

**Output:**
```http
HTTP/1.1 201 
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: SAMEORIGIN
Content-Type: application/json
Transfer-Encoding: chunked
Date: Mon, 02 Nov 2026 19:10:37 GMT

{"id":36,"name":"ROLE_DYNAMIC_2"}
```

