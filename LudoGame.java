import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.Timer;

public class LudoGame extends JFrame {

    public enum PlayerColor {
        RED("Red", new Color(229, 57, 53), new Color(255, 205, 210), new Color(183, 28, 28), 0),
        GREEN("Green", new Color(67, 160, 71), new Color(200, 230, 201), new Color(46, 125, 50), 13),
        YELLOW("Yellow", new Color(251, 192, 45), new Color(255, 249, 196), new Color(245, 127, 23), 26),
        BLUE("Blue", new Color(30, 136, 229), new Color(187, 222, 251), new Color(21, 101, 192), 39);

        public final String displayName;
        public final Color primary;
        public final Color light;
        public final Color dark;
        public final int startOffset;

        PlayerColor(String displayName, Color primary, Color light, Color dark, int startOffset) {
            this.displayName = displayName;
            this.primary = primary;
            this.light = light;
            this.dark = dark;
            this.startOffset = startOffset;
        }
    }

    public enum PieceStatus {
        HOME,
        ACTIVE,
        FINISHED
    }

    public enum GameState {
        WAITING_FOR_ROLL,
        ROLLING,
        WAITING_FOR_PIECE,
        ANIMATING_MOVE,
        GAME_OVER
    }

    public enum PerkType {
        EXTRA_TURN("Extra Roll", "Surprise! Extra turn awarded!", Color.MAGENTA),
        JETPACK("Jetpack", "Jetpack! Flew forward 4 spaces!", new Color(0, 150, 136)),
        BANANA_PEEL("Banana Peel", "Banana Peel! Slipped backward 3 spaces!", new Color(255, 112, 67));

        public final String title;
        public final String description;
        public final Color color;

        PerkType(String title, String description, Color color) {
            this.title = title;
            this.description = description;
            this.color = color;
        }
    }

    public static class BoardCoord {
        public final int col;
        public final int row;

        public BoardCoord(int col, int row) {
            this.col = col;
            this.row = row;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BoardCoord)) return false;
            BoardCoord that = (BoardCoord) o;
            return col == that.col && row == that.row;
        }

        @Override
        public int hashCode() {
            return Objects.hash(col, row);
        }
    }

    public static class Piece {
        public final PlayerColor color;
        public final int pieceIndex;
        public PieceStatus status = PieceStatus.HOME;
        public int stepsTaken = 0;

        public Piece(PlayerColor color, int pieceIndex) {
            this.color = color;
            this.pieceIndex = pieceIndex;
        }

        public String getId() {
            return color.name().toLowerCase() + "-" + pieceIndex;
        }

        public boolean canMove(int diceRoll) {
            if (status == PieceStatus.FINISHED) return false;
            if (status == PieceStatus.HOME) {
                return diceRoll == 6;
            }
            if (status == PieceStatus.ACTIVE) {
                return stepsTaken + diceRoll <= 57;
            }
            return false;
        }

        public void resetToHome() {
            this.status = PieceStatus.HOME;
            this.stepsTaken = 0;
        }
    }

    public static class Player {
        public final PlayerColor color;
        public String name;
        public final Piece[] pieces = new Piece[4];
        public int finishRank = 0;

        public Player(PlayerColor color, String name) {
            this.color = color;
            this.name = name;
            for (int i = 0; i < 4; i++) {
                pieces[i] = new Piece(color, i);
            }
        }

        public boolean hasFinished() {
            for (Piece p : pieces) {
                if (p.status != PieceStatus.FINISHED) return false;
            }
            return true;
        }

        public int getFinishedCount() {
            int count = 0;
            for (Piece p : pieces) if (p.status == PieceStatus.FINISHED) count++;
            return count;
        }

        public int getHomeCount() {
            int count = 0;
            for (Piece p : pieces) if (p.status == PieceStatus.HOME) count++;
            return count;
        }

        public int getActiveCount() {
            int count = 0;
            for (Piece p : pieces) if (p.status == PieceStatus.ACTIVE) count++;
            return count;
        }
    }

    private static final BoardCoord[] BOARD_PATH = new BoardCoord[]{
            new BoardCoord(7, 14), new BoardCoord(7, 13), new BoardCoord(7, 12), new BoardCoord(7, 11), new BoardCoord(7, 10),
            new BoardCoord(6, 9),  new BoardCoord(5, 9),  new BoardCoord(4, 9),  new BoardCoord(3, 9),  new BoardCoord(2, 9),  new BoardCoord(1, 9),
            new BoardCoord(1, 8),
            new BoardCoord(1, 7),  new BoardCoord(2, 7),  new BoardCoord(3, 7),  new BoardCoord(4, 7),  new BoardCoord(5, 7),  new BoardCoord(6, 7),
            new BoardCoord(7, 6),  new BoardCoord(7, 5),  new BoardCoord(7, 4),  new BoardCoord(7, 3),  new BoardCoord(7, 2),  new BoardCoord(7, 1),
            new BoardCoord(8, 1),
            new BoardCoord(9, 1),  new BoardCoord(9, 2),  new BoardCoord(9, 3),  new BoardCoord(9, 4),  new BoardCoord(9, 5),  new BoardCoord(9, 6),
            new BoardCoord(10, 7), new BoardCoord(11, 7), new BoardCoord(12, 7), new BoardCoord(13, 7), new BoardCoord(14, 7), new BoardCoord(15, 7),
            new BoardCoord(15, 8),
            new BoardCoord(15, 9), new BoardCoord(14, 9), new BoardCoord(13, 9), new BoardCoord(12, 9), new BoardCoord(11, 9), new BoardCoord(10, 9),
            new BoardCoord(9, 10), new BoardCoord(9, 11), new BoardCoord(9, 12), new BoardCoord(9, 13), new BoardCoord(9, 14), new BoardCoord(9, 15),
            new BoardCoord(8, 15),
            new BoardCoord(7, 15)
    };

    private static final Set<Integer> SAFE_SPOTS = new HashSet<>(Arrays.asList(0, 8, 13, 21, 26, 34, 39, 47));

    private static final Set<Integer> PERK_SPOTS = new HashSet<>(Arrays.asList(4, 10, 17, 23, 30, 36, 43, 49));

    private static final Map<PlayerColor, BoardCoord[]> HOME_LANES = new EnumMap<>(PlayerColor.class);
    private static final Map<PlayerColor, BoardCoord> WIN_AREAS = new EnumMap<>(PlayerColor.class);
    private static final Map<PlayerColor, BoardCoord[]> BASE_COORDS = new EnumMap<>(PlayerColor.class);

    static {
        HOME_LANES.put(PlayerColor.RED, new BoardCoord[]{
                new BoardCoord(8, 14), new BoardCoord(8, 13), new BoardCoord(8, 12), new BoardCoord(8, 11), new BoardCoord(8, 10)
        });
        HOME_LANES.put(PlayerColor.GREEN, new BoardCoord[]{
                new BoardCoord(2, 8), new BoardCoord(3, 8), new BoardCoord(4, 8), new BoardCoord(5, 8), new BoardCoord(6, 8)
        });
        HOME_LANES.put(PlayerColor.YELLOW, new BoardCoord[]{
                new BoardCoord(8, 2), new BoardCoord(8, 3), new BoardCoord(8, 4), new BoardCoord(8, 5), new BoardCoord(8, 6)
        });
        HOME_LANES.put(PlayerColor.BLUE, new BoardCoord[]{
                new BoardCoord(14, 8), new BoardCoord(13, 8), new BoardCoord(12, 8), new BoardCoord(11, 8), new BoardCoord(10, 8)
        });

        WIN_AREAS.put(PlayerColor.RED, new BoardCoord(8, 9));
        WIN_AREAS.put(PlayerColor.GREEN, new BoardCoord(7, 8));
        WIN_AREAS.put(PlayerColor.YELLOW, new BoardCoord(8, 7));
        WIN_AREAS.put(PlayerColor.BLUE, new BoardCoord(9, 8));

        BASE_COORDS.put(PlayerColor.GREEN, new BoardCoord[]{
                new BoardCoord(2, 2), new BoardCoord(5, 2), new BoardCoord(2, 5), new BoardCoord(5, 5)
        });
        BASE_COORDS.put(PlayerColor.YELLOW, new BoardCoord[]{
                new BoardCoord(11, 2), new BoardCoord(14, 2), new BoardCoord(11, 5), new BoardCoord(14, 5)
        });
        BASE_COORDS.put(PlayerColor.RED, new BoardCoord[]{
                new BoardCoord(2, 11), new BoardCoord(5, 11), new BoardCoord(2, 14), new BoardCoord(5, 14)
        });
        BASE_COORDS.put(PlayerColor.BLUE, new BoardCoord[]{
                new BoardCoord(11, 11), new BoardCoord(14, 11), new BoardCoord(11, 14), new BoardCoord(14, 14)
        });
        loadPieceImages();
    }

    private static final Map<PlayerColor, Image> PIECE_IMAGES = new EnumMap<>(PlayerColor.class);

    private static void loadPieceImages() {
        Map<PlayerColor, String> filenames = new HashMap<>();
        filenames.put(PlayerColor.RED, "red-piece.png");
        filenames.put(PlayerColor.GREEN, "green-piece.png");
        filenames.put(PlayerColor.YELLOW, "yellow-piece.png");
        filenames.put(PlayerColor.BLUE, "blue-piece.png");

        for (Map.Entry<PlayerColor, String> entry : filenames.entrySet()) {
            try {
                File f = new File("images", entry.getValue());
                if (!f.exists()) f = new File(entry.getValue());
                if (f.exists()) {
                    BufferedImage img = ImageIO.read(f);
                    if (img != null) {
                        PIECE_IMAGES.put(entry.getKey(), img);
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    private final List<Player> activePlayers = new ArrayList<>();
    private int currentPlayerIndex = 0;
    private int currentDiceRoll = 0;
    private boolean playerGetsExtraTurn = false;
    private GameState gameState = GameState.WAITING_FOR_ROLL;
    private final List<Player> finishedRanking = new ArrayList<>();
    private final Random random = new Random();

    private int diceAnimationTicks = 0;
    private Timer diceTimer;
    private Timer pieceMoveTimer;
    private Timer highlightPulseTimer;
    private float pulseAlpha = 0.5f;
    private boolean pulseIncreasing = true;

    private BoardPanel boardPanel;
    private JLabel turnBannerLabel;
    private JPanel turnColorBadge;
    private DiceComponent diceComponent;
    private JButton rollDiceButton;

    public LudoGame() {
        super("Ludo Game - Standalone Desktop Edition");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(920, 680));
        setPreferredSize(new Dimension(1000, 720));

        initTimers();
        buildUI();
        startNewGame(4, null);
        pack();
        setLocationRelativeTo(null);

        SwingUtilities.invokeLater(this::showSetupDialog);
    }

    private void initTimers() {
        highlightPulseTimer = new Timer(50, e -> {
            if (pulseIncreasing) {
                pulseAlpha += 0.05f;
                if (pulseAlpha >= 0.95f) {
                    pulseAlpha = 0.95f;
                    pulseIncreasing = false;
                }
            } else {
                pulseAlpha -= 0.05f;
                if (pulseAlpha <= 0.35f) {
                    pulseAlpha = 0.35f;
                    pulseIncreasing = true;
                }
            }
            if (boardPanel != null) boardPanel.repaint();
        });
        highlightPulseTimer.start();
    }

    private void buildUI() {
        JPanel rootPanel = new JPanel(new BorderLayout(15, 15));
        rootPanel.setBackground(new Color(245, 245, 250));
        rootPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        boardPanel = new BoardPanel();
        boardPanel.setPreferredSize(new Dimension(650, 650));
        rootPanel.add(boardPanel, BorderLayout.CENTER);

        JPanel sidePanel = buildSidePanel();
        sidePanel.setPreferredSize(new Dimension(280, 650));
        rootPanel.add(sidePanel, BorderLayout.EAST);

        setContentPane(rootPanel);

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_SPACE) {
                if (gameState == GameState.WAITING_FOR_ROLL && rollDiceButton.isEnabled()) {
                    triggerRollDice();
                    return true;
                }
            }
            return false;
        });
    }

    private JPanel buildSidePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(245, 245, 250));
        panel.setPreferredSize(new Dimension(280, 650));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setMaximumSize(new Dimension(260, 420));
        content.setPreferredSize(new Dimension(260, 360));

        JPanel turnCard = new JPanel(new BorderLayout(12, 0));
        turnCard.setBackground(Color.WHITE);
        turnCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(220, 220, 230), 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));
        turnCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));
        turnCard.setPreferredSize(new Dimension(260, 65));

        turnColorBadge = new JPanel();
        turnColorBadge.setPreferredSize(new Dimension(22, 22));
        turnColorBadge.setBackground(Color.RED);
        turnColorBadge.setBorder(new LineBorder(new Color(0, 0, 0, 40), 1, true));

        turnBannerLabel = new JLabel("Current Turn: Red");
        turnBannerLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        turnBannerLabel.setForeground(new Color(40, 40, 50));

        JPanel badgeWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 2));
        badgeWrapper.setOpaque(false);
        badgeWrapper.add(turnColorBadge);

        turnCard.add(badgeWrapper, BorderLayout.WEST);
        turnCard.add(turnBannerLabel, BorderLayout.CENTER);
        content.add(turnCard);
        content.add(Box.createVerticalStrut(14));

        JPanel diceCard = new JPanel();
        diceCard.setLayout(new BoxLayout(diceCard, BoxLayout.Y_AXIS));
        diceCard.setBackground(Color.WHITE);
        diceCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(220, 220, 230), 1, true),
                new EmptyBorder(22, 16, 22, 16)
        ));
        diceCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 195));
        diceCard.setPreferredSize(new Dimension(260, 195));

        diceComponent = new DiceComponent();
        diceComponent.setAlignmentX(Component.CENTER_ALIGNMENT);
        diceCard.add(diceComponent);
        diceCard.add(Box.createVerticalStrut(16));

        rollDiceButton = new JButton("Roll Dice (Space)");
        rollDiceButton.setFont(new Font("Segoe UI", Font.BOLD, 15));
        rollDiceButton.setBackground(new Color(33, 150, 243));
        rollDiceButton.setForeground(Color.WHITE);
        rollDiceButton.setFocusPainted(false);
        rollDiceButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        rollDiceButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        rollDiceButton.setMaximumSize(new Dimension(210, 42));
        rollDiceButton.setPreferredSize(new Dimension(210, 42));
        rollDiceButton.addActionListener(e -> triggerRollDice());
        diceCard.add(rollDiceButton);

        content.add(diceCard);
        content.add(Box.createVerticalStrut(14));

        JPanel actionCard = new JPanel(new GridLayout(1, 2, 10, 0));
        actionCard.setOpaque(false);
        actionCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        actionCard.setPreferredSize(new Dimension(260, 40));

        JButton newGameBtn = new JButton("New Match");
        newGameBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        newGameBtn.setFocusPainted(false);
        newGameBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        newGameBtn.addActionListener(e -> showSetupDialog());

        JButton rulesBtn = new JButton("Game Rules");
        rulesBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        rulesBtn.setFocusPainted(false);
        rulesBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        rulesBtn.addActionListener(e -> showRulesDialog());

        actionCard.add(newGameBtn);
        actionCard.add(rulesBtn);
        content.add(actionCard);

        panel.add(content);
        return panel;
    }

    private void showSetupDialog() {
        JDialog dialog = new JDialog(this, "Ludo Match Setup", true);
        dialog.setLayout(new BorderLayout(15, 15));
        dialog.setSize(440, 360);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(20, 25, 20, 25));
        content.setBackground(Color.WHITE);

        JLabel title = new JLabel("Start a New Ludo Match");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(title);
        content.add(Box.createVerticalStrut(15));

        JLabel countLabel = new JLabel("Select Number of Players:");
        countLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        countLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(countLabel);
        content.add(Box.createVerticalStrut(8));

        JPanel countPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        countPanel.setBackground(Color.WHITE);
        JRadioButton r2 = new JRadioButton("2 Players");
        JRadioButton r3 = new JRadioButton("3 Players");
        JRadioButton r4 = new JRadioButton("4 Players", true);
        ButtonGroup group = new ButtonGroup();
        group.add(r2); group.add(r3); group.add(r4);
        countPanel.add(r2); countPanel.add(r3); countPanel.add(r4);
        content.add(countPanel);
        content.add(Box.createVerticalStrut(15));

        JTextField[] nameFields = new JTextField[4];
        String[] defaultNames = {"Player 1", "Player 2", "Player 3", "Player 4"};
        PlayerColor[] colors = {PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE};

        JPanel namesGrid = new JPanel(new GridLayout(4, 2, 8, 8));
        namesGrid.setBackground(Color.WHITE);

        for (int i = 0; i < 4; i++) {
            JLabel lbl = new JLabel(colors[i].displayName + " Name:");
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            nameFields[i] = new JTextField(defaultNames[i]);
            namesGrid.add(lbl);
            namesGrid.add(nameFields[i]);
        }
        content.add(namesGrid);
        content.add(Box.createVerticalStrut(15));

        ActionListener countListener = e -> {
            int count = r2.isSelected() ? 2 : (r3.isSelected() ? 3 : 4);
            nameFields[0].setEnabled(true);
            nameFields[1].setEnabled(count >= 3);
            nameFields[2].setEnabled(count == 2 || count >= 3);
            nameFields[3].setEnabled(count >= 4);
        };
        r2.addActionListener(countListener);
        r3.addActionListener(countListener);
        r4.addActionListener(countListener);

        JButton startBtn = new JButton("Launch Game");
        startBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        startBtn.setBackground(new Color(76, 175, 80));
        startBtn.setForeground(Color.WHITE);
        startBtn.setFocusPainted(false);
        startBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        startBtn.setMaximumSize(new Dimension(160, 38));
        startBtn.addActionListener(e -> {
            int count = r2.isSelected() ? 2 : (r3.isSelected() ? 3 : 4);
            startNewGame(count, nameFields);
            dialog.dispose();
        });
        content.add(startBtn);

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private void startNewGame(int playerCount, JTextField[] nameFields) {
        activePlayers.clear();
        finishedRanking.clear();
        currentPlayerIndex = 0;
        currentDiceRoll = 0;
        playerGetsExtraTurn = false;
        gameState = GameState.WAITING_FOR_ROLL;

        if (playerCount == 2) {
            String nameRed = (nameFields != null && nameFields[0] != null && !nameFields[0].getText().trim().isEmpty())
                    ? nameFields[0].getText().trim() : "Player 1";
            String nameYellow = (nameFields != null && nameFields[2] != null && !nameFields[2].getText().trim().isEmpty())
                    ? nameFields[2].getText().trim() : "Player 2";
            activePlayers.add(new Player(PlayerColor.RED, nameRed));
            activePlayers.add(new Player(PlayerColor.YELLOW, nameYellow));
        } else if (playerCount == 3) {
            String n1 = (nameFields != null && nameFields[0] != null && !nameFields[0].getText().trim().isEmpty())
                    ? nameFields[0].getText().trim() : "Player 1";
            String n2 = (nameFields != null && nameFields[1] != null && !nameFields[1].getText().trim().isEmpty())
                    ? nameFields[1].getText().trim() : "Player 2";
            String n3 = (nameFields != null && nameFields[2] != null && !nameFields[2].getText().trim().isEmpty())
                    ? nameFields[2].getText().trim() : "Player 3";
            activePlayers.add(new Player(PlayerColor.RED, n1));
            activePlayers.add(new Player(PlayerColor.GREEN, n2));
            activePlayers.add(new Player(PlayerColor.YELLOW, n3));
        } else {
            for (int i = 0; i < 4; i++) {
                String n = (nameFields != null && nameFields[i] != null && !nameFields[i].getText().trim().isEmpty())
                        ? nameFields[i].getText().trim() : ("Player " + (i + 1));
                activePlayers.add(new Player(PlayerColor.values()[i], n));
            }
        }

        updateTurnDisplay();
        getContentPane().revalidate();
        getContentPane().repaint();
        boardPanel.repaint();
    }

    private void showRulesDialog() {
        String rulesHtml = "<html><body style='width: 360px; font-family: Segoe UI, sans-serif; padding: 5px;'>"
                + "<h2 style='color: #1e88e5; margin-bottom: 5px;'>Ludo Board Game Rules</h2>"
                + "<ul>"
                + "<li><b>Yard Exit:</b> You must roll a <b>6</b> to move a piece out of your yard onto step 1.</li>"
                + "<li><b>Bonus Rolls:</b> Rolling a <b>6</b>, capturing an opponent piece, or reaching the home goal gives you an extra roll!</li>"
                + "<li><b>Exact Goal:</b> Pieces cannot overshoot step 57. You need the exact number to finish.</li>"
                + "<li><b>Safe Spots:</b> Pieces on the 8 Star tiles are safe and cannot be captured.</li>"
                + "<li><b>Captures:</b> Landing on an opponent piece on a normal square sends it back to their yard base!</li>"
                + "<li><b>Mystery Perks:</b> Landing on a gift tile gives a surprise perk:"
                + "  <ul>"
                + "    <li><b>Extra Roll:</b> Roll the dice again!</li>"
                + "    <li><b>Jetpack:</b> Flies forward 4 spaces!</li>"
                + "    <li><b>Banana Peel:</b> Slips backward 3 spaces!</li>"
                + "  </ul>"
                + "</li>"
                + "<li><b>Winning:</b> The game ends when all but 1 player have finished all 4 pieces (Podium rankings).</li>"
                + "</ul>"
                + "<p><i>Tip: You can press the <b>Spacebar</b> to roll the dice quickly!</i></p>"
                + "</body></html>";

        JOptionPane.showMessageDialog(this, rulesHtml, "Rules & Instructions", JOptionPane.INFORMATION_MESSAGE);
    }

    private void triggerRollDice() {
        if (gameState != GameState.WAITING_FOR_ROLL || activePlayers.isEmpty()) return;

        gameState = GameState.ROLLING;
        rollDiceButton.setEnabled(false);
        diceAnimationTicks = 0;

        diceTimer = new Timer(35, e -> {
            diceAnimationTicks++;
            diceComponent.setRollingFace(random.nextInt(6) + 1);

            if (diceAnimationTicks >= 12) {
                diceTimer.stop();
                currentDiceRoll = random.nextInt(6) + 1;
                diceComponent.setFinalValue(currentDiceRoll);
                onDiceRollCompleted();
            }
        });
        diceTimer.start();
    }

    private void onDiceRollCompleted() {
        Player current = getCurrentPlayer();
        if (current == null) return;

        List<Piece> movablePieces = getMovablePieces(current, currentDiceRoll);

        if (movablePieces.isEmpty()) {
            Timer delayTimer = new Timer(1000, e -> endTurn());
            delayTimer.setRepeats(false);
            delayTimer.start();
        } else {
            gameState = GameState.WAITING_FOR_PIECE;
            turnBannerLabel.setText(current.name + ": Select a Piece");
            boardPanel.repaint();
        }
    }

    private List<Piece> getMovablePieces(Player player, int roll) {
        List<Piece> list = new ArrayList<>();
        for (Piece p : player.pieces) {
            if (p.canMove(roll)) {
                list.add(p);
            }
        }
        return list;
    }

    private void executePieceSelection(Piece piece) {
        if (gameState != GameState.WAITING_FOR_PIECE) return;
        Player current = getCurrentPlayer();
        if (current == null) return;
        if (piece.color != current.color) return;
        if (!piece.canMove(currentDiceRoll)) return;

        gameState = GameState.ANIMATING_MOVE;
        boardPanel.repaint();

        if (piece.status == PieceStatus.HOME) {
            piece.status = PieceStatus.ACTIVE;
            piece.stepsTaken = 1;
            boardPanel.repaint();
            afterMovementReached(piece);
        } else {
            int targetSteps = piece.stepsTaken + currentDiceRoll;
            pieceMoveTimer = new Timer(110, null);
            pieceMoveTimer.addActionListener(e -> {
                piece.stepsTaken++;
                if (piece.stepsTaken == 57) {
                    piece.status = PieceStatus.FINISHED;
                }
                boardPanel.repaint();

                if (piece.stepsTaken >= targetSteps || piece.status == PieceStatus.FINISHED) {
                    pieceMoveTimer.stop();
                    afterMovementReached(piece);
                }
            });
            pieceMoveTimer.start();
        }
    }

    private void afterMovementReached(Piece piece) {
        Player current = getCurrentPlayer();
        if (current == null) return;

        if (piece.status == PieceStatus.FINISHED) {
            playerGetsExtraTurn = true;

            if (current.hasFinished()) {
                current.finishRank = finishedRanking.size() + 1;
                finishedRanking.add(current);

                int threshold = Math.max(1, activePlayers.size() - 1);
                if (finishedRanking.size() >= threshold) {
                    onMatchCompleted();
                    return;
                }
            }
        }

        boolean captured = checkOpponentCapture(piece);
        if (captured) {
            playerGetsExtraTurn = true;
        }

        if (currentDiceRoll == 6 && !captured) {
            playerGetsExtraTurn = true;
        }

        if (piece.status == PieceStatus.ACTIVE) {
            int trackIndex = getTrackIndex(piece);
            if (trackIndex != -1 && PERK_SPOTS.contains(trackIndex)) {
                triggerSurprisePerk(piece);
                return;
            }
        }

        endTurn();
    }

    private boolean checkOpponentCapture(Piece movedPiece) {
        if (movedPiece.status != PieceStatus.ACTIVE) return false;
        int trackIndex = getTrackIndex(movedPiece);
        if (trackIndex == -1 || SAFE_SPOTS.contains(trackIndex)) return false;

        boolean didCapture = false;
        for (Player other : activePlayers) {
            if (other.color == movedPiece.color) continue;
            for (Piece otherPiece : other.pieces) {
                if (otherPiece.status == PieceStatus.ACTIVE && getTrackIndex(otherPiece) == trackIndex) {
                    otherPiece.resetToHome();
                    didCapture = true;
                }
            }
        }
        return didCapture;
    }

    private void triggerSurprisePerk(Piece piece) {
        PerkType[] allPerks = PerkType.values();
        PerkType chosen = allPerks[random.nextInt(allPerks.length)];

        showPerkNotification(chosen, () -> {
            switch (chosen) {
                case EXTRA_TURN:
                    playerGetsExtraTurn = true;
                    endTurn();
                    break;
                case JETPACK:
                    int targetSteps = Math.min(57, piece.stepsTaken + 4);
                    pieceMoveTimer = new Timer(90, null);
                    pieceMoveTimer.addActionListener(e -> {
                        piece.stepsTaken++;
                        if (piece.stepsTaken >= 57) {
                            piece.status = PieceStatus.FINISHED;
                        }
                        boardPanel.repaint();
                        if (piece.stepsTaken >= targetSteps || piece.status == PieceStatus.FINISHED) {
                            pieceMoveTimer.stop();
                            Player current = getCurrentPlayer();
                            if (piece.status == PieceStatus.FINISHED) {
                                playerGetsExtraTurn = true;
                                if (current != null && current.hasFinished()) {
                                    current.finishRank = finishedRanking.size() + 1;
                                    finishedRanking.add(current);
                                    int threshold = Math.max(1, activePlayers.size() - 1);
                                    if (finishedRanking.size() >= threshold) {
                                        onMatchCompleted();
                                        return;
                                    }
                                }
                            }
                            boolean captured = checkOpponentCapture(piece);
                            if (captured) {
                                playerGetsExtraTurn = true;
                            }
                            endTurn();
                        }
                    });
                    pieceMoveTimer.start();
                    break;
                case BANANA_PEEL:
                    int backwardTarget = Math.max(1, piece.stepsTaken - 3);
                    pieceMoveTimer = new Timer(90, null);
                    pieceMoveTimer.addActionListener(e -> {
                        piece.stepsTaken--;
                        boardPanel.repaint();
                        if (piece.stepsTaken <= backwardTarget) {
                            pieceMoveTimer.stop();
                            boolean captured = checkOpponentCapture(piece);
                            if (captured) {
                                playerGetsExtraTurn = true;
                            }
                            endTurn();
                        }
                    });
                    pieceMoveTimer.start();
                    break;
            }
        });
    }

    private void showPerkNotification(PerkType perk, Runnable onComplete) {
        JDialog toast = new JDialog(this, false);
        toast.setUndecorated(true);
        toast.setLayout(new BorderLayout());

        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBackground(perk.color);
        p.setBorder(new EmptyBorder(14, 24, 14, 24));

        JLabel title = new JLabel(perk.title, JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);

        JLabel desc = new JLabel(perk.description, JLabel.CENTER);
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        desc.setForeground(Color.WHITE);

        p.add(title, BorderLayout.NORTH);
        p.add(desc, BorderLayout.CENTER);
        toast.setContentPane(p);
        toast.pack();
        toast.setLocationRelativeTo(this);
        toast.setVisible(true);

        Timer t = new Timer(1600, e -> {
            toast.dispose();
            onComplete.run();
        });
        t.setRepeats(false);
        t.start();
    }

    private void endTurn() {
        if (gameState == GameState.GAME_OVER || activePlayers.isEmpty()) return;

        if (playerGetsExtraTurn) {
            playerGetsExtraTurn = false;
            Player current = getCurrentPlayer();
            if (current != null && current.hasFinished()) {
                advanceToNextPlayer();
            }
        } else {
            advanceToNextPlayer();
        }

        currentDiceRoll = 0;
        gameState = GameState.WAITING_FOR_ROLL;
        rollDiceButton.setEnabled(true);
        updateTurnDisplay();
        boardPanel.repaint();
    }

    private void advanceToNextPlayer() {
        if (activePlayers.isEmpty()) return;
        int loopGuard = 0;
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % activePlayers.size();
            loopGuard++;
        } while (getCurrentPlayer() != null && getCurrentPlayer().hasFinished() && loopGuard <= activePlayers.size());
    }

    private void onMatchCompleted() {
        gameState = GameState.GAME_OVER;
        rollDiceButton.setEnabled(false);
        boardPanel.repaint();

        Player loser = null;
        for (Player p : activePlayers) {
            if (!finishedRanking.contains(p)) {
                loser = p;
                break;
            }
        }

        StringBuilder msg = new StringBuilder();
        msg.append("<html><div style='text-align: center; font-family: Segoe UI, sans-serif;'>")
                .append("<h1 style='color: #2e7d32; margin-bottom: 5px;'>MATCH COMPLETED!</h1>")
                .append("<table align='center' cellpadding='6' style='font-size: 14px;'>");

        for (int i = 0; i < finishedRanking.size(); i++) {
            Player p = finishedRanking.get(i);
            String medal = (i == 0) ? "1st Place (Winner)" : (i == 1 ? "2nd Place (Runner-Up)" : "3rd Place");
            msg.append("<tr><td><b>").append(medal).append(":</b></td><td>").append(p.name)
                    .append(" (").append(p.color.displayName).append(")</td></tr>");
        }

        if (loser != null) {
            msg.append("<tr style='color: #c62828;'><td><b>Defeated:</b></td><td>").append(loser.name)
                    .append(" (").append(loser.color.displayName).append(")</td></tr>");
        }

        msg.append("</table><br><b>Would you like to play another match?</b></div></html>");

        int option = JOptionPane.showConfirmDialog(this, msg.toString(), "Match Results & Rankings",
                JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option == JOptionPane.YES_OPTION) {
            showSetupDialog();
        }
    }

    private Player getCurrentPlayer() {
        if (activePlayers.isEmpty()) return null;
        if (currentPlayerIndex < 0 || currentPlayerIndex >= activePlayers.size()) {
            currentPlayerIndex = 0;
        }
        return activePlayers.get(currentPlayerIndex);
    }

    private int getTrackIndex(Piece piece) {
        if (piece.status != PieceStatus.ACTIVE || piece.stepsTaken < 1 || piece.stepsTaken > 51) {
            return -1;
        }
        return (piece.color.startOffset + piece.stepsTaken - 1) % 52;
    }

    private void updateTurnDisplay() {
        if (activePlayers.isEmpty()) return;
        Player p = getCurrentPlayer();
        if (p == null) return;
        turnBannerLabel.setText("Current Turn: " + p.name);
        turnColorBadge.setBackground(p.color.primary);
    }

    private class BoardPanel extends JPanel {

        public BoardPanel() {
            setBackground(Color.WHITE);
            setBorder(new LineBorder(new Color(40, 40, 45), 3));
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (gameState != GameState.WAITING_FOR_PIECE) return;

                    int size = Math.min(getWidth(), getHeight());
                    int offsetX = (getWidth() - size) / 2;
                    int offsetY = (getHeight() - size) / 2;
                    double cellSize = size / 15.0;

                    int col = (int) ((e.getX() - offsetX) / cellSize) + 1;
                    int row = (int) ((e.getY() - offsetY) / cellSize) + 1;

                    if (col < 1 || col > 15 || row < 1 || row > 15) return;

                    Player current = getCurrentPlayer();
                    if (current == null) return;
                    List<Piece> movable = getMovablePieces(current, currentDiceRoll);

                    for (Piece p : movable) {
                        BoardCoord coord = getCoordinatesForPiece(p);
                        if (coord != null && coord.col == col && coord.row == row) {
                            executePieceSelection(p);
                            return;
                        }
                    }

                    for (Piece p : movable) {
                        if (p.status == PieceStatus.HOME) {
                            int qCol = (p.color == PlayerColor.GREEN || p.color == PlayerColor.RED) ? 1 : 10;
                            int qRow = (p.color == PlayerColor.GREEN || p.color == PlayerColor.YELLOW) ? 1 : 10;
                            if (col >= qCol && col < qCol + 6 && row >= qRow && row < qRow + 6) {
                                executePieceSelection(p);
                                return;
                            }
                        }
                    }

                    for (Piece p : movable) {
                        BoardCoord coord = getCoordinatesForPiece(p);
                        if (coord != null) {
                            double cx = offsetX + (coord.col - 1) * cellSize + cellSize / 2.0;
                            double cy = offsetY + (coord.row - 1) * cellSize + cellSize / 2.0;
                            if (Math.hypot(e.getX() - cx, e.getY() - cy) <= cellSize * 0.75) {
                                executePieceSelection(p);
                                return;
                            }
                        }
                    }
                }
            });
        }

        private BoardCoord getCoordinatesForPiece(Piece piece) {
            if (piece.status == PieceStatus.HOME) {
                return BASE_COORDS.get(piece.color)[piece.pieceIndex];
            }
            if (piece.status == PieceStatus.FINISHED) {
                return WIN_AREAS.get(piece.color);
            }
            if (piece.status == PieceStatus.ACTIVE) {
                if (piece.stepsTaken >= 1 && piece.stepsTaken <= 51) {
                    int trackIdx = (piece.color.startOffset + piece.stepsTaken - 1) % 52;
                    return BOARD_PATH[trackIdx];
                }
                if (piece.stepsTaken >= 52 && piece.stepsTaken <= 56) {
                    int laneIdx = piece.stepsTaken - 52;
                    return HOME_LANES.get(piece.color)[laneIdx];
                }
            }
            return null;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int size = Math.min(getWidth(), getHeight());
            int offsetX = (getWidth() - size) / 2;
            int offsetY = (getHeight() - size) / 2;
            double cellSize = size / 15.0;

            drawBoardCells(g2, offsetX, offsetY, cellSize);
            drawHomeBases(g2, offsetX, offsetY, cellSize);
            drawCenterGoal(g2, offsetX, offsetY, cellSize);
            drawSpecialSpotIcons(g2, offsetX, offsetY, cellSize);
            drawPieces(g2, offsetX, offsetY, cellSize);

            int boardSize = (int) Math.round(15 * cellSize);
            g2.setColor(new Color(30, 30, 35));
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawRect(offsetX, offsetY, boardSize, boardSize);

            g2.dispose();
        }

        private void drawBoardCells(Graphics2D g2, int ox, int oy, double cs) {
            for (int r = 1; r <= 15; r++) {
                for (int c = 1; c <= 15; c++) {
                    int x = (int) Math.round(ox + (c - 1) * cs);
                    int y = (int) Math.round(oy + (r - 1) * cs);
                    int w = (int) Math.round(cs);
                    int h = (int) Math.round(cs);

                    g2.setColor(Color.WHITE);
                    g2.fillRect(x, y, w, h);
                    g2.setColor(new Color(210, 210, 215));
                    g2.drawRect(x, y, w, h);
                }
            }

            paintCellColor(g2, ox, oy, cs, 2, 7, PlayerColor.GREEN.primary);
            paintCellColor(g2, ox, oy, cs, 9, 2, PlayerColor.YELLOW.primary);
            paintCellColor(g2, ox, oy, cs, 14, 9, PlayerColor.BLUE.primary);
            paintCellColor(g2, ox, oy, cs, 7, 14, PlayerColor.RED.primary);

            for (BoardCoord bc : HOME_LANES.get(PlayerColor.GREEN)) paintCellColor(g2, ox, oy, cs, bc.col, bc.row, PlayerColor.GREEN.primary);
            for (BoardCoord bc : HOME_LANES.get(PlayerColor.YELLOW)) paintCellColor(g2, ox, oy, cs, bc.col, bc.row, PlayerColor.YELLOW.primary);
            for (BoardCoord bc : HOME_LANES.get(PlayerColor.BLUE)) paintCellColor(g2, ox, oy, cs, bc.col, bc.row, PlayerColor.BLUE.primary);
            for (BoardCoord bc : HOME_LANES.get(PlayerColor.RED)) paintCellColor(g2, ox, oy, cs, bc.col, bc.row, PlayerColor.RED.primary);

            drawDirectionalArrow(g2, ox + 0.5 * cs, oy + 7.5 * cs, cs, Math.PI / 2.0, PlayerColor.GREEN.primary);
            drawDirectionalArrow(g2, ox + 7.5 * cs, oy + 0.5 * cs, cs, Math.PI, PlayerColor.YELLOW.primary);
            drawDirectionalArrow(g2, ox + 14.5 * cs, oy + 7.5 * cs, cs, -Math.PI / 2.0, PlayerColor.BLUE.primary);
            drawDirectionalArrow(g2, ox + 7.5 * cs, oy + 14.5 * cs, cs, 0.0, PlayerColor.RED.primary);
        }

        private void drawDirectionalArrow(Graphics2D g2, double cx, double cy, double cs, double angle, Color color) {
            double w = cs * 0.58;
            double h = cs * 0.62;
            double headH = h * 0.46;
            double shaftW = w * 0.38;

            Path2D.Double arrow = new Path2D.Double();
            arrow.moveTo(0, -h / 2.0);
            arrow.lineTo(w / 2.0, -h / 2.0 + headH);
            arrow.lineTo(shaftW / 2.0, -h / 2.0 + headH);
            arrow.lineTo(shaftW / 2.0, h / 2.0);
            arrow.lineTo(-shaftW / 2.0, h / 2.0);
            arrow.lineTo(-shaftW / 2.0, -h / 2.0 + headH);
            arrow.lineTo(-w / 2.0, -h / 2.0 + headH);
            arrow.closePath();

            AffineTransform oldTx = g2.getTransform();
            g2.translate(cx, cy);
            g2.rotate(angle);
            g2.setColor(color);
            g2.fill(arrow);
            g2.setColor(new Color(0, 0, 0, 80));
            g2.setStroke(new BasicStroke(1.0f));
            g2.draw(arrow);
            g2.setTransform(oldTx);
        }

        private void paintCellColor(Graphics2D g2, int ox, int oy, double cs, int c, int r, Color color) {
            int x = (int) Math.round(ox + (c - 1) * cs);
            int y = (int) Math.round(oy + (r - 1) * cs);
            int w = (int) Math.round(cs);
            int h = (int) Math.round(cs);

            g2.setColor(color);
            g2.fillRect(x, y, w, h);
            g2.setColor(new Color(0, 0, 0, 70));
            g2.drawRect(x, y, w, h);
        }

        private void drawHomeBases(Graphics2D g2, int ox, int oy, double cs) {
            drawBaseQuadrant(g2, ox, oy, cs, 1, 1, PlayerColor.GREEN);
            drawBaseQuadrant(g2, ox, oy, cs, 10, 1, PlayerColor.YELLOW);
            drawBaseQuadrant(g2, ox, oy, cs, 1, 10, PlayerColor.RED);
            drawBaseQuadrant(g2, ox, oy, cs, 10, 10, PlayerColor.BLUE);
        }

        private void drawBaseQuadrant(Graphics2D g2, int ox, int oy, double cs, int startCol, int startRow, PlayerColor col) {
            int x = (int) Math.round(ox + (startCol - 1) * cs);
            int y = (int) Math.round(oy + (startRow - 1) * cs);
            int w = (int) Math.round(6 * cs);
            int h = (int) Math.round(6 * cs);

            g2.setColor(col.primary);
            g2.fillRect(x, y, w, h);
            g2.setColor(Color.DARK_GRAY);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRect(x, y, w, h);

            BoardCoord[] slots = BASE_COORDS.get(col);
            for (BoardCoord slot : slots) {
                int cx = (int) Math.round(ox + (slot.col - 1) * cs + cs / 2.0);
                int cy = (int) Math.round(oy + (slot.row - 1) * cs + cs / 2.0);
                int r = (int) Math.round(cs * 0.40);

                g2.setColor(col.primary);
                g2.fillOval(cx - r, cy - r, 2 * r, 2 * r);
                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(1.8f));
                g2.drawOval(cx - r, cy - r, 2 * r, 2 * r);
            }
        }

        private void drawCenterGoal(Graphics2D g2, int ox, int oy, double cs) {
            int x = (int) Math.round(ox + 6 * cs);
            int y = (int) Math.round(oy + 6 * cs);
            int size = (int) Math.round(3 * cs);
            int cx = x + size / 2;
            int cy = y + size / 2;

            Polygon pGreen = new Polygon(new int[]{x, cx, x}, new int[]{y, cy, y + size}, 3);
            g2.setColor(PlayerColor.GREEN.primary);
            g2.fillPolygon(pGreen);

            Polygon pYellow = new Polygon(new int[]{x, cx, x + size}, new int[]{y, cy, y}, 3);
            g2.setColor(PlayerColor.YELLOW.primary);
            g2.fillPolygon(pYellow);

            Polygon pBlue = new Polygon(new int[]{x + size, cx, x + size}, new int[]{y, cy, y + size}, 3);
            g2.setColor(PlayerColor.BLUE.primary);
            g2.fillPolygon(pBlue);

            Polygon pRed = new Polygon(new int[]{x, cx, x + size}, new int[]{y + size, cy, y + size}, 3);
            g2.setColor(PlayerColor.RED.primary);
            g2.fillPolygon(pRed);

            g2.setColor(Color.DARK_GRAY);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRect(x, y, size, size);
            g2.drawLine(x, y, x + size, y + size);
            g2.drawLine(x + size, y, x, y + size);
        }

        private void drawSpecialSpotIcons(Graphics2D g2, int ox, int oy, double cs) {
            int[] starIndices = new int[]{8, 21, 34, 47};
            for (int idx : starIndices) {
                BoardCoord bc = BOARD_PATH[idx];
                double cx = ox + (bc.col - 1) * cs + cs / 2.0;
                double cy = oy + (bc.row - 1) * cs + cs / 2.0;
                drawStar(g2, cx, cy, cs * 0.38, cs * 0.17);
            }

            for (int idx : PERK_SPOTS) {
                BoardCoord bc = BOARD_PATH[idx];
                double cx = ox + (bc.col - 1) * cs + cs / 2.0;
                double cy = oy + (bc.row - 1) * cs + cs / 2.0;
                drawGiftBox(g2, cx, cy, cs);
            }
        }

        private void drawGiftBox(Graphics2D g2, double cx, double cy, double cs) {
            double boxW = cs * 0.44;
            double boxH = cs * 0.32;
            double lidW = cs * 0.52;
            double lidH = cs * 0.11;
            double bowW = cs * 0.22;
            double bowH = cs * 0.10;

            double totalH = bowH + lidH + boxH;
            double startY = cy - totalH / 2.0;

            double bowY = startY;
            double lidY = startY + bowH;
            double boxY = lidY + lidH;

            double lidX = cx - lidW / 2.0;
            double boxX = cx - boxW / 2.0;

            double ribbonW = cs * 0.09;
            double ribbonX = cx - ribbonW / 2.0;

            g2.setColor(new Color(255, 193, 7));
            g2.fill(new Ellipse2D.Double(cx - bowW / 2.0, bowY, bowW / 2.0, bowH));
            g2.fill(new Ellipse2D.Double(cx, bowY, bowW / 2.0, bowH));
            g2.setColor(new Color(160, 100, 0));
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(new Ellipse2D.Double(cx - bowW / 2.0, bowY, bowW / 2.0, bowH));
            g2.draw(new Ellipse2D.Double(cx, bowY, bowW / 2.0, bowH));

            g2.setColor(new Color(142, 36, 170));
            g2.fill(new RoundRectangle2D.Double(boxX, boxY, boxW, boxH, 4, 4));

            g2.setColor(new Color(171, 71, 188));
            g2.fill(new RoundRectangle2D.Double(lidX, lidY, lidW, lidH, 4, 4));

            g2.setColor(new Color(255, 215, 0));
            g2.fill(new Rectangle2D.Double(ribbonX, lidY, ribbonW, lidH));
            g2.fill(new Rectangle2D.Double(ribbonX, boxY, ribbonW, boxH));
            double hRibbonH = cs * 0.07;
            double hRibbonY = boxY + boxH * 0.42;
            g2.fill(new Rectangle2D.Double(boxX, hRibbonY, boxW, hRibbonH));

            g2.setColor(new Color(255, 235, 59));
            g2.fill(new Ellipse2D.Double(cx - cs * 0.04, bowY + bowH * 0.45, cs * 0.08, cs * 0.08));

            g2.setColor(new Color(60, 20, 70));
            g2.setStroke(new BasicStroke(1.3f));
            g2.draw(new RoundRectangle2D.Double(boxX, boxY, boxW, boxH, 4, 4));
            g2.draw(new RoundRectangle2D.Double(lidX, lidY, lidW, lidH, 4, 4));
        }

        private void drawStar(Graphics2D g2, double cx, double cy, double rOuter, double rInner) {
            Path2D.Double path = new Path2D.Double();
            double startAngle = -Math.PI / 2.0;
            for (int i = 0; i < 10; i++) {
                double angle = startAngle + i * (Math.PI / 5.0);
                double r = (i % 2 == 0) ? rOuter : rInner;
                double x = cx + r * Math.cos(angle);
                double y = cy + r * Math.sin(angle);
                if (i == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }
            path.closePath();

            g2.setColor(Color.WHITE);
            g2.fill(path);

            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(path);
        }

        private void drawPieces(Graphics2D g2, int ox, int oy, double cs) {
            Map<BoardCoord, List<Piece>> cellOccupants = new HashMap<>();

            for (Player player : activePlayers) {
                for (Piece piece : player.pieces) {
                    BoardCoord coord = getCoordinatesForPiece(piece);
                    if (coord != null) {
                        cellOccupants.computeIfAbsent(coord, k -> new ArrayList<>()).add(piece);
                    }
                }
            }

            Player current = getCurrentPlayer();
            List<Piece> movable = (current != null && gameState == GameState.WAITING_FOR_PIECE) ? getMovablePieces(current, currentDiceRoll) : Collections.emptyList();

            for (Map.Entry<BoardCoord, List<Piece>> entry : cellOccupants.entrySet()) {
                BoardCoord coord = entry.getKey();
                List<Piece> list = entry.getValue();

                int baseCenterX = (int) Math.round(ox + (coord.col - 1) * cs + cs / 2.0);
                int baseCenterY = (int) Math.round(oy + (coord.row - 1) * cs + cs / 2.0);

                int count = list.size();
                double pieceRadius = cs * 0.34;

                for (int i = 0; i < count; i++) {
                    Piece p = list.get(i);
                    int px = baseCenterX;
                    int py = baseCenterY;

                    if (count > 1) {
                        pieceRadius = cs * 0.26;
                        double angle = 2 * Math.PI * i / count;
                        double dist = cs * 0.22;
                        px += (int) (Math.cos(angle) * dist);
                        py += (int) (Math.sin(angle) * dist);
                    }

                    boolean isMovable = movable.contains(p);
                    Image pieceImg = PIECE_IMAGES.get(p.color);
                    if (pieceImg != null) {
                        drawRasterPiece(g2, px, py, (int) pieceRadius, pieceImg, isMovable);
                    } else {
                        drawVectorPawn(g2, px, py, (int) pieceRadius, p.color, isMovable);
                    }
                }
            }
        }

        private void drawRasterPiece(Graphics2D g2, int cx, int cy, int r, Image img, boolean isMovable) {
            if (isMovable) {
                int glowR = (int) (r * 1.5);
                g2.setColor(new Color(255, 215, 0, (int) (255 * pulseAlpha)));
                g2.setStroke(new BasicStroke(3.5f));
                g2.drawOval(cx - glowR, cy - glowR, glowR * 2, glowR * 2);

                g2.setColor(new Color(255, 235, 59, 100));
                g2.fillOval(cx - glowR, cy - glowR, glowR * 2, glowR * 2);
            }

            g2.setColor(new Color(0, 0, 0, 75));
            g2.fillOval(cx - r, cy + (int) (r * 0.4), r * 2, (int) (r * 0.8));

            int imgW = (int) Math.round(r * 2.2);
            int imgH = (int) Math.round(r * 2.2);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(img, cx - imgW / 2, cy - imgH / 2 - (int) (r * 0.1), imgW, imgH, null);
        }

        private void drawVectorPawn(Graphics2D g2, int cx, int cy, int r, PlayerColor color, boolean isMovable) {
            if (isMovable) {
                int glowR = (int) (r * 1.6);
                g2.setColor(new Color(255, 215, 0, (int) (255 * pulseAlpha)));
                g2.setStroke(new BasicStroke(3.5f));
                g2.drawOval(cx - glowR, cy - glowR, glowR * 2, glowR * 2);

                g2.setColor(new Color(255, 235, 59, 120));
                g2.fillOval(cx - glowR, cy - glowR, glowR * 2, glowR * 2);
            }

            g2.setColor(new Color(0, 0, 0, 70));
            g2.fillOval(cx - r + 2, cy + r / 2, (int) (r * 1.8), r);

            g2.setColor(color.dark);
            g2.fillOval(cx - r, cy + r / 4, r * 2, (int) (r * 0.75));
            g2.setColor(new Color(255, 255, 255, 100));
            g2.drawOval(cx - r, cy + r / 4, r * 2, (int) (r * 0.75));

            int bodyTopW = (int) (r * 0.7);
            int bodyBottomW = (int) (r * 1.4);
            int bodyTopY = cy - (int) (r * 0.6);
            int bodyBottomY = cy + (int) (r * 0.4);

            Polygon body = new Polygon(
                    new int[]{cx - bodyTopW / 2, cx + bodyTopW / 2, cx + bodyBottomW / 2, cx - bodyBottomW / 2},
                    new int[]{bodyTopY, bodyTopY, bodyBottomY, bodyBottomY},
                    4
            );
            GradientPaint bodyGrad = new GradientPaint(
                    cx - r, bodyTopY, color.primary,
                    cx + r, bodyBottomY, color.dark
            );
            g2.setPaint(bodyGrad);
            g2.fillPolygon(body);

            int headR = (int) (r * 0.85);
            int headX = cx - headR;
            int headY = cy - (int) (r * 1.4);

            RadialGradientPaint headGrad = new RadialGradientPaint(
                    new Point2D.Float(headX + headR * 0.7f, headY + headR * 0.7f),
                    headR * 1.2f,
                    new float[]{0f, 0.7f, 1f},
                    new Color[]{Color.WHITE, color.primary, color.dark}
            );
            g2.setPaint(headGrad);
            g2.fillOval(headX, headY, headR * 2, headR * 2);

            g2.setColor(new Color(0, 0, 0, 80));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawOval(headX, headY, headR * 2, headR * 2);
        }
    }

    private static class DiceComponent extends JPanel {
        private int value = 1;
        private boolean isRolling = false;

        public DiceComponent() {
            setPreferredSize(new Dimension(80, 80));
            setMaximumSize(new Dimension(80, 80));
            setBackground(Color.WHITE);
        }

        public void setRollingFace(int tempVal) {
            this.value = tempVal;
            this.isRolling = true;
            repaint();
        }

        public void setFinalValue(int finalVal) {
            this.value = finalVal;
            this.isRolling = false;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth() - 8;
            int h = getHeight() - 8;

            g2.setColor(new Color(0, 0, 0, 30));
            g2.fillRoundRect(8, 8, w, h, 18, 18);

            GradientPaint gp = new GradientPaint(0, 0, Color.WHITE, w, h, new Color(240, 240, 245));
            g2.setPaint(gp);
            g2.fillRoundRect(4, 4, w, h, 18, 18);

            g2.setColor(isRolling ? new Color(33, 150, 243) : new Color(180, 180, 190));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(4, 4, w, h, 18, 18);

            g2.setColor(isRolling ? new Color(33, 150, 243) : new Color(40, 40, 45));
            int pr = 12;
            int cx = 4 + w / 2;
            int cy = 4 + h / 2;
            int l = 4 + w / 4;
            int r = 4 + 3 * w / 4;
            int t = 4 + h / 4;
            int b = 4 + 3 * h / 4;

            switch (value) {
                case 1:
                    drawPip(g2, cx, cy, pr);
                    break;
                case 2:
                    drawPip(g2, l, t, pr);
                    drawPip(g2, r, b, pr);
                    break;
                case 3:
                    drawPip(g2, l, t, pr);
                    drawPip(g2, cx, cy, pr);
                    drawPip(g2, r, b, pr);
                    break;
                case 4:
                    drawPip(g2, l, t, pr);
                    drawPip(g2, r, t, pr);
                    drawPip(g2, l, b, pr);
                    drawPip(g2, r, b, pr);
                    break;
                case 5:
                    drawPip(g2, l, t, pr);
                    drawPip(g2, r, t, pr);
                    drawPip(g2, cx, cy, pr);
                    drawPip(g2, l, b, pr);
                    drawPip(g2, r, b, pr);
                    break;
                case 6:
                    drawPip(g2, l, t, pr);
                    drawPip(g2, r, t, pr);
                    drawPip(g2, l, cy, pr);
                    drawPip(g2, r, cy, pr);
                    drawPip(g2, l, b, pr);
                    drawPip(g2, r, b, pr);
                    break;
            }

            g2.dispose();
        }

        private void drawPip(Graphics2D g2, int x, int y, int d) {
            g2.fillOval(x - d / 2, y - d / 2, d, d);
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            LudoGame game = new LudoGame();
            game.setVisible(true);
        });
    }
}
