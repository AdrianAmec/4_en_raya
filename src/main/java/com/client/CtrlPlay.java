package com.client;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.SwingUtilities;

import org.json.JSONObject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.effect.Glow;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import com.shared.ClientData;
import com.shared.GameData;
import com.shared.GameObject;

public class CtrlPlay implements Initializable {

    @FXML
    public Button buttonExit;

    @FXML
    public javafx.scene.control.Label title,turno;

    @FXML
    private Canvas canvas;
    private GraphicsContext gc;
    private Boolean showFPS = false;

    private PlayTimer animationTimer;
    private PlayGrid grid;

    private Boolean mouseDragging = false;
    private double mouseOffsetX, mouseOffsetY;

    private GameObject selectedObject = null;
    private GameObject dummyAnimacion = null;

    @FXML
    public void backLobby(){
        UtilsViews.setViewAnimating("ViewMatch");
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // Get drawing context
        this.gc = canvas.getGraphicsContext2D();

        // Set listeners
        UtilsViews.parentContainer.heightProperty().addListener((observable, oldValue, newvalue) -> { onSizeChanged(); });
        UtilsViews.parentContainer.widthProperty().addListener((observable, oldValue, newvalue) -> { onSizeChanged(); });
        
        canvas.setOnMouseMoved(this::setOnMouseMoved);
        canvas.setOnMousePressed(this::onMousePressed);
        canvas.setOnMouseDragged(this::onMouseDragged);
        canvas.setOnMouseReleased(this::onMouseReleased);

        // Define grid
        grid = new PlayGrid(25, 100, 50, 6, 7);

        // Start run/draw timer bucle
        animationTimer = new PlayTimer(this::run, this::draw, 0);
        start();
    }

    // When window changes its size
    public void onSizeChanged() {

        double width = UtilsViews.parentContainer.getWidth();
        double height = UtilsViews.parentContainer.getHeight();
        canvas.setWidth(width);
        canvas.setHeight(height);
    }

    // Start animation timer
    public void start() {
        animationTimer.start();
    }

    // Stop animation timer
    public void stop() {
        animationTimer.stop();
    }

    private void setOnMouseMoved(MouseEvent event) {
        double mouseX = event.getX();
        double mouseY = event.getY();

        String color = Main.clients.stream()
            .filter(c -> c.name.equals(Main.clientName))
            .map(c -> c.color)
            .findFirst()
            .orElse("gray");

        String role = Main.clients.stream()
        .filter(c -> c.name.equals(Main.clientName))
        .map(c -> c.role)
        .findFirst()
        .orElse("");

        ClientData cd = new ClientData(
            Main.clientName, 
            color,
            (int)mouseX, 
            (int)mouseY,  
            grid.isPositionInsideGrid(mouseX, mouseY) ? grid.getRow(mouseY) : -1,
            grid.isPositionInsideGrid(mouseX, mouseY) ? grid.getCol(mouseX) : -1,
            role
            
        );

        JSONObject msg = new JSONObject();
        msg.put("type", "clientMouseMoving");
        msg.put("value", cd.toJSON());

        if (Main.wsClient != null) {
            Main.wsClient.safeSend(msg.toString());
        }
    }

    private void onMousePressed(MouseEvent event) {
        
        

        double mouseX = event.getX();
        double mouseY = event.getY();

        selectedObject = null;
        mouseDragging = false;

        for (GameObject go : Main.objects) {
            System.out.println("Comprovant objecte: "+go.id+" de color: "+go.role);
            if (isPositionInsideObject(mouseX, mouseY, go.x, go.y)) {
                selectedObject = new GameObject(go.id, go.x, go.y, go.role);
                mouseDragging = true;
                mouseOffsetX = event.getX() - go.x;
                mouseOffsetY = event.getY() - go.y;
                break;
            }
        }
    }

    private void onMouseDragged(MouseEvent event) {
        if (!isPlaying()){
            setOnMouseMoved(event);
            return;
        }
        if (mouseDragging) {
            double objX = event.getX() - mouseOffsetX;
            double objY = event.getY() - mouseOffsetY;

            selectedObject = new GameObject(selectedObject.id, (int)objX, (int)objY, selectedObject.role);

            JSONObject msg = new JSONObject();
            msg.put("type", "clientObjectMoving");
            msg.put("value", selectedObject.toJSON());

            if (Main.wsClient != null) {
                Main.wsClient.safeSend(msg.toString());
            }
        }
        setOnMouseMoved(event);
    }

    private void onMouseReleased(MouseEvent event) {
        if (!isPlaying()){return;}
        if (selectedObject != null) {
            System.out.println("nameClient = "+Main.clientName+"  Turno de = "+Main.gameData.getTurn() );
            

            String role = Main.clients.stream()
            .filter(c -> c.name.equals(Main.clientName))
            .map(c -> c.role)
            .findFirst()
            .orElse("");

            System.out.println("Server Turno: "+Main.gameData.getTurn()+"Soy "+Main.clientName+"\n ----Role ---"+selectedObject.role+" == es "+ role);
            if(!Main.gameData.getTurn().equals(Main.clientName)||!selectedObject.role.equals(role)){
                snapObjectLeftTop(selectedObject);


                JSONObject msg = new JSONObject();
                msg.put("type", "clientObjectMoving");
                msg.put("value", selectedObject.toJSON());
                if (Main.wsClient != null) Main.wsClient.safeSend(msg.toString());


                mouseDragging = false;
                selectedObject = null;
                
                return;
            }

            double objX = event.getX() - mouseOffsetX; // left tip X
            double objY = event.getY() - mouseOffsetY; // left tip Y

            // build object with dragged position (size stays in col/row)
            GameObject selectedObject2 = new GameObject(
                selectedObject.id,
                (int) objX,
                (int) objY,

                selectedObject.role
            );
            
            
            if(grid.getCol(objX)!=-1 && grid.getRow(objY)!=-1){
                System.out.println("Peça col·locada: "+selectedObject2.role);
                insertPieceInBoard(selectedObject,event.getX(), event.getY());
            }
            



    //        snap by left-top corner to underlying cell
            
            snapObjectLeftTop(selectedObject);
            

            JSONObject msg = new JSONObject();
            msg.put("type", "clientObjectMoving");
            msg.put("value", selectedObject.toJSON());
            if (Main.wsClient != null) Main.wsClient.safeSend(msg.toString());

            mouseDragging = false;
            selectedObject = null;
        }
    }

    private void insertPieceInBoard(GameObject selectedObject2, double x, double y) {
        String piece = selectedObject2.role;
        List<List<String>> board = Main.gameData.getBoard();
        int realx = grid.getCol(x);;
        int realy = -1;

        for (int i = board.size(); i > 0; i--) {
            if (board.get(i-1).get(realx).contains(" ")) {
                realy = i-1;
                break;
            }
        }
        if(realy==-1 || realx==-1) {return;}
        //System.out.println("Posició: "+realy+" , "+realx + " Peça: "+piece);
        


        JSONObject msg = new JSONObject();
        msg.put("type", "clientAddPiece");
        msg.put("value", new JSONObject()
                .put("piece", piece)
                .put("col", realx)
                .put("row", realy));

       
        if (Main.wsClient != null) Main.wsClient.safeSend(msg.toString());
        // GameData newGameData = new GameData(Main.gameData.toJSON());
        // newGameData.setPiece(realy, realx, piece);
        // newGameData.setLastMove(new JSONObject()
        // .put("row", realy)
        // .put("col", realx)
        // );
 
        // JSONObject msg = new JSONObject();
        //     msg.put("type", "clientAddPiece");
        //     msg.put("value", newGameData.toJSON());
        //     if (Main.wsClient != null) Main.wsClient.safeSend(msg.toString());
    }


    // Snap piece so its left-top corner sits exactly on the grid cell under its left tip.
    private void snapObjectLeftTop(GameObject obj) {
        

        // clamp inside grid
        int col = (int) (grid.getStartX()+grid.getCellSize()*7+100);
        int row = (int) grid.getStartY();

        if (obj.role.equals("R")){
            row += 100;
        }

        obj.x = col;
        obj.y = row;
    }

    public Boolean isPositionInsideObject(double positionX, double positionY, int objX, int objY) {
        double cellSize = grid.getCellSize();
        double objectWidth =cellSize;
        double objectHeight = cellSize;

        double objectLeftX = objX;
        double objectRightX = objX + objectWidth;
        double objectTopY = objY;
        double objectBottomY = objY + objectHeight;

        return positionX >= objectLeftX && positionX < objectRightX &&
               positionY >= objectTopY && positionY < objectBottomY;
    }

    // Run game (and animations)
    private void run(double fps) {

        if (animationTimer.fps < 1) { return; }

        // Update objects and animations here
    }

    // Draw game to canvas
    public void draw() {

        if (Main.clients == null) { return; }

        // Clean drawing area
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());


        if(isPlaying()){
            drawShadowCircle();
        }
        
        //Draw colored 'over' cells
        // for (ClientData clientData : Main.clients) {
        //     // Comprovar si està dins dels límits de la graella
        //     if (clientData.row >= 0 && clientData.col >= 0) {
        //         Color base = getColor(clientData.color);
        //         Color alpha = new Color(base.getRed(), base.getGreen(), base.getBlue(), 0.5);
        //         gc.setFill(alpha);
        //         gc.fillRect(grid.getCellX(clientData.col), grid.getCellY(0) - grid.getCellSize(), grid.getCellSize(), grid.getCellSize());
        //     }
        // }






        // Draw grid
        drawGrid();

        drawDummy();

        // Draw objects
        for (GameObject go : Main.objects) {
            if (selectedObject != null && go.id.equals(selectedObject.id)) {
                drawObject(selectedObject);
            } else {
                drawObject(go);
            }
        }

        drawWinningPieces();
        // Draw mouse circles
        for (ClientData clientData : Main.clients) {
            gc.setFill(getColor(clientData.color)); 
            gc.fillOval(clientData.mouseX - 5, clientData.mouseY - 5, 10, 10);
        }


        

        // Draw FPS if needed
        if (showFPS) { animationTimer.drawFPS(gc); }   
    }
    public void drawDummy(){
        Color color;
        int size = (int) (grid.getCellSize()-5);
        if(dummyAnimacion==null){return;}

        if(dummyAnimacion.role.equals("Y")){
                    color=Color.YELLOW;
                }else{
                    color=Color.RED;
                }
        gc.setFill(color);
        gc.fillOval(dummyAnimacion.x, dummyAnimacion.y, size,size);
        gc.strokeOval(dummyAnimacion.x, dummyAnimacion.y, size,size);
               
    }

    
    public void drawWinningPieces(){
        if(Main.gameData.getStatus().equals("win")){
            for (List<Integer> pieces : Main.glowPieces.getPieces()) {
                double x = grid.getCellX(pieces.get(1));
                double y = grid.getCellY(pieces.get(0));
                String role = Main.glowPieces.getRole();
                Color color;
                double size = grid.getCellSize()-5;


                if (role.equals("Y")){
                    color = Color.ORANGE;
                }else{
                    color = Color.PURPLE;
                }
                Color alpha = new Color(color.getRed(), color.getGreen(), color.getBlue(),Main.glowPieces.getGlow() );
                gc.setFill(alpha);
                gc.fillOval(x,y, size,size);
                gc.strokeOval(x, y, size,size);


            }
        }

    }

    public void drawGrid() {
        gc.setFill(Color.BLUE);
        double xtotal = grid.getCols() * grid.getCellSize();
        double ytotal = grid.getRows() * grid.getCellSize();
        gc.fillRoundRect(grid.getStartX(), grid.getStartY(), xtotal, ytotal, 0, 0);

        for (int row = 0; row < grid.getRows(); row++) {
            for (int col = 0; col < grid.getCols(); col++) {
                double cellSize = grid.getCellSize();
                double x = grid.getStartX() + col * cellSize;
                double y = grid.getStartY() + row * cellSize;

                
                List<List<String>> board = Main.gameData.getBoard();
                //gc.strokeRect(x, y, cellSize, cellSize);
                gc.setFill(Color.WHITESMOKE);
                if(board.get(row).get(col).equals("Y")){
                    gc.setFill(Color.YELLOW);
                }
                if(board.get(row).get(col).equals("R")){
                    gc.setFill(Color.RED); 
                }
                
                gc.fillOval(x, y, grid.getCellSize()-5, grid.getCellSize()-5);
            }
        }
    }

    public void drawObject(GameObject obj) {
        double cellSize = grid.getCellSize();


        int x = obj.x;
        int y = obj.y;
        double width =  cellSize - 5;
        double height = cellSize - 5;

        // Seleccionar un color basat en l'objectId
        Color color;
        
        switch (obj.id.toLowerCase()) {
            case "r_00":
                color = Color.RED;
                break;
            case "y_00":
                color = Color.YELLOW;
                break;
            default:
                color = Color.GRAY;
                break;
        }

        // Dibuixar el rectangle
        gc.setFill(color);
        gc.fillOval(x, y, width, height);

        // Dibuixar el contorn
        gc.setStroke(Color.BLACK);
        gc.strokeOval(x, y, width, height);

    }

    public Color getColor(String colorName) {
        switch (colorName.toLowerCase()) {
            case "red":
                return Color.RED;
            case "blue":
                return Color.BLUE;
            case "green":
                return Color.GREEN;
            case "yellow":
                return Color.YELLOW;
            case "orange":
                return Color.ORANGE;
            case "purple":
                return Color.PURPLE;
            case "pink":
                return Color.PINK;
            case "brown":
                return Color.BROWN;
            case "gray":
                return Color.GRAY;
            case "black":
                return Color.BLACK;
            default:
                return Color.LIGHTGRAY; // Default color
        }
    }

        private void drawShadowCircle() {
        // if (shadow == -1) return;
        // gc.setFill(Color.GRAY);
        // double size = grid.getCellSize() - 5;
        // double x = grid.getStartX() + shadow * grid.getCellSize();
        // double y = grid.getStartY() - grid.getCellSize();
        // gc.fillOval(x, y, size, size);
        
        for (ClientData clientData : Main.clients) {
            // Comprovar si està dins dels límits de la graella
            if (clientData.row >= 0 && clientData.col >= 0) {
                 Color base;
                if(clientData.role.equals("Y")){
                    base = Color.YELLOW;
                }else{
                    base = Color.RED;
                }

                
                Color alpha = new Color(base.getRed(), base.getGreen(), base.getBlue(), 0.5);
                double size = grid.getCellSize() - 5;
                double x = grid.getStartX()+grid.getCol(clientData.mouseX)*grid.getCellSize();
                double y = grid.getStartY() - grid.getCellSize();
                gc.setFill(alpha);
                gc.fillOval(x, y, size, size);
            }
        }
    }

   

    public boolean isPlaying(){
        if(Main.gameData.getStatus().equals("playing")){
            return true;
        }
        return false;
    }

    public void newDummyObject(int x,String role){

        int y = (int) (grid.getStartY()-grid.getCellSize());
        int xx = (int) (grid.getStartX()+x*grid.getCellSize());

        dummyAnimacion =new GameObject("dummy", xx, y, role);
    
    }

    public void startAnimation(int row, int col) {
        final int xx = col;
        final int yy = row;
        final int FINALY = grid.getCellY(col);

        ExecutorService animacion = Executors.newSingleThreadExecutor();

        Runnable runAnimacion = () -> {                



            final int VEL = 5;
            final int DELAY = 5;

            final int[] actY = {dummyAnimacion.y};

            while (actY[0] < FINALY) {
                // System.out.println(dummyAnimacion);
                actY[0] += VEL;

                if (actY[0] > FINALY){
                    actY[0] = FINALY;
                }

                SwingUtilities.invokeLater(() -> {
                    dummyAnimacion.y = actY[0];
                });

                
                try {
                    Thread.sleep(DELAY);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                
            }
            //System.out.println("Animacion acabada");
            if(Main.gameData.getTurn().equals(Main.clientName)){
                GameData newGameData = new GameData(Main.gameData.toJSON());
                newGameData.setPiece(xx, yy, dummyAnimacion.role);
                newGameData.setLastMove(new JSONObject()
                .put("row", xx)
                .put("col", yy)
                );
                JSONObject msg = new JSONObject();
                    msg.put("type", "clientAddPieceFinal");
                    msg.put("value", newGameData.toJSON());
                if (Main.wsClient != null) Main.wsClient.safeSend(msg.toString());
            }
            dummyAnimacion=null;
            animacion.shutdown();


        };

        animacion.submit(runAnimacion);
    }


    public void startWinningAnimation(){

        final boolean[] running = {true};
        final double[] vel = {0.02};

        ExecutorService animacion = Executors.newSingleThreadExecutor();

        Runnable runAnimacion = () -> {      
            
            final int DELAY = 10;
            
            final double[] actGlow = {Main.glowPieces.getGlow()};
            while(running[0]){

                try {
                    if(actGlow[0]+vel[0]>1 || actGlow[0]+vel[0] <0){
                    vel[0]*=-1;
                    }
                    actGlow[0] +=vel[0];
                    
                    SwingUtilities.invokeLater(() -> {
                        Main.glowPieces.setGlow(actGlow[0]);
                    });

                    Thread.sleep(DELAY);
                } catch (Exception e) {
                    // TODO: handle exception
                    Thread.currentThread().interrupt();
                    running[0]=false;
                    break;
                } 
            }
            animacion.shutdown();

        };

        animacion.submit(runAnimacion);
        System.out.println("testContinuidad");
    }


    




}

