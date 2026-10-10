# 🎲 Ludo Game — Full-Stack Multiplayer Board Game

[![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![JavaScript](https://img.shields.io/badge/JavaScript-ES6+-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black)](https://developer.mozilla.org/en-US/docs/Web/JavaScript)
[![HTML5](https://img.shields.io/badge/HTML5-E34F26?style=for-the-badge&logo=html5&logoColor=white)](https://developer.mozilla.org/en-US/docs/Web/HTML)
[![CSS3](https://img.shields.io/badge/CSS3-1572B6?style=for-the-badge&logo=css3&logoColor=white)](https://developer.mozilla.org/en-US/docs/Web/CSS)
[![License](https://img.shields.io/badge/License-MIT-green.svg?style=for-the-badge)](LICENSE)

A complete, responsive, full-stack implementation of the classic **Ludo** board game. Built with an interactive HTML5/CSS3/JavaScript frontend and a lightweight, high-performance **plain Java HTTP backend** backed by **MySQL / MariaDB** via JDBC, featuring **MiniMoth phone OTP authentication**, real-time move validation, match analysis, and session resumption.

**Repository:** [https://github.com/prsnxd71-cell/Ludo-Game](https://github.com/prsnxd71-cell/Ludo-Game)

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Key Features](#-key-features)
- [Technologies Used](#-technologies-used)
- [Project Structure](#-project-structure)
- [Prerequisites](#-prerequisites)
- [Database Setup](#-database-setup)
- [Configuration](#-configuration)
- [How to Run](#-how-to-run)
- [Deployment on Render (Docker)](#-deployment-on-render-docker)
- [Game Rules & Mechanics](#-game-rules--mechanics)
- [Automated Testing Suite](#-automated-testing-suite)
- [Troubleshooting & FAQ](#-troubleshooting--faq)
- [Security & Data Integrity](#-security--data-integrity)
- [Future Improvements](#-future-improvements)

---

## 🌟 Overview

This project brings the traditional Indian board game *Pachisi* / *Ludo* to life on modern web browsers with full persistence and authentication:

- **Zero Heavy Frameworks:** Built cleanly with standard Java APIs (`com.sun.net.httpserver`, JDBC) and vanilla JavaScript. No Spring Boot, Node.js, Maven, Gradle, or PHP required.
- **Full-Stack Integration:** Every turn, dice roll, token step, capture, perk effect, and match status is synchronized and validated between the browser and a MySQL database.
- **Passwordless Authentication:** Players authenticate securely with their phone number using **MiniMoth OTP** (sent over WhatsApp / SMS) before starting or resuming games.
- **Match Resumption & Disconnect Handling:** Unfinished matches can be resumed seamlessly upon reconnection. Graceful disconnect detection via `pagehide` and `navigator.sendBeacon` prevents lost games.

---

## 🚀 Key Features

### 1. Multiplayer Hotseat Gameplay
- Supports **2, 3, or 4 players** on a single device.
- Configurable **player names** and color assignments (🔴 Red, 🟢 Green, 🟡 Yellow, 🔵 Blue).
- Turn indicator displays whose turn it is, complete with matching player-themed banners.

### 2. Realistic 3D Ludo Pieces
- High-resolution, authentic 3D pawn pieces with transparent backgrounds for each player color (`blue-piece.png`, `red-piece.png`, `green-piece.png`, `yellow-piece.png`).
- Fluid CSS step-by-step movement animations and golden glow highlights for playable tokens.
- Smart stacking geometry with symmetrical offsets and scaling so multiple pieces on the same square never hide each other.
- Responsive design with automatic re-centering during window resizing.

### 3. Phone OTP Authentication (MiniMoth)
- Secure login flow requesting OTP verification via MiniMoth API.
- Rejects unauthorized moves and protects game state behind 64-character cryptographically random session tokens.
- Automatic session caching in `sessionStorage` with instant session verification on page refresh.

### 4. Realistic Dice Rolling & Controls
- Interactive 3D-styled animated dice cube showing authentic pip dots (1 through 6).
- Dual roll triggers: click the dice or press the <kbd>Spacebar</kbd>.
- Rolling a **6** grants an immediate bonus turn.

### 5. Board Movement & Safe Spots
- 52-tile shared outer track, 4 color-specific 5-tile home stretches, and final center triangles.
- **8 Safe Spots (Stars):** Cells `1`, `9`, `14`, `22`, `27`, `35`, `40`, and `48` protect pieces from being captured.
- Tokens cannot leave their home yard until the player rolls a **6**.
- Pieces require an exact roll to enter the final center triangle.

### 6. Captures & Combat
- Landing on an opponent's piece on any non-safe square **captures** it.
- Captured pieces are sent immediately back to their home yard (steps reset to 0).
- The capturing player is rewarded with an immediate **bonus turn**.

### 7. Surprise Perks (8 Mystery Gift Cells)
Evenly spaced on the board (cells `5`, `11`, `18`, `24`, `31`, `37`, `44`, `50`), landing on a Gift Spot triggers a surprise power-up:
- 🎁 **Extra Turn:** Immediate bonus roll without needing a 6 or capture.
- 🚀 **Jetpack:** Rockets the piece forward 4 additional spaces automatically.
- 🍌 **Banana Peel:** Causes the piece to slip backwards 3 spaces (safely stopping before the home yard).

### 8. Finishing & Fair Podium Rankings ($n - 1$ Rule)
- Moving a piece into the center home triangle awards a bonus roll.
- A player finishes when all 4 of their pieces reach the center.
- In 3-player and 4-player games, gameplay continues until only **one** player remains ($n - 1$ rule).
- Final winner screen showcases explicit podium rankings:
  - 🥇 **1st Place:** Winner 🏆
  - 🥈 **2nd Place:** Runner-Up
  - 🥉 **3rd Place:** 3rd Place
  - ❌ **Defeated (Loser):** Displayed with a defeated badge.

### 9. Match Analysis Dashboard
- Interactive post-game statistics modal breaking down:
  - Total moves played across the match.
  - Total captures executed and perks collected.
  - Total match duration (minutes and seconds).
  - Per-player breakdown table showing moves, captures, and perks collected per color.

### 10. Match Resumption & Disconnect Handling
- **Heartbeat:** Periodic ping (`/api/games/heartbeat`) keeps active games alive.
- **Graceful Leave:** Closing or reloading triggers `pagehide` using `fetch(..., { keepalive: true })` and `navigator.sendBeacon` to notify the backend.
- **Match Resumption:** Active unfinished games are detected upon login with a banner offering to restore the exact board state and pieces.
- **Stale Game Recovery:** On server startup, active matches inactive for $\ge 15$ minutes are safely transitioned to `abandoned`, protecting recent games from restart disruption.
- **Immutability:** Once a game reaches `finished`, its status can never be overwritten by disconnect or abandon events.

---

## 🛠 Technologies Used

| Layer | Technology | Purpose |
|---|---|---|
| **Frontend** | HTML5 | Accessible, semantic structure with board grid and overlays |
| **Styling** | CSS3 | Responsive grid/flexbox layouts, CSS keyframe animations, 3D dice styling |
| **Icons** | FontAwesome 6 | Clean icons for trophies, dice, users, settings, and perks |
| **Client Scripting** | JavaScript (ES6+) | Game state machine, token path logic, DOM rendering, async Fetch API, Beacon API |
| **Backend Server** | Java 17+ | Embedded HTTP server (`com.sun.net.httpserver.HttpServer`) |
| **Database** | MySQL / MariaDB (XAMPP) | Relational persistence for games, users, and movement logs |
| **Database Driver** | MySQL Connector/J 8.0.28 | Pure Java JDBC Type 4 driver located in `lib/` |
| **Authentication** | MiniMoth OTP API | REST API for passwordless phone OTP verification via WhatsApp / SMS |

---

## 📂 Project Structure

```text
Ludo-Game/
├── backend/
│   ├── src/
│   │   ├── Config.java              # Configuration loader for config.properties
│   │   ├── Database.java            # Reusable JDBC connection, queries & safe DDL
│   │   ├── DatabaseTest.java        # Simple JDBC connection verification script
│   │   ├── GameServer.java          # Java HTTP server, REST endpoints & static file server
│   │   ├── MiniMothClient.java      # MiniMoth REST API client (send & verify OTP)
│   │   ├── MoveValidator.java       # Server-side validation for game moves & perks
│   │   ├── SessionManager.java      # In-memory cryptographically secure session manager
│   │   ├── SimpleJson.java          # Zero-dependency, lightweight JSON parser & builder
│   │   └── BackendTest.java         # 10-point comprehensive backend verification test suite
│   ├── lib/
│   │   └── mysql-connector-java-8.0.28.jar   # Official MySQL JDBC Driver
│   ├── bin/                         # Compiled Java .class binaries (generated, gitignored)
│   ├── Dockerfile                   # Multi-stage production container for Render
│   ├── .dockerignore                # Excludes bin and local secrets from container
│   └── config.properties            # Local configuration (credentials & port, gitignored)
│
├── frontend/
│   ├── images/
│   │   ├── blue-piece.png           # Realistic 3D piece for Blue player
│   │   ├── red-piece.png            # Realistic 3D piece for Red player
│   │   ├── green-piece.png          # Realistic 3D piece for Green player
│   │   └── yellow-piece.png         # Realistic 3D piece for Yellow player
│   ├── index.html                   # Single-page game entry point & UI overlays
│   ├── style.css                    # Consolidated responsive stylesheet
│   └── game.js                      # Consolidated game engine & frontend API client
│
├── .gitignore                       # Ignores config.properties, *.class, bin/
├── .gitattributes                   # Line-ending & git configuration
└── README.md                        # Comprehensive project documentation
```

---

## ⚙️ Prerequisites

Before running the project locally, ensure you have installed:

1. **Java Development Kit (JDK 17 or higher):**
   ```bash
   java -version
   javac -version
   ```
2. **XAMPP (or standalone MySQL / MariaDB):**
   - Download and install [XAMPP](https://www.apachefriends.org/).
   - Start the **MySQL** module in the XAMPP Control Panel (default port: `3306`).
3. **Web Browser:** Any modern browser (Google Chrome, Microsoft Edge, Mozilla Firefox, Safari).

---

## 🗄 Database Setup

The backend connects to MySQL using JDBC. It automatically creates missing tables and adds required columns safely without deleting any existing data.

### Step 1: Start MySQL in XAMPP
Open the **XAMPP Control Panel** and click **Start** next to **MySQL**.

### Step 2: Create the Database
Open **phpMyAdmin** at [http://localhost/phpmyadmin](http://localhost/phpmyadmin) or run via the MySQL CLI:

```sql
CREATE DATABASE IF NOT EXISTS `ludo_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Step 3: Verified Database Schema

The Java backend (`backend/src/Database.java`) verifies and creates the following relational tables automatically upon startup:

#### 1. `games` Table
Stores overall match records, player counts, statuses, and full board state snapshots.
```sql
CREATE TABLE IF NOT EXISTS `games` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `game_name` VARCHAR(100) DEFAULT 'Ludo Match',
  `player_count` INT NOT NULL DEFAULT 2,
  `game_status` VARCHAR(20) NOT NULL DEFAULT 'active',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `last_activity` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `game_state` LONGTEXT DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### 2. `users` Table
Stores authenticated players and their associated phone numbers.
```sql
CREATE TABLE IF NOT EXISTS `users` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `phone` VARCHAR(20) NOT NULL UNIQUE,
  `identity_id` VARCHAR(100) DEFAULT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `last_login` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### 3. `movements` Table
Stores an immutable audit trail of every move, capture, base exit, and perk event.
```sql
CREATE TABLE IF NOT EXISTS `movements` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `game_id` INT NOT NULL,
  `user_id` INT DEFAULT NULL,
  `player_color` VARCHAR(10) NOT NULL,
  `piece_id` VARCHAR(20) NOT NULL,
  `from_position` INT NOT NULL,
  `to_position` INT NOT NULL,
  `dice_roll` INT NOT NULL,
  `event_type` VARCHAR(30) NOT NULL DEFAULT 'MOVE',
  `move_number` INT NOT NULL,
  `client_move_id` VARCHAR(64) NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_game_id` (`game_id`),
  INDEX `idx_user_id` (`user_id`),
  UNIQUE KEY `idx_game_client_move` (`game_id`, `client_move_id`),
  CONSTRAINT `fk_movements_game` FOREIGN KEY (`game_id`) REFERENCES `games` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

> [!NOTE]
> Historical integrity is enforced: `ON DELETE RESTRICT` prevents foreign-key cascade deletions on `movements`, ensuring game history is never accidentally lost.

---

## 🔧 Configuration

Create or update `backend/config.properties`. This file stores local database credentials, server settings, and your MiniMoth API key:

```properties
# ==========================================
# MySQL Database Configuration
# ==========================================
db.host=localhost
db.port=3306
db.name=ludo_db
db.user=root
db.password=

# ==========================================
# MiniMoth OTP API Configuration
# ==========================================
# Enter your MiniMoth API Key below (e.g. from https://minimoth.com)
minimoth.api_key=YOUR_MINIMOTH_API_KEY_HERE

# ==========================================
# HTTP Server Port
# ==========================================
server.port=8080
```

> [!IMPORTANT]
> Never commit `config.properties` to version control. The file is listed in `.gitignore` to protect your secrets and database credentials.

---

## 🏃 How to Run

### Step 1: Verify MySQL is Running
Ensure XAMPP MySQL is running on port `3306`.

### Step 2: Compile the Java Backend
You can compile either from the `backend/` folder or from the project root:

- **Option A: From `backend/` directory (Recommended):**
  ```powershell
  cd backend
  javac -cp "lib/*;src" -d bin src/*.java
  ```
  *(On Linux/macOS, use `:` instead of `;`: `javac -cp "lib/*:src" -d bin src/*.java`)*

- **Option B: From project root (`Ludo-Game/`):**
  ```powershell
  javac -cp "backend/lib/*;backend/src" -d backend/bin backend/src/*.java
  ```

### Step 3: Start the Game Server
Launch the HTTP server:

- **From `backend/` directory:**
  ```powershell
  java -cp "bin;lib/*" GameServer
  ```
  *(On Linux/macOS: `java -cp "bin:lib/*" GameServer`)*

- **Or from project root (`Ludo-Game/`):**
  ```powershell
  java -cp "backend/bin;backend/lib/*" GameServer
  ```

**Expected Console Output:**
```
[CONFIG] Successfully loaded c:\...\LUDO\backend\config.properties
[DB] Verified table 'users' exists.
[DB] Verified table 'movements' exists.
[DB] Verified column 'last_activity' exists in 'games'.
[SERVER] Recovered 0 stale active games -> marked 'abandoned'.
[SERVER] Ludo Game Server running at: http://localhost:8080
[SERVER] Open http://localhost:8080 in your browser to play.
=============================================
```

### Step 4: Play in Your Browser
Open your browser and navigate to:
```
http://localhost:8080
```

1. Enter your 10-digit mobile number and click **Send OTP via MiniMoth**.
2. Enter the OTP code received via WhatsApp / SMS and click **Verify & Login**.
3. Select the number of players (2, 3, or 4) and customize player names.
4. Click **Start Game** and enjoy playing!

---

## ☁️ Deployment on Render (Docker)

The project is pre-configured for seamless containerized deployment on [Render](https://render.com) as a **Web Service**, while fully preserving local development with XAMPP.

### 1. Architecture Overview
- **Containerization:** The Java backend, JDBC driver, and all frontend assets (HTML, CSS, JavaScript, realistic 3D piece images) are packaged inside a lightweight OpenJDK 17 Docker container (`backend/Dockerfile`).
- **Dynamic Port Binding:** Render dynamically assigns an execution port through the `PORT` environment variable. The Java server detects `PORT` automatically and binds to `0.0.0.0:$PORT` (falling back to port `8080` for local runs).
- **Dual Configuration System:** In production, Render environment variables take precedence over local files. Locally, the server seamlessly loads `config.properties`.
- **Database Connection:** Connects to any cloud-hosted MySQL database (such as [Aiven](https://aiven.io), [Railway](https://railway.app), [Clever Cloud](https://www.clever-cloud.com), etc.). Secure SSL (`useSSL=true`) is automatically enabled for all remote cloud hosts.

---

### 2. Render Web Service Settings

When creating a new **Web Service** on the Render dashboard, configure:

| Setting | Value | Notes |
|---|---|---|
| **Repository** | `https://github.com/prsnxd71-cell/Ludo-Game` | Connect your GitHub repository |
| **Branch** | `main` | Production branch |
| **Runtime** | `Docker` | Uses the Docker engine |
| **Root Directory** | *(leave blank / default)* | Project root |
| **Docker Build Context** | `backend` | Sets Docker context to the `backend/` directory |
| **Dockerfile Path** | `backend/Dockerfile` | Relative path to the Dockerfile |

---

### 3. Environment Variables on Render

In the Render dashboard under **Environment**, configure the following variables (never commit these to GitHub):

| Variable | Description | Example / Default |
|---|---|---|
| `PORT` | Web service listening port | Automatically provided by Render |
| `DB_HOST` | Hostname of your external cloud MySQL instance | `mysql-xxxx.aivencloud.com` |
| `DB_PORT` | Port of your external cloud MySQL instance | `3306` (or provider port) |
| `DB_NAME` | Database name | `ludo_db` |
| `DB_USER` | MySQL database username | `avnadmin` or `root` |
| `DB_PASSWORD` | MySQL database password | `your_cloud_db_password` |
| `DB_USE_SSL` | Enable/disable SSL for database connection | `true` (auto-defaults to `true` for remote hosts) |
| `MINIMOTH_API_KEY` | MiniMoth API key for phone OTP authentication | `your_minimoth_api_key` |

> [!TIP]
> Alternatively, if your cloud MySQL provider provides a single connection string (e.g., `mysql://user:pass@host:port/dbname`), you can supply it as `MYSQL_URL` or `DATABASE_URL`.

---

### 4. Automatic Database Provisioning in Production
Upon container startup on Render, `GameServer` will:
1. Connect securely to your cloud MySQL database using the configured credentials.
2. Automatically execute safe `CREATE TABLE IF NOT EXISTS` migrations for `games`, `users`, and `movements`.
3. Check and recover any stale active games.
4. Serve the full responsive game UI and API at your Render service URL (e.g., `https://ludo-game-xxxx.onrender.com`).

---

## 🎯 Game Rules & Mechanics

```
                       [ GREEN YARD ]
                             ▲
                             |
    [ BLUE YARD ] ──► [ CENTER HOME ] ◄── [ YELLOW YARD ]
                             |
                             ▼
                        [ RED YARD ]
```

### Path & Steps
- Each player controls **4 colored pieces**.
- The board has a 52-cell circular track.
- A piece travels a total of **57 steps**:
  - `Step 0`: In Home Yard.
  - `Step 1`: Starting tile outside home yard.
  - `Steps 2–51`: Common circular track.
  - `Steps 52–56`: Colored private Home Lane.
  - `Step 57`: Center Home Triangle (Piece is FINISHED).

### Rules Summary
1. **Starting a Piece:** You must roll a **6** to move a piece out of your yard onto the board (`Step 1`).
2. **Extra Turns:** A player gets an extra roll if:
   - They roll a **6**.
   - They **capture** an opposing piece.
   - One of their pieces reaches the **center home**.
   - They land on an **Extra Turn Surprise Perk**.
3. **Safe Spots:** Pieces on the 8 Star tiles cannot be captured by opponents.
4. **Captures:** Landing on an opponent on an unstarred cell captures them and sends their token back to their home yard (`Step 0`).
5. **Exact Roll to Finish:** A piece on step 54 needs a roll of 3 to finish. If the roll is higher than required to reach step 57, the piece cannot move.
6. **Winning Condition:** The game tracks finishers in order. When $n - 1$ players have finished all 4 pieces, the game concludes immediately with full podium awards.

---

## 🧪 Automated Testing Suite

The repository includes a comprehensive 10-point test suite in `backend/src/BackendTest.java` that validates database operations, session lifecycles, and move rules without modifying live game data.

To run the verification test suite:

- **From `backend/` directory (Recommended):**
  ```powershell
  cd backend
  java -cp "bin;lib/*" BackendTest
  ```
  *(On Linux/macOS: `java -cp "bin:lib/*" BackendTest`)*

- **Or from project root (`Ludo-Game/`):**
  ```powershell
  java -cp "backend/bin;backend/lib/*" BackendTest
  ```

### Test Coverage

| Test # | Focus | Description |
|:---:|---|---|
| **TEST 1** | Schema Initialization | Verifies safe creation of `users`, `movements`, and preservation of existing game records |
| **TEST 2** | Session Manager | Tests 64-char token creation, session lookup, and token invalidation |
| **TEST 3** | MiniMoth Validation | Verifies E.164 phone normalization (`+91...`) and rejection of invalid numbers |
| **TEST 4** | MoveValidator Core | Validates roll boundaries (1–6) and prevents pieces exiting base without a 6 |
| **TEST 5** | Perks & Captures | Validates `PERK_EXTRA_TURN`, `CAPTURE`, and `PERK_JETPACK` move integrity |
| **TEST 6** | Idempotency | Simulates duplicate network submissions using `client_move_id` to prevent double-moves |
| **TEST 7** | Game Analysis | Verifies post-match report generation including moves, captures, perks, and duration |
| **TEST 8** | Activity Tracking | Verifies `last_activity` timestamp updates via `touchGameActivity` |
| **TEST 9** | Status Immutability | Enforces that finished matches cannot be overwritten as `active` or `abandoned` |
| **TEST 10** | Disconnect & Recovery | Verifies game abandonment on leave and protects active games during stale recovery |

---

## 🔍 Troubleshooting & FAQ

### 1. `CommunicationsException: Communications link failure`
- **Cause:** MySQL server is not running or port 3306 is blocked.
- **Fix:** Open the XAMPP Control Panel and verify that **MySQL** is started with a green status indicator.

### 2. `Failed to bind to /0.0.0.0:8080` (Port in use)
- **Cause:** Another application or an existing instance of `GameServer` is already using port 8080.
- **Fix:** Stop the other process, or open `config.properties` and change `server.port=8081`, then re-run.

### 3. `MiniMoth API Key required in config.properties`
- **Cause:** `minimoth.api_key` in `config.properties` is empty.
- **Fix:** Obtain an API key from [MiniMoth](https://minimoth.com) and paste it into `config.properties`.

### 4. `ClassNotFoundException: com.mysql.cj.jdbc.Driver`
- **Cause:** MySQL Connector JAR was not included in the classpath.
- **Fix:** Ensure you include `lib/*` in your classpath argument:
  - Windows: `-cp "bin;lib/*"`
  - Linux/Mac: `-cp "bin:lib/*"`

---

## 🔒 Security & Data Integrity

- **SQL Injection Prevention:** Every database query in `Database.java` utilizes parameterized `PreparedStatement` queries. No dynamic string concatenation is used for user input.
- **Double-Move & Replay Prevention:** Each piece move generates a unique `client_move_id` (timestamp + random token). The database enforces a `UNIQUE KEY (game_id, client_move_id)` constraint, making move recording idempotent.
- **Session Tokens:** Authentication sessions are generated using `SecureRandom` producing 256-bit hexadecimal tokens with a configurable 24-hour expiration.
- **Strict Status Protection:** SQL `CASE` logic ensures that once a game reaches `finished`, subsequent leave or disconnect events cannot alter its status.
- **Secret Isolation:** API keys, database usernames, and passwords reside exclusively in `config.properties`, strictly isolated from git tracking via `.gitignore`.

---

## 🔮 Future Improvements

- [ ] **WebSockets / Server-Sent Events (SSE):** Enable real-time remote multiplayer across different devices.
- [ ] **AI Bot Opponents:** Add single-player mode with heuristic or minimax computer players.
- [ ] **Audio & Sound Effects:** Dice rattling, token hop sounds, capture sound effects, and fanfare on win.
- [ ] **User Profiles & Leaderboard:** Global win/loss statistics, trophies, and tournament leaderboards.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE) — feel free to modify and build upon it for learning and personal projects.

