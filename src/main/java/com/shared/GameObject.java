package com.shared;

import org.json.JSONObject;

public class GameObject {
    public String id;
    public int x;
    public int y;
    public String role;

    public GameObject(String id, int x, int y,  String role) {
        this.id = id;
        this.x = x;
        this.y = y;

        this.role = role;
    }

    @Override
    public String toString() {
        return this.toJSON().toString();
    }
    
    // Converteix l'objecte a JSON
    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("x", x);
        obj.put("y", y);
        obj.put("role", role);

        return obj;
    }

    // Crea un GameObjects a partir de JSON
    public static GameObject fromJSON(JSONObject obj) {
        GameObject go = new GameObject(
            obj.optString("id", null),
            obj.optInt("x", 0),
            obj.optInt("y", 0),
            obj.optString("role",  null)
        );
        return go;
    }
}
