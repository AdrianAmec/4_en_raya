package com.client;

import org.json.JSONObject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;


public class ControllerListItem{

    @FXML
    private Label title;

    private String from;
    private String to;
    

    public String getTitle(){
        return title.getText().toString();
    }



    // @FXML
    // public void glowOption(MouseEvent event){
    //     select.setEffect(new DropShadow(10, Color.GRAY));
    // }

    // @FXML
    // public void noGlowOption(MouseEvent event){
    //     select.setEffect(null);
    // }


    public void setDatos(String from, String to){
        this.title.setText(from +" te invito a jugar ");
        this.from=from;
        this.to=to;
    }

    @FXML
    public void actionAceptar(ActionEvent event){
        JSONObject msg = new JSONObject()
        .put("type", "clientAcceptInvitation")
        .put("from", from)
        .put("to",to);

        Main.wsClient.safeSend(msg.toString());
    }

    @FXML
    public void actionRechazar(ActionEvent event){
        System.out.println("Rechazo");
    }


    

}