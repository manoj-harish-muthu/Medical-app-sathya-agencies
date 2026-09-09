package com.pharmacyerp;

import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends Application {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Starting Pharmacy ERP application...");
        com.pharmacyerp.database.DatabaseManager.initialize();
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Initializing JavaFX stage...");
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            javafx.scene.Parent root = loader.load();

            primaryStage.setTitle("Sathya Agencies - Premium Pharmacy Management");
            primaryStage.setScene(new javafx.scene.Scene(root, 1366, 768));
            primaryStage.setMaximized(true);
            primaryStage.show();
            logger.info("Application started successfully.");
        } catch (Exception e) {
            logger.error("Failed to start application", e);
        }
    }
}
