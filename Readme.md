# GigaChat Backend API Integration Guide

**Base URL:** `http://localhost:8080/api/v1`

---

## 1. Authentication

All endpoints except `/auth/**` require JWT bearer auth.

**Header:** `Authorization: Bearer <access_token>`

### Auth Endpoints (`/auth`)

| Method | Route | Body | Description |
|:---|:---|:---|:---|
| POST | `/auth/register` | `RegisterRequest` | Create account. Returns `AuthResponse` (access + refresh tokens). |
| POST | `/auth/login` | `LoginRequest` | Login. Returns `AuthResponse`. |
| POST | `/auth/refresh` | `TokenRefreshRequest` | Exchange refresh token for new token pair. |
| POST | `/auth/logout` | `TokenRefreshRequest` | Invalidate refresh token. Returns **204 No Content**. |
| PUT | `/auth/profile` | `UpdateProfileRequest` | Update display name, username, bio. Returns updated `User`. |
| POST | `/auth/profile/avatar` | `MultipartFile` (form field `file`) | Upload/change profile picture. |

**`AuthResponse`** shape:
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "dGhpcyBpcyBh...",
  "expiresIn": 3600000
}
```

---

## 2. Connections (Friends) — `/connections`

Manage friend requests, blocking, and profile picture retrieval.

### Friend Management

| Method | Route | Body (`FriendRequest`) | Description |
|:---|:---|:---|:---|
| POST | `/connections/request` | `{ "friendId": Long }` | Send friend request. Returns **201 Created**. |
| PUT | `/connections/accept` | `{ "friendId": Long }` | Accept incoming request. |
| PUT | `/connections/reject` | `{ "friendId": Long }` | Reject incoming request. |
| PUT | `/connections/block` | `{ "friendId": Long }` | Block a user. |
| PUT | `/connections/unblock` | `{ "friendId": Long }` | Unblock a user. |

### Listing

| Method | Route | Description |
|:---|:---|:---|
| GET | `/connections` | Get all connections for the authenticated user. |
| GET | `/connections/requests/sent` | Pending requests you sent. |
| GET | `/connections/requests/received` | Pending requests sent to you. |

### Profile Pictures

| Method | Route | Description |
|:---|:---|:---|
| GET | `/connections/profile-picture/{userId}` | Returns raw image bytes (`image/png`). |
| GET | `/connections/profile-pictures?userIds=1,2,3` | Returns `Map<Long, String>` of userId → Base64. |

---

## 3. Groups — `/groups`

Full group lifecycle: create, search, join, manage members and roles.

### CRUD & Discovery

| Method | Route | Description |
|:---|:---|:---|
| POST | `/groups/create` | Create a group (creator becomes `ADMIN`). Body: `Group` JSON. |
| GET | `/groups/search?query=...` | Search public groups by name/description. |
| GET | `/groups/list` | List all public groups. |
| GET | `/groups/my-groups` | List groups the authenticated user belongs to. |
| GET | `/groups/{groupId}` | Get group details by ID. |

### Membership

| Method | Route | Description |
|:---|:---|:---|
| POST | `/groups/{groupId}/request-join` | Request to join a public group (role becomes `PENDING`). |
| GET | `/groups/{groupId}/members` | List members of a group. |
| POST | `/groups/{groupId}/leave` | Leave a group. |
| POST | `/groups/{groupId}/add/{userId}?role=MEMBER` | Admin: add a user with a specific role. |

### Admin Actions (require `ADMIN` role)

| Method | Route | Description |
|:---|:---|:---|
| GET | `/groups/{groupId}/requests` | List pending join requests. |
| PUT | `/groups/{groupId}/approve/{userId}` | Approve a join request (role → `MEMBER`). |
| PUT | `/groups/{groupId}/reject/{userId}` | Reject a join request (deletes the request). |
| PUT | `/groups/{groupId}/role/{userId}?role=ADMIN` | Change a member's role. |
| DELETE | `/groups/{groupId}/remove/{userId}` | Remove a member from the group. |

### Group Picture

| Method | Route | Description |
|:---|:---|:---|
| GET | `/groups/{groupId}/picture` | Returns raw image bytes (`image/png`). |
| POST | `/groups/{groupId}/picture` | Upload group picture (`MultipartFile`, field `file`). Admin only. |

---

## 4. Messaging — Firestore

Messages are stored in a Firestore collection named **`messages`**. The backend currently provides a single write endpoint (used internally). For real-time messaging, connect to Firestore directly from the client using the Firebase Web SDK.

### Message Document Shape

| Field | Type | Description |
|:---|:---|:---|
| `messageId` | String | Auto-generated Firestore document ID. |
| `senderId` | Long | ID of the sender. |
| `receiverId` | Long | ID of the recipient. |
| `timestamp` | Timestamp | Firestore server timestamp. |
| `textContent` | String | *(optional)* Plain text body. |
| `imageBlob` | bytes | *(optional)* Raw image data (encode as Base64 when sending/receiving over JSON). |

**Message subtypes** (use the fields above as needed):
- **TextMessage** — only `textContent`
- **ImageMessage** — only `imageBlob`
- **MixedMessage** — both `textContent` and `imageBlob`

---

## 5. Data Models

### User (`/auth/profile` response)

| Field | Type | Notes |
|:---|:---|:---|
| `userId` | Long | |
| `username` | String | Unique login name. |
| `email` | String | |
| `password` | String | Hashed (never returned). |
| `profileName` | String | Display name. |
| `bio` | String | |
| `profilePictureBlob` | byte[] | Use `/connections/profile-picture/{userId}` instead. |
| `isOnline` | boolean | |
| `lastSeenTimestamp` | Instant | |
| `createdAt` | Instant | |

### Connection

| Field | Type | Notes |
|:---|:---|:---|
| `id` | Long | |
| `userId` | Long | Initiator. |
| `friendId` | Long | Target user. |
| `status` | String | `PENDING`, `ACCEPTED`, `REJECTED`, `BLOCKED`. |
| `createdAt` | Long | Epoch millis. |
| `updatedAt` | Long | Epoch millis. |

### Group

| Field | Type | Notes |
|:---|:---|:---|
| `groupId` | Long | |
| `groupName` | String | Unique. |
| `description` | String | Max 255 chars. |
| `groupPictureBlob` | byte[] | Use `/groups/{groupId}/picture` instead. |
| `createdAt` | Instant | |
| `memberLimit` | int | Max members allowed. |
| `memberCount` | int | Current member count. |
| `memberLimitReached` | boolean | |
| `isPublic` | boolean | Public groups are discoverable. |

### GroupMembers (join table)

| Field | Type | Notes |
|:---|:---|:---|
| `id` | Long | |
| `group` | Group | |
| `user` | User | |
| `role` | String | `ADMIN`, `MEMBER`, or `PENDING` (for join requests). |
| `joinedAt` | Instant | |

### RefreshToken

| Field | Type |
|:---|:---|
| `id` | Long |
| `userId` | Long |
| `token` | String |
| `expiryDate` | Instant |

---

## 6. Error Handling

| Status | Meaning |
|:---|:---|
| 200 OK | Success. |
| 201 Created | Resource created. |
| 204 No Content | Success, no body (e.g., logout). |
| 400 Bad Request | Validation error or business rule violation. |
| 401 Unauthorized | Missing/expired/invalid token. |
| 403 Forbidden | Authenticated but lacks permission (e.g., non-admin). |
| 404 Not Found | Resource not found. |

### Error Response Shape
```json
{
  "timestamp": "2024-01-01T12:00:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "You have already sent a friend request to this user.",
  "path": "/api/v1/connections/request"
}
```

---

## 7. CORS

The server accepts requests from any origin with credentials. Allowed methods: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`.
