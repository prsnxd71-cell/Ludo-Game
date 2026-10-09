// ==========================================
// 1. GAME DATA AND SETTINGS
// ==========================================

// The board is a 15x15 grid. We use columns (col) and rows (row).
// This is the main physical path around the board (52 steps total).
const boardPath = [
  { col: 1, row: 7 },
  { col: 2, row: 7 },
  { col: 3, row: 7 },
  { col: 4, row: 7 },
  { col: 5, row: 7 },
  { col: 6, row: 7 },
  { col: 7, row: 6 },
  { col: 7, row: 5 },
  { col: 7, row: 4 },
  { col: 7, row: 3 },
  { col: 7, row: 2 },
  { col: 7, row: 1 },
  { col: 8, row: 1 },
  { col: 9, row: 1 },
  { col: 9, row: 2 },
  { col: 9, row: 3 },
  { col: 9, row: 4 },
  { col: 9, row: 5 },
  { col: 9, row: 6 },
  { col: 10, row: 7 },
  { col: 11, row: 7 },
  { col: 12, row: 7 },
  { col: 13, row: 7 },
  { col: 14, row: 7 },
  { col: 15, row: 7 },
  { col: 15, row: 8 },
  { col: 15, row: 9 },
  { col: 14, row: 9 },
  { col: 13, row: 9 },
  { col: 12, row: 9 },
  { col: 11, row: 9 },
  { col: 10, row: 9 },
  { col: 9, row: 10 },
  { col: 9, row: 11 },
  { col: 9, row: 12 },
  { col: 9, row: 13 },
  { col: 9, row: 14 },
  { col: 9, row: 15 },
  { col: 8, row: 15 },
  { col: 7, row: 15 },
  { col: 7, row: 14 },
  { col: 7, row: 13 },
  { col: 7, row: 12 },
  { col: 7, row: 11 },
  { col: 7, row: 10 },
  { col: 6, row: 9 },
  { col: 5, row: 9 },
  { col: 4, row: 9 },
  { col: 3, row: 9 },
  { col: 2, row: 9 },
  { col: 1, row: 9 },
  { col: 1, row: 8 },
];

// Player specific settings (Starting points, home base coordinates, and home lanes)
const playerSettings = {
  red: {
    startOffset: 1,
    baseCoords: [
      { col: 2, row: 2 },
      { col: 5, row: 2 },
      { col: 2, row: 5 },
      { col: 5, row: 5 },
    ],
    homeLane: [
      { col: 2, row: 8 },
      { col: 3, row: 8 },
      { col: 4, row: 8 },
      { col: 5, row: 8 },
      { col: 6, row: 8 },
    ],
    winArea: { col: 7, row: 8 },
  },
  green: {
    startOffset: 14,
    baseCoords: [
      { col: 11, row: 2 },
      { col: 14, row: 2 },
      { col: 11, row: 5 },
      { col: 14, row: 5 },
    ],
    homeLane: [
      { col: 8, row: 2 },
      { col: 8, row: 3 },
      { col: 8, row: 4 },
      { col: 8, row: 5 },
      { col: 8, row: 6 },
    ],
    winArea: { col: 8, row: 7 },
  },
  yellow: {
    startOffset: 27,
    baseCoords: [
      { col: 11, row: 11 },
      { col: 14, row: 11 },
      { col: 11, row: 14 },
      { col: 14, row: 14 },
    ],
    homeLane: [
      { col: 14, row: 8 },
      { col: 13, row: 8 },
      { col: 12, row: 8 },
      { col: 11, row: 8 },
      { col: 10, row: 8 },
    ],
    winArea: { col: 9, row: 8 },
  },
  blue: {
    startOffset: 40,
    baseCoords: [
      { col: 2, row: 11 },
      { col: 5, row: 11 },
      { col: 2, row: 14 },
      { col: 5, row: 14 },
    ],
    homeLane: [
      { col: 8, row: 14 },
      { col: 8, row: 13 },
      { col: 8, row: 12 },
      { col: 8, row: 11 },
      { col: 8, row: 10 },
    ],
    winArea: { col: 8, row: 9 },
  },
};

// Safe cells where pieces cannot be captured
const safeSpots = [1, 9, 14, 22, 27, 35, 40, 48];

// The 8 spots on the outer track where SURPRISE PERKS are hidden (evenly distributed, non-safe)
const perkSpots = [5, 11, 18, 24, 31, 37, 44, 50];

// ==========================================
// 2. GAME STATE (Changing Variables)
// ==========================================

let activePlayers = []; // e.g. ["blue", "green"]
let currentPlayerIndex = 0; // Keeps track of whose turn it is (0, 1, 2, or 3)
let allPieces = []; // Array of all piece objects

let currentGameState = "SETUP"; // Can be: SETUP, WAITING_FOR_ROLL, WAITING_FOR_PIECE, MOVING, GAME_OVER
let currentDiceRoll = 0;
let playerGetsExtraTurn = false;

// Custom Player Names & Rankings State
let playerNames = { red: "Red", green: "Green", yellow: "Yellow", blue: "Blue" };
let finishedPlayers = []; // Colors in order of finishing all 4 pieces: ["blue", "red", ...]
let playerRanks = {}; // Map of color -> rank
let pendingPlayerCount = 2;

function getPlayerName(color) {
  if (!color) return "";
  return playerNames[color] || color.toUpperCase();
}

// Backend & Authentication State
let authToken = sessionStorage.getItem("ludo_auth_token") || null;
let currentUser = null;
let currentGameId = 1;
let clientMoveCounter = 0;
let pendingPhone = "";

// ==========================================
// 2B. AUTHENTICATION & SESSION MANAGEMENT
// ==========================================

async function checkExistingSession() {
  const token = sessionStorage.getItem("ludo_auth_token");
  if (!token) {
    showAuthScreen();
    return;
  }
  try {
    const res = await fetch("/api/auth/session", {
      headers: { "Authorization": "Bearer " + token }
    });
    const data = await res.json();
    if (data.authenticated && data.user) {
      authToken = token;
      currentUser = data.user;
      onAuthSuccess();
    } else {
      sessionStorage.removeItem("ludo_auth_token");
      authToken = null;
      showAuthScreen();
    }
  } catch (err) {
    showAuthScreen();
  }
}

async function requestOtp() {
  const phoneInput = document.getElementById("auth-phone");
  const phone = phoneInput ? phoneInput.value.trim() : "";
  const errorBox = document.getElementById("auth-error");
  const successBox = document.getElementById("auth-success");
  errorBox.style.display = "none";
  successBox.style.display = "none";

  if (!phone || phone.length !== 10 || !/^[6-9]\d{9}$/.test(phone)) {
    errorBox.innerText = "Please enter a valid 10-digit mobile number starting with 6-9.";
    errorBox.style.display = "block";
    return;
  }

  pendingPhone = "+91" + phone;
  const sendBtn = document.getElementById("send-otp-btn");
  sendBtn.disabled = true;
  sendBtn.innerText = "Sending OTP...";

  try {
    const res = await fetch("/api/auth/otp/send", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ phone: pendingPhone })
    });
    const data = await res.json();
    if (data.success) {
      document.getElementById("auth-phone-section").style.display = "none";
      document.getElementById("auth-otp-section").style.display = "block";
      document.getElementById("otp-phone-display").innerText = pendingPhone;
      successBox.innerText = "OTP sent successfully! Check WhatsApp or SMS.";
      successBox.style.display = "block";
    } else {
      let errMsg = data.error || "Failed to send OTP.";
      if (data.code === "CONFIG_REQUIRED") {
        errMsg = "MiniMoth API Key required in config.properties to send live OTPs.";
      }
      errorBox.innerText = errMsg;
      errorBox.style.display = "block";
    }
  } catch (err) {
    errorBox.innerText = "Network error connecting to backend: " + err.message;
    errorBox.style.display = "block";
  } finally {
    sendBtn.disabled = false;
    sendBtn.innerText = "Send OTP via MiniMoth";
  }
}

async function verifyOtp() {
  const codeInput = document.getElementById("auth-code");
  const code = codeInput ? codeInput.value.trim() : "";
  const errorBox = document.getElementById("auth-error");
  const successBox = document.getElementById("auth-success");
  errorBox.style.display = "none";
  successBox.style.display = "none";

  if (!code || code.length !== 6 || !/^\d{6}$/.test(code)) {
    errorBox.innerText = "Please enter the 6-digit OTP code.";
    errorBox.style.display = "block";
    return;
  }

  const verifyBtn = document.getElementById("verify-otp-btn");
  verifyBtn.disabled = true;
  verifyBtn.innerText = "Verifying...";

  try {
    const res = await fetch("/api/auth/otp/verify", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ phone: pendingPhone, code: code })
    });
    const data = await res.json();
    if (data.success && data.token) {
      authToken = data.token;
      currentUser = data.user;
      sessionStorage.setItem("ludo_auth_token", authToken);
      onAuthSuccess();
    } else {
      errorBox.innerText = data.error || "Invalid OTP code.";
      errorBox.style.display = "block";
    }
  } catch (err) {
    errorBox.innerText = "Verification failed: " + err.message;
    errorBox.style.display = "block";
  } finally {
    verifyBtn.disabled = false;
    verifyBtn.innerText = "Verify & Continue";
  }
}

function backToPhoneInput() {
  document.getElementById("auth-phone-section").style.display = "block";
  document.getElementById("auth-otp-section").style.display = "none";
  document.getElementById("auth-error").style.display = "none";
  document.getElementById("auth-success").style.display = "none";
}

function showAuthScreen() {
  const authScreen = document.getElementById("auth-screen");
  if (authScreen) authScreen.style.display = "flex";
  const setupScreen = document.getElementById("setup-screen");
  if (setupScreen) setupScreen.style.display = "none";
  const badge = document.getElementById("user-badge");
  if (badge) badge.style.display = "none";
}

function onAuthSuccess() {
  const authScreen = document.getElementById("auth-screen");
  if (authScreen) authScreen.style.display = "none";
  const setupScreen = document.getElementById("setup-screen");
  if (setupScreen) setupScreen.style.display = "flex";
  const badge = document.getElementById("user-badge");
  if (badge) badge.style.display = "flex";
  const phoneText = document.getElementById("user-phone-text");
  if (phoneText) phoneText.innerText = currentUser ? currentUser.phone : "Player";

  // Check if there is an active ongoing game that can be resumed
  checkActiveOngoingGame();
}

async function logout() {
  stopHeartbeat();
  if (currentGameId && currentGameState !== "GAME_OVER") {
    try {
      await fetch("/api/games/leave", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": "Bearer " + authToken
        },
        body: JSON.stringify({ game_id: currentGameId }),
        keepalive: true
      });
    } catch (e) {}
  }
  if (authToken) {
    try {
      await fetch("/api/auth/logout", {
        method: "POST",
        headers: { "Authorization": "Bearer " + authToken }
      });
    } catch (e) {}
  }
  authToken = null;
  currentUser = null;
  sessionStorage.removeItem("ludo_auth_token");
  location.reload();
}

// ==========================================
// 3. HELPER FUNCTIONS
// ==========================================

// Creates a delay for animations
function pause(milliseconds) {
  return new Promise((resolve) => setTimeout(resolve, milliseconds));
}

// Writes text to the small log box on the screen
function logMessage(message) {
  console.log(message);
  const logBox = document.getElementById("game-log");
  if (logBox) {
    logBox.innerHTML += `<div>> ${message}</div>`;
    logBox.scrollTop = logBox.scrollHeight;
  }
}

// Gives us the color of the current player
function getCurrentColor() {
  return activePlayers[currentPlayerIndex];
}

// ==========================================
// 4. GAME INITIALIZATION
// ==========================================

function choosePlayerCount(count) {
  pendingPlayerCount = count;
  const setupScreen = document.getElementById("setup-screen");
  if (setupScreen) setupScreen.style.display = "none";

  const setupModal = document.getElementById("player-setup-screen");
  if (setupModal) setupModal.style.display = "flex";

  const fieldsContainer = document.getElementById("player-input-fields");
  if (fieldsContainer) fieldsContainer.innerHTML = "";
  const errBox = document.getElementById("player-setup-error");
  if (errBox) errBox.style.display = "none";

  const defaultColors = ["blue", "red", "green", "yellow"];
  const colorOptions = [
    { value: "red", label: "Red" },
    { value: "green", label: "Green" },
    { value: "yellow", label: "Yellow" },
    { value: "blue", label: "Blue" }
  ];

  for (let i = 0; i < count; i++) {
    const row = document.createElement("div");
    row.className = "player-row";

    const defaultColor = defaultColors[i % defaultColors.length];
    const defaultName = `Player ${i + 1}`;

    row.innerHTML = `
      <label>Player ${i + 1}:</label>
      <input type="text" id="setup-name-${i}" placeholder="Enter name" value="${defaultName}" maxlength="20">
      <select id="setup-color-${i}">
        ${colorOptions.map(c => `<option value="${c.value}" ${c.value === defaultColor ? "selected" : ""}>${c.label}</option>`).join("")}
      </select>
    `;
    fieldsContainer.appendChild(row);
  }
}

function backToPlayerCountSelect() {
  const playerSetup = document.getElementById("player-setup-screen");
  if (playerSetup) playerSetup.style.display = "none";
  const setupScreen = document.getElementById("setup-screen");
  if (setupScreen) setupScreen.style.display = "flex";
}

function submitPlayerSetup() {
  const errBox = document.getElementById("player-setup-error");
  if (errBox) errBox.style.display = "none";

  const chosenPlayers = [];
  const chosenColors = [];

  for (let i = 0; i < pendingPlayerCount; i++) {
    const nameInput = document.getElementById(`setup-name-${i}`);
    const colorSelect = document.getElementById(`setup-color-${i}`);
    const name = nameInput ? nameInput.value.trim() : "";
    const color = colorSelect ? colorSelect.value : "";

    if (!name) {
      if (errBox) {
        errBox.innerText = `Please enter a non-empty name for Player ${i + 1}.`;
        errBox.style.display = "block";
      }
      return;
    }

    if (chosenColors.includes(color)) {
      if (errBox) {
        errBox.innerText = `Duplicate color selected! Each player must have a unique color.`;
        errBox.style.display = "block";
      }
      return;
    }

    chosenColors.push(color);
    chosenPlayers.push({ name: name, color: color });
  }

  // Update global players config
  activePlayers = chosenColors;
  playerNames = {};
  for (let p of chosenPlayers) {
    playerNames[p.color] = p.name;
  }

  const playerSetup = document.getElementById("player-setup-screen");
  if (playerSetup) playerSetup.style.display = "none";

  startGameWithPlayers(activePlayers, playerNames);
}

function startGame(numberOfPlayers) {
  choosePlayerCount(numberOfPlayers);
}

async function startGameWithPlayers(playersList, namesMap) {
  activePlayers = [...playersList];
  playerNames = { ...namesMap };
  finishedPlayers = [];
  playerRanks = {};
  currentPlayerIndex = 0;

  // 1. Configure and show side panels with custom player names
  const allColors = ["blue", "red", "green", "yellow"];
  for (let color of allColors) {
    const panel = document.getElementById(`panel-${color}`);
    if (panel) {
      if (!activePlayers.includes(color)) {
        panel.style.visibility = "hidden";
      } else {
        panel.style.visibility = "visible";
        panel.innerHTML = `<div>${color.toUpperCase()}</div><div class="player-name-label">${getPlayerName(color)}</div>`;
      }
    }
  }

  // 2. Create pieces & board
  createPieces();
  drawBoardGrid();
  drawPieces();

  // 3. Update screens
  const setupScreen = document.getElementById("setup-screen");
  if (setupScreen) setupScreen.style.display = "none";
  const gameContainer = document.getElementById("game-container");
  if (gameContainer) gameContainer.style.display = "flex";

  // 4. Start first turn
  currentGameState = "WAITING_FOR_ROLL";
  updateScreenText();
  logMessage(`Game started! ${getPlayerName(getCurrentColor())} (${getCurrentColor().toUpperCase()})'s turn.`);

  // 5. Connect to backend MySQL game record
  await initGameOnBackend(activePlayers.length);
}

async function initGameOnBackend(playerCount) {
  if (!authToken) return;
  try {
    const res = await fetch("/api/games/init", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": "Bearer " + authToken
      },
      body: JSON.stringify({
        player_count: playerCount,
        game_name: `Match: ${activePlayers.map(c => getPlayerName(c)).join(" vs ")}`,
        game_state: JSON.stringify({
          activePlayers,
          players: activePlayers.map(c => ({ color: c, name: getPlayerName(c) })),
          currentPlayerIndex: 0,
          currentGameState: "WAITING_FOR_ROLL"
        })
      })
    });
    const data = await res.json();
    if (data.success && data.game_id) {
      currentGameId = data.game_id;
      clientMoveCounter = 0;
      logMessage(`Game #${currentGameId} registered in MySQL.`);
      startHeartbeat();
    }
  } catch (err) {
    console.warn("Could not register game on backend:", err);
  }
}

async function recordAcceptedMove(piece, fromPosition, toPosition, diceRoll, eventType) {
  if (!authToken || !currentGameId) return;

  clientMoveCounter++;
  const clientMoveId = (typeof crypto !== "undefined" && crypto.randomUUID) ?
      crypto.randomUUID() :
      ("move-" + Date.now() + "-" + Math.random().toString(36).substring(2, 9));

  const payload = {
    game_id: currentGameId,
    player_color: piece.color,
    piece_id: piece.id,
    from_position: fromPosition,
    to_position: toPosition,
    dice_roll: diceRoll,
    event_type: eventType,
    move_number: clientMoveCounter,
    client_move_id: clientMoveId
  };

  try {
    const res = await fetch("/api/moves", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": "Bearer " + authToken
      },
      body: JSON.stringify(payload)
    });
    const data = await res.json();
    if (data.success) {
      console.log(`[DB] Move #${clientMoveCounter} recorded in MySQL (ID: ${data.movement_id})`);
    } else {
      console.warn("[DB] Move record warning:", data.error);
    }
  } catch (err) {
    console.warn("[DB] Network error recording move:", err);
  }

  // Update snapshot in games table
  saveGameState();
}

async function saveGameState() {
  if (!authToken || !currentGameId) return;
  const snapshot = {
    activePlayers,
    currentPlayerIndex,
    currentGameState,
    currentDiceRoll,
    playerGetsExtraTurn,
    pieces: allPieces.map(p => ({
      id: p.id,
      color: p.color,
      status: p.status,
      stepsTaken: p.stepsTaken
    }))
  };

  try {
    await fetch("/api/games/state", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": "Bearer " + authToken
      },
      body: JSON.stringify({
        game_id: currentGameId,
        game_state: JSON.stringify(snapshot),
        game_status: currentGameState === "GAME_OVER" ? "finished" : "active"
      })
    });
  } catch (e) {}
}


function createPieces() {
  allPieces = [];
  const container = document.getElementById("pieces-container");
  container.innerHTML = "";

  for (let color of activePlayers) {
    for (let i = 0; i < 4; i++) {
      // Create a simple object for each piece
      const piece = {
        id: `${color}-${i}`, // e.g., "blue-0", "blue-1"
        color: color,
        status: "HOME", // Can be: "HOME", "ACTIVE", "FINISHED"
        stepsTaken: 0, // 0 = home, 1-51 = track, 52-56 = home lane, 57 = finished
      };

      allPieces.push(piece);

      // Create the HTML circle for the piece
      const htmlElement = document.createElement("div");
      htmlElement.id = piece.id;
      htmlElement.classList.add("piece", color);

      // When the user clicks the piece on the screen, trigger onPieceClick
      htmlElement.onclick = function () {
        onPieceClick(piece.id);
      };

      container.appendChild(htmlElement);
    }
  }
}

// ==========================================
// 5. DRAWING THE BOARD (HTML/CSS visuals)
// ==========================================

function drawBoardGrid() {
  const grid = document.getElementById("grid-container");
  grid.innerHTML = "";

  // Loop through 15 rows and 15 columns
  for (let row = 1; row <= 15; row++) {
    for (let col = 1; col <= 15; col++) {
      const cell = document.createElement("div");
      cell.classList.add("grid-cell");

      // Give every cell an ID so we can easily inject Perk icons later
      cell.id = "cell-" + col + "-" + row;

      // Draw colored bases
      if (row <= 6 && col <= 6) applyBaseStyle(cell, row, col, 2, 5, "red");
      else if (row <= 6 && col >= 10)
        applyBaseStyle(cell, row, col, 11, 14, "green");
      else if (row >= 10 && col >= 10)
        applyBaseStyle(cell, row, col, 11, 14, "yellow");
      else if (row >= 10 && col <= 6)
        applyBaseStyle(cell, row, col, 2, 5, "blue");
      // Draw Center winning square
      else if (row >= 7 && row <= 9 && col >= 7 && col <= 9) {
        cell.style.background = "#333";
        if (row === 8 && col === 8) {
          cell.innerHTML =
            '<i class="fas fa-trophy" style="color:gold; font-size:2em;"></i>';
        }
      }

      // Draw Home Lanes
      else if (row === 8 && col >= 2 && col <= 6)
        cell.style.background = "red"; // Red lane
      else if (col === 8 && row >= 2 && row <= 6)
        cell.style.background = "green"; // Green lane
      else if (row === 8 && col >= 10 && col <= 14)
        cell.style.background = "yellow"; // Yellow lane
      else if (col === 8 && row >= 10 && row <= 14)
        cell.style.background = "blue"; // Blue lane
      // Draw Starting cells
      else if (row === 7 && col === 2) cell.style.background = "red";
      else if (row === 2 && col === 9) cell.style.background = "green";
      else if (row === 9 && col === 14) cell.style.background = "yellow";
      else if (row === 14 && col === 7) cell.style.background = "blue";
      // Draw Safe Stars
      else if (
        (row === 3 && col === 7) ||
        (row === 7 && col === 13) ||
        (row === 13 && col === 9) ||
        (row === 9 && col === 3)
      ) {
        cell.innerHTML =
          '<i class="fas fa-star" style="color: #aaa; font-size: 1.2em;"></i>';
      }

      grid.appendChild(cell);
    }
  }

  // Add the glowing Surprise Perk gifts to the board
  for (let index of perkSpots) {
    let coords = boardPath[index];
    let targetCell = document.getElementById(
      "cell-" + coords.col + "-" + coords.row,
    );
    if (targetCell) {
      targetCell.innerHTML = '<i class="fas fa-gift perk-icon"></i>';
      targetCell.style.background = "#f3e5f5"; // Very light purple
    }
  }
}

function applyBaseStyle(cell, row, col, minWhite, maxWhite, colorName) {
  // Color every cell in the home area
  const homeColors = {
    red: "red",
    green: "green",
    yellow: "yellow",
    blue: "blue",
  };

  cell.style.background = homeColors[colorName];

  // Keep the four piece positions visible
  const isSpot =
    (row === 2 || row === 5 || row === 11 || row === 14) &&
    (col === 2 || col === 5 || col === 11 || col === 14);

  if (isSpot) {
    cell.innerHTML = `<div class="home-spot-circle ${colorName}"></div>`;
  }
}

// ==========================================
// 6. PIECE COORDINATES (Logical to Visual)
// ==========================================

// Figuring out exactly where a piece should be placed on the grid
function getPieceCoordinates(piece) {
  if (piece.status === "HOME") {
    // Extract the number (0 to 3) from the ID (e.g. "blue-2" -> "2")
    let pieceNumber = parseInt(piece.id.split("-")[1]);
    return playerSettings[piece.color].baseCoords[pieceNumber];
  }

  if (piece.status === "FINISHED") {
    return playerSettings[piece.color].winArea;
  }

  if (piece.status === "ACTIVE") {
    if (piece.stepsTaken >= 1 && piece.stepsTaken <= 51) {
      // It is on the main track. Figure out the index out of 52 total squares.
      let start = playerSettings[piece.color].startOffset;
      let trackIndex = (start + piece.stepsTaken - 1) % 52;
      return boardPath[trackIndex];
    }

    if (piece.stepsTaken >= 52 && piece.stepsTaken <= 56) {
      // It has entered its colored home lane.
      let laneIndex = piece.stepsTaken - 52;
      return playerSettings[piece.color].homeLane[laneIndex];
    }
  }
}

// Places the HTML circles in the correct spot on the board
function drawPieces() {
  let cellsOccupied = {};

  // 1. Move each piece to its correct coordinate percentage
  for (let piece of allPieces) {
    let coords = getPieceCoordinates(piece);
    let htmlElement = document.getElementById(piece.id);

    // Convert the 1-15 coordinate to a CSS percentage for placement
    htmlElement.style.left = (coords.col - 1) * (100 / 15) + "%";
    htmlElement.style.top = (coords.row - 1) * (100 / 15) + "%";

    // Track how many pieces share this spot (to arrange them neatly later)
    if (piece.status === "ACTIVE") {
      let key = coords.col + "," + coords.row;
      if (!cellsOccupied[key]) {
        cellsOccupied[key] = [];
      }
      cellsOccupied[key].push(piece);
    } else {
      // Home and finished pieces stay centered
      htmlElement.style.transform = "translate(0px, 0px) scale(1)";
    }
  }

  // 2. If multiple pieces share one track cell, shrink and shift them slightly
  for (let key in cellsOccupied) {
    let piecesOnSpot = cellsOccupied[key];

    if (piecesOnSpot.length > 1) {
      // Arrange stacked pieces so they don't completely hide each other
      const visualOffsets = [
        { x: 0, y: 0 },
        { x: 6, y: 6 },
        { x: -6, y: -6 },
        { x: 6, y: -6 },
        { x: -6, y: 6 },
      ];

      for (let i = 0; i < piecesOnSpot.length; i++) {
        let piece = piecesOnSpot[i];
        let htmlElement = document.getElementById(piece.id);
        let offset = visualOffsets[i % 5];
        htmlElement.style.transform = `translate(${offset.x}px, ${offset.y}px) scale(0.8)`;
      }
    } else {
      // Just one piece here, make sure it's centered and normal size
      let piece = piecesOnSpot[0];
      let htmlElement = document.getElementById(piece.id);
      htmlElement.style.transform = "translate(0px, 0px) scale(1)";
    }
  }
}

// ==========================================
// 7. TURN & DICE LOGIC
// ==========================================

function updateScreenText() {
  // Remove the glowing effect from everyone
  let panels = document.querySelectorAll(".player-panel");
  for (let panel of panels) {
    panel.classList.remove("active");
  }

  // Add the glow to the current player
  let currentColor = getCurrentColor();
  const currentPanel = document.getElementById(`panel-${currentColor}`);
  if (currentPanel) currentPanel.classList.add("active");

  // Update the text with player name and color
  let indicator = document.getElementById("turn-indicator");
  if (indicator) {
    indicator.innerText = `Current Turn: ${getPlayerName(currentColor)} (${currentColor.toUpperCase()})`;
  }

  // Disable or enable the roll button
  let rollBtn = document.getElementById("roll-btn");
  if (rollBtn) {
    if (currentGameState === "WAITING_FOR_ROLL") {
      rollBtn.disabled = false;
    } else {
      rollBtn.disabled = true;
    }
  }
}

function rollDice() {
  // Only roll if it's the correct time
  if (currentGameState !== "WAITING_FOR_ROLL") return;

  currentGameState = "ROLLING";
  document.getElementById("roll-btn").disabled = true;

  const diceHtml = document.getElementById("dice-value");
  diceHtml.classList.add("rolling");

  // Play a rolling animation by changing faces quickly
  let animationTicks = 0;
  const diceFaces = ["⚀", "⚁", "⚂", "⚃", "⚄", "⚅"];

  let rollInterval = setInterval(function () {
    let randomFace = Math.floor(Math.random() * 6);
    diceHtml.innerText = diceFaces[randomFace];
    animationTicks++;

    // Stop animation after 15 quick changes
    if (animationTicks >= 15) {
      clearInterval(rollInterval);
      diceHtml.classList.remove("rolling");

      // Set the final actual number
      currentDiceRoll = Math.floor(Math.random() * 6) + 1;
      diceHtml.innerText = diceFaces[currentDiceRoll - 1];
      logMessage(
        `${getCurrentColor().toUpperCase()} rolled ${currentDiceRoll}`,
      );

      handleDiceResult();
    }
  }, 40);
}

function getClickablePieces() {
  let clickableList = [];

  for (let piece of allPieces) {
    // 1. Only pieces of the current player can move
    if (piece.color !== getCurrentColor()) continue;

    // 2. Finished pieces can never move again
    if (piece.status === "FINISHED") continue;

    // 3. Pieces inside the HOME base need a 6 to come out
    if (piece.status === "HOME") {
      if (currentDiceRoll === 6) {
        clickableList.push(piece);
      }
      continue;
    }

    // 4. ACTIVE pieces must not overshoot the final square
    if (piece.status === "ACTIVE") {
      if (piece.stepsTaken + currentDiceRoll <= 57) {
        clickableList.push(piece);
      }
    }
  }

  return clickableList;
}

function handleDiceResult() {
  let validPieces = getClickablePieces();

  if (validPieces.length === 0) {
    logMessage(`No valid moves for ${getCurrentColor()}`);
    setTimeout(endTurn, 1000); // Wait 1 second so the player can see what happened
  } else {
    currentGameState = "WAITING_FOR_PIECE";
    // Make valid pieces glow so the user knows what to click
    for (let piece of validPieces) {
      document.getElementById(piece.id).classList.add("highlight");
    }
  }
}

// ==========================================
// 8. MOVEMENT LOGIC
// ==========================================

async function onPieceClick(clickedPieceId) {
  if (currentGameState !== "WAITING_FOR_PIECE") return;

  // Find the piece object that matches the ID
  let selectedPiece = null;
  for (let piece of allPieces) {
    if (piece.id === clickedPieceId) {
      selectedPiece = piece;
    }
  }

  // Ensure the piece clicked is actually allowed to move
  let validPieces = getClickablePieces();
  let isAllowed = false;
  for (let piece of validPieces) {
    if (piece.id === selectedPiece.id) {
      isAllowed = true;
    }
  }

  if (!isAllowed) return; // Ignore click if invalid

  // Remove the visual highlights from all pieces
  for (let piece of allPieces) {
    document.getElementById(piece.id).classList.remove("highlight");
  }

  await movePiece(selectedPiece);
}

async function movePiece(piece) {
  currentGameState = "MOVING";
  let fromPos = piece.stepsTaken;
  let eventType = (piece.status === "HOME") ? "MOVE_OUT" : "MOVE";

  if (piece.status === "HOME") {
    // Bring piece out of the base and onto step 1
    piece.status = "ACTIVE";
    piece.stepsTaken = 1;
    drawPieces();
    await pause(200);
  } else {
    // Move the piece exactly one step at a time, for 'currentDiceRoll' times
    for (let i = 0; i < currentDiceRoll; i++) {
      piece.stepsTaken++;

      if (piece.stepsTaken === 57) {
        piece.status = "FINISHED";
      }

      drawPieces();
      await pause(200); // Wait between steps so the player sees the animation
    }
  }

  let toPos = piece.stepsTaken;

  // Record move in MySQL backend
  await recordAcceptedMove(piece, fromPos, toPos, currentDiceRoll, eventType);

  checkRulesAfterMovement(piece);
}

// ==========================================
// 9. RULES (Capture, Winning, & PERKS)
// ==========================================

function getTrackIndex(piece) {
  // Helps us check if a piece is on the main-board square
  if (piece.status === "ACTIVE" && piece.stepsTaken <= 51) {
    let start = playerSettings[piece.color].startOffset;
    return (start + piece.stepsTaken - 1) % 52;
  }
  return -1; // Not on the main track
}

function checkRulesAfterMovement(movedPiece) {
  // RULE A: Piece finished
  if (movedPiece.status === "FINISHED") {
    let finishedCount = 0;
    for (let p of allPieces) {
      if (p.color === movedPiece.color && p.status === "FINISHED") {
        finishedCount++;
      }
    }

    if (finishedCount === 4) {
      if (!finishedPlayers.includes(movedPiece.color)) {
        finishedPlayers.push(movedPiece.color);
      }
      logMessage(`🎉 ${getPlayerName(movedPiece.color)} (${movedPiece.color.toUpperCase()}) has finished all 4 pieces! (Rank #${finishedPlayers.length})`);

      // n - 1 rule: Game ends immediately when n - 1 players finish all 4 pieces
      const threshold = Math.max(1, activePlayers.length - 1);
      if (finishedPlayers.length >= threshold) {
        endGameWithRankings();
        return;
      } else {
        // Player is finished, cannot take another turn; pass to next active player
        playerGetsExtraTurn = false;
        endTurn();
        return;
      }
    } else {
      logMessage(`🎯 ${getPlayerName(movedPiece.color)} moved a piece home! (${finishedCount}/4) Bonus turn.`);
      playerGetsExtraTurn = true;
      endTurn();
      return;
    }
  }

  let capturedSomeone = false;
  let finalTrackIndex = getTrackIndex(movedPiece);

  // RULE B: Capture logic
  if (finalTrackIndex !== -1) {
    let isSafeSpot = false;
    for (let spot of safeSpots) {
      if (spot === finalTrackIndex) isSafeSpot = true;
    }

    // If they did not land on a star cell...
    if (!isSafeSpot) {
      for (let otherPiece of allPieces) {
        // Check if there is an opponent on the exact same square
        if (
          otherPiece.color !== movedPiece.color &&
          otherPiece.status === "ACTIVE"
        ) {
          if (getTrackIndex(otherPiece) === finalTrackIndex) {
            // Capture! Send them back home.
            let capturedOldPos = otherPiece.stepsTaken;
            otherPiece.status = "HOME";
            otherPiece.stepsTaken = 0;
            capturedSomeone = true;
            playerGetsExtraTurn = true; // Bonus turn for capturing
            logMessage(
              `⚔️ ${getPlayerName(movedPiece.color)} (${movedPiece.color.toUpperCase()}) CAPTURED ${getPlayerName(otherPiece.color)} (${otherPiece.color.toUpperCase()})!`,
            );
            // Record capture event in MySQL backend
            recordAcceptedMove(otherPiece, capturedOldPos, 0, 0, "CAPTURE");
          }
        }
      }

      if (capturedSomeone) {
        drawPieces();
      }
    }
  }

  // RULE C: Roll 6 gives a bonus turn
  if (currentDiceRoll === 6 && !capturedSomeone) {
    logMessage(`Rolled 6! Extra turn for ${getPlayerName(getCurrentColor())}`);
    playerGetsExtraTurn = true;
  }

  // RULE D: The new Surprise Perk logic!
  if (finalTrackIndex !== -1 && perkSpots.includes(finalTrackIndex)) {
    triggerSurprisePerk(movedPiece);
    return; // The triggerSurprisePerk function will handle ending the turn for us
  }

  endTurn();
}

async function triggerSurprisePerk(piece) {
  logMessage(`🎁 ${piece.color.toUpperCase()} landed on a Surprise Perk!`);

  // 1. Pick a random perk effect
  const allPerks = ["EXTRA_TURN", "JETPACK", "BANANA_PEEL"];
  const chosenPerk = allPerks[Math.floor(Math.random() * allPerks.length)];

  // 2. Prepare the beautiful pop-up box
  const alertBox = document.getElementById("perk-alert");
  alertBox.style.display = "block";

  // Wait a tiny fraction of a second before showing to let CSS animations kick in
  setTimeout(() => {
    alertBox.classList.add("show");
  }, 20);

  if (chosenPerk === "EXTRA_TURN") {
    alertBox.innerHTML = "🎁 Surprise!<br>You get an extra roll!";
    playerGetsExtraTurn = true;

    // Record surprise perk extra turn in MySQL
    await recordAcceptedMove(piece, piece.stepsTaken, piece.stepsTaken, 0, "PERK_EXTRA_TURN");

    await pause(2000); // Wait for player to read
    hidePerkAlert(alertBox);

    endTurn();
  } else if (chosenPerk === "JETPACK") {
    alertBox.innerHTML = "🚀 Jetpack!<br>Fly forward 4 spaces!";

    await pause(2000);
    hidePerkAlert(alertBox);

    // Move piece forward automatically
    let fromJetpack = piece.stepsTaken;
    for (let i = 0; i < 4; i++) {
      piece.stepsTaken++;
      if (piece.stepsTaken === 57) {
        piece.status = "FINISHED";
        break;
      }
      drawPieces();
      await pause(200);
    }
    let toJetpack = piece.stepsTaken;
    await recordAcceptedMove(piece, fromJetpack, toJetpack, 0, "PERK_JETPACK");

    // Re-check the rules for the square they landed on after the Jetpack
    checkRulesAfterMovement(piece);
  } else if (chosenPerk === "BANANA_PEEL") {
    alertBox.innerHTML = "🍌 Banana Peel!<br>Slip backward 3 spaces!";

    await pause(2000);
    hidePerkAlert(alertBox);

    // Move piece backward
    let fromBanana = piece.stepsTaken;
    for (let i = 0; i < 3; i++) {
      // Stop them from slipping backwards back into their HOME base
      if (piece.stepsTaken > 1) {
        piece.stepsTaken--;
      }
      drawPieces();
      await pause(200);
    }
    let toBanana = piece.stepsTaken;
    await recordAcceptedMove(piece, fromBanana, toBanana, 0, "PERK_BANANA");

    // Re-check the rules for the square they landed on after sliding
    checkRulesAfterMovement(piece);
  }
}

async function hidePerkAlert(alertBox) {
  alertBox.classList.remove("show");
  await pause(300); // Wait for the fade-out CSS animation
  alertBox.style.display = "none";
}

function endGameWithRankings() {
  currentGameState = "GAME_OVER";

  // Identify loser: player in activePlayers who has not finished
  const loserColor = activePlayers.find(c => !finishedPlayers.includes(c)) || null;

  const rankings = [];
  finishedPlayers.forEach((color, idx) => {
    rankings.push({
      rank: idx + 1,
      color: color,
      name: getPlayerName(color),
      role: idx === 0 ? "Winner" : (idx === 1 ? "Runner Up" : "3rd Place")
    });
  });

  const loserObj = loserColor ? {
    color: loserColor,
    name: getPlayerName(loserColor),
    role: "Loser"
  } : null;

  // Render rankings cards on winner screen
  const rankingsContainer = document.getElementById("rankings-container");
  if (rankingsContainer) {
    let html = "";
    rankings.forEach(r => {
      const badgeText = r.rank === 1 ? "1st" : (r.rank === 2 ? "2nd" : "3rd");
      const statusClass = r.rank === 1 ? "status-winner" : "status-runner";
      const statusText = r.rank === 1 ? "Winner 🏆" : (r.rank === 2 ? "Runner Up 🥈" : "3rd Place 🥉");
      html += `
        <div class="rank-card rank-${r.rank}">
          <div class="rank-badge">${badgeText}</div>
          <div class="rank-player-info">
            <span class="color-pill ${r.color}"></span>
            <span>${r.name} (${r.color.toUpperCase()})</span>
          </div>
          <div class="rank-status ${statusClass}">${statusText}</div>
        </div>
      `;
    });

    if (loserObj) {
      html += `
        <div class="rank-card loser">
          <div class="rank-badge">❌</div>
          <div class="rank-player-info">
            <span class="color-pill ${loserObj.color}"></span>
            <span>${loserObj.name} (${loserObj.color.toUpperCase()})</span>
          </div>
          <div class="rank-status status-loser">Defeated (Loser)</div>
        </div>
      `;
    }
    rankingsContainer.innerHTML = html;
  }

  // Update announcement title
  const winnerText = document.getElementById("winner-text");
  if (winnerText && finishedPlayers.length > 0) {
    const winnerColor = finishedPlayers[0];
    winnerText.innerText = `🏆 ${getPlayerName(winnerColor)} (${winnerColor.toUpperCase()}) WINS!`;
  }

  const winnerScreen = document.getElementById("winner-screen");
  if (winnerScreen) {
    winnerScreen.style.display = "flex";
  }

  logMessage(`🏆 GAME OVER! 1st Place: ${finishedPlayers.length > 0 ? getPlayerName(finishedPlayers[0]) : "N/A"}`);
  if (loserObj) {
    logMessage(`Defeated / Loser: ${loserObj.name} (${loserObj.color.toUpperCase()})`);
  }

  // Stop heartbeat
  stopHeartbeat();

  // Save final state and rankings to MySQL
  saveFinalGameResult(rankings, loserObj);
}

async function saveFinalGameResult(rankings, loserObj) {
  if (!authToken || !currentGameId) return;

  const snapshot = {
    activePlayers,
    players: activePlayers.map(c => ({ color: c, name: getPlayerName(c) })),
    rankings: rankings,
    loser: loserObj,
    finishedPlayers: finishedPlayers,
    totalMoves: clientMoveCounter
  };

  try {
    const res = await fetch("/api/games/state", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": "Bearer " + authToken
      },
      body: JSON.stringify({
        game_id: currentGameId,
        game_state: JSON.stringify(snapshot),
        game_status: "finished"
      })
    });
    const data = await res.json();
    if (data.success) {
      logMessage(`Final rankings saved to MySQL database for Game #${currentGameId}.`);
    }
  } catch (err) {
    console.warn("Could not save final game state to MySQL:", err);
  }
}

function checkWinCondition() {
  let finishedCount = 0;
  for (let piece of allPieces) {
    if (piece.color === getCurrentColor() && piece.status === "FINISHED") {
      finishedCount++;
    }
  }

  if (finishedCount === 4) {
    if (!finishedPlayers.includes(getCurrentColor())) {
      finishedPlayers.push(getCurrentColor());
    }
    const threshold = Math.max(1, activePlayers.length - 1);
    if (finishedPlayers.length >= threshold) {
      endGameWithRankings();
      return;
    }
  }
  endTurn();
}

function endTurn() {
  if (currentGameState === "GAME_OVER") return;

  if (playerGetsExtraTurn) {
    playerGetsExtraTurn = false;
    if (finishedPlayers.includes(getCurrentColor())) {
      advanceToNextPlayer();
    }
  } else {
    advanceToNextPlayer();
  }

  currentDiceRoll = 0;
  currentGameState = "WAITING_FOR_ROLL";
  updateScreenText();
}

function advanceToNextPlayer() {
  const threshold = Math.max(1, activePlayers.length - 1);
  if (finishedPlayers.length >= threshold) {
    return;
  }
  let loopCount = 0;
  do {
    currentPlayerIndex++;
    if (currentPlayerIndex >= activePlayers.length) {
      currentPlayerIndex = 0;
    }
    loopCount++;
  } while (finishedPlayers.includes(getCurrentColor()) && loopCount <= activePlayers.length);

  logMessage(`Turn ends. Next: ${getPlayerName(getCurrentColor())} (${getCurrentColor().toUpperCase()})`);
}

// ==========================================
// POST-GAME ANALYSIS MODAL
// ==========================================

async function openGameAnalysis() {
  const modal = document.getElementById("analysis-modal");
  const content = document.getElementById("analysis-content");
  if (!modal || !content) return;

  modal.style.display = "flex";
  content.innerHTML = `<div style="text-align: center; padding: 30px;"><i class="fas fa-spinner fa-spin" style="font-size: 2em; color: #1e88e5;"></i><p style="margin-top: 10px;">Loading game analysis from MySQL...</p></div>`;

  if (!authToken) {
    content.innerHTML = `<div style="color: #d32f2f; padding: 20px;">Authentication token missing. Please log in first.</div>`;
    return;
  }

  try {
    const res = await fetch(`/api/games/analysis?game_id=${currentGameId}`, {
      headers: {
        "Authorization": "Bearer " + authToken
      }
    });

    if (!res.ok) {
      const errData = await res.json().catch(() => ({}));
      content.innerHTML = `<div style="color: #d32f2f; padding: 20px;">Failed to load analysis: ${errData.error || res.statusText}</div>`;
      return;
    }

    const data = await res.json();
    renderGameAnalysis(data);
  } catch (err) {
    content.innerHTML = `<div style="color: #d32f2f; padding: 20px;">Network error loading analysis: ${err.message}</div>`;
  }
}

function closeGameAnalysis() {
  const modal = document.getElementById("analysis-modal");
  if (modal) modal.style.display = "none";
}

function renderGameAnalysis(data) {
  const content = document.getElementById("analysis-content");
  if (!content) return;

  const durationMin = Math.floor((data.duration_seconds || 0) / 60);
  const durationSec = (data.duration_seconds || 0) % 60;
  const durationText = durationMin > 0 ? `${durationMin}m ${durationSec}s` : `${durationSec}s`;

  let html = `
    <div class="analysis-stats-grid">
      <div class="stat-card">
        <div class="val">${data.total_moves || 0}</div>
        <div class="lbl">Total Moves</div>
      </div>
      <div class="stat-card">
        <div class="val">${data.captures_count || 0}</div>
        <div class="lbl">Captures</div>
      </div>
      <div class="stat-card">
        <div class="val">${data.perks_count || 0}</div>
        <div class="lbl">Perks Triggered</div>
      </div>
      <div class="stat-card">
        <div class="val">${durationText}</div>
        <div class="lbl">Duration</div>
      </div>
    </div>
  `;

  // Players & Per-Player Breakdown Table
  html += `
    <div class="analysis-section-title"><i class="fas fa-users"></i> Player Performance</div>
    <table class="analysis-table">
      <thead>
        <tr>
          <th>Player</th>
          <th>Color</th>
          <th>Moves Recorded</th>
          <th>Captures Made</th>
          <th>Perks Collected</th>
        </tr>
      </thead>
      <tbody>
  `;

  const movesPerPlayer = data.moves_per_player || {};
  const capturesPerPlayer = data.captures_per_player || {};
  const perksPerPlayer = data.perks_per_player || {};

  activePlayers.forEach(c => {
    const name = getPlayerName(c);
    const moves = movesPerPlayer[c] || 0;
    const captures = capturesPerPlayer[c] || 0;
    const perks = perksPerPlayer[c] || 0;

    html += `
      <tr>
        <td><strong>${name}</strong></td>
        <td><span class="color-pill ${c}"></span> ${c.toUpperCase()}</td>
        <td>${moves}</td>
        <td>${captures}</td>
        <td>${perks}</td>
      </tr>
    `;
  });
  html += `</tbody></table>`;

  // Surprise Perks Breakdown
  if (data.perk_breakdown && Object.keys(data.perk_breakdown).length > 0) {
    html += `
      <div class="analysis-section-title"><i class="fas fa-gift"></i> Surprise Perks Triggered</div>
      <table class="analysis-table">
        <thead>
          <tr>
            <th>Perk Type</th>
            <th>Times Triggered</th>
          </tr>
        </thead>
        <tbody>
    `;
    for (let perkKey in data.perk_breakdown) {
      let label = perkKey;
      if (perkKey === "PERK_JETPACK") label = "🚀 Jetpack (+4 steps)";
      else if (perkKey === "PERK_BANANA") label = "🍌 Banana Peel (-3 steps)";
      else if (perkKey === "PERK_EXTRA_TURN") label = "🎲 Extra Turn";
      html += `
        <tr>
          <td>${label}</td>
          <td><strong>${data.perk_breakdown[perkKey]}</strong></td>
        </tr>
      `;
    }
    html += `</tbody></table>`;
  }

  // Recent Movements Log
  const movements = data.movements || [];
  if (movements.length > 0) {
    html += `
      <div class="analysis-section-title"><i class="fas fa-history"></i> Movement History Audit (${movements.length} records in MySQL)</div>
      <div style="max-height: 250px; overflow-y: auto; border: 1px solid #e0e0e0; border-radius: 6px;">
        <table class="analysis-table" style="margin-top: 0;">
          <thead>
            <tr>
              <th>#</th>
              <th>Player</th>
              <th>Piece</th>
              <th>From &rarr; To</th>
              <th>Roll</th>
              <th>Event</th>
              <th>Timestamp</th>
            </tr>
          </thead>
          <tbody>
    `;
    movements.forEach(m => {
      const color = m.player_color || "";
      const pName = getPlayerName(color);
      html += `
        <tr>
          <td>${m.move_number}</td>
          <td><span class="color-pill ${color}"></span> ${pName}</td>
          <td><code>${m.piece_id}</code></td>
          <td>${m.from_position} &rarr; ${m.to_position}</td>
          <td>${m.dice_roll}</td>
          <td><span style="font-weight: 600; font-size: 0.9em;">${m.event_type}</span></td>
          <td style="color: #666; font-size: 0.85em;">${m.created_at || ""}</td>
        </tr>
      `;
    });
    html += `</tbody></table></div>`;
  }

  content.innerHTML = html;
}

// ==========================================
// 10. KEYBOARD SUPPORT
// ==========================================

window.addEventListener("keydown", function (event) {
  // Check if the pressed key is Spacebar
  if (event.code === "Space" || event.key === " ") {
    // Only trigger if it is currently time to roll the dice
    if (currentGameState === "WAITING_FOR_ROLL") {
      event.preventDefault(); // Prevents the webpage from scrolling down
      rollDice();
    }
  }
});

// Automatically check session on page load
window.addEventListener("DOMContentLoaded", function () {
  checkExistingSession();
});

// ==========================================
// 11. LIFECYCLE, LEAVE & RECOVERY MANAGEMENT
// ==========================================

let heartbeatInterval = null;
let ongoingActiveGame = null;

function startHeartbeat() {
  stopHeartbeat();
  heartbeatInterval = setInterval(sendHeartbeat, 30000);
}

function stopHeartbeat() {
  if (heartbeatInterval) {
    clearInterval(heartbeatInterval);
    heartbeatInterval = null;
  }
}

async function sendHeartbeat() {
  if (!authToken || !currentGameId || currentGameState === "GAME_OVER") {
    return;
  }
  try {
    await fetch("/api/games/heartbeat", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": "Bearer " + authToken
      },
      body: JSON.stringify({ game_id: currentGameId })
    });
  } catch (e) {}
}

// Browser Close / Disconnect Handler
window.addEventListener("pagehide", handleBrowserLeave);
window.addEventListener("beforeunload", handleBrowserLeave);

function handleBrowserLeave(event) {
  // Only trigger if an actual game is ongoing and not finished
  if (!currentGameId || !authToken || currentGameState === "GAME_OVER") {
    return;
  }

  const payload = JSON.stringify({ game_id: currentGameId });

  // 1. fetch with keepalive: true (supports custom auth headers and JSON)
  let sent = false;
  if (typeof fetch === "function") {
    try {
      fetch("/api/games/leave", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": "Bearer " + authToken
        },
        body: payload,
        keepalive: true
      });
      sent = true;
    } catch (e) {}
  }

  // 2. navigator.sendBeacon fallback
  if (!sent && navigator.sendBeacon) {
    try {
      const url = "/api/games/leave?token=" + encodeURIComponent(authToken);
      const blob = new Blob([payload], { type: "application/json" });
      navigator.sendBeacon(url, blob);
    } catch (e) {}
  }
}

// Manual Leave Game
async function confirmLeaveGame() {
  if (currentGameState === "GAME_OVER") {
    location.reload();
    return;
  }
  if (!confirm("Are you sure you want to leave this game? The match will be abandoned in MySQL.")) {
    return;
  }
  await leaveCurrentGame();
}

async function leaveCurrentGame() {
  stopHeartbeat();
  if (currentGameId && authToken) {
    try {
      await fetch("/api/games/leave", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": "Bearer " + authToken
        },
        body: JSON.stringify({ game_id: currentGameId })
      });
      logMessage(`Game #${currentGameId} was abandoned.`);
    } catch (e) {
      console.warn("Could not notify leave:", e);
    }
  }
  currentGameId = null;
  currentGameState = "GAME_OVER";
  location.reload();
}

// Check ongoing active game on startup/login
async function checkActiveOngoingGame() {
  if (!authToken) return;
  try {
    const res = await fetch("/api/games/active", {
      headers: { "Authorization": "Bearer " + authToken }
    });
    const data = await res.json();
    if (data.has_active_game && data.game) {
      ongoingActiveGame = data.game;
      const banner = document.getElementById("resume-game-banner");
      const text = document.getElementById("resume-banner-text");
      if (banner && text) {
        text.innerText = `Ongoing match found: Game #${data.game.id} (${data.game.game_name || "Ludo Match"}).`;
        banner.style.display = "block";
      }
    } else {
      const banner = document.getElementById("resume-game-banner");
      if (banner) banner.style.display = "none";
    }
  } catch (e) {
    console.warn("Could not check active game:", e);
  }
}

async function discardAndStartNewGame() {
  const banner = document.getElementById("resume-game-banner");
  if (banner) banner.style.display = "none";

  if (ongoingActiveGame && ongoingActiveGame.id) {
    try {
      await fetch("/api/games/leave", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": "Bearer " + authToken
        },
        body: JSON.stringify({ game_id: ongoingActiveGame.id })
      });
      console.log(`[DB] Discarded prior game #${ongoingActiveGame.id} as abandoned.`);
    } catch (e) {}
    ongoingActiveGame = null;
  }
}

function resumeActiveGame() {
  if (!ongoingActiveGame || !ongoingActiveGame.game_state) return;
  try {
    const state = typeof ongoingActiveGame.game_state === "string" ?
        JSON.parse(ongoingActiveGame.game_state) : ongoingActiveGame.game_state;

    currentGameId = ongoingActiveGame.id;
    activePlayers = state.activePlayers || ["blue", "red"];
    if (state.players) {
      playerNames = {};
      state.players.forEach(p => { playerNames[p.color] = p.name; });
    }
    currentPlayerIndex = state.currentPlayerIndex || 0;
    currentGameState = state.currentGameState || "WAITING_FOR_ROLL";
    currentDiceRoll = state.currentDiceRoll || 0;
    playerGetsExtraTurn = state.playerGetsExtraTurn || false;
    clientMoveCounter = state.totalMoves || 0;
    finishedPlayers = state.finishedPlayers || [];

    // Hide setup screens, show board
    const setupScreen = document.getElementById("setup-screen");
    if (setupScreen) setupScreen.style.display = "none";
    const gameContainer = document.getElementById("game-container");
    if (gameContainer) gameContainer.style.display = "flex";

    // Setup side panels
    const allColors = ["blue", "red", "green", "yellow"];
    for (let color of allColors) {
      const panel = document.getElementById(`panel-${color}`);
      if (panel) {
        if (!activePlayers.includes(color)) {
          panel.style.visibility = "hidden";
        } else {
          panel.style.visibility = "visible";
          panel.innerHTML = `<div>${color.toUpperCase()}</div><div class="player-name-label">${getPlayerName(color)}</div>`;
        }
      }
    }

    // Recreate pieces and restore their positions
    createPieces();
    if (state.pieces && Array.isArray(state.pieces)) {
      state.pieces.forEach(savedP => {
        const liveP = allPieces.find(p => p.id === savedP.id);
        if (liveP) {
          liveP.status = savedP.status;
          liveP.stepsTaken = savedP.stepsTaken;
        }
      });
    }

    drawBoardGrid();
    drawPieces();
    updateScreenText();

    startHeartbeat();
    logMessage(`Resumed Game #${currentGameId} from saved MySQL state.`);

  } catch (err) {
    console.error("Error resuming game:", err);
    alert("Could not restore saved game. Starting fresh match.");
    discardAndStartNewGame();
  }
}


