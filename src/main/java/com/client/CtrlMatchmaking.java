package com.client;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.shared.ClientData;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

public class CtrlMatchmaking implements Initializable{

    public String client;

    
    @FXML private TableView<ItemTabla> tabla;
    @FXML private TableColumn<ItemTabla,String> nombreCol;
    @FXML private TableColumn<ItemTabla,Void> accionCol;

    private ObservableList<ItemTabla> datosTabla;

    public CtrlMatchmaking(String client){
        this.client=client;
    }

    public CtrlMatchmaking(){

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        datosTabla = FXCollections.observableArrayList();
        for (ClientData player : Main.clients) {
            ItemTabla it = new ItemTabla(player.getNombre());
            datosTabla.add(it);
        }

        nombreCol.setCellValueFactory(new PropertyValueFactory<>("nombre"));
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
        accionCol.setCellFactory(cellFactory);
    }

    public void addItem(ClientData player){
        datosTabla.add(new ItemTabla(player.getNombre()));
    }

    public void removeItem(ClientData player){
        for (int i = 0; i < datosTabla.size(); i++) {
            if(datosTabla.get(i).getNombre().equals(player.getNombre())){
                datosTabla.remove(i);
                return;
            }
            
        }

    }
    
}

class ItemTabla{
    private SimpleStringProperty nombre;

    public ItemTabla(String nombre){
        this.nombre = new SimpleStringProperty(nombre);
    }

    public String getNombre(){
        return nombre.get();
    }

    public SimpleStringProperty nombProperty(){
        return nombre;
    }

    public void setNombre(String nombre){
        this.nombre = new SimpleStringProperty(nombre);
    }
}