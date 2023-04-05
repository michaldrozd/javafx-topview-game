package topviewgame.item;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Building {
    private final double x, y;
    private final double width, height;
    private final Rectangle shape;

    /**
     * Constructs a new Building at the specified coordinates with default size.
     *
     * @param x The x-coordinate of the building.
     * @param y The y-coordinate of the building.
     */
    public Building(double x, double y) {
        this(x, y, 100, 100); // Default width and height
    }

    /**
     * Constructs a new Building at the specified coordinates with specified size.
     *
     * @param x The x-coordinate of the building.
     * @param y The y-coordinate of the building.
     * @param width The width of the building.
     * @param height The height of the building.
     */
    public Building(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        // Use a Rectangle for borders or custom-sized buildings
        shape = new Rectangle(x, y, width, height);

        // Optionally, set the fill color or style
        shape.setFill(Color.DARKGRAY);
    }

    /**
     * Gets the x-coordinate of the building.
     *
     * @return The x-coordinate.
     */
    public double getX() {
        return x;
    }

    /**
     * Gets the y-coordinate of the building.
     *
     * @return The y-coordinate.
     */
    public double getY() {
        return y;
    }

    /**
     * Gets the ImageView representing the building.
     *
     * @return The ImageView of the building.
     */
    public Rectangle getShape() {
        return shape;
    }
}
