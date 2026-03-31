# ADC Individual Project – REST API

This project implements a RESTful backend service using Java, Google App Engine, and Datastore (Firestore in Datastore mode).

The system supports user management, authentication, and role-based authorization.

---

## How to run

### 1. Build the project
```bash
mvn clean package
```
### 2. Run localy
```bash
mvn appengine:run
```
The server will start locally at: `http://localhost:8080`

## Base URL
Local: `http://localhost:8080/rest/`
Deployed: `https://<your-project-id>.appspot.com/rest/`

## Avaliable Enpoints (POST)
| Endpoint | Description | Access |
| -- | -- | -- |
|`/createaccount`| Create new account | Public
| `/login` | Authenticate user | Public |
| `/logout` | Logout user (invalidate token) | Authenticated |
| `/showusers` | List all users | ADMIN/BOFFICER |
| `/showauthsessions` | List active sessions | ADMIN |
| `/showuserrole` | Get user role | ADMIN / BOFFICER |
| `/changeuserrole` | Change user role | ADMIN |
| `/deleteaccount` | Delete account | ADMIN |
| `/changeuserpwd` | Change own password | Authenticated (self) |
| `/modaccount` | Modify account data | Role-based |

## Authentication
- After login, a token is returned.
- This token must be included in protected requests.
- Tokens expire automatically and are invalidated on:
  - logout
  - password change
  - role change
  - account deletion

## Request Format
All requests use JSON:
```JSON
{
  "input": { ... },
  "token": { ... }
}
```
## Response Format
All responses follow:
```JSON
{
  "status": "success",
  "data": { ... }
}
```
or
```JSON
{
  "status": "ERROR_CODE",
  "data": "Error description"
}
```

## Data Storage
- Uses Google Cloud Datastore (Firestore in Datastore mode)
- Entities:
  - Account
  - Token

## Testing
Endpoints should be tested using:
- Postman
- or similar HTTP client
