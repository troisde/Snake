package src.main.java.logic;

import src.main.java.configuration.Configs;
import src.main.java.frame.GameBoardPanel;
import src.main.java.frame.MenuPanel;
import src.main.java.frame.OverViewPanel;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

public class Game {

    private static Game game;
    private Rectangle item = new Rectangle();
    private MenuPanel menue = MenuPanel.getInstance();
    private OverViewPanel overview = OverViewPanel.getInstance();
    private GameBoardPanel gameboard = GameBoardPanel.getInstance();
    private Snake snake = Snake.getInstance();
    private Configs configs = Configs.getInstance();

    private ArrayList<Rectangle> temporarysafer = new ArrayList<Rectangle>();

    private boolean gameRunning = true;
    private char direction = ' ';
    private int score = 1;
    private boolean add = false;
    private boolean directionChangeBlocked = false;
    private boolean isAllowed = false;
    private boolean aiControlled = false;
    private boolean stopAi = false;
    private ArrayList<Character> directions = new ArrayList<>();


    private long start = 0;
    private long end = 0;

    /*Singleton pattern*/
    public static Game getInstance() throws InterruptedException {
        if (Game.game == null)
            game = new Game();
        return game;
    }

    private Game() throws InterruptedException {

        /* If the startbutton isnt enabled, the game has been started by pressing a random direction key on the keyboard. In
         *  this case, the starbutton and the AI button has be disabled manually.
         * If the startbutton is enabled, it means the player has started the game by pressing the start button. In this case, the
         * AI Button and the startbutton are disabled by clicking the button.  */
        if (menue.isStartButtonEnabled()) {
            menue.disableStartButton();
            menue.disableAiButton();
        } else {
            setStartDirection();
        }

        /*Every time a new game starts, following things has be set... */
        resetScore();
        resetSnake();
        generateSnakeHead();
        generateItem();

        Thread gameThread = new Thread(() -> {
            try {
                startGame();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        gameThread.start();
    }


    public void startGame() throws InterruptedException {
        start = System.currentTimeMillis();
        if (aiControlled) {
            for(int i  = 0 ; i< 12; i++){
            directions.add('l');
                directions.add('u');
            }
            char savePriorDirection = directions.get(directions.size()-2);

            while (gameRunning) {
                if (directions.size() != 0 ){
                    System.out.println("Size: " + directions.size()+", direction: " + directions.get(0));
                    savePriorDirection = directions.get(directions.size()-1);
                }

                if(directions.size() == 0 ) {

                    generateDirectionsForAI(savePriorDirection);
                }
                gameboard.setSnake(snake.getSnake());
                directionChangeBlocked = false;
                expandSnakeWhenEndOfSnakeReachedItem();
                moveSnake(snake.getSnake(), directions.get(0));
                directions.remove(0);
                if (snakeHeadReachedItem(snake.getSnake())) {
                    temporarysafer.add(new Rectangle(item.x - 1, item.y - 1, 20, 20));
                    generateItem();
                    overview.setScore(++score);

                }
                if (detectCollision(snake.getSnake())  ||  !aiControlled) {
                    reset();
                    aiControlled = false;
                }
                Thread.sleep(configs.getSpeed());
            }
        } else {
            while (gameRunning) {
                directionChangeBlocked = false;
                if (snakeHeadReachedItem(snake.getSnake())) {
                    temporarysafer.add(new Rectangle(item.x - 1, item.y - 1, 20, 20));
                    generateItem();
                    overview.setScore(++score);
                }
                expandSnakeWhenEndOfSnakeReachedItem();
                moveSnake(snake.getSnake(), direction);
                GameBoardPanel.getInstance().setSnake(snake.getSnake());
                if (detectCollision(snake.getSnake()))
                    reset();
                Thread.sleep(configs.getSpeed());
            }
        }
    }

    private void expandSnakeWhenEndOfSnakeReachedItem() {
        if (add) {
            snake.add((int) temporarysafer.get(0).getX(), (int) temporarysafer.get(0).getY(), 20, 20);
            temporarysafer.remove(0);
            add = false;
        }

        if (temporarysafer.size() != 0) {
            if (snake.getTail().getX() == temporarysafer.get(0).getX()
                    && snake.getTail().getY() == temporarysafer.get(0).getY()) {
                add = true;
            }
        }
    }

    public void reset() {
        gameRunning = false;
        menue.enableStartButton();
        menue.enableAiButton();
        Game.game = null;
        end = System.currentTimeMillis();
        overview.setTime(end - start);
    }

    private void generateDirectionsForAI(char PriorDirection) {
           char futureDirection = switch (PriorDirection) {
               case 'u' -> 'r';
               case 'r' -> 'd';
               case 'd' -> 'l';
               case 'l' -> 'u';
               default -> ' ';
           };
        for (int i = 0 ; i < 24; i++){
                directions.add(futureDirection);
            }
    }
    private boolean detectCollision(ArrayList<Rectangle2D> snake) {
        if (snake.get(0).getX() < 0 || snake.get(0).getX() > 480 || snake.get(0).getY() < 0
                || snake.get(0).getY() > 480 || stopAi) return true;
        for (int i = 1; i < snake.size(); i++) {
            if (snake.get(0).getX() == snake.get(i).getX() &&
                    snake.get(0).getY() == snake.get(i).getY()) {
                return true;
            }
        }
        return false;
    }

    /* During the game*/
    private boolean snakeHeadReachedItem(ArrayList<Rectangle2D> snake) {
        return snake.get(0).getX() == item.getX() - 1 && snake.get(0).getY() == item.getY() - 1;
    }
    private void moveSnake(ArrayList<Rectangle2D> snakeList, char direction) {
        /* When the snake moves, every rectangle takes the position of the rectangle on position above*/
        for (int i = snakeList.size() - 1; i > 0; i--) {
            snakeList.get(i).setRect(snakeList.get(i - 1).getX(), snakeList.get(i - 1).getY(), 20,
                    20);
        }
        /*After every rectangle is set, the new position of the snakehead has to be determined by checking the position*/
        switch (direction) {
            case 'u' -> snakeList.get(0).setRect(snakeList.get(0).getX(), snakeList.get(0).getY() - 20, 20, 20);
            case 'd' -> snakeList.get(0).setRect(snakeList.get(0).getX(), snakeList.get(0).getY() + 20, 20, 20);
            case 'l' -> snakeList.get(0).setRect(snakeList.get(0).getX() - 20, snakeList.get(0).getY(), 20, 20);
            case 'r' -> snakeList.get(0).setRect(snakeList.get(0).getX() + 20, snakeList.get(0).getY(), 20, 20);
        }

    }
    void generateItem() {
        double x = 0, y = 0;
        while (!isAllowed) {
            x = ((int) (Math.random() * 100)) % 25 * 20;
            y = ((int) (Math.random() * 100)) % 25 * 20;
            isAllowed = checkItemNotOnSnake(x, y);
        }
        item.setRect(x + 1, y + 1, 18, 18);
        gameboard.setItem(item);
        isAllowed = false;
    }
    private boolean checkItemNotOnSnake(double x, double y) {
        for (int i = 0; i < snake.getSnake().size(); i++) {
            if (snake.getSnake().get(i).getX() == x && snake.getSnake().get(i).getY() == y)
                return false;
        }
        return true;
    }
    public void setDirection(char direction) {
        if (directionChangeBlocked || aiControlled) return;
        if ((this.direction == 'l') && (direction == 'r') || (this.direction == 'r') && (direction == 'l')
                || (this.direction == 'u') && (direction == 'd') || (this.direction == 'd') && (direction == 'u'))
            return;
        this.direction = direction;
        directionChangeBlocked = true;
    }


    /* When a game has startet*/
    private void generateSnakeHead() {
        snake.add(240, 240, 20, 20);
        GameBoardPanel.getInstance().setSnake(snake.getSnake());
    }
    private void setStartDirection() {
        double random = Math.random();
        if (random < .25)
            direction = 'u';
        else if (random < .5)
            direction = 'd';
        else if (random < .75)
            direction = 'l';
        else direction = 'r';
    }
    public void switchAiControlled() {
        aiControlled = !aiControlled;
    }

    /*When a game has ended*/
    private void resetScore() {
        this.score = 1;
    }
    private void resetSnake() {
        snake.clearSnake();
        GameBoardPanel.getInstance().setSnake(snake.getSnake());
        OverViewPanel.getInstance().setScore(score);
    }




}