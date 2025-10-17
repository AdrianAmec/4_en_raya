package com.server;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.exceptions.WebsocketNotConnectedException;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.shared.ClientData;
import com.shared.GameData;
import com.shared.GameObject;

/**
 * Servidor WebSocket que manté l'estat complet dels clients i objectes seleccionables.
 *
 * Protocol simplificat:
 *  - Client -> Server:  { "type": "clientData", "data": { ...ClientData... } }
 *  - Server -> Clients: { "type": "state", "clientId": <clientId>, "clients": [ ...ClientData... ], "gameObjects": { ... }, "countdown": n? }
 */
public class Main extends WebSocketServer {

    /** Port per defecte on escolta el servidor. */
    public static final int DEFAULT_PORT = 3000;

    /** Llista de noms disponibles per als clients connectats. */
    private static final List<String> PLAYER_NAMES = Arrays.asList(
        "Bulbasaur", "Charizard", "Blaziken", "Umbreon", "Mewtwo", "Pikachu", "Wartortle"
    );

    /** Llista de colors disponibles per als clients connectats. */
    private static final List<String> PLAYER_COLORS = Arrays.asList(
        "GREEN", "ORANGE", "RED", "GRAY", "PURPLE", "YELLOW", "BLUE"
    );

    /** Nombre de clients necessaris per iniciar el compte enrere. */
    private static final int REQUIRED_CLIENTS = 2;

    // Claus JSON
    private static final String K_TYPE = "type";
    private static final String K_VALUE = "value";
    private static final String K_CLIENT_NAME = "clientName";
    private static final String K_CLIENTS_LIST = "clientsList";             
    private static final String K_OBJECTS_LIST = "objectsList"; 
    private static final String K_GAME_DATA = "gameData";

    // Tipus de missatge nous i (alguns) heretats
    private static final String T_CLIENT_MOUSE_MOVING = "clientMouseMoving";  // client -> server
    private static final String T_CLIENT_OBJECT_MOVING = "clientObjectMoving";// client -> server
    private static final String T_CLIENT_ADD_PIECE = "clientAddPiece";        // client -> server
      private static final String T_CLIENT_ADD_PIECE_FINAL = "clientAddPieceFinal";
    private static final String T_CLIENT_SENT_INVITATION = "clientSendInvitation";
    private static final String T_CLIENT_ACCEPT_INVITATION= "clientAcceptInvitation";
    private static final String T_SERVER_DATA = "serverData";                 // server -> clients
    private static final String T_COUNTDOWN = "countdown";                    // server -> clients
    private static final String T_SERVER_START_GAME = "startGame";
  

    /** Registre de clients i assignació de noms (pool integrat). */
    private final ClientRegistry clients;

    /** Mapa d’estat per client (source of truth del servidor). Clau = name/id. */
    private final Map<String, ClientData> clientsData = new HashMap<>();



    /** Mapa d'objectes seleccionables compartits. */
    private final Map<String, GameObject> gameObjects = new HashMap<>();

    private volatile boolean countdownRunning = false;


    /** Estat del joc. */
    private final GameData gameData = new GameData();


    /** Freqüència d’enviament de l’estat (frames per segon). */
    private static final int SEND_FPS = 30;
    private final ScheduledExecutorService ticker;

    /**
     * Crea un servidor WebSocket que escolta a l'adreça indicada.
     *
     * @param address adreça i port d'escolta del servidor
     */
    public Main(InetSocketAddress address) {
        super(address);
        this.clients = new ClientRegistry(PLAYER_NAMES);
        initializegameObjects();

        ThreadFactory tf = r -> {
            Thread t = new Thread(r, "ServerTicker");
            t.setDaemon(true);
            return t;
        };
        this.ticker = Executors.newSingleThreadScheduledExecutor(tf);
    }

    /**
     * Inicialitza els objectes seleccionables predefinits.
     */
    private void initializegameObjects() {
        String objId = "R_00";
        GameObject obj0 = new GameObject(objId, 300, 50, "R");
        gameObjects.put(objId, obj0);

        objId = "Y_00";
        GameObject obj1 = new GameObject(objId, 300, 100,  "Y");
        gameObjects.put(objId, obj1);
    }

    /**
     * Obté el color per un nom de client.
     *
     * @return color assignat
     */
    private synchronized String getColorForName(String name) {
        int idx = PLAYER_NAMES.indexOf(name);
        if (idx < 0) idx = 0; // fallback si el nom no està a la llista
        return PLAYER_COLORS.get(idx % PLAYER_COLORS.size());
    }

    /** Envia un compte enrere (5..0) com a part del mateix STATE.
     *  Evita comptes simultanis i es cancel·la si baixa el nombre de clients. */
    private void sendCountdown() {
        synchronized (this) {
            if (countdownRunning) return;
            countdownRunning = true;
        }

        new Thread(() -> {
            try {
                for (int i = 3; i >= 0; i--) {
                    // Si durant el compte enrere ja no hi ha els clients requerits, cancel·la
                    if (clientsData.size() < REQUIRED_CLIENTS) {
                        break;
                    }

                    sendCountdownToAll(i);
                    if (i > 0) Thread.sleep(750); // ritme del compte enrere
                }
                
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } finally {
                countdownRunning = false;
            }
        }, "CountdownThread").start();
    }

    // ----------------- Helpers JSON -----------------

    /** Crea un objecte JSON amb el camp type inicialitzat. */
    private static JSONObject msg(String type) {
        return new JSONObject().put(K_TYPE, type);
    }

    /** Envia de forma segura un payload i, si el socket no està connectat, el neteja del registre. */
    private void sendSafe(WebSocket to, String payload) {
        if (to == null) return;
        try {
            to.send(payload);
        } catch (WebsocketNotConnectedException e) {
            String name = clients.cleanupDisconnected(to);
            clientsData.remove(name);
            System.out.println("Client desconnectat durant send: " + name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Envia un missatge a tots els clients excepte l'emissor. */
    private void broadcastExcept(WebSocket sender, String payload) {
        for (Map.Entry<WebSocket, String> e : clients.snapshot().entrySet()) {
            WebSocket conn = e.getKey();
            if (!Objects.equals(conn, sender)) sendSafe(conn, payload);
        }
    }

    //envia los datos de los jugadores disponibles y muestra tu nombre
    private void sendClientsMatch(){
        JSONObject rst = msg("serverClientsMatch")
        .put(K_VALUE, clients.currentNames());
        broadcastExcept(null, rst.toString());
    }

    private void broadcastAnimation (JSONObject msg){

        JSONObject rst = msg("serverAnimation")
            .put(K_VALUE, msg);

        for (Map.Entry<String, ClientData> e : clientsData.entrySet()) {
            String name = e.getKey();
            WebSocket conn = clients.socketByName(name);
            rst.put(K_CLIENT_NAME, name);
            sendSafe(conn, rst.toString());
        }
    }

    private void broadcastStatus() {

        JSONArray arrClients = new JSONArray();
        for (ClientData c : clientsData.values()) {
            arrClients.put(c.toJSON());
        }

        JSONArray arrObjects = new JSONArray();
        for (GameObject obj : gameObjects.values()) {
            arrObjects.put(obj.toJSON());
        }

        JSONObject rst = msg(T_SERVER_DATA)
                        .put(K_CLIENTS_LIST, arrClients)
                        .put(K_OBJECTS_LIST, arrObjects)
                        .put(K_GAME_DATA, gameData.toJSON());

        for (Map.Entry<String, ClientData> e : clientsData.entrySet()) {
            
            String name = e.getKey();
            WebSocket conn = clients.socketByName(name);
            rst.put(K_CLIENT_NAME, name);
            sendSafe(conn, rst.toString());
        }
    }

    /** Envia a tots els clients el compte enrere. */
    private void sendCountdownToAll(int n) {
        JSONObject rst = msg(T_COUNTDOWN).put(K_VALUE, n);

        sendDataToCurrentPlayers(rst.toString());
    }


    private void sendDataToCurrentPlayers(String msg){
        for (Map.Entry<String, ClientData> entry : clientsData.entrySet()) {
            String name = entry.getKey();
            WebSocket ws = clients.socketByName(name);
            sendSafe(ws, msg);
        }
    }
    // ----------------- WebSocketServer overrides -----------------

    /** Assigna un nom i color al client i envia l’STATE complet. */
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String name = clients.add(conn);
        String color = getColorForName(name);

        //ClientData client= new ClientData(name, color);
        // if(gameData.getTurn().equals("")){
        //     gameData.setTurn(name);
        //     client.setRole("Y");
        //     System.out.println(gameData.getTurn()+"Es el primero ");
        // }else{
        //     client.setRole("R");
        // }

        // actualiza los jugadores a todos

        //lo envia al view Matchmaking
        JSONObject msg = msg("serverMatchAdd")
        .put(K_CLIENT_NAME, name);
        sendSafe(conn, msg.toString());

        
        System.out.println("WebSocket client connected: " + name + " (" + color + ")");
        
        sendClientsMatch();
        //sendCountdown();
    }

    /** Elimina el client del registre i envia l’STATE complet. */

    public void endGameDC(String nameDc){
        String w="";
        for (Map.Entry<String,ClientData> client : clientsData.entrySet()) {
            w = client.getKey();
            if(!w.equals(nameDc)){break;}
        }
        gameData.setWinner(w);
        gameData.setStatus("win");

        //broadcastStatus();
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String name = clients.remove(conn);
        if(clientsData.containsKey(name)){
            //si estaba jugando, gana automaticamente el que sigue jugando
            if(gameData.getStatus().equals("playing")){
                endGameDC(name);
            }
            
            clientsData.remove(name);
        }
        //actualiza la lista de clientes
        sendClientsMatch();
    }

    /** Processa els missatges rebuts. */
    @Override
    public void onMessage(WebSocket conn, String message) {
        JSONObject obj;
        try {
            obj = new JSONObject(message);
        } catch (Exception ex) {
            return; // JSON invàlid
        }

        String type = obj.optString(K_TYPE, "");
        switch (type) {
            case T_CLIENT_MOUSE_MOVING -> {
                String clientName = clients.nameBySocket(conn);
                clientsData.put(clientName, ClientData.fromJSON(obj.getJSONObject(K_VALUE))); 
            }

            case T_CLIENT_OBJECT_MOVING -> {
                GameObject objData = GameObject.fromJSON(obj.getJSONObject(K_VALUE));
                gameObjects.put(objData.id, objData);
            }
            
            case T_CLIENT_ADD_PIECE -> {
                System.out.println(obj.get(K_VALUE));
                JSONObject msg = obj.getJSONObject(K_VALUE);
                broadcastAnimation(msg);
            }

            case T_CLIENT_ADD_PIECE_FINAL -> {
                gameData.fromJSON(obj.getJSONObject(K_VALUE));
                int row = gameData.getLastMove().getInt("row");
                int col = gameData.getLastMove().getInt("col");
                if(checkWin(row, col,gameData.getBoard())){
                    gameData.setWinner(gameData.getTurn());
                    gameData.setStatus("win");
                }
                if(checkDraw()){
                    gameData.setStatus("draw");
                }
                nextTurn();
                System.out.println("GameData updated from client: " + gameData.toString());
            }

            case T_CLIENT_SENT_INVITATION ->{
                String from =obj.getString("from");
                String to = obj.getString("to");
                JSONObject msg = msg("serverNewInvitation")
                .put("from", from)
                .put("to", to);

                WebSocket toWSocket = clients.socketByName(to);
                System.out.println("enviando invitacion a "+to);
                sendSafe(toWSocket, msg.toString());

            }
            case T_CLIENT_ACCEPT_INVITATION->{
                String from = obj.getString("from");
                String to = obj.getString("to");
                List<String> data = Arrays.asList(from, to);
                System.out.println(data.toString());
                clientsData.clear();
                if(!gameData.getStatus().equals("playing")){
                    for (int i = 0; i < data.size(); i++) {
                        String color = getColorForName(data.get(i));
                        ClientData player = new ClientData(data.get(i), color, (i == 0 ? "Y" : "R"));
                        clientsData.put(data.get(i), player);
                    }
                    gameData.newGame();
                    gameData.setStatus("playing");
                    Random random = new Random();
                    gameData.setTurn(data.get(random.nextInt(data.size())));
                    
                    JSONObject msg = msg(T_SERVER_START_GAME);
                    for (String name : data) {
                        WebSocket ws = clients.socketByName(name);
                        sendSafe(ws, msg.toString());
                        
                    }
                    //broadcastStatus();
                    sendCountdown();
                }

            }


            // case "clientPlay"->{
            //     int row = obj.getInt("row");
            //     int col = obj.getInt("col");
            //     String piece = obj.getString("piece");
            //     gameData.setLastMove(new JSONObject()
            //     .put("row", row)
            //     .put("col", col));

            //     gameData.setPiece(row, col, piece);

            //     if(checkWin(row, col,gameData.getBoard())){
            //         gameData.setWinner(gameData.getTurn());
            //         gameData.setStatus("win");
            //     }
            //     if(checkDraw()){
            //         gameData.setStatus("draw");
            //     }
            //     nextTurn();
            //     System.out.println("GameData updated from client: " + gameData.toString());
                
            // }
            default -> {
                // Ignora altres tipus
            }
        }
    }

    /** Log d'error global o de socket concret. */
    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    /** Arrencada: log i configuració del timeout de connexió perduda. */
    @Override
    public void onStart() {
        System.out.println("WebSocket server started on port: " + getPort());
        setConnectionLostTimeout(100);
        startTicker();
    }

    // ----------------- Lifecycle util -----------------

    /** Registra un shutdown hook per aturar netament el servidor en finalitzar el procés. */
    private static void registerShutdownHook(Main server) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Aturant servidor (shutdown hook)...");
            try {
                server.stopTicker();      // <- atura el bucle periòdic
                server.stop(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
            System.out.println("Servidor aturat.");
        }));
    }

    /** Bloqueja el fil principal indefinidament fins que sigui interromput. */
    private static void awaitForever() {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            latch.await();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }


    // ----------------- Ticker util -----------------

    private void startTicker() {
        long periodMs = Math.max(1, 1000 / SEND_FPS);
        ticker.scheduleAtFixedRate(() -> {
            try {
                // Opcional: si no hi ha clients, evita enviar
                if (!clientsData.isEmpty()) {
                    //System.out.println("testticket");
                    //sendClientsMatch();
                    broadcastStatus();

                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, periodMs, TimeUnit.MILLISECONDS);
    }

    private void stopTicker() {
        try {
            ticker.shutdownNow();
            ticker.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /** Punt d'entrada. */
    public static void main(String[] args) {
        Main server = new Main(new InetSocketAddress(DEFAULT_PORT));
        server.start();
        registerShutdownHook(server);

        System.out.println("Server running on port " + DEFAULT_PORT + ". Press Ctrl+C to stop it.");
        awaitForever();
    }

    public boolean checkDraw(){
        List<List<String>> board = gameData.getBoard();

        for (List<String> row : board) {
            for (String cell : row) {
                if (cell.equals(" ")) {
                    return false;
                }
            }
        }
        return true;
    }

        /**
     * Verifica si hay 4 en raya alrededor de la celda (r, c) para el jugador actual.
     * @param r La fila (row) de la última pieza jugada.
     * @param c La columna (column) de la última pieza jugada.
     * @return true si se encuentra un 4 en raya, false en caso contrario.
     */
    public boolean checkWin(int r, int c, List<List<String>> board) {
        
        String player = board.get(r).get(c); // Pieza a buscar
        
        if (player.trim().isEmpty()) {
            return false; // Nunca debe pasar si llamas esto después de un movimiento
        }
        
        // Definición de las 8 direcciones posibles (dx, dy)
        // Direcciones: Horizontal (izq/der), Vertical (arriba/abajo), Diagonal (\ y /)
        int[][] directions = {
            {0, 1},   // Horizontal
            {1, 0},   // Vertical
            {1, 1},   // Diagonal principal (\)
            {1, -1}   // Diagonal secundaria (/)
        };
        
        // Solo necesitamos verificar 4 direcciones, ya que la dirección opuesta se cubre
        // al multiplicar por -1 (ej: derecha es lo opuesto a izquierda)

        for (int[] dir : directions) {
            int dr = dir[0];
            int dc = dir[1];
            
            // Contamos la pieza central como 1
            int count = 1;

            // 1. Contar en la dirección POSITIVA (ej: hacia la derecha, hacia abajo)
            count += countDirection(r, c, dr, dc, player, board);

            // 2. Contar en la dirección OPUESTA/NEGATIVA (ej: hacia la izquierda, hacia arriba)
            // Usamos -dr y -dc para la dirección opuesta
            count += countDirection(r, c, -dr, -dc, player, board);

            // Si la cuenta total (incluyendo la pieza central) es 4 o más, ganamos.
            if (count >= 4) {
                return true;
            }
        }

        return false;
    }

    /**
     * Función auxiliar para contar piezas consecutivas en una sola dirección.
     */
    private int countDirection(int startR, int startC, int dr, int dc, String player, List<List<String>> board) {
        int rows = board.size();
        int cols = board.get(0).size();
        int count = 0;
        
        // Moverse un paso en la dirección dada y contar
        for (int i = 1; i <= 3; i++) { // Solo necesitamos contar hasta 3 veces más
            int currentRow = startR + i * dr;
            int currentCol = startC + i * dc;

            // Verificar límites del tablero
            if (currentRow < 0 || currentRow >= rows || currentCol < 0 || currentCol >= cols) {
                break; 
            }

            // Verificar si la celda coincide con la pieza del jugador
            // Usamos .trim().equals() para ignorar los espacios si es necesario
            if (board.get(currentRow).get(currentCol).trim().equals(player.trim())) {
                count++;
            } else {
                break; // Se rompe la secuencia
            }
        }
        return count;
    }

    public void nextTurn(){
        java.util.Iterator<String> cl = clientsData.keySet().iterator();
        String c1 = cl.next();
        String c2 = cl.next();
        if(c1.equals(gameData.getTurn())){
            gameData.setTurn(c2);
            return;
        }
        gameData.setTurn(c1);
    }

    
}
