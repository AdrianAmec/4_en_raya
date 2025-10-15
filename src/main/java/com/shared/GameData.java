package com.shared;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class GameData {

    private String status;
    private List<List<String>> board;
    private String turn;
    private JSONObject lastMove;
    private String winner;

    public GameData() {
        this.status = "";
        this.turn = "";
        this.lastMove = new JSONObject()
                        .put("lastMove",new JSONObject()
                            .put("row",-1)
                            .put("col",-1)
                        );
        
        this.board = new ArrayList<List<String>>() {{
            add(new ArrayList<>(Arrays.asList(" "," "," "," "," "," "," ")));
            add(new ArrayList<>(Arrays.asList(" "," "," "," "," "," "," ")));
            add(new ArrayList<>(Arrays.asList(" "," "," "," "," "," "," ")));
            add(new ArrayList<>(Arrays.asList(" "," "," "," "," "," "," ")));
            add(new ArrayList<>(Arrays.asList(" "," "," "," "," "," "," ")));
            add(new ArrayList<>(Arrays.asList(" "," "," "," "," "," "," ")));
            }};
        this.winner = "";
    }

    public GameData(JSONObject jsonObject) {
        fromJSON(jsonObject);
    }

    @Override
    public String toString() {
        return this.toJSON().toString();
    }
    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        obj.put("status", status);
        obj.put("board", board);
        obj.put("turn", turn);
        obj.put("lastMove", lastMove);
        obj.put("winner", winner);
        return obj;
    }

    public void setPiece(int row, int col, String piece) {
        board.get(row).set(col, piece);
        
    }

    public String getStatus() {
        return status;
    }

    public String getTurn() {
        return turn;
    }

    public JSONObject getLastMove() {
        return lastMove;
    }

    public String getWinner() {
        return winner;
    }

    public List<List<String>> getBoard() {
        return board;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setTurn(String turn) {
        this.turn = turn;
    }

    public void setLastMove(JSONObject lastMove) {
        this.lastMove = lastMove;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }

    public void fromJSON(JSONObject jsonObject) {
        
        this.status = jsonObject.optString("status", "");
        this.turn = jsonObject.optString("turn", "");
        this.lastMove = jsonObject.optJSONObject("lastMove");
        this.winner = jsonObject.optString("winner", "");
        this.board = new ArrayList<>();
        JSONArray boardArray = jsonObject.optJSONArray("board");
        if (boardArray != null) {
            for (int i = 0; i < boardArray.length(); i++) {
                JSONArray rowArray = boardArray.optJSONArray(i);
                List<String> row = new ArrayList<>();
                if (rowArray != null) {
                    for (int j = 0; j < rowArray.length(); j++) {
                        row.add(rowArray.optString(j, " "));
                    }
                }
                this.board.add(row);
            }
        }
    }
}
