package com.client;

import java.util.ArrayList;
import java.util.List;

public class GlowPieces {
    List<List<Integer>> pieces;
    double glow;
    String role;
    public GlowPieces(){
        pieces= new ArrayList<>();
        glow=0.0;
        role="";
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
