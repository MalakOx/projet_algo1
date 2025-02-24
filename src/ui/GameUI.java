package ui;

import game.WordLabyrinth;
import model.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameUI extends JFrame {
    private WordLabyrinth game;
    private JPanel gridPanel;
    private JLabel scoreLabel;
    private JLabel movesLabel;
    private JLabel wordsLabel;
    private JLabel currentWordLabel;
    private JButton[][] cellButtons;
    private JTextArea foundWordsArea;
    private String playerName;
    private HighScoreManager highScoreManager;
    private static final Color LAST_CLICKED_COLOR = new Color(255, 182, 193);  // Light pink
    private static final Color[] PATH_COLORS = {
        new Color(135, 206, 250),  // Light blue
        new Color(255, 218, 185),  // Peach
        new Color(216, 191, 216)   // Thistle
    };
    private static final Color VALIDATED_WORD_COLOR = new Color(0, 0, 255);  // Blue for validated words
    private static final Color BONUS_CELL_COLOR = new Color(255, 255, 0);  // Bright yellow for bonus cells
    private static final Color CURRENT_POSITION_COLOR = new Color(255, 165, 0); // Orange for current position
    private Cell lastClickedCell;
    private int currentPathColorIndex = 0;
    private Map<List<Cell>, Integer> pathColors;  // Maps paths to their color indices

    public GameUI() {
        setTitle("Word Labyrinth");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        highScoreManager = new HighScoreManager();
        pathColors = new HashMap<>();
        getPlayerNameAndStart();
    }

    private void getPlayerNameAndStart() {
        playerName = JOptionPane.showInputDialog(this, "Enter your name:", "Player Name", JOptionPane.QUESTION_MESSAGE);
        if (playerName == null || playerName.trim().isEmpty()) {
            System.exit(0);
        }
        initializeGame();
    }

    private void initializeGame() {
        // Get grid size
        JTextField rowsField = new JTextField("8");
        JTextField colsField = new JTextField("8");
        Object[] message = {
            "Number of rows (5-15):", rowsField,
            "Number of columns (5-15):", colsField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Grid Size", JOptionPane.OK_CANCEL_OPTION);
        if (option != JOptionPane.OK_OPTION) {
            System.exit(0);
        }

        // Validate and get grid size
        int rows, cols;
        try {
            rows = Integer.parseInt(rowsField.getText().trim());
            cols = Integer.parseInt(colsField.getText().trim());
            rows = Math.max(5, Math.min(15, rows));
            cols = Math.max(5, Math.min(15, cols));
        } catch (NumberFormatException e) {
            rows = cols = 8;
        }

        // Get difficulty level
        Object[] options = {"Easy", "Medium", "Hard"};
        int choice = JOptionPane.showOptionDialog(this,
            "Select difficulty level:",
            "Word Labyrinth",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]);

        model.Dictionary.DifficultyLevel level;
        switch (choice) {
            case 1:
                level = model.Dictionary.DifficultyLevel.MEDIUM;
                break;
            case 2:
                level = model.Dictionary.DifficultyLevel.HARD;
                break;
            default:
                level = model.Dictionary.DifficultyLevel.EASY;
        }

        game = new WordLabyrinth(level, rows, cols);
        createUI();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void createUI() {
        setLayout(new BorderLayout(10, 10));

        // Create main panel with BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create legend panel on the left
        JPanel legendPanel = createLegendPanel();
        mainPanel.add(legendPanel, BorderLayout.WEST);

        // Create grid panel in the center
        gridPanel = createGridPanel();
        mainPanel.add(gridPanel, BorderLayout.CENTER);

        // Create info panel on the right
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        // Score section
        scoreLabel = new JLabel("Score: 0");
        scoreLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(scoreLabel);
        infoPanel.add(Box.createVerticalStrut(10));

        // Words Found section
        wordsLabel = new JLabel(String.format("Words Found: 0/%d", game.getRequiredWords()));
        wordsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(wordsLabel);
        infoPanel.add(Box.createVerticalStrut(10));

        // Moves Left section
        movesLabel = new JLabel("Moves Left: " + game.getMovesLeft());
        movesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(movesLabel);
        infoPanel.add(Box.createVerticalStrut(10));

        // Current Word section
        currentWordLabel = new JLabel("Current Word: ");
        currentWordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(currentWordLabel);
        infoPanel.add(Box.createVerticalStrut(10));

        // Found Words section
        JLabel foundWordsTitle = new JLabel("Found Words:");
        foundWordsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(foundWordsTitle);
        infoPanel.add(Box.createVerticalStrut(5));

        foundWordsArea = new JTextArea(10, 15);
        foundWordsArea.setEditable(false);
        foundWordsArea.setLineWrap(true);
        foundWordsArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(foundWordsArea);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(scrollPane);

        mainPanel.add(infoPanel, BorderLayout.EAST);

        // Create control panel at the bottom
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton submitButton = new JButton("Submit Word");
        submitButton.addActionListener(e -> handleSubmitWord());
        JButton resetButton = new JButton("Reset Path");
        resetButton.addActionListener(e -> resetPath());
        controlPanel.add(submitButton);
        controlPanel.add(resetButton);
        mainPanel.add(controlPanel, BorderLayout.SOUTH);

        // Add the main panel to the frame
        add(mainPanel);

        updateUI();
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel createLegendPanel() {
        JPanel legendPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        legendPanel.setBorder(BorderFactory.createTitledBorder("Legend"));
        
        addLegendItem(legendPanel, Color.WHITE, "Normal Cell");
        addLegendItem(legendPanel, BONUS_CELL_COLOR, "Bonus Cell (+25 points)");
        addLegendItem(legendPanel, Color.GREEN, "Start Cell");
        addLegendItem(legendPanel, Color.RED, "Finish Cell");
        addLegendItem(legendPanel, LAST_CLICKED_COLOR, "Current Path");
        addLegendItem(legendPanel, Color.BLACK, "Blocked Cell");
        addLegendItem(legendPanel, VALIDATED_WORD_COLOR, "Validated Word");
        addLegendItem(legendPanel, CURRENT_POSITION_COLOR, "Current Position");
        
        return legendPanel;
    }

    private void addLegendItem(JPanel legendPanel, Color color, String text) {
        JPanel itemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));

        // Create color square
        JPanel colorSquare = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(20, 20);
            }
        };
        colorSquare.setBackground(color);
        colorSquare.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        // Create label
        JLabel label = new JLabel(text);

        // Add components
        itemPanel.add(colorSquare);
        itemPanel.add(label);

        // Add some vertical spacing between items
        legendPanel.add(Box.createVerticalStrut(5));
        legendPanel.add(itemPanel);
    }

    private JPanel createGridPanel() {
        Grid grid = game.getGrid();
        JPanel gridPanel = new JPanel(new GridLayout(grid.getRows(), grid.getCols()));
        cellButtons = new JButton[grid.getRows()][grid.getCols()];

        for (int i = 0; i < grid.getRows(); i++) {
            for (int j = 0; j < grid.getCols(); j++) {
                Cell cell = grid.getCell(i, j);
                JButton button = new JButton(String.valueOf(cell.getLetter()));
                button.setPreferredSize(new Dimension(50, 50));
                button.setFont(new Font("Arial", Font.BOLD, 16));

                if (cell.isBlocked()) {
                    button.setBackground(Color.BLACK);
                    button.setEnabled(false);
                } else if (cell.isSpecial()) {
                    button.setBackground(BONUS_CELL_COLOR);
                } else if (cell == grid.getStartCell()) {
                    button.setBackground(Color.GREEN);
                } else if (cell == grid.getDestinationCell()) {
                    button.setBackground(Color.RED);
                }

                final int row = i;
                final int col = j;
                button.addActionListener(e -> handleCellClick(row, col));

                cellButtons[i][j] = button;
                gridPanel.add(button);
            }
        }

        return gridPanel;
    }

    private void showGameOver() {
        String scoreDetails = game.getFinalScoreDetails();
        String message = String.format("Game Over %s!\n\n%s", playerName, scoreDetails);

        // Check if it's a high score
        if (highScoreManager.isHighScore(game.getFinalScore())) {
            highScoreManager.addScore(playerName, game.getFinalScore());
            message += "\n\nNew High Score!";
        }

        // Show all high scores
        message += "\n\nHigh Scores:\n" + highScoreManager.getHighScoresText();

        int option = JOptionPane.showConfirmDialog(this,
            message + "\n\nWould you like to play again?",
            "Game Over",
            JOptionPane.YES_NO_OPTION);

        if (option == JOptionPane.YES_OPTION) {
            dispose();
            new GameUI();
        } else {
            System.exit(0);
        }
    }

    private void handleSubmitWord() {
        if (game.submitWord()) {
            updateUI();

            // Check if all words are found
            if (game.isComplete()) {
                JOptionPane.showMessageDialog(this,
                    "Congratulations! You've found all the words in this labyrinth! \n" +
                    "You earn a 50-point bonus for your achievement!",
                    "Labyrinth Complete!",
                    JOptionPane.INFORMATION_MESSAGE);
                showGameOver();
            }
        } else {
            JOptionPane.showMessageDialog(this,
                "Invalid word or already found!",
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleCellClick(int row, int col) {
        Cell cell = game.getGrid().getCell(row, col);

        // If it's the destination cell, end the game immediately
        if (cell == game.getGrid().getDestinationCell()) {
            showGameOver();
            return;
        }

        if (game.move(cell)) {
            lastClickedCell = cell;
            updateUI();

            if (game.hasWon() || game.hasLost()) {
                showGameOver();
            }
        }
    }

    private void resetPath() {
        if (!game.getCurrentPath().isEmpty()) {
            game.resetPath();
            updateUI();
        }
    }

    private void updateUI() {
        // Update labels
        updateScoreLabel();
        movesLabel.setText("Moves left: " + game.getMovesLeft());
        wordsLabel.setText("Words found: " + game.getFoundWords().size() + 
                         "/" + game.getRequiredWords());
        
        // Update current word
        StringBuilder currentWord = new StringBuilder();
        for (Cell cell : game.getCurrentPath()) {
            currentWord.append(cell.getLetter());
        }
        currentWordLabel.setText("Current Word: " + currentWord.toString());
        
        // Add hint for new round
        if (game.isNewRound()) {
            currentWordLabel.setText(currentWordLabel.getText() + " (Start from green cell)");
        }
        
        // Update found words area
        foundWordsArea.setText(String.join(", ", game.getFoundWords()));

        // Reset all buttons to their default state
        Grid grid = game.getGrid();
        for (int i = 0; i < grid.getRows(); i++) {
            for (int j = 0; j < grid.getCols(); j++) {
                Cell cell = grid.getCell(i, j);
                JButton button = cellButtons[i][j];
                
                // Set default background color
                if (cell.isBlocked()) {
                    button.setBackground(Color.BLACK);
                    button.setForeground(Color.WHITE);
                } else {
                    button.setBackground(Color.WHITE);
                    button.setForeground(Color.BLACK);
                }

                // Show bonus cells in yellow
                if (cell.isSpecial() && cell != grid.getStartCell() && cell != grid.getDestinationCell()) {
                    button.setBackground(BONUS_CELL_COLOR);
                }
                
                // Color the current path
                if (game.getCurrentPath().contains(cell)) {
                    button.setBackground(LAST_CLICKED_COLOR);
                }
                
                // Color cells that are part of validated words in blue
                boolean isPartOfValidatedWord = false;
                for (List<Cell> path : game.getPreviousPaths()) {
                    if (path.contains(cell)) {
                        button.setBackground(VALIDATED_WORD_COLOR);
                        isPartOfValidatedWord = true;
                        break;
                    }
                }
                
                // Override colors for special cells - these take precedence
                if (cell == grid.getStartCell()) {
                    button.setBackground(Color.GREEN);
                } else if (cell == grid.getDestinationCell()) {
                    button.setBackground(Color.RED);
                } else if (cell.isSpecial() && !isPartOfValidatedWord && !game.getCurrentPath().contains(cell)) {
                    // Keep bonus cells yellow unless they're part of a validated word or current path
                    button.setBackground(BONUS_CELL_COLOR);
                }
                
                // Highlight current position if not a new round
                if (!game.isNewRound() && cell == game.getCurrentPosition()) {
                    button.setBackground(CURRENT_POSITION_COLOR);
                }

                // Make buttons more visible
                button.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
                button.setFont(button.getFont().deriveFont(Font.BOLD));
            }
        }
    }

    private void updateScoreLabel() {
        scoreLabel.setText(String.format("Score: %d | Words Found: %d/%d | Moves Left: %d",
            game.getCurrentScore(), game.getFoundWords().size(), game.getRequiredWords(), game.getMovesLeft()));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameUI gameUI = new GameUI();
        });
    }
}
