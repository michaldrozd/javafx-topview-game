package topviewgame.item;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import topviewgame.GamePanel;

import java.util.List;

/**
 * Represents a Non-Player Character (NPC) that pursues the player.
 */
public class NPC {
    // Position and speed
    private double x;
    private double y;
    private static final double SPEED = 2; // Adjust speed as needed

    // Target position
    private double targetX;
    private double targetY;

    // Game dimensions
    private final double gameWidth;
    private final double gameHeight;

    // References to game components
    private final GamePanel gamePanel;
    private final List<Building> buildings;
    private final List<NPC> npcs;

    // Time management
    private long lastTargetUpdateTime = 0;
    private static final long TARGET_UPDATE_INTERVAL = 500; // Update target every 0.5 seconds

    // Visual representation
    private final ImageView shape;
    private final Animation walkingAnimation;

    // Peace time flag
    private boolean peaceTime;

    /**
     * Constructs a new NPC at the specified coordinates.
     *
     * @param x         The x-coordinate of the NPC.
     * @param y         The y-coordinate of the NPC.
     * @param gamePanel Reference to the main game panel.
     */
    public NPC(double x, double y, GamePanel gamePanel) {
        this.x = x;
        this.y = y;
        this.gamePanel = gamePanel;
        this.gameWidth = gamePanel.getPrefWidth();
        this.gameHeight = gamePanel.getPrefHeight();
        this.buildings = gamePanel.getBuildings();
        this.npcs = gamePanel.getNpcs();

        // Initialize peace time flag based on the game's current state
        this.peaceTime = gamePanel.isPeaceTime();

        // Load the NPC image
        shape = new ImageView();
        shape.setFitWidth(40);
        shape.setFitHeight(40);
        shape.setX(x);
        shape.setY(y);

        // Initialize walking animation
        walkingAnimation = createWalkingAnimation();
        walkingAnimation.play();

        pickNewTarget();
    }

    /**
     * Creates the walking animation for the NPC.
     *
     * @return The walking animation.
     */
    private Animation createWalkingAnimation() {
        // Load the walking frames
        var walkingFrames = new Image[]{
                new Image(getClass().getResource("/npc_walk1.png").toExternalForm()),
                new Image(getClass().getResource("/npc_walk2.png").toExternalForm())
        };

        // Create a Timeline to cycle through the frames
        var timeline = new Timeline();
        timeline.setCycleCount(Animation.INDEFINITE);
        var frameDuration = Duration.millis(200); // Adjust the frame duration as needed

        var keyFrame1 = new KeyFrame(Duration.ZERO, e -> shape.setImage(walkingFrames[0]));
        var keyFrame2 = new KeyFrame(frameDuration, e -> shape.setImage(walkingFrames[1]));

        timeline.getKeyFrames().addAll(keyFrame1, keyFrame2);

        return timeline;
    }

    /**
     * Sets the peace time flag for the NPC.
     *
     * @param peaceTime True if the game is in peace time; false otherwise.
     */
    public void setPeaceTime(boolean peaceTime) {
        this.peaceTime = peaceTime;
    }

    /**
     * Picks a new target position, aiming for the predicted player location.
     */
    public void pickNewTarget() {
        // Prediction time in milliseconds
        double predictionTime = 500; // 0.5 seconds

        // Get player's current position and velocity
        double playerX = gamePanel.getCharacterX();
        double playerY = gamePanel.getCharacterY();
        double playerVelocityX = gamePanel.getPlayerVelocityX();
        double playerVelocityY = gamePanel.getPlayerVelocityY();

        // Predict player's future position
        double predictedX = playerX + playerVelocityX * predictionTime / 16; // Divided by frame time (~16ms)
        double predictedY = playerY + playerVelocityY * predictionTime / 16;

        // Clamp predicted position within game bounds
        predictedX = Math.clamp(predictedX, 0, gameWidth - shape.getFitWidth());
        predictedY = Math.clamp(predictedY, 0, gameHeight - shape.getFitHeight());

        // If player is stationary, target current position
        if (playerVelocityX == 0 && playerVelocityY == 0) {
            predictedX = playerX;
            predictedY = playerY;
        }

        targetX = predictedX;
        targetY = predictedY;
    }

    /**
     * Moves the NPC towards its target, adjusting for obstacles.
     */
    public void moveTowardsTarget() {
        if (peaceTime) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTargetUpdateTime >= TARGET_UPDATE_INTERVAL) {
            pickNewTarget();
            lastTargetUpdateTime = currentTime;
        }

        double deltaX = targetX - x;
        double deltaY = targetY - y;
        double distance = Math.hypot(deltaX, deltaY);

        if (distance < SPEED) {
            x = targetX;
            y = targetY;
        } else {
            double stepX = (deltaX / distance) * SPEED;
            double stepY = (deltaY / distance) * SPEED;

            // Adjust movement to avoid obstacles
            double[] adjustedStep = adjustForObstacles(stepX, stepY);
            x += adjustedStep[0];
            y += adjustedStep[1];
        }
    }

    /**
     * Adjusts the NPC's movement to avoid obstacles.
     *
     * @param stepX The desired step in the X direction.
     * @param stepY The desired step in the Y direction.
     * @return An array containing the adjusted steps [adjustedStepX, adjustedStepY].
     */
    private double[] adjustForObstacles(double stepX, double stepY) {
        double angle = Math.atan2(stepY, stepX);
        int numAttempts = 16; // Number of angles to try around the original direction
        double angleIncrement = Math.toRadians(360.0 / numAttempts);

        for (int i = 0; i < numAttempts; i++) {
            double newAngle = angle + i * angleIncrement;
            double adjustedStepX = Math.cos(newAngle) * SPEED;
            double adjustedStepY = Math.sin(newAngle) * SPEED;
            double nextX = x + adjustedStepX;
            double nextY = y + adjustedStepY;
            var nextPosition = new Rectangle(nextX, nextY, shape.getFitWidth(), shape.getFitHeight());

            if (!isCollision(nextPosition)) {
                return new double[]{adjustedStepX, adjustedStepY};
            }
        }

        // If all adjustments fail, stay in place for now
        return new double[]{0, 0};
    }

    /**
     * Checks if the NPC collides with any obstacles at the given position.
     *
     * @param position The rectangle representing the NPC's next position.
     * @return True if there is a collision; false otherwise.
     */
    private boolean isCollision(Rectangle position) {
        // Check for collisions with buildings
        for (Building building : buildings) {
            if (position.getBoundsInParent().intersects(building.getShape().getBoundsInParent())) {
                return true;
            }
        }

        // Check for collisions with other NPCs
        for (NPC otherNpc : npcs) {
            if (otherNpc != this) {
                if (position.getBoundsInParent().intersects(otherNpc.getShape().getBoundsInParent())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Updates the NPC's position on the JavaFX Application Thread.
     */
    public void updatePosition() {
        Platform.runLater(() -> {
            shape.setX(x);
            shape.setY(y);
        });
    }

    /**
     * Gets the ImageView representing the NPC.
     *
     * @return The ImageView of the NPC.
     */
    public ImageView getShape() {
        return shape;
    }

    /**
     * Gets the x-coordinate of the NPC.
     *
     * @return The x-coordinate.
     */
    public double getX() {
        return x;
    }

    /**
     * Gets the y-coordinate of the NPC.
     *
     * @return The y-coordinate.
     */
    public double getY() {
        return y;
    }
}
