# GigaChat — Backend API

GigaChat is a real-time chat application backend built with **Java 21**, **Spring Boot 4.0.6**, **PostgreSQL**, and **Google Firestore**. It provides user authentication, friend management, direct messaging with reactions, and group management — all exposed via a RESTful API with WebSocket real-time notifications.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Configuration](#configuration)
  - [Running the Server](#running-the-server)
- [API Overview](#api-overview)
  - [Authentication](#1-authentication-apiv1auth)
  - [Connections / Friends](#2-connections-friends-apiv1connections)
  - [Messaging & Chat](#3-messaging--chat-apiv1messages)
  - [WebSocket Events](#4-websocket-events)
  - [Groups](#5-groups-apiv1groups)
- [Frontend Integration Guide](#frontend-integration-guide)
  - [Authentication Flow](#authentication-flow)
  - [Real-Time Chat with WebSocket](#real-time-chat-with-websocket)
  - [Suggested Frontend Stack](#suggested-frontend-stack)
- [Features Yet To Be Implemented](#features-yet-to-be-implemented)

---

## Tech Stack

| Layer               | Technology                                                         |
| ------------------- | ------------------------------------------------------------------ |
| Language            | Java 21                                                            |
| Framework           | Spring Boot 4.0.6                                                  |
| Database (auth/connections/groups) | PostgreSQL (via Spring Data JPA / Hibernate)      |
| Chat Storage        | Google Firestore (NoSQL)                                           |
| Authentication      | JWT (access + refresh tokens) with Spring Security                 |
| Real-time           | STOMP over WebSocket with SockJS fallback                          |
| Build               | Maven                                                              |

---

## Getting Started

### Prerequisites

- **Java 21+**
- **Maven**
- **PostgreSQL** (running on `localhost:5433`, or adjust in config)
- **Google Firebase project** with Firestore enabled and a service account key

### Configuration

All settings are in `app/src/main/resources/application.properties`:

```properties
# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5433/gigachat
spring.datasource.username=your_db_user
spring.datasource.password=your_db_password
spring.jpa.hibernate.ddl-auto=update

# Firebase (place your service-account.json in src/main/resources/)
app.firebase.config-file=service-account.json

# JWT
app.jwt.secret=your-256-bit-secret-here-change-in-production
app.jwt.access-token-expiration=3600000
app.jwt.refresh-token-expiration=1209600000
```

### Running the Server

```bash
cd app
mvn spring-boot:run
```

The server starts on `http://localhost:8080` with CORS allowing all origins.

---

## API Overview

All authenticated endpoints require the header:
```
Authorization: Bearer <access_token>
```

Responses follow a standard JSON format. Paginated endpoints accept `page` (0-based, default 0) and `size` (default 20) query parameters.

---

### 1. Authentication (`/api/v1/auth`)

| Method | Endpoint              | Auth     | Description                           |
| ------ | --------------------- | -------- | ------------------------------------- |
| POST   | `/register`           | No       | Register a new user                   |
| POST   | `/login`              | No       | Login with username + password        |
| POST   | `/refresh`            | No       | Exchange a refresh token for new tokens |
| POST   | `/logout`             | Yes      | Invalidate the refresh token          |
| GET    | `/profile`            | Yes      | Get the current user's profile        |
| PUT    | `/profile`            | Yes      | Update profile (username, profileName, bio) |
| POST   | `/profile/avatar`     | Yes      | Upload avatar image (multipart, converted to PNG) |
| GET    | `/users/search`       | Yes      | Search users by username or profileName |

**Register Request:**
```json
{
  "username": "john",
  "email": "john@example.com",
  "password": "securePass123"
}
```

**Login Response:**
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "dGhpcyBpcyBh...",
  "expiresIn": 3600000,
  "user": {
    "id": "uuid",
    "username": "john",
    "email": "john@example.com",
    "profileName": null,
    "bio": null,
    "avatarUrl": null,
    "role": "USER",
    "createdAt": "2025-01-01T00:00:00"
  }
}
```

---

### 2. Connections / Friends (`/api/v1/connections`)

| Method | Endpoint                | Auth | Description                             |
| ------ | ----------------------- | ---- | --------------------------------------- |
| POST   | `/request`              | Yes  | Send a friend request                   |
| PUT    | `/accept`               | Yes  | Accept a pending friend request         |
| PUT    | `/reject`               | Yes  | Reject a pending friend request         |
| PUT    | `/block`                | Yes  | Block a user                            |
| PUT    | `/unblock`              | Yes  | Unblock a user                          |
| GET    | `/`                     | Yes  | List accepted friends (paginated)       |
| GET    | `/requests/sent`        | Yes  | List sent pending requests              |
| GET    | `/requests/received`    | Yes  | List received pending requests          |
| DELETE | `/friend/{friendId}`    | Yes  | Remove a friend                         |
| GET    | `/blocked`              | Yes  | List blocked users                      |
| GET    | `/status/{userId}`      | Yes  | Get connection status with a user       |

**Friend Request Body:**
```json
{
  "receiverId": "target-user-uuid",
  "message": "optional request message"
}
```

**Connection Status Response:**
```json
{
  "status": "FRIENDS"
}
```

Possible statuses: `NONE`, `FRIENDS`, `REQUEST_SENT`, `REQUEST_RECEIVED`, `BLOCKED_BY_ME`, `BLOCKED_BY_THEM`.

---

### 3. Messaging / Chat (`/api/v1/messages`)

| Method | Endpoint                     | Auth | Description                          |
| ------ | ---------------------------- | ---- | ------------------------------------ |
| POST   | `/`                          | Yes  | Send a message (multipart form)      |
| GET    | `/`                          | Yes  | Get conversation with a user         |
| POST   | `/{messageId}/reactions`     | Yes  | Add a reaction emoji to a message    |
| DELETE | `/{messageId}/reactions`     | Yes  | Remove your reaction from a message  |

**Send Message:** `multipart/form-data`
- `receiverId` (string) — recipient user ID
- `textContent` (string, optional) — message text
- `file` (multipart, optional) — attached image

**Get Conversation:** query params
- `userId` — the user you're chatting with
- `limit` (default 50) — max messages
- `lastMessageId` (optional) — cursor for pagination (Firestore document ID)

**Reaction Request:**
```json
{
  "emoji": "🔥"
}
```

Messages are stored in **Firestore** under the `messages` collection.

---

### 4. WebSocket Events

**Connection:**
```
WebSocket URL: ws://localhost:8080/ws
STOMP endpoint: /ws (with SockJS fallback)
```

**Authentication:** Send JWT as a STOMP header on CONNECT:
```
Authorization: Bearer <access_token>
```

**Subscribe to receive real-time events:**
```
/user/{userId}/queue/messages
```

**Publishing messages (alternative to REST):**
```
/app/chat.sendMessage
```

**MessageEvent payload** (received on the user queue):
```json
{
  "type": "NEW_MESSAGE",
  "senderId": "uuid",
  "receiverId": "uuid",
  "message": {
    "id": "firestore-doc-id",
    "senderId": "uuid",
    "receiverId": "uuid",
    "textContent": "Hello!",
    "imageUrl": null,
    "reactions": {},
    "timestamp": "2025-01-01T00:00:00Z"
  }
}
```

Event types: `NEW_MESSAGE`, `REACTION_ADDED`, `REACTION_REMOVED`.

---

### 5. Groups (`/api/v1/groups`)

| Method | Endpoint                          | Auth     | Description                       |
| ------ | --------------------------------- | -------- | --------------------------------- |
| POST   | `/create`                         | Yes      | Create a new group                |
| PUT    | `/{groupId}`                      | Admin    | Update group settings             |
| DELETE | `/{groupId}`                      | Admin    | Delete a group                    |
| GET    | `/search`                         | Yes      | Search public groups              |
| GET    | `/{groupId}`                      | Yes      | Get group details                 |
| GET    | `/list`                           | Yes      | List all public groups (paginated)|
| GET    | `/my-groups`                      | Yes      | List user's groups                |
| GET    | `/{groupId}/members`              | Yes      | List group members                |
| GET    | `/{groupId}/requests`             | Admin    | List pending join requests        |
| GET    | `/{groupId}/picture`              | Yes      | Get group picture (PNG bytes)     |
| POST   | `/{groupId}/picture`              | Admin    | Upload group picture              |
| POST   | `/{groupId}/request-join`         | Yes      | Request to join a group           |
| PUT    | `/{groupId}/approve/{userId}`     | Admin    | Approve a join request            |
| PUT    | `/{groupId}/reject/{userId}`      | Admin    | Reject a join request             |
| POST   | `/{groupId}/leave`                | Yes      | Leave a group                     |
| POST   | `/{groupId}/add/{userId}`         | Admin    | Add a member with a role          |
| PUT    | `/{groupId}/role/{userId}`        | Admin    | Change a member's role            |
| DELETE | `/{groupId}/remove/{userId}`      | Admin    | Remove a member                   |

**Create Group Request:**
```json
{
  "name": "Gamers Club",
  "description": "For gaming enthusiasts",
  "memberLimit": 50
}
```

Valid roles: `ADMIN`, `MODERATOR`, `POSTER`, `COMMENTER`, `MEMBER`, `PENDING`.

---

## Frontend Integration Guide

### Authentication Flow

1. **Register / Login** → `POST /api/v1/auth/login` → receive `accessToken` and `refreshToken`.
2. **Store tokens** — save `accessToken` in memory (or `localStorage`/`sessionStorage`) and `refreshToken` securely.
3. **Attach token** — include `Authorization: Bearer <accessToken>` in every authenticated request.
4. **Token refresh** — when the API returns `401`, call `POST /api/v1/auth/refresh` with `{ "refreshToken": "..." }` to get a new access token. The old refresh token is invalidated (rotation).
5. **Logout** — call `POST /api/v1/auth/logout` to invalidate the refresh token server-side.

### Real-Time Chat with WebSocket

**Step 1: Connect**

Use a STOMP client (e.g., `@stomp/stompjs` in JavaScript):

```js
import { Client } from '@stomp/stompjs';

const client = new Client({
  brokerURL: 'ws://localhost:8080/ws',
  connectHeaders: {
    Authorization: 'Bearer <accessToken>'
  },
  onConnect: () => {
    console.log('Connected');
  }
});

client.activate();
```

**Step 2: Subscribe to incoming messages**

```js
client.subscribe('/user/' + userId + '/queue/messages', (message) => {
  const event = JSON.parse(message.body);
  // event.type: "NEW_MESSAGE" | "REACTION_ADDED" | "REACTION_REMOVED"
  // event.message: the Message object
});
```

**Step 3: Send a message (via REST)**

Use `POST /api/v1/messages` with `multipart/form-data` containing `receiverId`, `textContent`, and optionally `file`.

The backend will store the message in Firestore and push a `MessageEvent` to the recipient via WebSocket.

### Suggested Frontend Stack

This backend is framework-agnostic. Any HTTP + WebSocket client can consume it. Recommended choices:

| Platform  | Suggested Framework / Libraries               |
| --------- | --------------------------------------------- |
| Web       | React / Next.js + `@stomp/stompjs` + `axios`  |
| Mobile    | Flutter + `stomp_dart_client` + `dio`          |
| Mobile    | Kotlin Multiplatform / Android native + OkHttp |

**Pages / Screens to build:**

| Page                    | Backend Endpoints Used                                      |
| ----------------------- | ----------------------------------------------------------- |
| Register / Login        | `POST /auth/register`, `POST /auth/login`                   |
| Profile Settings        | `GET /auth/profile`, `PUT /auth/profile`, `POST /auth/profile/avatar` |
| User Search             | `GET /auth/users/search`                                    |
| Friends List            | `GET /connections/`                                         |
| Friend Requests         | `GET /connections/requests/sent`, `GET /connections/requests/received` |
| Friend Request Actions  | `POST /connections/request`, `PUT /connections/accept\|reject` |
| Blocked Users           | `GET /connections/blocked`                                  |
| Chat (Direct Messages)  | `GET /messages/`, `POST /messages/`, WebSocket subscription |
| Chat Reactions          | `POST /messages/{id}/reactions`, `DELETE /messages/{id}/reactions` |
| Group Discovery / Search| `GET /groups/search`, `GET /groups/list`                    |
| Group Details & Members | `GET /groups/{id}`, `GET /groups/{id}/members`              |
| Group Management        | `POST /groups/create`, `PUT /groups/{id}`, `DELETE /groups/{id}` |
| Group Join Requests     | `GET /groups/{id}/requests`, `PUT /groups/{id}/approve\|reject/{userId}` |
| Member Management       | `POST /groups/{id}/add/{userId}`, `PUT /groups/{id}/role/{userId}`, `DELETE /groups/{id}/remove/{userId}` |
| My Groups               | `GET /groups/my-groups`                                     |

---

## Features Yet To Be Implemented

The following features are **not yet implemented** in this backend:

### Messaging & Chat
- **Message editing & deletion** — no endpoints exist to edit or delete sent messages
- **Read receipts / seen status** — no mechanism to track whether a message has been delivered or read
- **Typing indicators** — no WebSocket event for "user is typing..."
- **Message search** — no endpoint to search messages by text content
- **Group messaging** — messaging is currently 1-on-1 only; group chat is not implemented

### Authentication & Security
- **Email verification** — registration succeeds immediately without email confirmation
- **Password reset / forgot password** — no endpoint for resetting a forgotten password
- **Rate limiting** — no protection against brute-force login attempts or API spam
- **Externalized JWT secret** — the JWT secret is hardcoded in `application.properties` and should be moved to environment variables or a secrets manager

### Groups
- **Role-based permissions** — roles (`MODERATOR`, `POSTER`, `COMMENTER`) are defined but not enforced; only `ADMIN` checks are implemented. There is no restriction on who can post or comment in a group
- **Private / invite-only groups** — only public groups exist (anyone can find and request to join)

### Notifications
- **Push notifications** — no Firebase Cloud Messaging (FCM) or APNs integration for mobile push
- **Email notifications** — no email notifications for friend requests, group invites, etc.

### Testing
- **Tests** — the `src/test/` directory is empty; no unit, integration, or end-to-end tests exist

### Frontend
- **No frontend** — this is a pure backend. All frontend code (web, mobile, or desktop) needs to be built separately using the API and WebSocket endpoints described above

---

## Project Structure

```
D:\GigaChat\
├── app/
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/com/
│           │   ├── Application.java
│           │   ├── auth/          # Authentication, users, JWT
│           │   │   ├── config/
│           │   │   ├── controller/
│           │   │   ├── service/
│           │   │   ├── repository/
│           │   │   ├── model/
│           │   │   └── dto/
│           │   ├── connection/    # Friend system
│           │   │   ├── controller/
│           │   │   ├── service/
│           │   │   ├── repository/
│           │   │   ├── model/
│           │   │   └── dto/
│           │   ├── chat/          # Messaging + WebSocket
│           │   │   ├── controller/
│           │   │   ├── service/
│           │   │   ├── config/
│           │   │   ├── model/
│           │   │   └── dto/
│           │   └── group/         # Groups
│           │       ├── controller/
│           │       ├── service/
│           │       ├── repository/
│           │       └── model/
│           └── resources/
│               ├── application.properties
│               └── service-account.json
├── firestore.indexes.json
└── README.md
```
