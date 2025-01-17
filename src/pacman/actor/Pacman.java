package pacman.actor;

import pacman.PacmanActor;
import pacman.PacmanGame;
import pacman.PacmanGame.State;
import pacman.infra.Keyboard;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;

/**
 * Pacman class.
 * 
 * @author pc
 */
public class Pacman extends PacmanActor {
    
    public int col;
    public int row;
    public int desiredDirection;
    public int direction;
    public int dx;
    public int dy;
    public long diedTime;
    
    public Pacman(PacmanGame game) {
        super(game);
    }

    @Override
    public void init() {
        String[] pacmanFrameNames = new String[30];
        for (int d=0; d<4; d++) {
            for (int i=0; i<4; i++) {
                pacmanFrameNames[i + 4 * d] = "/images/pacman_" + d + "_" + i + ".png";
            }
        }
        for (int i=0; i<14; i++) {
            pacmanFrameNames[16 + i] = "/images/pacman_died_" + i + ".png";
        }
        loadFrames(pacmanFrameNames);
        reset();
        collider = new Rectangle(0, 0, 8, 8);
    }

    private void reset() {
        col = 18;
        row = 23;
        updatePosition();
        frame = frames[0];
        direction = desiredDirection = 0;
    }
    
    public void updatePosition() {
        x = col * 8 - 4 - 32 - 4;
        y = (row + 3) * 8 - 4;
    }

    private boolean moveToTargetPosition(int targetX, int targetY, int velocity) {
        int sx = (int) (targetX - x);
        int sy = (int) (targetY - y);
        int vx = Math.abs(sx) < velocity ? Math.abs(sx) : velocity;
        int vy = Math.abs(sy) < velocity ? Math.abs(sy) : velocity;
        int idx = vx * (sx == 0 ? 0 : sx > 0 ? 1 : -1);
        int idy = vy * (sy == 0 ? 0 : sy > 0 ? 1 : -1);
        x += idx;
        y += idy;
        return sx != 0 || sy != 0;
    }

    @Override
    public void updateTitle() {
        yield:
        while (true) {
            switch (instructionPointer) {
                case 0:
                    waitTime = System.currentTimeMillis();
                    instructionPointer = 1;
                case 1:
                    if (System.currentTimeMillis() - waitTime < 3000) {
                        break yield;
                    }
                    instructionPointer = 2;
                case 2:
                    direction = 0;
                    if (!moveToTargetPosition(250, 200, 1)) {
                    waitTime = System.currentTimeMillis();
                        instructionPointer = 3;
                    }
                    break yield;
                case 3:
                    if (System.currentTimeMillis() - waitTime < 3000) {
                        break yield;
                    }
                    instructionPointer = 4;
                case 4:
                    direction = 2;
                    if (!moveToTargetPosition(-100, 200, 1)) {
                        instructionPointer = 0;
                    }
                    break yield;
            }
        }
        updateAnimation();
    }
    
    @Override
    public void updatePlaying() {
        if (!visible) {
            return;
        }
        
        if (Keyboard.keyPressed[KeyEvent.VK_LEFT]) {
            desiredDirection = 2;
        }
        else if (Keyboard.keyPressed[KeyEvent.VK_RIGHT]) {
            desiredDirection = 0;
        }
        else if (Keyboard.keyPressed[KeyEvent.VK_UP]) {
            desiredDirection = 3;
        }
        else if (Keyboard.keyPressed[KeyEvent.VK_DOWN]) {
            desiredDirection = 1;
        }
        // mod
        else if (Keyboard.keyPressed[KeyEvent.VK_G] ){
            game.startGhostVulnerableMode();
        }
        
        yield:
        while (true) {
            switch (instructionPointer) {
                case 0:
                    double angle = Math.toRadians(desiredDirection * 90);
                    dx = (int) Math.cos(angle);
                    dy = (int) Math.sin(angle);
                    if (game.maze[row + dy][col + dx] == 0) {
                        direction = desiredDirection;
                    } 
                    
                    angle = Math.toRadians(direction * 90);
                    dx = (int) Math.cos(angle);
                    dy = (int) Math.sin(angle);
                    if (game.maze[row + dy][col + dx] == -1) {
                        break yield;
                    } 
                    
                    col += dx;
                    row += dy;
                    instructionPointer = 1;
                case 1:
                    int targetX = col * 8 - 4 - 32;
                    int targetY = (row + 3) * 8 - 4;
                    int difX = (targetX - (int) x);
                    int difY = (targetY - (int) y);
                    x += difX == 0 ? 0 : difX > 0 ? 1 : -1;
                    y += difY == 0 ? 0 : difY > 0 ? 1 : -1;
                    if (difX == 0 && difY == 0) {
                        instructionPointer = 0;
                        if (col == 1) {
                            col = 34;
                            x = col * 8 - 4 - 24;
                        }
                        else if (col == 34) {
                            col = 1;
                            x = col * 8 - 4 - 24;
                        }
                    }
                    break yield;
            }
        }
        updateAnimation();
        if (game.isLevelCleared()) {
            game.levelCleared();
        }
    }
    
    private void updateAnimation() {
        int frameIndex = 4 * direction + (int) (System.nanoTime() * 0.00000002) % 4;
        frame = frames[frameIndex];
    }
    
    @Override
    public void updatePacmanDied() {
        yield:
        while (true) {
            switch (instructionPointer) {
                case 0:
                    waitTime = System.currentTimeMillis();
                    instructionPointer = 1;
                case 1:
                    if (System.currentTimeMillis() - waitTime < 2000) {
                        break yield;
                    }
                    diedTime = System.currentTimeMillis();
                    instructionPointer = 2;
                case 2:
                    int frameIndex = 16 + (int) ((System.currentTimeMillis() - diedTime) * 0.0075);
                    frame = frames[frameIndex];
                    if (frameIndex == 29) {
                        waitTime = System.currentTimeMillis();
                        instructionPointer = 3;
                    }
                    break yield;
                case 3:
                    if (System.currentTimeMillis() - waitTime < 1500) {
                        break yield;
                    }
                    instructionPointer = 4;
                case 4:
                    game.nextLife();
                    break yield;
            }
        }
    }
    
    @Override
    public void updateCollider() {
        collider.setLocation((int) (x + 4), (int) (y + 4));
    }
    
    // broadcast messages

    @Override
    public void stateChanged() {
        if (game.getState() == PacmanGame.State.TITLE) {
            x = -100;
            y = 200;
            instructionPointer = 0;
            visible = true;
        }
        else if (game.getState() == State.READY) {
            visible = false;
        }
        else if (game.getState() == State.READY2) {
            reset();
        }
        else if (game.getState() == State.PLAYING) {
            instructionPointer = 0;
        }
        else if (game.getState() == State.PACMAN_DIED) {
            instructionPointer = 0;
        }
        else if (game.getState() == State.LEVEL_CLEARED) {
            frame = frames[0];
        }
    }

    public void showAll() {
        visible = true;
    }

    public void hideAll() {
        visible = false;
    }
    
}
