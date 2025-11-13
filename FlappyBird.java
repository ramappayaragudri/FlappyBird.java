import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Random;
import java.io.*;
import javax.sound.sampled.*;

public class FlappyBird extends JPanel implements ActionListener, KeyListener {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int GROUND_HEIGHT = 50;
    private static final int BIRD_WIDTH = 40;
    private static final int BIRD_HEIGHT = 30;
    private static final int PIPE_WIDTH = 80;
    private static final int PIPE_GAP = 200;
    private static final int PIPE_SPACING = 300;
    private static final double GRAVITY = 0.5;
    private static final double JUMP_STRENGTH = -10;
    
    private Timer timer;
    private double birdY;
    private double birdVelocity;
    private ArrayList<Pipe> pipes;
    private Random random;
    private int score;
    private int highScore;
    private boolean gameOver;
    private boolean gameStarted;
    private boolean gamePaused;
    private boolean hardMode;
    private boolean nightMode;
    
    // Animation
    private int birdAnimationFrame = 0;
    private int animationDelay = 0;
    private int backgroundOffset = 0;
    
    // Sound system
    private Clip jumpSound;
    private Clip scoreSound;
    private Clip hitSound;
    private Clip selectSound;
    private boolean soundsEnabled = true;
    
    // High score file
    private static final String HIGH_SCORE_FILE = "flappybird_highscore.dat";

    public FlappyBird() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addKeyListener(this);
        
        timer = new Timer(16, this);
        random = new Random();
        
        loadHighScore();
        loadSounds();
        resetGame();
    }
    
    private void loadHighScore() {
        try {
            File file = new File(HIGH_SCORE_FILE);
            if (file.exists()) {
                DataInputStream dis = new DataInputStream(new FileInputStream(file));
                highScore = dis.readInt();
                dis.close();
            }
        } catch (IOException e) {
            highScore = 0;
        }
    }
    
    private void saveHighScore() {
        try {
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(HIGH_SCORE_FILE));
            dos.writeInt(highScore);
            dos.close();
        } catch (IOException e) {
            System.err.println("Could not save high score: " + e.getMessage());
        }
    }
    
    private void loadSounds() {
        try {
            // Try to load from WAV files first
            jumpSound = loadSoundFromFile("jump.wav");
            scoreSound = loadSoundFromFile("score.wav");
            hitSound = loadSoundFromFile("hit.wav");
            selectSound = loadSoundFromFile("select.wav");
            
            // If files don't exist, create synthetic sounds
            if (jumpSound == null) jumpSound = createTone(800, 100, 0.3f);
            if (scoreSound == null) scoreSound = createTone(1200, 150, 0.3f);
            if (hitSound == null) hitSound = createTone(300, 500, 0.5f);
            if (selectSound == null) selectSound = createTone(600, 100, 0.2f);
            
            System.out.println("Sounds loaded successfully!");
            
        } catch (Exception e) {
            System.err.println("Error loading sounds: " + e.getMessage());
            System.out.println("Falling back to system beeps...");
            soundsEnabled = false;
        }
    }
    
    private Clip loadSoundFromFile(String filename) {
        try {
            File soundFile = new File(filename);
            if (!soundFile.exists()) {
                System.out.println("Sound file not found: " + filename);
                return null;
            }
            
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            return clip;
            
        } catch (Exception e) {
            System.err.println("Error loading sound file " + filename + ": " + e.getMessage());
            return null;
        }
    }
    
    private Clip createTone(int frequency, int duration, float volume) throws LineUnavailableException {
        Clip clip = AudioSystem.getClip();
        AudioFormat format = new AudioFormat(44100, 8, 1, true, true);
        byte[] buffer = new byte[(int)(format.getFrameRate() * duration / 1000)];
        
        for (int i = 0; i < buffer.length; i++) {
            double angle = i / (format.getFrameRate() / frequency) * 2.0 * Math.PI;
            buffer[i] = (byte)(Math.sin(angle) * 127.0 * volume);
        }
        
        clip.open(format, buffer, 0, buffer.length);
        return clip;
    }
    
    private void playSound(Clip sound) {
        if (!soundsEnabled || sound == null) {
            // Fallback to system beep
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        
        try {
            if (sound.isRunning()) {
                sound.stop();
            }
            sound.setFramePosition(0);
            sound.start();
        } catch (Exception e) {
            System.err.println("Error playing sound: " + e.getMessage());
            Toolkit.getDefaultToolkit().beep(); // Fallback
        }
    }
    
    private void resetGame() {
        birdY = HEIGHT / 2;
        birdVelocity = 0;
        pipes = new ArrayList<>();
        score = 0;
        gameOver = false;
        gameStarted = false;
        gamePaused = false;
        birdAnimationFrame = 0;
        backgroundOffset = 0;
        
        // Create initial pipes
        for (int i = 0; i < 3; i++) {
            addPipe(WIDTH + i * PIPE_SPACING);
        }
    }
    
    private void addPipe(int x) {
        int minHeight = hardMode ? 50 : 100;
        int maxHeight = HEIGHT - PIPE_GAP - GROUND_HEIGHT - (hardMode ? 50 : 100);
        int pipeHeight = random.nextInt(maxHeight - minHeight) + minHeight;
        pipes.add(new Pipe(x, pipeHeight));
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw background
        drawBackground(g2d);
        
        // Draw pipes
        for (Pipe pipe : pipes) {
            drawPipe(g2d, pipe);
        }
        
        // Draw ground
        drawGround(g2d);
        
        // Draw bird
        if (gameStarted && !gameOver) {
            drawBird(g2d);
        }
        
        // Draw UI
        drawUI(g2d);
        
        // Draw appropriate screen
        if (!gameStarted) {
            drawStartScreen(g2d);
        } else if (gamePaused) {
            drawPauseScreen(g2d);
        } else if (gameOver) {
            drawGameOverScreen(g2d);
        }
    }
    
    private void drawBackground(Graphics2D g2d) {
        if (nightMode) {
            // Night background
            GradientPaint skyGradient = new GradientPaint(0, 0, new Color(0, 0, 80), 0, HEIGHT/2, new Color(25, 25, 112));
            g2d.setPaint(skyGradient);
            g2d.fillRect(0, 0, WIDTH, HEIGHT/2);
            
            // Stars
            g2d.setColor(Color.WHITE);
            for (int i = 0; i < 50; i++) {
                int x = (i * 16) % WIDTH;
                int y = (i * 7) % (HEIGHT/2);
                g2d.fillOval(x, y, 1, 1);
            }
            
            // Moon
            g2d.setColor(new Color(240, 240, 240));
            g2d.fillOval(650, 50, 60, 60);
        } else {
            // Day background
            GradientPaint skyGradient = new GradientPaint(0, 0, new Color(135, 206, 235), 0, HEIGHT/2, new Color(176, 226, 255));
            g2d.setPaint(skyGradient);
            g2d.fillRect(0, 0, WIDTH, HEIGHT/2);
            
            // Clouds
            g2d.setColor(Color.WHITE);
            for (int i = 0; i < 5; i++) {
                int x = (backgroundOffset + i * 200) % (WIDTH + 200) - 100;
                int y = 50 + (i * 30) % 100;
                g2d.fillOval(x, y, 80, 40);
                g2d.fillOval(x + 30, y - 10, 70, 35);
            }
        }
        
        // Forest (same for both modes)
        g2d.setColor(nightMode ? new Color(0, 50, 0) : new Color(34, 139, 34));
        for (int i = 0; i < 20; i++) {
            int x = (i * 120) % WIDTH;
            int height = 80 + random.nextInt(120);
            g2d.fillRect(x, HEIGHT/2 - height, 40, height);
        }
    }
    
    private void drawPipe(Graphics2D g2d, Pipe pipe) {
        Color pipeColor = nightMode ? new Color(0, 100, 0) : new Color(0, 180, 0);
        Color pipeCapColor = nightMode ? new Color(0, 80, 0) : new Color(0, 140, 0);
        
        // Top pipe
        g2d.setColor(pipeColor);
        g2d.fillRect(pipe.x, 0, PIPE_WIDTH, pipe.height);
        
        // Bottom pipe
        int bottomPipeY = pipe.height + (hardMode ? PIPE_GAP - 50 : PIPE_GAP);
        g2d.fillRect(pipe.x, bottomPipeY, PIPE_WIDTH, HEIGHT - bottomPipeY - GROUND_HEIGHT);
        
        // Pipe caps
        g2d.setColor(pipeCapColor);
        g2d.fillRect(pipe.x - 5, pipe.height - 20, PIPE_WIDTH + 10, 20);
        g2d.fillRect(pipe.x - 5, bottomPipeY, PIPE_WIDTH + 10, 20);
        
        // Hard mode indicator
        if (hardMode) {
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString("HARD", pipe.x + 20, pipe.height - 10);
            g2d.drawString("MODE", pipe.x + 18, bottomPipeY + 20);
        }
    }
    
    private void drawGround(Graphics2D g2d) {
        // Ground
        g2d.setColor(new Color(139, 69, 19));
        g2d.fillRect(0, HEIGHT - GROUND_HEIGHT, WIDTH, GROUND_HEIGHT);
        
        // Grass
        g2d.setColor(nightMode ? new Color(0, 100, 0) : new Color(34, 139, 34));
        g2d.fillRect(0, HEIGHT - GROUND_HEIGHT, WIDTH, 10);
        
        // Ground pattern
        g2d.setColor(new Color(160, 82, 45));
        for (int i = 0; i < WIDTH; i += 20) {
            g2d.fillRect((i + backgroundOffset) % WIDTH, HEIGHT - GROUND_HEIGHT + 15, 10, 5);
        }
    }
    
    private void drawBird(Graphics2D g2d) {
        int birdX = WIDTH / 4 - BIRD_WIDTH / 2;
        
        // Calculate rotation based on velocity
        double rotation = Math.toRadians(Math.min(30, Math.max(-90, birdVelocity * 3)));
        
        AffineTransform oldTransform = g2d.getTransform();
        g2d.rotate(rotation, birdX + BIRD_WIDTH / 2, birdY + BIRD_HEIGHT / 2);
        
        // Bird body
        g2d.setColor(Color.YELLOW);
        g2d.fillOval(birdX, (int)birdY - BIRD_HEIGHT / 2, BIRD_WIDTH, BIRD_HEIGHT);
        
        // Wing animation
        g2d.setColor(Color.ORANGE);
        int wingOffset = (birdAnimationFrame == 1) ? 2 : (birdAnimationFrame == 2) ? -2 : 0;
        g2d.fillOval(birdX + 10, (int)birdY - BIRD_HEIGHT / 2 + 10 + wingOffset, 15, 8);
        
        // Eye
        g2d.setColor(Color.BLACK);
        g2d.fillOval(birdX + BIRD_WIDTH - 15, (int)birdY - BIRD_HEIGHT / 2 + 10, 6, 6);
        g2d.setColor(Color.WHITE);
        g2d.fillOval(birdX + BIRD_WIDTH - 14, (int)birdY - BIRD_HEIGHT / 2 + 11, 2, 2);
        
        // Beak
        g2d.setColor(Color.ORANGE);
        int[] xPoints = {birdX + BIRD_WIDTH - 5, birdX + BIRD_WIDTH, birdX + BIRD_WIDTH - 5};
        int[] yPoints = {(int)birdY, (int)birdY + 3, (int)birdY + 6};
        g2d.fillPolygon(xPoints, yPoints, 3);
        
        g2d.setTransform(oldTransform);
    }
    
    private void drawUI(Graphics2D g2d) {
        // Score
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        String scoreText = "" + score;
        g2d.drawString(scoreText, WIDTH / 2 - g2d.getFontMetrics().stringWidth(scoreText) / 2, 50);
        
        // High score
        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        g2d.drawString("High Score: " + highScore, 20, 30);
        
        // Sound indicator
        g2d.setColor(soundsEnabled ? Color.GREEN : Color.RED);
        g2d.drawString("Sound: " + (soundsEnabled ? "ON" : "OFF"), 20, 50);
        
        // Mode indicators
        if (hardMode) {
            g2d.setColor(Color.RED);
            g2d.drawString("HARD MODE", WIDTH - 120, 30);
        }
        if (nightMode) {
            g2d.setColor(Color.BLUE);
            g2d.drawString("NIGHT MODE", WIDTH - 120, 50);
        }
        
        // Pause button
        if (gameStarted && !gameOver) {
            g2d.setColor(new Color(255, 255, 255, 150));
            g2d.fillRoundRect(WIDTH - 50, 10, 40, 40, 10, 10);
            g2d.setColor(Color.BLACK);
            g2d.fillRect(WIDTH - 40, 20, 5, 20);
            g2d.fillRect(WIDTH - 30, 20, 5, 20);
        }
    }
    
    private void drawStartScreen(Graphics2D g2d) {
        // Semi-transparent overlay
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
        
        // Title
        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        String title = "FLAPPY BIRD";
        g2d.drawString(title, WIDTH / 2 - g2d.getFontMetrics().stringWidth(title) / 2, 150);
        
        // Menu options
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        
        int menuY = 250;
        g2d.drawString("1. START GAME", WIDTH / 2 - 100, menuY);
        g2d.drawString("2. HARD MODE: " + (hardMode ? "ON" : "OFF"), WIDTH / 2 - 100, menuY + 40);
        g2d.drawString("3. NIGHT MODE: " + (nightMode ? "ON" : "OFF"), WIDTH / 2 - 100, menuY + 80);
        g2d.drawString("4. SOUND: " + (soundsEnabled ? "ON" : "OFF"), WIDTH / 2 - 100, menuY + 120);
        
        // Instructions
        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        g2d.drawString("Press SPACE to jump", WIDTH / 2 - 80, menuY + 180);
        g2d.drawString("Press P to pause", WIDTH / 2 - 60, menuY + 210);
        g2d.drawString("Press R to restart", WIDTH / 2 - 65, menuY + 240);
        
        // Draw demo bird
        int demoBirdY = 150 + (int)(Math.sin(System.currentTimeMillis() * 0.005) * 20);
        drawDemoBird(g2d, WIDTH / 2 - 150, demoBirdY);
    }
    
    private void drawDemoBird(Graphics2D g2d, int x, int y) {
        g2d.setColor(Color.YELLOW);
        g2d.fillOval(x, y, BIRD_WIDTH * 2, BIRD_HEIGHT * 2);
        
        g2d.setColor(Color.ORANGE);
        g2d.fillOval(x + 20, y + 20, 30, 16);
        
        g2d.setColor(Color.BLACK);
        g2d.fillOval(x + BIRD_WIDTH * 2 - 30, y + 20, 12, 12);
    }
    
    private void drawPauseScreen(Graphics2D g2d) {
        // Semi-transparent overlay
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
        
        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        String paused = "PAUSED";
        g2d.drawString(paused, WIDTH / 2 - g2d.getFontMetrics().stringWidth(paused) / 2, HEIGHT / 2 - 50);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        g2d.drawString("Press P to resume", WIDTH / 2 - 100, HEIGHT / 2 + 20);
        g2d.drawString("Press R to restart", WIDTH / 2 - 100, HEIGHT / 2 + 60);
        g2d.drawString("Press M for menu", WIDTH / 2 - 100, HEIGHT / 2 + 100);
    }
    
    private void drawGameOverScreen(Graphics2D g2d) {
        // Semi-transparent overlay
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
        
        g2d.setColor(Color.RED);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        String gameOverText = "GAME OVER";
        g2d.drawString(gameOverText, WIDTH / 2 - g2d.getFontMetrics().stringWidth(gameOverText) / 2, HEIGHT / 2 - 80);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        g2d.drawString("Score: " + score, WIDTH / 2 - 70, HEIGHT / 2 - 20);
        g2d.drawString("High Score: " + highScore, WIDTH / 2 - 100, HEIGHT / 2 + 20);
        
        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        g2d.drawString("Press R to play again", WIDTH / 2 - 100, HEIGHT / 2 + 70);
        g2d.drawString("Press M for main menu", WIDTH / 2 - 110, HEIGHT / 2 + 100);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameStarted || gameOver || gamePaused) return;
        
        // Update animations
        animationDelay++;
        if (animationDelay >= 5) {
            birdAnimationFrame = (birdAnimationFrame + 1) % 3;
            animationDelay = 0;
        }
        
        // Scroll background
        backgroundOffset = (backgroundOffset + 1) % WIDTH;
        
        // Update bird physics
        birdVelocity += GRAVITY * (hardMode ? 1.2 : 1.0);
        birdY += birdVelocity;
        
        // Update pipes
        for (int i = 0; i < pipes.size(); i++) {
            Pipe pipe = pipes.get(i);
            pipe.x -= 5 * (hardMode ? 1.3 : 1.0);
            
            // Check if pipe passed bird
            if (!pipe.passed && pipe.x + PIPE_WIDTH < WIDTH / 4) {
                pipe.passed = true;
                score++;
                
                // Update high score
                if (score > highScore) {
                    highScore = score;
                    saveHighScore();
                }
                
                // Play score sound
                playSound(scoreSound);
            }
            
            // Remove off-screen pipes and add new ones
            if (pipe.x + PIPE_WIDTH < 0) {
                pipes.remove(i);
                addPipe(pipes.get(pipes.size() - 1).x + PIPE_SPACING);
            }
            
            // Check collision
            if (checkCollision(pipe)) {
                gameOver = true;
                playSound(hitSound);
                timer.stop();
                return;
            }
        }
        
        // Check ground and ceiling collision
        if (birdY + BIRD_HEIGHT / 2 > HEIGHT - GROUND_HEIGHT || birdY - BIRD_HEIGHT / 2 < 0) {
            gameOver = true;
            playSound(hitSound);
            timer.stop();
        }
        
        repaint();
    }
    
    private boolean checkCollision(Pipe pipe) {
        int birdLeft = WIDTH / 4 - BIRD_WIDTH / 2 + 5;
        int birdRight = WIDTH / 4 + BIRD_WIDTH / 2 - 5;
        int birdTop = (int)birdY - BIRD_HEIGHT / 2 + 5;
        int birdBottom = (int)birdY + BIRD_HEIGHT / 2 - 5;
        
        int pipeLeft = pipe.x + 5;
        int pipeRight = pipe.x + PIPE_WIDTH - 5;
        int pipeGap = hardMode ? PIPE_GAP - 50 : PIPE_GAP;
        int pipeTopBottom = pipe.height;
        int pipeBottomTop = pipe.height + pipeGap;
        
        if (birdRight > pipeLeft && birdLeft < pipeRight) {
            if (birdTop < pipeTopBottom || birdBottom > pipeBottomTop) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        
        switch (key) {
            case KeyEvent.VK_SPACE:
                if (!gameStarted && !gameOver) {
                    gameStarted = true;
                    timer.start();
                    playSound(selectSound);
                } else if (gameStarted && !gameOver && !gamePaused) {
                    birdVelocity = JUMP_STRENGTH * (hardMode ? 0.9 : 1.0);
                    playSound(jumpSound);
                }
                break;
                
            case KeyEvent.VK_P:
                if (gameStarted && !gameOver) {
                    gamePaused = !gamePaused;
                    playSound(selectSound);
                    repaint();
                }
                break;
                
            case KeyEvent.VK_R:
                if (gameOver || gamePaused) {
                    resetGame();
                    if (gameStarted) {
                        timer.start();
                    }
                    playSound(selectSound);
                    repaint();
                }
                break;
                
            case KeyEvent.VK_M:
                if (gameOver || gamePaused) {
                    resetGame();
                    playSound(selectSound);
                    repaint();
                }
                break;
                
            case KeyEvent.VK_1:
                if (!gameStarted) {
                    hardMode = false;
                    nightMode = false;
                    playSound(selectSound);
                    repaint();
                }
                break;
                
            case KeyEvent.VK_2:
                if (!gameStarted) {
                    hardMode = !hardMode;
                    playSound(selectSound);
                    repaint();
                }
                break;
                
            case KeyEvent.VK_3:
                if (!gameStarted) {
                    nightMode = !nightMode;
                    playSound(selectSound);
                    repaint();
                }
                break;
                
            case KeyEvent.VK_4:
                if (!gameStarted) {
                    soundsEnabled = !soundsEnabled;
                    playSound(selectSound);
                    repaint();
                }
                break;
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {}
    
    @Override
    public void keyTyped(KeyEvent e) {}
    
    private class Pipe {
        int x;
        int height;
        boolean passed;
        
        Pipe(int x, int height) {
            this.x = x;
            this.height = height;
            this.passed = false;
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Flappy Bird - Ultimate Edition");
            FlappyBird game = new FlappyBird();
            
            frame.add(game);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.setVisible(true);
            
            game.requestFocusInWindow();
        });
    }
}