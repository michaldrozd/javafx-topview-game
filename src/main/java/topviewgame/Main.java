package topviewgame;


import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        GamePanel gamePane = new GamePanel();
        Scene scene = new Scene(gamePane, 800, 600);

        primaryStage.setTitle("Top View Game");
        primaryStage.setScene(scene);
        primaryStage.show();

        gamePane.requestFocus();
    }
}