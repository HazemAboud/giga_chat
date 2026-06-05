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

## 4. Messaging — `/messages`

Messages are stored in a Firestore collection named **`messages`**. For real-time messaging, connect to Firestore directly from the client using the Firebase Web SDK.

### Message Endpoints

| Method | Route | Body | Description |
|:---|:---|:---|:---|
| POST | `/messages` | `SendMessageRequest` | Send a text message. Returns the message ID. |
| GET | `/messages?userId={userId}` | — | Get conversation history with a specific user. |
| POST | `/messages/{messageId}/reactions` | `ReactionRequest` | Add or update a reaction on a message. |
| DELETE | `/messages/{messageId}/reactions` | — | Remove your reaction from a message. |

**`SendMessageRequest`** shape:
```json
{
  "receiverId": 2,
  "textContent": "Hello!"
}
```

Validation: `receiverId` must be a non-null Long; `textContent` must be non-blank. Returns **400** on violation.

**`ReactionRequest`** shape:
```json
{
  "emoji": "👍"
}
```

Validation: `emoji` must be non-blank and at most 10 characters. Returns **400** on violation.

### Message Document Shape

| Field | Type | Description |
|:---|:---|:---|
| `messageId` | String | Auto-generated Firestore document ID. |
| `senderId` | Long | ID of the sender. |
| `receiverId` | Long | ID of the recipient. |
| `timestamp` | Timestamp | Firestore server timestamp. |
| `textContent` | String | *(optional)* Plain text body. |
| `imageBlob` | bytes | *(optional)* Raw image data (encode as Base64 when sending/receiving over JSON). |
| `reactions` | Map\<String, String\> | *(optional)* Map of userId → emoji, e.g. `{"2": "👍", "3": "❤️"}`. |

### Reaction Behavior

- Each user can have **at most one reaction per message**. Adding a reaction when one already exists **updates** it to the new emoji.
- Removing a reaction (DELETE) deletes only the calling user's reaction entry.
- Reactions are stored inline in the message document as `reactions.<userId>`.

---

## 5. Real-Time Events — WebSocket

The server exposes a STOMP-over-WebSocket endpoint for real-time events. When a message is sent or a reaction is added/removed, both conversation participants receive a `MessageEvent` JSON payload.

### Connection

| Endpoint | Protocol | Auth |
|:---|:---|:---|
| `/ws` | STOMP over WebSocket (SockJS fallback) | JWT in `Authorization` STOMP header (`Bearer <token>`) |

**Connect example (STOMP):**
```
CONNECT
Authorization: Bearer <access_token>

```

**Connect example (JavaScript with SockJS + STOMP):**
```js
const socket = new SockJS('http://localhost:8080/ws');
const client = Stomp.over(socket);
client.connect({ Authorization: 'Bearer <access_token>' }, frame => {
    // connected
});
```

### Subscriptions

| Destination | Event Types | Payload Fields |
|:---|:---|:---|
| `/user/queue/messages` | `NEW_MESSAGE`, `REACTION_ADDED`, `REACTION_REMOVED` | See below |

### MessageEvent Payload

```json
{
  "type": "NEW_MESSAGE",
  "messageId": "abc123",
  "senderId": 1,
  "receiverId": 2,
  "userId": null,
  "emoji": null
}
```

| Field | Type | Description |
|:---|:---|:---|
| `type` | String | `NEW_MESSAGE` / `REACTION_ADDED` / `REACTION_REMOVED` |
| `messageId` | String | ID of the affected message |
| `senderId` | Long | *(NEW_MESSAGE only)* Sender of the message |
| `receiverId` | Long | *(NEW_MESSAGE only)* Recipient of the message |
| `userId` | Long | *(REACTION events only)* User who reacted |
| `emoji` | String | *(REACTION_ADDED only)* The emoji that was added |

Clients should fetch the full message document from Firestore upon receiving a `NEW_MESSAGE` event.

---

## 6. Firestore Composite Index

The `GET /messages?userId={id}` endpoint queries Firestore with `whereIn` on `senderId` and `receiverId` combined with `orderBy("timestamp")`. This requires a **composite index** on the `messages` collection.

### Index Definition (`firestore.indexes.json`)

```json
{
  "indexes": [
    {
      "collectionGroup": "messages",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "senderId", "order": "ASCENDING" },
        { "fieldPath": "receiverId", "order": "ASCENDING" },
        { "fieldPath": "timestamp", "order": "ASCENDING" }
      ]
    }
  ],
  "fieldOverrides": []
}
```

### How to Deploy

1. Install the Firebase CLI:
   ```bash
   npm install -g firebase-tools
   ```
2. Log in and initialize Firestore in your project:
   ```bash
   firebase login
   firebase init firestore
   ```
3. Replace the generated `firestore.indexes.json` with the definition above.
4. Deploy the indexes:
   ```bash
   firebase deploy --only firestore:indexes
   ```
5. Indexes take 1–5 minutes to build. The query will fail with a URL to the Firebase console until the index is ready — clicking that link lets you create the index manually as well.

---

## 7. Data Models

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

## 8. Error Handling

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

## 9. CORS

The server accepts requests from any origin with credentials. Allowed methods: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`.
