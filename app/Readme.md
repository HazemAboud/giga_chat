# GigaChat Backend API Integration Guide

This document provides the necessary details for frontend developers to integrate with the GigaChat API.

## 1. Authentication
The backend uses JWT (JSON Web Tokens) for security. Most endpoints require an `Authorization` header.

**Header Format:** `Authorization: Bearer <access_token>`

### Auth Endpoints (Inferred from AuthService)
| Method | Route | Description |
|:--- |:--- |:--- |
| POST | `/api/v1/auth/register` | Register a new user. |
| POST | `/api/v1/auth/login` | Login and receive Access & Refresh tokens. |
| POST | `/api/v1/auth/refresh` | Exchange a Refresh Token for a new Access Token. |
| POST | `/api/v1/auth/logout` | Invalidate the current refresh token. |
| PUT | `/api/v1/auth/profile` | Update profile (bio, profile name, username). |
| POST | `/api/v1/auth/avatar` | Upload profile picture (Multipart/form-data). |

---

## 2. Connection Management
Handled by `ConnController`, these routes manage friend requests and blocking.

**Base URL:** `/api/v1/connections`

| Method | Route | Body (JSON) | Description |
|:--- |:--- |:--- |:--- |
| POST | `/request` | `{ "friendId": Long }` | Send a friend request to a user. |
| PUT | `/accept` | `{ "friendId": Long }` | Accept a received friend request. |
| PUT | `/reject` | `{ "friendId": Long }` | Reject a received friend request. |
| PUT | `/block` | `{ "friendId": Long }` | Block a specific user. |
| PUT | `/unblock` | `{ "friendId": Long }` | Unblock a previously blocked user. |
| GET | `/` | None | Get all connections for the logged-in user. |
| GET | `/requests/sent` | None | Get all pending requests sent by you. |
| GET | `/requests/received` | None | Get all pending requests sent to you. |

---

## 3. Messaging (Firestore)
Messages are stored in Google Cloud Firestore in a collection named `messages`.

**Message Types:**
- **TextMessage**: contains `textContent`.
- **ImageMessage**: contains `imageBlob`.
- **MixedMessage**: contains both `textContent` and `imageBlob`.

---

## 4. Data Models & Entities

### User Entity (Relational)
| Field | Type | Description |
|:--- |:--- |:--- |
| `userId` | Long | Unique identifier. |
| `username` | String | Unique login name. |
| `email` | String | User email address. |
| `password` | String | Hashed password. |
| `profileName`| String | Display name. |
| `bio` | String | User biography. |
| `profilePictureBlob` | byte[] | Raw image data (returned as Base64 in some responses). |
| `isOnline` | boolean | Current connection status. |
| `lastSeenTimestamp` | Instant | Last activity timestamp. |
| `createdAt` | Instant | Account creation timestamp. |

### Connection Entity (Relational)
| Field | Type | Description |
|:--- |:--- |:--- |
| `id` | Long | Connection ID. |
| `userId` | Long | The user who initiated the relationship. |
| `friendId` | Long | The target user. |
| `status` | String | `PENDING`, `ACCEPTED`, `REJECTED`, `BLOCKED`. |
| `createdAt` | Long | Epoch timestamp of creation. |
| `updatedAt` | Long | Epoch timestamp of last update. |

### Refresh Token Entity (Relational)
| Field | Type | Description |
|:--- |:--- |:--- |
| `id` | Long | Unique identifier for the token record. |
| `userId` | Long | Foreign key reference to the User. |
| `token` | String | The unique refresh token string. |
| `expiryDate` | Instant | Timestamp after which the token is invalid. |

### Message Document (NoSQL - Firestore)
| Field | Type | Description |
|:--- |:--- |:--- |
| `messageId` | String | Firestore Document ID. |
| `senderId` | Long | User ID of the sender. |
| `receiverId` | Long | User ID of the recipient. |
| `timestamp` | Timestamp | Firestore server timestamp. |
| `textContent`| String | (Optional) Text body. |
| `imageBlob` | byte[] | (Optional) Raw image data (Base64 in JSON). |

---

## 5. Error Handling
The API returns standard HTTP status codes:
- `200 OK` / `201 Created`: Success.
- `400 Bad Request`: Validation error or business logic violation (e.g., "Cannot send request to yourself").
- `401 Unauthorized`: Missing or expired token.
- `403 Forbidden`: Authenticated but lacks permission.
- `404 Not Found`: Resource does not exist.

### Example Error Response
```json
{
  "timestamp": "2023-10-27T10:00:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "You have already sent a friend request to this user.",
  "path": "/api/v1/connections/request"
}
```
```

<!--
[PROMPT_SUGGESTION]Can you generate a Swagger/OpenAPI configuration for these controllers?[PROMPT_SUGGESTION]
[PROMPT_SUGGESTION]How should the frontend handle the byte array profilePictureBlob for image display?[PROMPT_SUGGESTION]
