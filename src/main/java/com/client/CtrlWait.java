package com.client;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class CtrlWait implements Initializable {

    @FXML
    public Button buttonExit;

    @FXML
    public Label txtTitle;

    @FXML
    public Label txtPlayer0;

    @FXML
    public Label txtPlayer1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

    }
    
    @FXML
    public void backLobby(){
        UtilsViews.setViewAnimating("ViewMatch");
    }
}