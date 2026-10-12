# 🎲 Ludo Game — Standalone Java Swing Desktop Application

A complete, self-contained, 100% standalone desktop implementation of the classic **Ludo** board game built with **Java Swing** and **Java 2D (AWT)**.

Featuring realistic graphics, fluid stepping movement animations, full rule validation, safe star tiles, opponent captures, surprise mystery perks, and full podium rankings.

**No browser, no web server, no database, and no external dependencies required.**

---

## ⚡ Quick Start

### Prerequisites
- **Java SE Development Kit (JDK) 11+** (JDK 17, 21, or newer recommended).
- Ensure `javac` and `java` are available in your system path:
  ```powershell
  java -version
  javac -version
  ```

### Compile & Run

Open your terminal or PowerShell in the project root directory and execute:

```powershell
# 1. Compile the single-file application
javac LudoGame.java

# 2. Launch the desktop application
java LudoGame
```

---

## 🌟 Key Features

### 1. Standalone Single-File Architecture
- Entire application logic, GUI rendering, animation loops, and rules engine reside in **`LudoGame.java`**.
- Zero external libraries, frameworks (no Spring, Maven, or Gradle), databases (no MySQL/JDBC), or web components.
- Instant, lightweight startup directly on the desktop.

### 2. Multi-Player Hotseat Modes
- Supports **2, 3, or 4 players** on a single computer.
- Custom player names and color assignments:
  - 🔴 **Red** (Bottom-Left / Start at Step 1)
  - 🟢 **Green** (Top-Right / Start at Step 14)
  - 🟡 **Yellow** (Bottom-Right / Start at Step 27)
  - 🔵 **Blue** (Top-Left / Start at Step 40)
- In 2-player matches, players sit diagonally opposite (Red vs. Yellow) per traditional competition rules.

### 3. Realistic Graphical Board & Pieces
- **15x15 Custom Board:** Handcrafted with anti-aliased vector rendering, vibrant color paths, and central triangle goals.
- **Realistic Pieces:** Automatically loads high-resolution transparent 3D piece images from the `images/` directory (`blue-piece.png`, `red-piece.png`, `green-piece.png`, `yellow-piece.png`), with an integrated procedural vector 3D pawn fallback.
- **Multi-Piece Stacking:** When multiple pieces share the same tile, they are dynamically arranged with radial offsets and proportional scaling so all pieces remain clearly visible.
- **Visual Turn Feedback:** Pulsing golden glow rings highlight only the pieces that can legally be moved.

### 4. Interactive 3D Animated Dice
- Visual 3D-styled dice with authentic pip dots (1 through 6) and rolling animations.
- Roll by clicking the **Roll Dice** button or pressing the <kbd>Spacebar</kbd>.
- Automatic detection of no-move situations with a gentle delay before passing the turn.

### 5. Full Ludo Rules Engine
- **Yard Exit:** Pieces must roll a **6** to deploy from the yard base onto the track.
- **Bonus Turns:**
  - Rolling a **6** grants an extra turn.
  - Capturing an opponent's piece grants an extra turn.
  - Bringing a piece into the center home goal grants an extra turn.
- **Captures & Yard Returns:** Landing on an opponent's piece on a non-safe cell captures it and sends it back to their yard.
- **8 Safe Star Tiles (★):** Pieces resting on star tiles (cells 1, 9, 14, 22, 27, 35, 40, and 48) are immune to capture.
- **Exact Home Goal:** Pieces cannot overshoot step 57; players need an exact roll to reach the center.

### 6. Mystery Perk Spots (🎁)
Eight special surprise gift tiles are distributed across the outer track:
- 🎁 **Extra Turn:** Immediate free bonus roll!
- 🚀 **Jetpack:** Propels the piece forward 4 bonus tiles!
- 🍌 **Banana Peel:** Causes the piece to slide backward 3 tiles!

### 7. Match Event Log & Dashboard
- **Current Turn Banner:** Displays active player name and color badge.
- **Live Status Cards:** Real-time counters showing pieces in Yard, Track, and Home Goal for each player.
- **Event Logger:** Scrollable chronological log of all dice rolls, deployments, captures, and perk activations.

### 8. Podium Rankings & Fair Completion
- In multi-player matches, the game continues until only one player remains ($n - 1$ rule).
- Final results dialog crowns:
  - 🥇 **1st Place (Winner)**
  - 🥈 **2nd Place**
  - 🥉 **3rd Place**
  - ❌ **Defeated (Loser)**
- Option to immediately restart a new match with one click.

---

## 📁 Project Structure

```text
LUDO/
├── images/
│   ├── blue-piece.png         # Transparent 3D Blue piece
│   ├── green-piece.png        # Transparent 3D Green piece
│   ├── red-piece.png          # Transparent 3D Red piece
│   └── yellow-piece.png       # Transparent 3D Yellow piece
├── LudoGame.java              # Complete standalone Java Swing application
├── README.md                  # Project documentation
└── .gitignore                 # Git ignore rules for compiled classes & IDEs
```

---

## 🎮 How to Play

1. **Launch the Game:** Run `javac LudoGame.java` and `java LudoGame`.
2. **Setup Match:** In the popup dialog, choose 2, 3, or 4 players and type your names, then click **Launch Game**.
3. **Roll Dice:** On your turn, click the **Roll Dice** button or hit the <kbd>Spacebar</kbd>.
4. **Select Piece:**
   - If you rolled a **6**, you can click any piece in your yard to deploy it, or click an active piece on the track.
   - If you have pieces on the track, click any glowing piece to advance it.
5. **Win the Game:** Navigate all 4 of your pieces around the board and into the center goal!
