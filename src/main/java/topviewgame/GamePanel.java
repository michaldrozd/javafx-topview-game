package topviewgame;

import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.util.Duration;
import topviewgame.item.Building;
import topviewgame.item.NPC;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents the main game panel where the game is played.
 */
public class GamePanel extends Pane {
    // Constants
    private static final int CHARACTER_SIZE = 40;
    private static final double BUILDING_SPACING = 2 * CHARACTER_SIZE; // Ensures at least 2x player size spacing
    private static final double SPEED = 5;
    private static final long NPC_SPAWN_INTERVAL = 2500;
    private static final long PEACE_TIME_DURATION = 3000;
    // Interval to increase the number of NPCs to spawn
    private static final long SPAWN_INCREASE_INTERVAL = 30000; // 30 secoonds

    // Game dimensions
    private double GAME_WIDTH;
    private double GAME_HEIGHT;

    // Player position and velocity
    private double characterX;
    private double characterY;
    private double playerVelocityX = 0;
    private double playerVelocityY = 0;

    // Game entities
    private final List<NPC> npcs = new ArrayList<>();
    private final List<Building> buildings = new ArrayList<>();
    private ImageView character;

    // Animations and timers
    private Animation walkingAnimation;
    private AnimationTimer timer;

    // Game state flags
    private boolean gameOver = false;
    private boolean peaceTime = true;

    // Movement flags
    private boolean movingUp = false;
    private boolean movingDown = false;
    private boolean movingLeft = false;
    private boolean movingRight = false;

    // Timer variables
    private Text timerText;
    private Text npcCountText;
    private long startTime;
    private long lastNPCSpawnTime;
    private long peaceStartTime;

    // Game Over Text
    private Text gameOverText;

    // NPC spawn management
    private int npcsToSpawn = 1;
    private long nextSpawnIncreaseTime;

    public GamePanel() {
        initializeGame();
    }

    /**
     * Initializes the game state.
     */
    private void initializeGame() {
        // Clear previous game state
        getChildren().clear();
        npcs.clear();
        buildings.clear();
        gameOver = false;

        // Reset movement flags
        movingUp = false;
        movingDown = false;
        movingLeft = false;
        movingRight = false;

        // Get full-screen dimensions
        GAME_WIDTH = Screen.getPrimary().getBounds().getWidth();
        GAME_HEIGHT = Screen.getPrimary().getBounds().getHeight();

        setStyle("-fx-background-color: green;");
        setPrefSize(GAME_WIDTH, GAME_HEIGHT); // Set the preferred size of the pane

        // Initialize timer variables
        startTime = System.currentTimeMillis();
        lastNPCSpawnTime = startTime;
        peaceStartTime = startTime;
        peaceTime = true;
        nextSpawnIncreaseTime = startTime + SPAWN_INCREASE_INTERVAL;
        npcsToSpawn = 1;

        // Generate game elements
        generateBorders();
        generateMap();
        initializeCharacter();
        generateNPCs();

        // Setup walking animation
        walkingAnimation = createWalkingAnimation();

        // Initialize timer display
        timerText = new Text();
        timerText.setFill(Color.WHITE);
        timerText.setFont(Font.font("Verdana", 40));
        timerText.setX(20);
        timerText.setY(50);
        getChildren().add(timerText);

        // Initialize NPC count display
        npcCountText = new Text();
        npcCountText.setFill(Color.WHITE);
        npcCountText.setFont(Font.font("Verdana", 40));
        npcCountText.setX(20);
        npcCountText.setY(100);
        getChildren().add(npcCountText);

        setFocusTraversable(true);
        addEventHandlers();

        // Stop previous timer if it exists
        if (timer != null) {
            timer.stop();
        }

        // Start the game loop
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (gameOver) {
                    return;
                }
                updateTimer();
                updateNpcCount(); // Update NPC count display
                moveCharacter();
                if (!peaceTime) {
                    moveNPCs();
                    checkAndAddNPCs();
                } else {
                    checkPeaceTime();
                }
                handleCollisions();
            }
        };
        timer.start();
    }

    /**
     * Generates the borders around the map that act as walls.
     */
    private void generateBorders() {
        // Create borders as buildings
        // Top border
        var topBorder = new Building(0, 0, GAME_WIDTH, 10);
        buildings.add(topBorder);

        // Bottom border
        var bottomBorder = new Building(0, GAME_HEIGHT - 10, GAME_WIDTH, 10);
        buildings.add(bottomBorder);

        // Left border
        var leftBorder = new Building(0, 0, 10, GAME_HEIGHT);
        buildings.add(leftBorder);

        // Right border
        var rightBorder = new Building(GAME_WIDTH - 10, 0, 10, GAME_HEIGHT);
        buildings.add(rightBorder);

        // Set border appearance
        topBorder.getShape().setFill(Color.DARKGRAY);
        bottomBorder.getShape().setFill(Color.DARKGRAY);
        leftBorder.getShape().setFill(Color.DARKGRAY);
        rightBorder.getShape().setFill(Color.DARKGRAY);

        // Add borders to the scene graph to make them visible
        getChildren().addAll(
            topBorder.getShape(),
            bottomBorder.getShape(),
            leftBorder.getShape(),
            rightBorder.getShape()
        );
    }

    /**
     * Initializes the player character.
     */
    private void initializeCharacter() {
        // Spawn character near the center
        characterX = GAME_WIDTH / 2 - CHARACTER_SIZE / 2;
        characterY = GAME_HEIGHT / 2 - CHARACTER_SIZE / 2;

        // Ensure character doesn't overlap with buildings
        int attempts = 0;
        boolean overlaps;
        do {
            overlaps = false;

            var characterShape = new Rectangle(characterX, characterY, CHARACTER_SIZE, CHARACTER_SIZE);

            // Check overlap with buildings
            for (Building building : buildings) {
                if (characterShape.getBoundsInParent().intersects(building.getShape().getBoundsInParent())) {
                    overlaps = true;
                    // Adjust character position slightly and retry
                    characterX += 10;
                    characterY += 10;
                    break;
                }
            }

            attempts++;
        } while (overlaps && attempts < 100);

        if (attempts >= 100) {
            System.err.println("Could not find a suitable spawn location for the character.");
        }

        // Load the character idle image
        var characterImage = new Image(getClass().getResource("/player_idle.png").toExternalForm());
        character = new ImageView(characterImage);
        character.setFitWidth(CHARACTER_SIZE);
        character.setFitHeight(CHARACTER_SIZE);
        character.setX(characterX);
        character.setY(characterY);
        getChildren().add(character);
    }

    /**
     * Generates the map with buildings.
     */
    private void generateMap() {
        var random = ThreadLocalRandom.current();
        int attempts;
        int numBuildings = (int) ((GAME_WIDTH * GAME_HEIGHT) / 80000); // Adjusted for fewer buildings
        double width = 100;
        double height = 100;

        double minX = BUILDING_SPACING / 2;
        double maxX = GAME_WIDTH - width - BUILDING_SPACING / 2;
        double minY = BUILDING_SPACING / 2;
        double maxY = GAME_HEIGHT - height - BUILDING_SPACING / 2;

        for (int i = 0; i < numBuildings; i++) {
            attempts = 0;
            boolean overlaps;
            double x, y;

            do {
                overlaps = false;
                x = random.nextDouble(minX, maxX);
                y = random.nextDouble(minY, maxY);

                var expandedNewBuildingShape = new Rectangle(
                        x - BUILDING_SPACING / 2,
                        y - BUILDING_SPACING / 2,
                        width + BUILDING_SPACING,
                        height + BUILDING_SPACING
                );

                for (Building building : buildings) {
                    double buildingX = building.getX();
                    double buildingY = building.getY();
                    double buildingWidth = building.getShape().getBoundsInParent().getWidth();
                    double buildingHeight = building.getShape().getBoundsInParent().getHeight();

                    var expandedExistingBuildingShape = new Rectangle(
                            buildingX - BUILDING_SPACING / 2,
                            buildingY - BUILDING_SPACING / 2,
                            buildingWidth + BUILDING_SPACING,
                            buildingHeight + BUILDING_SPACING
                    );

                    if (expandedNewBuildingShape.getBoundsInParent()
                            .intersects(expandedExistingBuildingShape.getBoundsInParent())) {
                        overlaps = true;
                        break;
                    }
                }

                attempts++;
            } while (overlaps && attempts < 100);

            if (attempts < 100) {
                var building = new Building(x, y);
                buildings.add(building);
                getChildren().add(building.getShape());
            }
        }
    }

    /**
     * Generates initial NPCs.
     */
    private void generateNPCs() {
        var random = ThreadLocalRandom.current();
        int attempts;
        int numNPCs = (int) ((GAME_WIDTH * GAME_HEIGHT) / 100000); // Adjusted for more NPCs
        for (int i = 0; i < numNPCs; i++) {
            attempts = 0;
            boolean overlaps;
            double x, y;
            do {
                overlaps = false;
                x = random.nextDouble(0, GAME_WIDTH - 40);
                y = random.nextDouble(0, GAME_HEIGHT - 40);

                var npcShape = new Rectangle(x, y, 40, 40);

                // Check distance from the character
                double distanceToCharacter = Math.hypot(x - characterX, y - characterY);
                if (distanceToCharacter < 200) { // Ensure NPCs are not spawned too close to the player
                    overlaps = true;
                }

                // Check overlap with buildings
                if (!overlaps) {
                    for (Building building : buildings) {
                        if (npcShape.getBoundsInParent().intersects(building.getShape().getBoundsInParent())) {
                            overlaps = true;
                            break;
                        }
                    }
                }

                // Check overlap with existing NPCs
                if (!overlaps) {
                    for (NPC existingNpc : npcs) {
                        if (npcShape.getBoundsInParent()
                                .intersects(existingNpc.getShape().getBoundsInParent())) {
                            overlaps = true;
                            break;
                        }
                    }
                }
                attempts++;
            } while (overlaps && attempts < 100);

            if (attempts < 100) {
                var npc = new NPC(x, y, this);
                npcs.add(npc);
                getChildren().add(npc.getShape());
            }
        }
    }

    /**
     * Adds event handlers for key presses.
     */
    private void addEventHandlers() {
        setOnKeyPressed(e -> {
            KeyCode keyCode = e.getCode();
            if (gameOver) {
                if (keyCode == KeyCode.SPACE || keyCode == KeyCode.ENTER) {
                    initializeGame(); // Restart the game
                }
                return;
            }

            switch (keyCode) {
                case UP -> movingUp = true;
                case DOWN -> movingDown = true;
                case LEFT -> movingLeft = true;
                case RIGHT -> movingRight = true;
                default -> {
                }
            }
            if (walkingAnimation.getStatus() != Animation.Status.RUNNING) {
                walkingAnimation.play();
            }
        });

        setOnKeyReleased(e -> {
            KeyCode keyCode = e.getCode();
            if (gameOver) {
                return;
            }

            switch (keyCode) {
                case UP -> movingUp = false;
                case DOWN -> movingDown = false;
                case LEFT -> movingLeft = false;
                case RIGHT -> movingRight = false;
                default -> {
                }
            }
            if (!movingUp && !movingDown && !movingLeft && !movingRight) {
                walkingAnimation.stop();
                // Reset to idle image
                character.setImage(new Image(getClass().getResource("/player_idle.png").toExternalForm()));
            }
        });
    }

    /**
     * Moves the player character based on input.
     */
    private void moveCharacter() {
        double oldX = characterX;
        double oldY = characterY;

        if (movingUp) {
            characterY -= SPEED;
        }
        if (movingDown) {
            characterY += SPEED;
        }
        if (movingLeft) {
            characterX -= SPEED;
        }
        if (movingRight) {
            characterX += SPEED;
        }

        // Calculate velocity
        playerVelocityX = characterX - oldX;
        playerVelocityY = characterY - oldY;

        // Keep the character within bounds using Math.clamp (Java 21 feature)
        characterX = Math.clamp(characterX, 0, GAME_WIDTH - CHARACTER_SIZE);
        characterY = Math.clamp(characterY, 0, GAME_HEIGHT - CHARACTER_SIZE);

        character.setX(characterX);
        character.setY(characterY);

        // Handle collisions with buildings
        for (Building building : buildings) {
            if (character.getBoundsInParent().intersects(building.getShape().getBoundsInParent())) {
                characterX = oldX;
                characterY = oldY;
                character.setX(characterX);
                character.setY(characterY);
                // Reset velocity due to collision
                playerVelocityX = 0;
                playerVelocityY = 0;
                break;
            }
        }
    }

    /**
     * Creates the walking animation for the player.
     */
    private Animation createWalkingAnimation() {
        // Load the walking frames
        var walkingFrames = new Image[]{
                new Image(getClass().getResource("/player_walk1.png").toExternalForm()),
                new Image(getClass().getResource("/player_walk2.png").toExternalForm())
        };

        // Create a Timeline to cycle through the frames
        var timeline = new Timeline();
        timeline.setCycleCount(Animation.INDEFINITE);
        var frameDuration = Duration.millis(200); // Adjust the frame duration as needed

        var keyFrame1 = new KeyFrame(Duration.ZERO, e -> character.setImage(walkingFrames[0]));
        var keyFrame2 = new KeyFrame(frameDuration, e -> character.setImage(walkingFrames[1]));

        timeline.getKeyFrames().addAll(keyFrame1, keyFrame2);

        return timeline;
    }

    /**
     * Moves all NPCs towards the player.
     */
    private void moveNPCs() {
        for (NPC npc : npcs) {
            npc.moveTowardsTarget();
            npc.updatePosition();
        }
    }

    /**
     * Handles collisions between the player and NPCs.
     */
    private void handleCollisions() {
        if (gameOver) {
            return;
        }

        // Handle collision with NPCs
        for (NPC npc : npcs) {
            if (character.getBoundsInParent().intersects(npc.getShape().getBoundsInParent())) {
                gameOver = true;
                showGameOver();
                break;
            }
        }
    }

    /**
     * Displays the Game Over screen.
     */
    private void showGameOver() {
        // Stop the timer
        timer.stop();

        // Stop animations
        walkingAnimation.stop();

        // Display GAME OVER message
        gameOverText = new Text("GAME OVER\nPress SPACE or ENTER to restart");
        gameOverText.setFill(Color.RED);
        gameOverText.setFont(Font.font("Verdana", 50));
        gameOverText.setX(GAME_WIDTH / 2 - 200);
        gameOverText.setY(GAME_HEIGHT / 2);
        getChildren().add(gameOverText);
    }

    /**
     * Updates the on-screen timer.
     */
    private void updateTimer() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;
        long elapsedSeconds = elapsedTime / 1000;
        long minutes = elapsedSeconds / 60;
        long seconds = elapsedSeconds % 60;
        timerText.setText(String.format("Time: %02d:%02d", minutes, seconds));
    }

    /**
     * Updates the NPC count display.
     */
    private void updateNpcCount() {
        npcCountText.setText("NPCs: " + npcs.size());
    }

    /**
     * Checks if peace time has ended.
     */
    private void checkPeaceTime() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - peaceStartTime >= PEACE_TIME_DURATION) {
            peaceTime = false;
            npcs.forEach(npc -> npc.setPeaceTime(false));
        }
    }

    /**
     * Checks if it's time to add new NPCs and increases spawn count every interval.
     */
    private void checkAndAddNPCs() {
        long currentTime = System.currentTimeMillis();

        // Spawn NPCs at intervals
        if (currentTime - lastNPCSpawnTime >= NPC_SPAWN_INTERVAL) {
            for (int i = 0; i < npcsToSpawn; i++) {
                addNewNPC();
            }
            lastNPCSpawnTime = currentTime;
        }

        // Increase the number of NPCs to spawn every interval
        if (currentTime >= nextSpawnIncreaseTime) {
            npcsToSpawn++;
            nextSpawnIncreaseTime += SPAWN_INCREASE_INTERVAL;
        }
    }

    /**
     * Adds a new NPC near the player to the game.
     */
    private void addNewNPC() {
        var random = ThreadLocalRandom.current();
        int attempts = 0;
        boolean overlaps;
        double x = 0, y = 0;

        // Define minimum and maximum spawn distances from the player
        final double MIN_SPAWN_DISTANCE = 200; // Minimum distance from the player
        final double MAX_SPAWN_DISTANCE = 400; // Maximum distance from the player

        do {
            overlaps = false;

            // Generate a random angle
            double angle = random.nextDouble(0, 2 * Math.PI);

            // Generate a random distance within the specified range
            double distance = random.nextDouble(MIN_SPAWN_DISTANCE, MAX_SPAWN_DISTANCE);

            // Calculate NPC position based on angle and distance from player
            x = characterX + distance * Math.cos(angle);
            y = characterY + distance * Math.sin(angle);

            // Ensure the NPC is within the game bounds
            x = Math.clamp(x, 0, GAME_WIDTH - 40);
            y = Math.clamp(y, 0, GAME_HEIGHT - 40);

            var npcShape = new Rectangle(x, y, 40, 40);

            // Check overlap with buildings
            for (Building building : buildings) {
                if (npcShape.getBoundsInParent()
                        .intersects(building.getShape().getBoundsInParent())) {
                    overlaps = true;
                    break;
                }
            }

            // Check overlap with existing NPCs
            if (!overlaps) {
                for (NPC existingNpc : npcs) {
                    if (npcShape.getBoundsInParent()
                            .intersects(existingNpc.getShape().getBoundsInParent())) {
                        overlaps = true;
                        break;
                    }
                }
            }

            attempts++;
        } while ((overlaps || distanceToEdge(x, y) < MIN_SPAWN_DISTANCE) && attempts < 100);

        if (attempts < 100) {
            var npc = new NPC(x, y, this);
            // Set peaceTime flag according to current game state
            npc.setPeaceTime(this.peaceTime);
            npcs.add(npc);
            getChildren().add(npc.getShape());
        }
    }

    /**
     * Calculates the minimum distance from the given point to the edges of the game area.
     *
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return The minimum distance to the edge.
     */
    private double distanceToEdge(double x, double y) {
        double distanceRight = GAME_WIDTH - x;
        double distanceBottom = GAME_HEIGHT - y;
        return Math.min(Math.min(x, distanceRight), Math.min(y, distanceBottom));
    }

    public double getCharacterX() {
        return characterX;
    }

    public double getCharacterY() {
        return characterY;
    }

    public double getPlayerVelocityX() {
        return playerVelocityX;
    }

    public double getPlayerVelocityY() {
        return playerVelocityY;
    }

    public List<Building> getBuildings() {
        return buildings;
    }

    public List<NPC> getNpcs() {
        return npcs;
    }

    public boolean isPeaceTime() {
        return peaceTime;
    }
}
