package com.client;

import javafx.beans.property.SimpleStringProperty;

public class ItemTabla{
    private SimpleStringProperty nombre;

    public ItemTabla(String nombre){
        this.nombre = new SimpleStringProperty(nombre);
    }

    public String getNombre(){
        return nombre.get();
    }

    public SimpleStringProperty nombreProperty(){
        return nombre;
    }

    public void setNombre(String nombre){
        this.nombre = new SimpleStringProperty(nombre);
    }
}