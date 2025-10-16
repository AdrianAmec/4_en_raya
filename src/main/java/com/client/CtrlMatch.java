package com.client;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.json.JSONArray;
import org.json.JSONObject;

import com.shared.ClientData;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

public class CtrlMatch implements Initializable{

    //public String client;

    @FXML private VBox inviPanel;
    @FXML private TableView<ItemTabla> table;
    @FXML private TableColumn<ItemTabla,String> jugadorCol;
    @FXML private TableColumn<ItemTabla,Void> acctionCol;
    @FXML private Label txtTitle;

    private ObservableList<ItemTabla> datosTabla;


    // public void setClient(String client){
    //     this.client=client;
    // }

    // public CtrlMatch(String client){
    //     this.client=client;
    // }

    // public CtrlMatch(){
    //     client="";
    // }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        datosTabla = FXCollections.observableArrayList();
        table.setItems(datosTabla);
        jugadorCol.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        confAcctionCol();
    }

    private void confAcctionCol(){

        Callback<TableColumn<ItemTabla,Void>,TableCell<ItemTabla,Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<ItemTabla, Void> call(final TableColumn<ItemTabla, Void> param) {
                TableCell<ItemTabla, Void> cell = new TableCell<>() {
                    private final Button btn = new Button("Invitar");
                    {
                        btn.setOnAction(event -> {
                            ItemTabla data = getTableView().getItems().get(getIndex());

                            JSONObject msg = new JSONObject()
                            .put("type", "clientSendInvitation")
                            .put("from",Main.clientName)
                            .put("to",data.getNombre());
                            Main.wsClient.safeSend(msg.toString());
                            

                            System.out.println("invitacion a " + data.getNombre());
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            // Asigna el botón como el contenido gráfico de la celda
                            setGraphic(btn);
                        }
                    }
                };
                return cell;
            }
        };
        acctionCol.setCellFactory(cellFactory);
    }

    public void addItem(ClientData player){
       //System.out.println(player.getNombre()+" vs "+Main.clientName+" comprobando ClientData");
        if(player.getNombre().equals(Main.clientName)){return;}
        datosTabla.add(new ItemTabla(player.getNombre()));
    }

    public void addItem(String player){
        //System.out.println(player+" vs "+Main.clientName+" comprobando String");

        if(Main.clientName.equals("")||player.equals(Main.clientName)){return;}
            datosTabla.add(new ItemTabla(player));
    }

    public void setItemsFromJSONArray(JSONArray array){
        datosTabla.clear();
        for (int i = 0; i < array.length(); i++) {
            addItem(array.getString(i));
        }

    }

    public void removeItem(ClientData player){
        for (int i = 0; i < datosTabla.size(); i++) {
            if(datosTabla.get(i).getNombre().equals(player.getNombre())){
                datosTabla.remove(i);
                return;
            }
            
        }

    }
    
    public void addNewInvitacion(String from,String to){
        try {
            
            URL resource = this.getClass().getResource("/assets/listItem.fxml");
            //System.out.println("\n\n\n\n"+resource+"\n\n\n\n");
            FXMLLoader loader = new FXMLLoader(resource);
           // System.out.println("test1");
            Parent itemTemplate = loader.load();
            //System.out.println("test2");
            ControllerListItem itemController = loader.getController();
            //System.out.println("test3");
            itemController.setDatos(from, to);
            
            inviPanel.getChildren().add(itemTemplate);
            //System.out.println("invitacion subida! en teoria");

            
        } catch (Exception e) {
            // System.out.println("ERROR AL SUBIR !!!");
            // System.out.println(e.getMessage());
        }
        
    }
}

