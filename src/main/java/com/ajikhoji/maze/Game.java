package com.ajikhoji.maze;

import java.util.ArrayList;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 *
 * @author Sujit T K
 */
public class Game extends Application {

    private Pane pane_content = null;
    private Pane pane_maze = null;
    private MazeMapConfiguration mapConfig;
    private ResourceLoader res;
    private ImageView[] imgTile;
    private ImageView imgPlayer;
    private boolean blnListenToKeyInput = true;
    private int intPlayerX = 0, intPlayerY = 0;
    private double DBL_TILE_SIDE_LENGTH;
    private ChaserBot cb, cb2, cb3;
    private UltraBot ub;
    private int LEVEL = 1;

    @Override
    public void start(Stage primaryStage) {
        this.pane_content = new Pane();//#9F0F4A
        this.pane_content.setBackground(new Background(new BackgroundFill(Color.web("#420F31"), CornerRadii.EMPTY, Insets.EMPTY)));
        Scene scene = new Scene(this.pane_content, Values.DBL_WINDOW_WIDTH, Values.DBL_WINDOW_HEIGHT);
        //primaryStage.setMaximized(true);
        //primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setTitle("MAZE!!!");
        primaryStage.setScene(scene);
        //primaryStage.setResizable(false);
        primaryStage.show();
        scene.setOnKeyPressed(eh->{
            if(blnListenToKeyInput) {
                blnListenToKeyInput = false;
                movePlayer(eh.getCode().toString());
            }
        });
        configMap(LEVEL);
        createMazeMap();
        Platform.runLater(()->{
            //figureOutPath();
        });
    }

    private void figureOutPath() {
        //blnListenToKeyInput = false;
        ArrayList<Integer> path = findShortestPath(intPlayerY,intPlayerX, new ArrayList<Integer>(), new boolean[mapConfig.INT_ROWS][mapConfig.INT_COLUMNS]);
        if(path != null) {
            Timeline t_show_path = new Timeline();
            for(int i = 0; i < path.size(); i++) {
                Glow g = new Glow();
                imgTile[path.get(i)].setEffect(g);
                //blnShowPath[i] = true;
                t_show_path.getKeyFrames().addAll(new KeyFrame(Duration.ZERO, new KeyValue(g.levelProperty(),0.0D)),
                        new KeyFrame(Duration.millis(2000.0D), new KeyValue(g.levelProperty(),0.75D)));
            }
            t_show_path.setCycleCount(Timeline.INDEFINITE);
            t_show_path.setAutoReverse(true);
            t_show_path.playFromStart();
            int division = 5, curr = 0;
            /*for(int i = 0; i < path.size(); i++) {
                Glow g = new Glow();
                imgTile[path.get(i)].setEffect(g);
                Timeline t_reveal_path = new Timeline(new KeyFrame(Duration.millis(curr*1000.0D), new KeyValue(g.levelProperty(),0.0D)),
                        new KeyFrame(Duration.millis(2000.0D+curr*1000.0D), new KeyValue(g.levelProperty(),1.00D)));
                if (curr == 4) {
                    curr = 0;
                } else {
                    curr++;
                }
                t_reveal_path.setCycleCount(Timeline.INDEFINITE);
                t_reveal_path.setAutoReverse(true);
                t_reveal_path.playFromStart();
            }*/
        } else {
            //blnListenToKeyInput = true;
        }
    }

    private final boolean[][] getCopyArray(final boolean[][] GIVEN) {
        boolean[][] REQUIRED = new boolean[GIVEN.length][GIVEN[0].length];
        for(int row = 0; row < GIVEN.length; row++) {
            for(int col  = 0; col < GIVEN[0].length; col++) {
                REQUIRED[row][col] = GIVEN[row][col];
            }
        }
        return REQUIRED;
    }

    private int[] dangerZone;
    private boolean[] blnShowPath;

    private class ChaserBot extends ImageView {
        public boolean[][] blnPathBlocked, blnInitial;
        public int ROW_LOC, COL_LOC;
        public Timeline tl_move = null;
        private Glow g  = new Glow();
        private int[] vulnerableZones;
        private int intTotalVulnerableZones = 0;

        public ChaserBot(final Image img, final boolean[][] mazemap, final int ROW_START, final int COL_START) {
            g.setLevel(0.6D);
            this.vulnerableZones = new int[5];
            this.setImage(img);
            this.blnPathBlocked = getCopyArray(mazemap);
            this.blnInitial = getCopyArray(mazemap);
            this.ROW_LOC = ROW_START;
            this.COL_LOC = COL_START;
        }

        public final void start() {
            if (blnPathBlocked[ROW_LOC][COL_LOC]) System.err.println("ERROR! Bot cannot be moved since its current blnPathBlocked[ROW_LOC][COL_LOC] is blocked.");
            else {
                blnPathBlocked[ROW_LOC][COL_LOC] = true;
                this.updateDangerZones(ROW_LOC,COL_LOC);
                this.markTiles(0.4D);
                decide_move_direction();
            }
        }

        public final void stop() {
            if(this.tl_move.getStatus() == Timeline.Status.RUNNING) this.tl_move.stop();
        }

        private void decide_move_direction() {
            if (this.tl_move == null) this.tl_move = new Timeline();
            this.tl_move.getKeyFrames().clear();
            ArrayList<String> newPos = new ArrayList<String>();
            if(isFreeToMove(ROW_LOC,COL_LOC-1)) newPos.add("LEFT");
            if(isFreeToMove(ROW_LOC,COL_LOC+1)) newPos.add("RIGHT");
            if(isFreeToMove(ROW_LOC-1,COL_LOC)) newPos.add("TOP");
            if(isFreeToMove(ROW_LOC+1,COL_LOC)) newPos.add("BOTTOM");
            if(newPos.size() == 0) {
                this.blnPathBlocked = getCopyArray(this.blnInitial);
                /*for(int i = 0; i < this.blnPathBlocked.length; i++) {
                    for(int j = 0; j < this.blnPathBlocked[0].length; j++) {
                        System.out.print(((this.blnPathBlocked[i][j]) ? 1 : 0) + " ");
                    }
                    System.out.println("");
                }*/
                if(isFreeToMove(ROW_LOC,COL_LOC-1)) newPos.add("LEFT");
                if(isFreeToMove(ROW_LOC,COL_LOC+1)) newPos.add("RIGHT");
                if(isFreeToMove(ROW_LOC-1,COL_LOC)) newPos.add("TOP");
                if(isFreeToMove(ROW_LOC+1,COL_LOC)) newPos.add("BOTTOM");
            }
            if(newPos.size() == 0) {
                System.err.println("Stopped at row,col = " + this.ROW_LOC+","+this.COL_LOC);
            } else {
                String position = newPos.get((int)(Math.random()*newPos.size()));
                switch (position) {
                    case "LEFT" -> {
                        this.move(ROW_LOC,COL_LOC-1);
                    }
                    case "RIGHT" -> {
                        this.move(ROW_LOC,COL_LOC+1);
                    }
                    case "TOP" -> {
                        this.move(ROW_LOC-1,COL_LOC);
                    }
                    case "BOTTOM" -> {
                        this.move(ROW_LOC+1,COL_LOC);
                    }
                    default -> {
                        System.err.println("Unexpected movement requested for ChaserBot : " + position);
                    }
                }}
        }

        private boolean isFreeToMove(final int ROW, final int COL) {
            return ROW > -1 && COL > -1 && ROW < blnPathBlocked.length && COL < blnPathBlocked[0].length && !blnPathBlocked[ROW][COL];
        }

        private boolean isBlockFree(final int ROW, final int COL) {
            return ROW > -1 && COL > -1 && ROW < blnPathBlocked.length && COL < blnPathBlocked[0].length && !blnInitial[ROW][COL];
        }

        /*public final boolean checkForOverlap() {
            int playerLocation = (intPlayerY*mapConfig.INT_COLUMNS) + intPlayerX;
            for(int i = 0 ; i < this.intTotalDangerZones; i++) {
                if(dangerZone[i] == playerLocation) {
                    this.tl_move.stop();
                    return true;
                }
            }
            return false;
        }*/

        private void updateDangerZones(final int CURRENT_ROW, final int CURRENT_COL) {
            int currentDangerZones = 0, currentTileLocation = (CURRENT_ROW*mapConfig.INT_COLUMNS) + CURRENT_COL;
            this.vulnerableZones[currentDangerZones++] = currentTileLocation;
            if(isBlockFree(CURRENT_ROW,CURRENT_COL-1)) this.vulnerableZones[currentDangerZones++] = currentTileLocation - 1;//Left
            if(isBlockFree(CURRENT_ROW,CURRENT_COL+1)) this.vulnerableZones[currentDangerZones++] = currentTileLocation + 1;//Right
            if(isBlockFree(CURRENT_ROW-1,CURRENT_COL)) this.vulnerableZones[currentDangerZones++] = currentTileLocation - mapConfig.INT_COLUMNS;//Top
            if(isBlockFree(CURRENT_ROW+1,CURRENT_COL)) this.vulnerableZones[currentDangerZones++] = currentTileLocation + mapConfig.INT_COLUMNS;//Bottom
            this.intTotalVulnerableZones = currentDangerZones;
        }

        private void markTiles(final double opacity) {
            int playerTileLoc = (intPlayerY*mapConfig.INT_COLUMNS) + intPlayerX;
            boolean blnPlayerCaught = false;
            for(int i = 0; i < this.intTotalVulnerableZones; i++) {
                if(opacity == 1.0D) {
                    dangerZone[this.vulnerableZones[i]]--;
                    if(dangerZone[this.vulnerableZones[i]] == 0) {
                        imgTile[this.vulnerableZones[i]].setEffect(null);
                    }
                } else {
                    if(this.vulnerableZones[i] == playerTileLoc) blnPlayerCaught = true;
                    if(dangerZone[this.vulnerableZones[i]] == 0) {
                        imgTile[this.vulnerableZones[i]].setEffect(g);
                    }
                    dangerZone[this.vulnerableZones[i]]++;
                }
            }
            if(blnPlayerCaught) {
                this.tl_move.stop();
                gameLost(Reason.CAUGHT_BY_BOT);
            }
        }

        private void move(final int DESTINATION_ROW, final int DESTINATION_COL) {
            final double DBL_DURATION_ANIM_MILLIS = 600.0D;
            this.tl_move.getKeyFrames().add(new KeyFrame(Duration.millis(DBL_DURATION_ANIM_MILLIS/2.0D), e -> {
                this.markTiles(1.0D);
                this.updateDangerZones(DESTINATION_ROW, DESTINATION_COL);
                this.markTiles(0.4D);
            }));
            if(this.ROW_LOC == DESTINATION_ROW) this.tl_move.getKeyFrames().add(new KeyFrame(Duration.millis(DBL_DURATION_ANIM_MILLIS), new KeyValue(this.xProperty(),this.getX()+(DESTINATION_COL-this.COL_LOC)*DBL_TILE_SIDE_LENGTH)));
            else this.tl_move.getKeyFrames().add(new KeyFrame(Duration.millis(DBL_DURATION_ANIM_MILLIS), new KeyValue(this.yProperty(),this.getY()+(DESTINATION_ROW-this.ROW_LOC)*DBL_TILE_SIDE_LENGTH)));
            tl_move.setOnFinished(eh -> {
                this.decide_move_direction();
            });
            tl_move.playFromStart();
            blnPathBlocked[DESTINATION_ROW][DESTINATION_COL] = true;
            this.ROW_LOC = DESTINATION_ROW;
            this.COL_LOC = DESTINATION_COL;
        }
    }

    private class UltraBot extends ImageView {
        public boolean[][] blnPathBlocked, blnInitial;
        public int ROW_LOC, COL_LOC;
        public Timeline tl_move = null;
        private ArrayList<Integer> arrVulnerableZones;
        private Glow g  = new Glow();

        public UltraBot(final Image img, final boolean[][] mazemap, final int ROW_START, final int COL_START) {
            g.setLevel(0.6D);
            this.arrVulnerableZones = new ArrayList<Integer>();
            this.setImage(img);
            this.blnPathBlocked = getCopyArray(mazemap);
            this.blnInitial = getCopyArray(mazemap);
            this.ROW_LOC = ROW_START;
            this.COL_LOC = COL_START;
        }

        public final void start() {
            if (blnPathBlocked[ROW_LOC][COL_LOC]) System.err.println("ERROR! Bot cannot be moved since its current blnPathBlocked[ROW_LOC][COL_LOC] is blocked.");
            else {
                blnPathBlocked[ROW_LOC][COL_LOC] = true;
                this.updateDangerZones(ROW_LOC,COL_LOC);
                this.markTiles(0.4D);
                decide_move_direction();
            }
        }

        public final void stop() {
            if(this.tl_move.getStatus() == Timeline.Status.RUNNING) this.tl_move.stop();
        }

        private void decide_move_direction() {
            if (this.tl_move == null) this.tl_move = new Timeline();
            this.tl_move.getKeyFrames().clear();
            ArrayList<String> newPos = new ArrayList<String>();
            if(isFreeToMove(ROW_LOC,COL_LOC-1)) newPos.add("LEFT");
            if(isFreeToMove(ROW_LOC,COL_LOC+1)) newPos.add("RIGHT");
            if(isFreeToMove(ROW_LOC-1,COL_LOC)) newPos.add("TOP");
            if(isFreeToMove(ROW_LOC+1,COL_LOC)) newPos.add("BOTTOM");
            if(newPos.size() == 0) {
                this.blnPathBlocked = getCopyArray(this.blnInitial);
                /*for(int i = 0; i < this.blnPathBlocked.length; i++) {
                    for(int j = 0; j < this.blnPathBlocked[0].length; j++) {
                        System.out.print(((this.blnPathBlocked[i][j]) ? 1 : 0) + " ");
                    }
                    System.out.println("");
                }*/
                if(isFreeToMove(ROW_LOC,COL_LOC-1)) newPos.add("LEFT");
                if(isFreeToMove(ROW_LOC,COL_LOC+1)) newPos.add("RIGHT");
                if(isFreeToMove(ROW_LOC-1,COL_LOC)) newPos.add("TOP");
                if(isFreeToMove(ROW_LOC+1,COL_LOC)) newPos.add("BOTTOM");
            }
            if(newPos.size() == 0) {
                System.err.println("Stopped at row,col = " + this.ROW_LOC+","+this.COL_LOC);
            } else {
                String position = newPos.get((int)(Math.random()*newPos.size()));
                switch (position) {
                    case "LEFT" -> {
                        this.move(ROW_LOC,COL_LOC-1);
                    }
                    case "RIGHT" -> {
                        this.move(ROW_LOC,COL_LOC+1);
                    }
                    case "TOP" -> {
                        this.move(ROW_LOC-1,COL_LOC);
                    }
                    case "BOTTOM" -> {
                        this.move(ROW_LOC+1,COL_LOC);
                    }
                    default -> {
                        System.err.println("Unexpected movement requested for ChaserBot : " + position);
                    }
                }}
        }

        private boolean isFreeToMove(final int ROW, final int COL) {
            return ROW > -1 && COL > -1 && ROW < blnPathBlocked.length && COL < blnPathBlocked[0].length && !blnPathBlocked[ROW][COL];
        }

        private void updateDangerZones(final int CURRENT_ROW, final int CURRENT_COL) {
            arrVulnerableZones.clear();
            arrVulnerableZones.ensureCapacity(mapConfig.INT_COLUMNS+mapConfig.INT_ROWS);
            int currentTileLocation = (CURRENT_ROW*mapConfig.INT_COLUMNS) + CURRENT_COL, count = 0;
            for(int col = CURRENT_COL - 1; col > -1 && !this.blnInitial[CURRENT_ROW][col]; col--) {
                arrVulnerableZones.add(currentTileLocation-(++count));
            }
            count = 0;
            for(int col = CURRENT_COL + 1; col < mapConfig.INT_COLUMNS && !this.blnInitial[CURRENT_ROW][col]; col++) {
                arrVulnerableZones.add(currentTileLocation+(++count));
            }
            count = 0;
            for(int row = CURRENT_ROW - 1; row > -1 && !this.blnInitial[row][CURRENT_COL]; row--) {
                arrVulnerableZones.add(currentTileLocation-((++count)*mapConfig.INT_COLUMNS));
            }
            count = 0;
            for(int row = CURRENT_ROW + 1; row < mapConfig.INT_ROWS && !this.blnInitial[row][CURRENT_COL]; row++) {
                arrVulnerableZones.add(currentTileLocation+((++count)*mapConfig.INT_COLUMNS));
            }
            arrVulnerableZones.add(currentTileLocation);
        }

        private void markTiles(final double opacity) {
            int playerTileLoc = (intPlayerY*mapConfig.INT_COLUMNS) + intPlayerX;
            boolean blnPlayerCaught = false;
            for(int i = 0; i < this.arrVulnerableZones.size(); i++) {
                if(opacity == 1.0D) {
                    dangerZone[this.arrVulnerableZones.get(i)]--;
                    if(dangerZone[this.arrVulnerableZones.get(i)] == 0) {
                        imgTile[this.arrVulnerableZones.get(i)].setEffect(null);
                    }
                } else {
                    if(this.arrVulnerableZones.get(i) == playerTileLoc) blnPlayerCaught = true;
                    if(dangerZone[this.arrVulnerableZones.get(i)] == 0) {
                        imgTile[this.arrVulnerableZones.get(i)].setEffect(g);
                    }
                    dangerZone[this.arrVulnerableZones.get(i)]++;
                }
            }
            if(blnPlayerCaught) {
                this.tl_move.stop();
                gameLost(Reason.CAUGHT_BY_BOT);
            }
        }

        private void move(final int DESTINATION_ROW, final int DESTINATION_COL) {
            final double DBL_DURATION_ANIM_MILLIS = 600.0D;
            this.tl_move.getKeyFrames().add(new KeyFrame(Duration.millis(DBL_DURATION_ANIM_MILLIS/2.0D), e -> {
                this.markTiles(1.0D);
                this.updateDangerZones(DESTINATION_ROW, DESTINATION_COL);
                this.markTiles(0.4D);
            }));
            if(this.ROW_LOC == DESTINATION_ROW) this.tl_move.getKeyFrames().add(new KeyFrame(Duration.millis(DBL_DURATION_ANIM_MILLIS), new KeyValue(this.xProperty(),this.getX()+(DESTINATION_COL-this.COL_LOC)*DBL_TILE_SIDE_LENGTH)));
            else this.tl_move.getKeyFrames().add(new KeyFrame(Duration.millis(DBL_DURATION_ANIM_MILLIS), new KeyValue(this.yProperty(),this.getY()+(DESTINATION_ROW-this.ROW_LOC)*DBL_TILE_SIDE_LENGTH)));
            tl_move.setOnFinished(eh -> {
                this.decide_move_direction();
            });
            tl_move.playFromStart();
            blnPathBlocked[DESTINATION_ROW][DESTINATION_COL] = true;
            this.ROW_LOC = DESTINATION_ROW;
            this.COL_LOC = DESTINATION_COL;
        }
    }

    private ArrayList<Integer> findShortestPath(final int R, final int C, final ArrayList<Integer> locs, final boolean[][] visited) {
        if(R < 0 || C < 0 || C >= mapConfig.INT_COLUMNS || R >= mapConfig.INT_ROWS || mapConfig.blnBlocked[R][C] || visited[R][C]) {
            return null;
        }
        visited[R][C] = true;
        locs.add(R*mapConfig.INT_COLUMNS + C);
        if(R == mapConfig.INT_END_ROW && C == mapConfig.INT_END_COL) {
            return locs;
        }
        boolean[][][] visit = new boolean[4][mapConfig.INT_ROWS][mapConfig.INT_COLUMNS];
        ArrayList<Integer>[] loc = new ArrayList[4];
        for(int i = 0; i < 4; i++) {
            loc[i] = new ArrayList<Integer>();
            loc[i].addAll(locs);
        }
        for(int i = 0; i < mapConfig.INT_ROWS; i++) {
            for(int j = 0; j < mapConfig.INT_COLUMNS; j++) {
                for(int k = 0; k < 4; k++) {
                    visit[k][i][j] = visited[i][j];
                }
            }
        }
        ArrayList<Integer> right = findShortestPath(R,C+1,loc[0],visit[0]);
        int rightPathLength = (right == null) ? Integer.MAX_VALUE : right.size();
        ArrayList<Integer> bottom = findShortestPath(R+1,C,loc[1],visit[1]);
        int bottomPathLength = (bottom == null) ? Integer.MAX_VALUE : bottom.size();
        ArrayList<Integer> left = findShortestPath(R,C-1,loc[2],visit[2]);
        int leftPathLength = (left == null) ? Integer.MAX_VALUE : left.size();
        ArrayList<Integer> top = findShortestPath(R-1,C,loc[3],visit[3]);
        int topPathLength = (top == null) ? Integer.MAX_VALUE : top.size();

        /*ArrayList<Integer> right = findShortestPath(R,C+1,(ArrayList<Integer>) locs.clone(),(boolean[][]) visited.clone());
        int rightPathLength = (right == null) ? Integer.MAX_VALUE : right.size();
        ArrayList<Integer> bottom = findShortestPath(R+1,C,(ArrayList<Integer>) locs.clone(),(boolean[][]) visited.clone());
        int bottomPathLength = (bottom == null) ? Integer.MAX_VALUE : bottom.size();
        ArrayList<Integer> left = findShortestPath(R,C-1,(ArrayList<Integer>) locs.clone(),(boolean[][]) visited.clone());
        int leftPathLength = (left == null) ? Integer.MAX_VALUE : left.size();
        ArrayList<Integer> top = findShortestPath(R-1,C,(ArrayList<Integer>) locs.clone(),(boolean[][]) visited.clone());
        int topPathLength = (top == null) ? Integer.MAX_VALUE : top.size();*/

        int shortestPathLength = Math.min(rightPathLength, Math.min(leftPathLength,Math.min(topPathLength,bottomPathLength)));
        if(shortestPathLength == leftPathLength) return left;
        if(shortestPathLength == rightPathLength) return right;
        if(shortestPathLength == topPathLength) return top;
        if(shortestPathLength == bottomPathLength) return bottom;
        return null;
    }

    private void configMap(final int LEVEL) {
        switch (LEVEL) {
            case 0 -> {//inteded for testing purpose
                this.LEVEL = 0;
                mapConfig = new MazeMapConfiguration(5,5,4,4);
                mapConfig.assignAsBlocks(new int[][]{{0,2},{1,3},{2,3},{3,4},{2,1},{2,2}});
            }
            case 1 -> {
                this.LEVEL = 1;
                mapConfig = new MazeMapConfiguration(10,10,9,9);
                mapConfig.assignAsBlocks(new int[][]{{1,1},{2,1},{3,1},{4,1},{6,1},{8,1},{9,1},{3,2},{6,2},{1,3},{5,3},{6,3},{7,3},{8,3},{1,4},
                        {3,4},{4,4},{1,5},{2,5},{3,5},{6,5},{7,5},{8,5},{5,6},{3,6},{6,6},{0,7},{9,3},{1,7},{3,7},{5,7},{8,7},{9,7},{1,8},{3,8},{5,8},{7,8},{8,8},{5,9}});
            }
            case 3 -> {
                this.LEVEL = 3;
                mapConfig = new MazeMapConfiguration(25,25,0,24);
                this.dangerZone = new int[mapConfig.INT_TOTAL];
                mapConfig.assignAsBlocks(new int[][]{{0,13},{0,23},{1,1},{1,2},{1,4},{1,6},{1,7},{1,8},{1,9},{1,11},{1,12},{1,13},{1,15},{1,16},{1,18},{1,19},
                        {1,20},{1,21},{1,23},{2,1},{2,4},{2,8},{2,11},{2,21},{3,4},{3,5},{3,6},{3,11},{3,13},{3,14},{3,15},{3,16},{3,19},{3,22},{3,23},{3,24},{4,0}
                        ,{4,2},{4,6},{4,8},{4,9},{4,11},{4,16},{4,18},{4,19},{4,20},{4,21},{5,2},{5,4},{5,5},{5,6},{5,9},{5,12},{5,14},{5,20},{5,23},{6,1},{6,2}
                        ,{6,7},{6,9},{6,12},{6,13},{6,14},{6,15},{6,16},{6,17},{6,18},{6,20},{6,22},{6,23},{7,4},{7,5},{7,7},{7,9},{7,12},{7,18},{8,1},{8,2},
                        {8,5},{8,7},{8,9},{8,10},{8,11},{8,12},{8,14},{8,15},{8,16},{8,18},{8,19},{8,20},{8,21},{8,22},{8,23},{9,5},{9,7},{9,14},{9,16},{9,18},
                        {9,23},{10,0},{10,1},{10,2},{10,3},{10,4},{10,7},{10,8},{10,9},{10,10},{10,11},{10,12},{10,13},{10,14},{10,16},{10,20},{10,21},{10,23},
                        {11,5},{11,7},{11,12},{11,16},{11,18},{11,19},{11,20},{11,23},{12,1},{12,2},{12,3},{12,5},{12,9},{12,12},{12,14},{12,22},{12,23},{13,1},
                        {13,3},{13,5},{13,6},{13,7},{13,8},{13,11},{13,12},{13,14},{13,15},{13,16},{13,18},{13,19},{13,21},{14,1},{14,3},{14,9},{14,12},{14,22},
                        {14,23},{15,1},{15,3},{15,5},{15,6},{15,7},{15,9},{15,10},{15,11},{15,12},{15,13},{15,14},{15,15},{15,16},{15,18},{15,22},{16,7},{16,18},
                        {16,19},{16,20},{16,21},{16,22},{16,24},{17,1},{17,3},{17,4},{17,5},{17,9},{17,10},{17,11},{17,13},{17,14},{17,15},{17,16},{17,17},{17,19},
                        {17,24},{18,1},{18,3},{18,6},{18,7},{18,9},{18,11},{18,16},{18,19},{18,22},{18,23},{18,24},{19,1},{19,7},{19,14},{19,16},{19,18},{19,21},
                        {20,1},{20,4},{20,7},{20,8},{20,9},{20,10},{20,11},{20,12},{20,13},{20,14},{20,16},{20,19},{20,21},{20,22},{20,24},{21,4},{21,7},{21,8},
                        {21,14},{21,19},{21,22},{21,24},{22,1},{22,2},{22,3},{22,4},{22,11},{22,14},{22,16},{22,17},{22,19},{22,22},{23,6},{23,7},{23,8},{23,9},
                        {23,10},{23,12},{23,14},{23,15},{23,17},{23,18},{23,22},{23,23},{24,1},{24,3},{24,4},{24,11}});
                //mapConfig.assignAsPathWay(new int[][]{{4,11}});
            }
            case 2 -> {
                this.LEVEL = 2;
                mapConfig = new MazeMapConfiguration(20,20,19,19);
                intPlayerY = 1;
                this.dangerZone = new int[mapConfig.INT_TOTAL];
                mapConfig.assignAsBlocks(new int[][]{{0,0},{0,1},{0,2},{0,4},{0,5},{0,6},{0,7},{0,10},{0,15},{1,4},{1,10},{1,12},
                        {1,13},{1,17},{1,18},{2,0},{2,2},{2,4},{2,6},{2,7},{2,8},{2,10},{2,12},{2,13},
                        {2,14},{2,15},{2,16},{2,17},{2,18},{3,0},{3,2},{3,4},{3,8},{3,10},{3,17},{4,0},
                        {4,2},{4,5},{4,6},{4,8},{4,10},{4,12},{4,13},{4,14},{4,15},{4,16},{4,17},{4,18},
                        {5,3},{5,8},{5,10},{5,12},{6,0},{6,2},{6,3},{6,4},{6,5},{6,6},{6,8},{6,10},
                        {6,12},{6,14},{6,15},{6,17},{6,18},{6,19},{7,0},{7,5},{7,8},{7,10},{7,12},{7,14},
                        {8,0},{8,1},{8,2},{8,3},{8,4},{8,5},{8,7},{8,8},{8,9},{8,10},{8,12},{8,14},{8,16},
                        {8,17},{8,18},{9,9},{9,12},{9,14},{9,16},{10,0},{10,1},{10,3},{10,4},{10,5},
                        {10,6},{10,7},{10,9},{10,11},{10,12},{10,14},{10,16},{10,18},{11,3},{11,4},{11,6},
                        {11,9},{11,11},{11,14},{12,1},{12,2},{12,3},{12,4},{12,6},{12,8},{12,9},{12,11},
                        {12,13},{12,14},{12,15},{12,16},{12,17},{12,18},{12,19},{13,6},{13,8},{13,11},
                        {14,0},{14,1},{14,2},{14,3},{14,4},{14,5},{14,6},{14,8},{14,10},{14,11},{14,13},
                        {14,14},{14,15},{14,16},{14,18},{15,8},{15,10},{15,11},{15,13},{15,17},{15,18},
                        {16,1},{16,2},{16,3},{16,4},{16,5},{16,6},{16,7},{16,8},{16,12},{16,13},{16,14},
                        {16,15},{16,17},{16,18},{17,10},{17,15},{18,1},{18,2},{18,3},{18,4},{18,5},{18,6},
                        {18,7},{18,8},{18,9},{18,10},{18,12},{18,16},{18,17},{18,18},{19,10},{19,14},{19,16}
                        ,{19,17},{19,18}});
            }
            case 4 -> {
                this.LEVEL = 4;
                mapConfig = new MazeMapConfiguration(25,30,24,29);
                this.intPlayerX = 3;
                this.intPlayerY = 1;
                this.dangerZone = new int[mapConfig.INT_TOTAL];
                mapConfig.assignAsBlocks(new int[][]{{0,0},{0,1},{0,2},{0,3},{0,4},{0,6},{0,10},{0,16},{1,4},{1,6},{1,8},{1,9},{1,10},
                        {1,12},{1,13},{1,14},{1,15},{1,16},{1,18},{1,19},{1,20},{1,21},{1,22},{1,23},{1,24},{1,25},{1,27},{1,28},{2,1},{2,2},{2,3},
                        {2,4},{2,6},{2,8},{2,18},{2,19},{2,25},{2,27},{2,28},{3,1},{3,4},{3,6},{3,8},{3,10},{3,11},{3,12},{3,13},{3,15},{3,16},{3,17},
                        {3,18},{3,19},{3,21},{3,23},{3,25},{3,27},{3,28},{4,1},{4,3},{4,4},{4,13},{4,23},{4,25},{5,1},{5,3},{5,4},{5,6},{5,7},{5,8},
                        {5,9},{5,10},{5,11},{5,12},{5,13},{5,14},{5,15},{5,16},{5,17},{5,18},{5,19},{5,20},{5,21},{5,22},{5,23},{5,25},{5,27},{5,28},
                        {6,6},{6,25},{6,27},{6,28},{7,0},{7,1},{7,2},{7,3},{7,4},{7,5},{7,6},{7,8},{7,9},{7,10},{7,11},{7,12},{7,13},{7,14},{7,15},
                        {7,16},{7,17},{7,18},{7,19},{7,20},{7,21},{7,22},{7,23},{7,24},{7,25},{7,27},{7,28},{8,0},{8,8},{9,0},{9,2},{9,4},{9,5},{9,6},
                        {9,7},{9,8},{9,10},{9,11},{9,12},{9,13},{9,14},{9,15},{9,16},{9,17},{9,18},{9,19},{9,20},{9,21},{9,22},{9,23},{9,24},{9,25},
                        {9,26},{9,27},{9,28},{9,29},{10,0},{10,2},{10,8},{10,29},{11,0},{11,2},{11,3},{11,4},{11,5},{11,6},{11,7},{11,8},{11,9},{11,10},
                        {11,11},{11,12},{11,13},{11,14},{11,15},{11,16},{11,17},{11,18},{11,19},{11,20},{11,21},{11,22},{11,23},{11,24},{11,25},{11,26},
                        {11,27},{11,29},{12,0},{12,6},{12,7},{12,11},{12,15},{12,19},{12,23},{12,27},{12,29},{13,0},{13,1},{13,2},{13,3},{13,4},{13,6},
                        {13,7},{13,9},{13,13},{13,17},{13,21},{13,25},{13,29},{14,6},{14,7},{14,9},{14,10},{14,11},{14,12},{14,13},{14,14},{14,15},
                        {14,16},{14,17},{14,18},{14,19},{14,20},{14,21},{14,22},{14,23},{14,24},{14,25},{14,26},{14,27},{14,28},{14,29},{15,1},{15,2},
                        {15,3},{15,4},{15,5},{15,6},{15,7},{16,1},{16,7},{16,8},{16,9},{16,10},{16,11},{16,12},{16,13},{16,14},{16,15},{16,16},{16,17},
                        {16,18},{16,19},{16,20},{16,21},{16,22},{16,23},{16,24},{16,25},{16,26},{16,28},{17,1},{17,3},{17,5},{17,7},{17,26},{18,1},
                        {18,3},{18,5},{18,7},{18,9},{18,10},{18,11},{18,12},{18,13},{18,14},{18,15},{18,16},{18,17},{18,18},{18,19},{18,20},{18,21},
                        {18,22},{18,23},{18,24},{18,26},{18,28},{19,1},{19,5},{19,7},{19,9},{19,26},{20,3},{20,4},{20,5},{20,7},{20,9},{20,11},{20,12},
                        {20,13},{20,14},{20,15},{20,16},{20,17},{20,18},{20,19},{20,20},{20,21},{20,22},{20,23},{20,24},{20,25},{20,26},{20,27},{20,29},
                        {21,1},{21,4},{21,5},{21,7},{21,9},{21,11},{21,15},{21,27},{21,29},{22,1},{22,2},{22,4},{22,5},{22,7},{22,9},{22,11},{22,13},{22,15},
                        {22,17},{22,19},{22,21},{22,23},{22,24},{22,25},{22,26},{22,27},{22,29},{23,4},{23,5},{23,9},{23,11},{23,13},{23,15},{23,17},{23,19},
                        {23,21},{23,23},{23,27},{23,28},{23,29},{24,0},{24,1},{24,2},{24,3},{24,4},{24,5},{24,6},{24,7},{24,8},{24,9},{24,13},{24,17},{24,19},
                        {24,21},{24,25}});
            }
            case 5 -> {
                this.LEVEL = 5;
                mapConfig = new MazeMapConfiguration(50,50,26,26);
                this.dangerZone = new int[mapConfig.INT_TOTAL];
                mapConfig.assignAsBlocks(new int[][]{{0,1},{0,2},{0,3},{0,4},{0,5},{0,6},{0,7},{0,8},{0,9},{0,10},{0,11},{0,12},{0,13},{0,14},{0,15},
                        {0,16},{0,17},{0,18},{0,19},{0,20},{0,21},{0,22},{0,23},{0,24},{0,25},{0,26},{0,27},{0,28},{0,29},{0,30},{0,31},{0,32},{0,33},
                        {0,34},{0,35},{0,36},{0,37},{0,38},{0,39},{0,40},{0,41},{0,42},{0,43},{0,44},{0,45},{0,46},{0,47},{0,48},{0,49},{1,12},{1,19},
                        {1,29},{1,30},{1,31},{1,37},{1,43},{1,44},{1,49},{2,0},{2,2},{2,3},{2,4},{2,6},{2,7},{2,8},{2,11},{2,12},{2,14},{2,15},{2,17},
                        {2,19},{2,21},{2,22},{2,23},{2,24},{2,25},{2,26},{2,27},{2,29},{2,30},{2,31},{2,33},{2,34},{2,35},{2,37},{2,39},{2,41},{2,43},
                        {2,44},{2,46},{2,47},{2,48},{2,49},{3,0},{3,4},{3,10},{3,14},{3,15},{3,17},{3,21},{3,27},{3,31},{3,33},{3,37},{3,39},{3,40},
                        {3,41},{3,49},{4,0},{4,1},{4,2},{4,4},{4,5},{4,6},{4,7},{4,8},{4,9},{4,10},{4,12},{4,13},{4,14},{4,15},{4,17},{4,19},{4,21},
                        {4,23},{4,25},{4,27},{4,28},{4,29},{4,31},{4,33},{4,35},{4,36},{4,37},{4,39},{4,41},{4,43},{4,44},{4,46},{4,47},{4,48},{4,49},
                        {5,0},{5,1},{5,2},{5,10},{5,14},{5,15},{5,17},{5,19},{5,21},{5,23},{5,25},{5,31},{5,33},{5,49},{6,0},{6,1},{6,2},{6,3},{6,4},
                        {6,5},{6,6},{6,7},{6,8},{6,10},{6,11},{6,12},{6,17},{6,19},{6,21},{6,23},{6,25},{6,26},{6,27},{6,28},{6,29},{6,30},{6,31},{6,33},
                        {6,34},{6,35},{6,36},{6,37},{6,38},{6,39},{6,40},{6,41},{6,42},{6,43},{6,44},{6,45},{6,46},{6,47},{6,48},{6,49},{7,0},{7,1},{7,8},
                        {7,14},{7,15},{7,16},{7,17},{7,19},{7,21},{7,25},{7,26},{7,27},{7,28},{7,29},{7,30},{7,48},{7,49},{8,0},{8,1},{8,3},{8,4},{8,5},
                        {8,6},{8,8},{8,9},{8,10},{8,11},{8,12},{8,13},{8,14},{8,15},{8,16},{8,17},{8,19},{8,23},{8,24},{8,25},{8,26},{8,27},{8,28},{8,29},
                        {8,30},{8,32},{8,34},{8,35},{8,36},{8,37},{8,38},{8,39},{8,40},{8,41},{8,43},{8,44},{8,45},{8,47},{8,48},{8,49},{9,0},{9,15},{9,16},
                        {9,17},{9,19},{9,20},{9,21},{9,25},{9,29},{9,30},{9,39},{9,47},{9,48},{9,49},{10,0},{10,2},{10,3},{10,4},{10,5},{10,6},{10,7},{10,8},
                        {10,9},{10,10},{10,11},{10,12},{10,13},{10,15},{10,16},{10,17},{10,19},{10,20},{10,21},{10,22},{10,23},{10,25},{10,27},{10,29},{10,30},
                        {10,31},{10,32},{10,33},{10,34},{10,35},{10,36},{10,37},{10,39},{10,41},{10,42},{10,43},{10,44},{10,45},{10,46},{10,47},{10,48},{10,49},
                        {11,0},{11,13},{11,19},{11,23},{11,27},{11,29},{11,37},{11,39},{11,41},{11,45},{11,49},{12,0},{12,1},{12,2},{12,3},{12,4},{12,5},{12,6},
                        {12,7},{12,8},{12,9},{12,10},{12,11},{12,13},{12,14},{12,15},{12,16},{12,17},{12,18},{12,19},{12,21},{12,23},{12,24},{12,25},{12,26},{12,27},
                        {12,29},{12,31},{12,33},{12,35},{12,37},{12,39},{12,41},{12,43},{12,45},{12,47},{12,49},{13,0},{13,13},{13,21},{13,24},{13,25},{13,26},
                        {13,27},{13,31},{13,35},{13,39},{13,43},{13,47},{13,49},{14,0},{14,2},{14,3},{14,4},{14,5},{14,6},{14,7},{14,8},{14,9},{14,10},{14,11},
                        {14,12},{14,13},{14,15},{14,16},{14,17},{14,18},{14,19},{14,20},{14,21},{14,22},{14,24},{14,25},{14,26},{14,27},{14,28},{14,29},{14,30},
                        {14,31},{14,32},{14,33},{14,34},{14,35},{14,36},{14,37},{14,38},{14,39},{14,40},{14,41},{14,42},{14,43},{14,44},{14,45},{14,46},{14,47},
                        {14,49},{15,0},{15,2},{15,11},{15,15},{15,22},{15,49},{16,0},{16,2},{16,4},{16,5},{16,6},{16,7},{16,8},{16,9},{16,11},{16,13},{16,14},
                        {16,15},{16,17},{16,18},{16,19},{16,20},{16,22},{16,23},{16,24},{16,25},{16,26},{16,27},{16,28},{16,29},{16,30},{16,31},{16,32},{16,33},
                        {16,34},{16,35},{16,36},{16,37},{16,38},{16,39},{16,40},{16,41},{16,42},{16,43},{16,44},{16,45},{16,46},{16,47},{16,49},{17,0},{17,2},
                        {17,9},{17,11},{17,13},{17,17},{17,20},{17,47},{17,49},{18,0},{18,2},{18,3},{18,4},{18,5},{18,6},{18,7},{18,9},{18,11},{18,13},{18,15},
                        {18,16},{18,20},{18,21},{18,22},{18,23},{18,24},{18,25},{18,26},{18,27},{18,28},{18,29},{18,30},{18,31},{18,32},{18,33},{18,34},{18,35},
                        {18,36},{18,37},{18,38},{18,39},{18,40},{18,41},{18,42},{18,43},{18,44},{18,45},{18,47},{18,49},{19,0},{19,7},{19,9},{19,11},{19,13},
                        {19,15},{19,16},{19,18},{19,40},{19,41},{19,45},{19,47},{19,49},{20,0},{20,2},{20,3},{20,4},{20,5},{20,9},{20,11},{20,13},{20,15},{20,16},
                        {20,18},{20,19},{20,20},{20,21},{20,22},{20,23},{20,24},{20,25},{20,26},{20,27},{20,28},{20,29},{20,30},{20,31},{20,32},{20,33},{20,34},
                        {20,35},{20,36},{20,37},{20,38},{20,40},{20,41},{20,43},{20,45},{20,47},{20,49},{21,0},{21,7},{21,8},{21,9},{21,11},{21,13},{21,15},{21,16},
                        {21,18},{21,22},{21,26},{21,33},{21,37},{21,38},{21,40},{21,41},{21,45},{21,47},{21,49},{22,0},{22,1},{22,2},{22,3},{22,4},{22,5},{22,6},
                        {22,7},{22,8},{22,9},{22,11},{22,13},{22,15},{22,18},{22,20},{22,24},{22,26},{22,28},{22,29},{22,30},{22,31},{22,32},{22,33},{22,35},{22,37},
                        {22,38},{22,40},{22,41},{22,43},{22,45},{22,47},{22,49},{23,0},{23,11},{23,13},{23,15},{23,17},{23,18},{23,20},{23,21},{23,22},{23,23},{23,24},
                        {23,26},{23,28},{23,35},{23,40},{23,41},{23,43},{23,45},{23,47},{23,49},{24,0},{24,2},{24,3},{24,4},{24,5},{24,6},{24,7},{24,8},{24,9},
                        {24,11},{24,13},{24,15},{24,18},{24,22},{24,26},{24,30},{24,32},{24,33},{24,34},{24,36},{24,37},{24,39},{24,40},{24,41},{24,43},{24,45},
                        {24,47},{24,49},{25,0},{25,2},{25,7},{25,11},{25,13},{25,15},{25,16},{25,18},{25,19},{25,20},{25,22},{25,24},{25,25},{25,26},{25,27},
                        {25,29},{25,30},{25,32},{25,34},{25,36},{25,37},{25,39},{25,43},{25,45},{25,47},{25,49},{26,0},{26,2},{26,4},{26,5},{26,7},{26,9},
                        {26,11},{26,13},{26,15},{26,18},{26,22},{26,27},{26,30},{26,39},{26,41},{26,42},{26,43},{26,45},{26,47},{26,49},{27,0},{27,2},{27,4},
                        {27,5},{27,7},{27,9},{27,11},{27,13},{27,15},{27,17},{27,18},{27,20},{27,21},{27,22},{27,23},{27,24},{27,25},{27,26},{27,27},{27,28},
                        {27,29},{27,30},{27,31},{27,32},{27,33},{27,34},{27,35},{27,36},{27,37},{27,38},{27,39},{27,41},{27,43},{27,45},{27,47},{27,49},{28,0},
                        {28,4},{28,5},{28,9},{28,11},{28,13},{28,15},{28,18},{28,41},{28,45},{28,47},{28,49},{29,0},{29,1},{29,2},{29,3},{29,4},{29,5},{29,6},
                        {29,7},{29,8},{29,9},{29,11},{29,13},{29,15},{29,16},{29,18},{29,19},{29,20},{29,21},{29,22},{29,23},{29,24},{29,25},{29,26},{29,27},
                        {29,28},{29,29},{29,30},{29,31},{29,32},{29,33},{29,34},{29,35},{29,36},{29,37},{29,38},{29,39},{29,40},{29,41},{29,42},{29,43},{29,45},
                        {29,47},{29,49},{30,0},{30,11},{30,13},{30,15},{30,18},{30,19},{30,23},{30,27},{30,31},{30,36},{30,41},{30,45},{30,47},{30,49},{31,0},
                        {31,1},{31,2},{31,3},{31,4},{31,5},{31,6},{31,7},{31,8},{31,9},{31,10},{31,11},{31,13},{31,15},{31,17},{31,18},{31,19},{31,21},{31,25},
                        {31,29},{31,33},{31,34},{31,38},{31,39},{31,43},{31,44},{31,45},{31,47},{31,49},{32,0},{32,13},{32,15},{32,19},{32,21},{32,22},{32,23},
                        {32,24},{32,25},{32,26},{32,27},{32,28},{32,29},{32,30},{32,31},{32,32},{32,33},{32,34},{32,35},{32,36},{32,37},{32,38},{32,39},{32,40},
                        {32,41},{32,42},{32,43},{32,44},{32,45},{32,47},{32,49},{33,0},{33,2},{33,3},{33,4},{33,5},{33,6},{33,7},{33,8},{33,9},{33,10},{33,11},
                        {33,12},{33,13},{33,15},{33,17},{33,19},{33,45},{33,47},{33,49},{34,0},{34,2},{34,7},{34,15},{34,16},{34,17},{34,19},{34,20},{34,21},
                        {34,22},{34,23},{34,24},{34,25},{34,26},{34,27},{34,28},{34,29},{34,30},{34,31},{34,32},{34,33},{34,34},{34,35},{34,36},{34,37},{34,38},
                        {34,39},{34,40},{34,41},{34,42},{34,43},{34,45},{34,47},{34,49},{35,0},{35,2},{35,4},{35,5},{35,7},{35,9},{35,10},{35,11},{35,12},{35,13},
                        {35,20},{35,21},{35,22},{35,23},{35,24},{35,25},{35,26},{35,27},{35,28},{35,29},{35,30},{35,31},{35,32},{35,33},{35,34},{35,35},{35,36},
                        {35,37},{35,38},{35,39},{35,40},{35,41},{35,45},{35,47},{35,49},{36,0},{36,2},{36,4},{36,7},{36,8},{36,9},{36,10},{36,11},{36,12},{36,13},
                        {36,14},{36,15},{36,16},{36,17},{36,18},{36,19},{36,20},{36,21},{36,29},{36,35},{36,36},{36,41},{36,43},{36,44},{36,45},{36,47},{36,49},
                        {37,0},{37,2},{37,4},{37,6},{37,7},{37,20},{37,21},{37,23},{37,24},{37,25},{37,26},{37,27},{37,29},{37,31},{37,33},{37,35},{37,36},{37,38},
                        {37,39},{37,41},{37,45},{37,47},{37,49},{38,0},{38,2},{38,4},{38,7},{38,9},{38,10},{38,11},{38,12},{38,13},{38,14},{38,16},{38,17},{38,18},
                        {38,20},{38,21},{38,23},{38,24},{38,25},{38,26},{38,27},{38,29},{38,31},{38,33},{38,35},{38,36},{38,38},{38,41},{38,42},{38,43},{38,45},
                        {38,47},{38,49},{39,0},{39,2},{39,4},{39,5},{39,7},{39,9},{39,10},{39,11},{39,12},{39,13},{39,14},{39,16},{39,17},{39,18},{39,20},{39,21},
                        {39,27},{39,29},{39,31},{39,33},{39,35},{39,36},{39,38},{39,40},{39,41},{39,45},{39,47},{39,49},{40,0},{40,2},{40,4},{40,7},{40,14},{40,20},
                        {40,21},{40,23},{40,24},{40,25},{40,27},{40,29},{40,31},{40,33},{40,35},{40,36},{40,38},{40,41},{40,43},{40,44},{40,45},{40,47},{40,49},
                        {41,0},{41,2},{41,4},{41,6},{41,7},{41,8},{41,10},{41,11},{41,12},{41,14},{41,16},{41,17},{41,18},{41,20},{41,21},{41,23},{41,25},{41,27},
                        {41,29},{41,31},{41,33},{41,35},{41,36},{41,38},{41,39},{41,41},{41,45},{41,47},{41,49},{42,0},{42,2},{42,4},{42,6},{42,7},{42,8},{42,10},
                        {42,11},{42,12},{42,14},{42,18},{42,23},{42,27},{42,29},{42,31},{42,33},{42,35},{42,36},{42,38},{42,41},{42,42},{42,43},{42,45},{42,47},
                        {42,49},{43,0},{43,2},{43,4},{43,14},{43,15},{43,16},{43,17},{43,18},{43,19},{43,20},{43,21},{43,22},{43,23},{43,24},{43,25},{43,26},
                        {43,27},{43,31},{43,33},{43,38},{43,45},{43,47},{43,49},{44,0},{44,2},{44,4},{44,5},{44,6},{44,7},{44,8},{44,9},{44,10},{44,11},{44,12},
                        {44,13},{44,14},{44,15},{44,23},{44,24},{44,25},{44,26},{44,27},{44,28},{44,29},{44,30},{44,31},{44,32},{44,33},{44,34},{44,35},{44,36},
                        {44,37},{44,38},{44,39},{44,40},{44,41},{44,42},{44,43},{44,44},{44,45},{44,47},{44,49},{45,0},{45,2},{45,17},{45,18},{45,19},{45,20},
                        {45,21},{45,25},{45,26},{45,31},{45,47},{45,49},{46,0},{46,2},{46,3},{46,4},{46,5},{46,6},{46,7},{46,8},{46,9},{46,10},{46,11},{46,12},
                        {46,13},{46,14},{46,15},{46,16},{46,17},{46,21},{46,22},{46,23},{46,25},{46,26},{46,28},{46,29},{46,31},{46,33},{46,34},{46,35},{46,36},
                        {46,37},{46,38},{46,39},{46,40},{46,41},{46,42},{46,43},{46,44},{46,45},{46,46},{46,47},{46,49},{47,0},{47,19},{47,21},{47,22},{47,23},
                        {47,28},{47,29},{47,31},{47,49},{48,0},{48,1},{48,2},{48,3},{48,4},{48,5},{48,6},{48,7},{48,8},{48,9},{48,10},{48,11},{48,12},{48,13},
                        {48,14},{48,15},{48,16},{48,17},{48,18},{48,19},{48,21},{48,22},{48,23},{48,24},{48,25},{48,26},{48,27},{48,28},{48,29},{48,31},{48,32},
                        {48,34},{48,35},{48,37},{48,38},{48,40},{48,41},{48,43},{48,44},{48,45},{48,47},{48,48},{48,49},{49,0},{49,1},{49,2},{49,3},{49,4},
                        {49,5},{49,6},{49,7},{49,8},{49,9},{49,10},{49,11},{49,12},{49,13},{49,14},{49,15},{49,16},{49,17},{49,18},{49,19},{49,31},{49,32},
                        {49,37},{49,38},{49,40},{49,41},{49,47},{49,48},{49,49}});
            }
        }
    }

    private enum Reason {
        CAUGHT_BY_BOT;
    }

    private void gameLost(Reason R) {
        blnListenToKeyInput = false;
        switch (R) {
            case CAUGHT_BY_BOT -> {
                System.out.println("Caught by bot!!");
            }
        }
    }

    private void checkForGameCompletion() {
        if(intPlayerX == this.mapConfig.INT_END_COL && intPlayerY == this.mapConfig.INT_END_ROW) {
            final double DBL_FADE_MILLIS = 1600.0D;
            Timeline t_fade = new Timeline(new KeyFrame(Duration.millis(DBL_FADE_MILLIS), new KeyValue(imgPlayer.opacityProperty(),0.0D)));
            for(int i = 0; i < mapConfig.INT_TOTAL; i++) {
                t_fade.getKeyFrames().add(new KeyFrame(Duration.millis(DBL_FADE_MILLIS), new KeyValue(imgTile[i].opacityProperty(),0.0D)));
            }
            t_fade.setOnFinished(eh->{
                this.pane_maze.getChildren().clear();
            });
            t_fade.setDelay(Duration.millis(250.0D));
            t_fade.playFromStart();
        } else {
            blnListenToKeyInput = true;
        }
    }

    private final boolean isCaughtByBot() {
        if(LEVEL == 2 || LEVEL == 3) {
            int playerLocation = (intPlayerY*mapConfig.INT_COLUMNS) + intPlayerX;
            for(int i = 0; i < this.dangerZone.length; i++) {
                if(i == playerLocation && this.dangerZone[i] > 0) return true;
            }
        }
        return false;
    }

    private void movePlayer(final String direction) {
        switch (direction) {
            case "W", "UP" -> {
                if(intPlayerY>0 && !mapConfig.blnBlocked[intPlayerY-1][intPlayerX]) {
                    intPlayerY--;
                    Timeline t_move = new Timeline(new KeyFrame(Duration.millis(Values.DBL_PLAYER_MOVE_MILLIS),
                            new KeyValue(imgPlayer.yProperty(), (imgPlayer.getY()-DBL_TILE_SIDE_LENGTH))));
                    t_move.setOnFinished(e->{
                        if(this.isCaughtByBot()) {
                            this.gameLost(Reason.CAUGHT_BY_BOT);
                        } else {
                            checkForGameCompletion();
                        }
                    });
                    t_move.playFromStart();
                } else {
                    Platform.runLater(()->{
                        MediaPlayer mp = new MediaPlayer(res.mSFXInvalidMove.getMedia());
                        mp.play();
                    });
                    blnListenToKeyInput = true;
                }
            }
            case "S", "DOWN" -> {
                if(intPlayerY<mapConfig.INT_ROWS-1 && !mapConfig.blnBlocked[intPlayerY+1][intPlayerX]) {
                    intPlayerY++;
                    Timeline t_move = new Timeline(new KeyFrame(Duration.millis(Values.DBL_PLAYER_MOVE_MILLIS),
                            new KeyValue(imgPlayer.yProperty(), (imgPlayer.getY()+DBL_TILE_SIDE_LENGTH))));
                    t_move.setOnFinished(e->{
                        if(this.isCaughtByBot()) {
                            this.gameLost(Reason.CAUGHT_BY_BOT);
                        } else {
                            checkForGameCompletion();
                        }
                    });
                    t_move.playFromStart();
                } else {
                    Platform.runLater(()->{
                        MediaPlayer mp = new MediaPlayer(res.mSFXInvalidMove.getMedia());
                        mp.play();
                    });
                    blnListenToKeyInput = true;
                }
            }
            case "A", "LEFT" -> {
                if(intPlayerX>0 && !mapConfig.blnBlocked[intPlayerY][intPlayerX-1]) {
                    intPlayerX--;
                    Timeline t_move = new Timeline(new KeyFrame(Duration.millis(Values.DBL_PLAYER_MOVE_MILLIS),
                            new KeyValue(imgPlayer.xProperty(), (imgPlayer.getX()-DBL_TILE_SIDE_LENGTH))));
                    t_move.setOnFinished(e->{
                        if(this.isCaughtByBot()) {
                            this.gameLost(Reason.CAUGHT_BY_BOT);
                        } else {
                            checkForGameCompletion();
                        }
                    });
                    t_move.playFromStart();
                } else {
                    Platform.runLater(()->{
                        MediaPlayer mp = new MediaPlayer(res.mSFXInvalidMove.getMedia());
                        mp.play();
                    });
                    blnListenToKeyInput = true;
                }
            }
            case "D", "RIGHT" -> {
                if(intPlayerX<mapConfig.INT_COLUMNS-1 && !mapConfig.blnBlocked[intPlayerY][intPlayerX+1]) {
                    intPlayerX++;
                    Timeline t_move = new Timeline(new KeyFrame(Duration.millis(Values.DBL_PLAYER_MOVE_MILLIS),
                            new KeyValue(imgPlayer.xProperty(), (imgPlayer.getX()+DBL_TILE_SIDE_LENGTH))));
                    t_move.setOnFinished(e->{
                        if(this.isCaughtByBot()) {
                            this.gameLost(Reason.CAUGHT_BY_BOT);
                        } else {
                            checkForGameCompletion();
                        }
                    });
                    t_move.playFromStart();
                } else {
                    Platform.runLater(()->{
                        MediaPlayer mp = new MediaPlayer(res.mSFXInvalidMove.getMedia());
                        mp.play();
                    });
                    blnListenToKeyInput = true;
                }
            }
            default -> {
                Platform.runLater(()->{
                    MediaPlayer mp = new MediaPlayer(res.mSFXInvalidMove.getMedia());
                    mp.play();
                });
                blnListenToKeyInput = true;
            }
        }
    }

    private void createMazeMap() {
        this.pane_maze = new Pane();
        this.pane_maze.setLayoutX((Values.DBL_WINDOW_WIDTH-Values.DBL_MAP_WIDTH)/2.0D);
        this.pane_maze.setLayoutY((Values.DBL_WINDOW_HEIGHT-Values.DBL_MAP_HEIGHT)/2.0D);
        this.pane_maze.setBackground(new Background(new BackgroundFill(Color.web("#97ef55"), CornerRadii.EMPTY, Insets.EMPTY)));
        final double DBL_CELL_SIDE_LENGTH = Math.max(Math.min(Values.DBL_MAP_WIDTH/mapConfig.INT_COLUMNS, Values.DBL_MAP_HEIGHT/mapConfig.INT_ROWS),30.0D);
        this.DBL_TILE_SIDE_LENGTH = DBL_CELL_SIDE_LENGTH;
        this.pane_maze.setPrefSize(DBL_CELL_SIDE_LENGTH*mapConfig.INT_COLUMNS, DBL_CELL_SIDE_LENGTH*mapConfig.INT_ROWS);
        this.pane_maze.setMinSize(DBL_CELL_SIDE_LENGTH*mapConfig.INT_COLUMNS, DBL_CELL_SIDE_LENGTH*mapConfig.INT_ROWS);
        this.pane_maze.setMaxSize(DBL_CELL_SIDE_LENGTH*mapConfig.INT_COLUMNS, DBL_CELL_SIDE_LENGTH*mapConfig.INT_ROWS);
        res = new ResourceLoader(DBL_CELL_SIDE_LENGTH);
        imgTile = new ImageView[mapConfig.INT_TOTAL];
        double X = 0.0D, Y = 0.0D;
        for(int row = 0; row < mapConfig.INT_ROWS; row++) {
            for(int col = 0; col < mapConfig.INT_COLUMNS; col++) {
                imgTile[(row*mapConfig.INT_COLUMNS)+col] = new ImageView(((mapConfig.blnBlocked[row][col]) ? res.imgTileBlock : res.imgTileNormal));
                imgTile[(row*mapConfig.INT_COLUMNS)+col].setLayoutX(col*DBL_CELL_SIDE_LENGTH);
                imgTile[(row*mapConfig.INT_COLUMNS)+col].setLayoutY(row*DBL_CELL_SIDE_LENGTH);
                imgTile[(row*mapConfig.INT_COLUMNS)+col].setFitWidth(DBL_CELL_SIDE_LENGTH);
                imgTile[(row*mapConfig.INT_COLUMNS)+col].setFitHeight(DBL_CELL_SIDE_LENGTH);
                if(row != mapConfig.INT_END_ROW || col != mapConfig.INT_END_COL) this.pane_maze.getChildren().add(imgTile[(row*mapConfig.INT_COLUMNS)+col]);
            }
        }
        imgPlayer = new ImageView(res.imgCharacter);
        imgPlayer.setLayoutX(5.0D+(this.intPlayerX*DBL_CELL_SIDE_LENGTH));
        imgPlayer.setLayoutY(5.0D+(this.intPlayerY*DBL_CELL_SIDE_LENGTH));
        this.pane_maze.getChildren().add(imgPlayer);
        if (LEVEL == 2 || LEVEL == 3) {
            this.cb = new ChaserBot(res.imgChaserBot,mapConfig.blnBlocked.clone(),9,10);
            this.cb.setLayoutX(5.0D+(10*DBL_CELL_SIDE_LENGTH));
            this.cb.setLayoutY(5.0D+(9*DBL_CELL_SIDE_LENGTH));
            this.pane_maze.getChildren().add(this.cb);
            this.cb.start();
            /*this.cb2 = new ChaserBot(res.imgChaserBot,mapConfig.blnBlocked.clone(),11,15);
            this.cb2.setLayoutX(5.0D+(15*DBL_CELL_SIDE_LENGTH));
            this.cb2.setLayoutY(5.0D+(11*DBL_CELL_SIDE_LENGTH));
            this.pane_maze.getChildren().add(this.cb2);
            this.cb2.start();*/
            if (LEVEL == 3) {
                this.cb3 = new ChaserBot(res.imgChaserBot,mapConfig.blnBlocked.clone(),13,22);
                this.cb3.setLayoutX(5.0D+(22*DBL_CELL_SIDE_LENGTH));
                this.cb3.setLayoutY(5.0D+(13*DBL_CELL_SIDE_LENGTH));
                this.pane_maze.getChildren().add(this.cb3);
                this.cb3.start();
            }
            if (LEVEL == 3) {
                this.ub = new UltraBot(res.imgUltraBot,mapConfig.blnBlocked.clone(),11,15);
                this.ub.setLayoutX(5.0D+(15*DBL_CELL_SIDE_LENGTH));
                this.ub.setLayoutY(5.0D+(11*DBL_CELL_SIDE_LENGTH));
                this.pane_maze.getChildren().add(this.ub);
                this.ub.start();
            }
        } else if (LEVEL == 4) {
            this.cb = new ChaserBot(res.imgChaserBot,mapConfig.blnBlocked.clone(),8,9);
            this.cb.setLayoutX(5.0D+(9*DBL_CELL_SIDE_LENGTH));
            this.cb.setLayoutY(5.0D+(8*DBL_CELL_SIDE_LENGTH));
            this.pane_maze.getChildren().add(this.cb);
            this.cb.start();
            this.cb2 = new ChaserBot(res.imgChaserBot,mapConfig.blnBlocked.clone(),12,1);
            this.cb2.setLayoutX(5.0D+(1*DBL_CELL_SIDE_LENGTH));
            this.cb2.setLayoutY(5.0D+(12*DBL_CELL_SIDE_LENGTH));
            this.pane_maze.getChildren().add(this.cb2);
            this.cb2.start();
        }
        ScrollPane sp = new ScrollPane(pane_maze);
        sp.setPrefSize(1500,750);
        sp.setLayoutX(20.0D);
        sp.setLayoutY(20.0D);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        final double DBL_SCREEN_HEIGHT = java.awt.Toolkit.getDefaultToolkit().getScreenSize().height;
        this.imgPlayer.yProperty().addListener((ol,ov,nv) -> {
            Bounds screenBounds = this.imgPlayer.localToScreen(this.imgPlayer.getBoundsInLocal());
            if(screenBounds.getCenterY() > (DBL_SCREEN_HEIGHT*0.8)) {
                if(sp.getVvalue() + 40.0D/750.0D > 1.0D) sp.setVvalue(1.0D);
                else sp.setVvalue(sp.getVvalue() + 40.0D/750.0D);
            }
            if(screenBounds.getCenterY() < (DBL_SCREEN_HEIGHT*0.2)) {
                if(sp.getVvalue() - 40.0D/750.0D < 0.0D) sp.setVvalue(0.0D);
                else sp.setVvalue(sp.getVvalue() - 40.0D/750.0D);
            }
        });
        sp.setStyle("-fx-background: #420F31;\n -fx-background-color: #420F31");
        this.pane_content.getChildren().add(sp);
    }

    private final class Values {
        private final static double DBL_WINDOW_WIDTH = java.awt.Toolkit.getDefaultToolkit().getScreenSize().width, DBL_WINDOW_HEIGHT = java.awt.Toolkit.getDefaultToolkit().getScreenSize().height - 70.0D;
        //private final static double DBL_WINDOW_WIDTH = 1100.0D, DBL_WINDOW_HEIGHT = 530.0D;
        private final static double DBL_MAP_WIDTH = 0.9*DBL_WINDOW_WIDTH, DBL_MAP_HEIGHT = 0.9*DBL_WINDOW_HEIGHT, DBL_PLAYER_MOVE_MILLIS = 300.0D;
    }

    private final class MazeMapConfiguration {
        private final int INT_ROWS, INT_COLUMNS, INT_TOTAL;
        private final int INT_END_ROW, INT_END_COL;
        private boolean[][] blnBlocked;

        public MazeMapConfiguration(final int ROWS, final int COLUMNS, final int END_X, final int END_Y) {
            this.INT_END_ROW = END_X;
            this.INT_END_COL = END_Y;
            this.INT_ROWS = ROWS;
            this.INT_COLUMNS = COLUMNS;
            this.INT_TOTAL = ROWS*COLUMNS;
            this.blnBlocked = new boolean[ROWS][COLUMNS];
        }

        private final void assignAsBlocks(int[]...loc) {
            for(int[] L : loc) blnBlocked[L[0]][L[1]] = true;
        }
        private final void assignAsPathWay(int[]...loc) {
            for(int[] L : loc) blnBlocked[L[0]][L[1]] = false;
        }
    }

    private class ResourceLoader {

        public final Image imgTileBlock, imgTileNormal, imgCharacter, imgChaserBot, imgUltraBot;
        public final MediaPlayer mSFXInvalidMove;

        public ResourceLoader(final double CELL_SIDE_LENGTH) {
            Media sfx_m = new Media(getClass().getResource("maze/sfx/invalid move.mp3").toString());
            mSFXInvalidMove = new MediaPlayer(sfx_m);
            this.imgTileBlock = new Image(Game.class.getResource("maze/img/tile_blocked.png").toString(), CELL_SIDE_LENGTH, CELL_SIDE_LENGTH, true, true);
            this.imgTileNormal = new Image(Game.class.getResource("maze/img/tile_free.png").toString(), CELL_SIDE_LENGTH, CELL_SIDE_LENGTH, true, true);
            this.imgCharacter = new Image(Game.class.getResource("maze/img/aq_original.png").toString(), CELL_SIDE_LENGTH-10.0D, CELL_SIDE_LENGTH-10.0D, true, true);
            this.imgChaserBot = new Image(Game.class.getResource("maze/img/bk_original.png").toString(), CELL_SIDE_LENGTH-10.0D, CELL_SIDE_LENGTH-10.0D, true, true);
            this.imgUltraBot = new Image(Game.class.getResource("maze/img/gw_original.png").toString(), CELL_SIDE_LENGTH-10.0D, CELL_SIDE_LENGTH-10.0D, true, true);
        }
    }

    public static void main(String[] args) {
        launch();
    }
}