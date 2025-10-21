package com.client;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.shared.ClientData;
import com.shared.GameData;
import com.shared.GameObject;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.scene.paint.Color;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Main extends Application {

    public static UtilsWS wsClient;

    public static String clientName = "";
    public static List<ClientData> clients;
    public static List<GameObject> objects;
    public static GameData gameData= new GameData();

    public static CtrlConfig ctrlConfig;
    public static CtrlWait ctrlWait;
    public static CtrlPlay ctrlPlay;
    public static CtrlMatch ctrlMatch;

    public static void main(String[] args) {

        // Iniciar app JavaFX   
        launch(args);
    }
    
    @Override
    public void start(Stage stage) throws Exception {

        final int windowWidth = 400;
        final int windowHeight = 300;

        UtilsViews.parentContainer.setStyle("-fx-font: 14 arial;");
        UtilsViews.addView(getClass(), "ViewConfig", "/assets/viewConfig.fxml"); 
        UtilsViews.addView(getClass(), "ViewWait", "/assets/viewWait.fxml");
        UtilsViews.addView(getClass(), "ViewPlay", "/assets/viewPlay.fxml");
        System.out.println("test11");

        UtilsViews.addView(getClass(), "ViewMatch", "/assets/viewMatch.fxml");
        System.out.println("test22");

        ctrlConfig = (CtrlConfig) UtilsViews.getController("ViewConfig");
        ctrlWait = (CtrlWait) UtilsViews.getController("ViewWait");
        ctrlPlay = (CtrlPlay) UtilsViews.getController("ViewPlay");
        System.out.println("test1");
        ctrlMatch = (CtrlMatch) UtilsViews.getController("ViewMatch");
        System.out.println("test2");
        Scene scene = new Scene(UtilsViews.parentContainer);
        
        stage.setScene(scene);
        stage.onCloseRequestProperty(); // Call close method when closing window
        stage.setTitle("JavaFX");
        stage.setMinWidth(windowWidth);
        stage.setMinHeight(windowHeight);
        stage.show();

        // Add icon only if not Mac
        if (!System.getProperty("os.name").contains("Mac")) {
            Image icon = new Image("file:/icons/icon.png");
            stage.getIcons().add(icon);
        }
    }

    @Override
    public void stop() { 
        if (wsClient != null) {
            wsClient.forceExit();
        }
        System.exit(1); // Kill all executor services
    }

    public static void pauseDuring(long milliseconds, Runnable action) {
        PauseTransition pause = new PauseTransition(Duration.millis(milliseconds));
        pause.setOnFinished(event -> Platform.runLater(action));
        pause.play();
    }

    public static <T> List<T> jsonArrayToList(JSONArray array, Class<T> clazz) {
        List<T> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            T value = clazz.cast(array.get(i));
            list.add(value);
        }
        return list;
    }

    public static void connectToServer() {

        ctrlConfig.txtMessage.setTextFill(Color.BLACK);
        ctrlConfig.txtMessage.setText("Connecting ...");
    
        pauseDuring(1500, () -> { // Give time to show connecting message ...

            String protocol = ctrlConfig.txtProtocol.getText();
            String host = ctrlConfig.txtHost.getText();
            String port = ctrlConfig.txtPort.getText();
            wsClient = UtilsWS.getSharedInstance(protocol + "://" + host + ":" + port);
    
            wsClient.onMessage((response) -> { Platform.runLater(() -> { wsMessage(response); }); });
            wsClient.onError((response) -> { Platform.runLater(() -> { wsError(response); }); });
        });
    }
   
    private static void wsMessage(String response) {
        
        // System.out.println(response);
        
        JSONObject msgObj = new JSONObject(response);
        switch (msgObj.getString("type")) {
            case "serverData":
                clientName = msgObj.getString("clientName");

                JSONArray arrClients = msgObj.getJSONArray("clientsList");
                List<ClientData> newClients = new ArrayList<>();
                for (int i = 0; i < arrClients.length(); i++) {
                    JSONObject obj = arrClients.getJSONObject(i);
                    newClients.add(ClientData.fromJSON(obj));
                }
                clients = newClients;

                JSONArray arrObjects = msgObj.getJSONArray("objectsList");
                List<GameObject> newObjects = new ArrayList<>();
                for (int i = 0; i < arrObjects.length(); i++) {
                    JSONObject obj = arrObjects.getJSONObject(i);
                    newObjects.add(GameObject.fromJSON(obj));
                }
                objects = newObjects;

                gameData.fromJSON(msgObj.getJSONObject("gameData"));
                

                if (clients.size() == 1) {

                    ctrlWait.txtPlayer0.setText(clients.get(0).name);

                } else if (clients.size() > 1) {

                    ctrlWait.txtPlayer0.setText(clients.get(0).name);
                    ctrlWait.txtPlayer1.setText(clients.get(1).name);
                    ctrlPlay.title.setText(clients.get(0).name + " vs " + clients.get(1).name);
                    
                }
                
                if(gameData.getStatus().equals("win")){
                    ctrlPlay.turno.setText("");
                    if(!Main.clientName.equals(gameData.getTurn())){
                        ctrlPlay.title.setText("Ganaste la partida!");

                    }else{
                    ctrlPlay.title.setText( gameData.getWinner()+" gano la partida!");
                    }
                    break;
                }
                if(gameData.getStatus().equals("draw")){
                    ctrlPlay.turno.setText("");
                    ctrlPlay.title.setText("draw!");
                    break;
                }

                if(clientName.equals(gameData.getTurn())){
                    ctrlPlay.turno.setText("Tu turno");
                }else{
                    ctrlPlay.turno.setText("Turno de "+gameData.getTurn());
                }

                
                break;
            
            case "countdown":
                int value = msgObj.getInt("value");
                String txt = String.valueOf(value);
                if (value == 0) {
                    UtilsViews.setViewAnimating("ViewPlay");
                    txt = "GO";
                    
                }
                ctrlWait.txtTitle.setText(txt);
                break;

            case "serverAnimation":
                
                JSONObject ani = msgObj.getJSONObject("value");
                int x = ani.getInt("col");
                int y = ani.getInt("row");
                String piece = ani.getString("piece");
                 

                ctrlPlay.newDummyObject(x,piece);
                ctrlPlay.startAnimation(x,y);
                

                break;

            case "serverClientsMatch":
                System.out.println("test3");
                ctrlMatch.setItemsFromJSONArray(msgObj.getJSONArray("value"));
                //ctrlMatch.setClient(msgObj.getString("clientName"));

                break;

            case "serverMatchAdd":
                Main.clientName=msgObj.getString("clientName");
                UtilsViews.setViewAnimating("ViewMatch");
                break;

            case "serverNewInvitation":
                String from = msgObj.getString("from");
                String to = msgObj.getString("to");
                System.out.println("invitacion recivida . soy "+to+"  ---  " + " de -- "+from);
                ctrlMatch.addNewInvitacion(from, to);
                break;

            case "startGame":
                UtilsViews.setView("ViewWait");
                break;
        }
    }



    private static void wsError(String response) {
        String connectionRefused = "Connection refused";
        if (response.indexOf(connectionRefused) != -1) {
            ctrlConfig.txtMessage.setTextFill(Color.RED);
            ctrlConfig.txtMessage.setText(connectionRefused);
            pauseDuring(1500, () -> {
                ctrlConfig.txtMessage.setText("");
            });
        }
    }
}
