package com.shared;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class GlowPieces {
    List<List<Integer>> pieces;
    double glow;
    String role;
    public GlowPieces(){
        pieces= new ArrayList<>();
        glow=0.0;
        role="";
    }

    public void setFromJsonObject(JSONObject jsonGlowObject){
        JSONArray piecesArray = jsonGlowObject.getJSONArray("pieces");
        List<List<Integer>> pieces = new ArrayList<>();
        for (int i = 0; i < piecesArray.length(); i++) {
            JSONArray array = piecesArray.getJSONArray(i);
            List<Integer> piece = new ArrayList<>();
            for (int j = 0; j < array.length(); j++) {
                piece.add(array.getInt(j));
            }
            pieces.add(piece);

        this.pieces=pieces;
        this.glow=jsonGlowObject.getDouble("glow");
        this.role=jsonGlowObject.getString("role");
    }


    }

    public JSONObject toJsonObject(){
        JSONArray piecesArray = new JSONArray(this.pieces);
        JSONObject json = new JSONObject()
        .put("pieces", piecesArray)
        .put("glow", this.glow)
        .put("role", this.role);
        return json;
    }

    public GlowPieces(List<List<Integer>> pieces, String role){
        this.pieces=pieces;
        this.role=role;
        glow=0.0;

    }
    public List<List<Integer>> getPieces() {
        return pieces;
    }
    public void setPieces(List<List<Integer>> pieces) {
        this.pieces = pieces;
    }
    public double getGlow() {
        return glow;
    }
    public void setGlow(double glow) {
        this.glow = glow;
    }
    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }
    public void addPiece(List<Integer> piece){
        pieces.add(piece);
    }
    public void clearPieces(){
        pieces.clear();
    }
    

    
}
