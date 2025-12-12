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
    
    // ========== NEW: Variable Speed System ==========
    private int speedLevel = 1; // 1=Slow, 2=Medium, 3=Fast
    private final int[] SPEED_THRESHOLDS = {5, 15}; // Scores to change speed
    private final double[] BIRD_SPEEDS = {3.0, 4.0, 5.0}; // Pipe movement speeds
    private final double[] JUMP_MODIFIERS = {1.0, 0.9, 0.8}; // Jump strength modifiers
    // ================================================
    
    // Animation
    private int birdAnimationFrame = 0;
    private int animationDelay = 0;
    private int backgroundOffset = 0;
    
    // ========== NEW: Enhanced Background Variables ==========
    private int cloudOffset = 0;
    private int starOffset = 0;
    private ArrayList<Cloud> clouds;
    private ArrayList<Star> stars;
    // ========================================================
    
    // Sound system
    private Clip jumpSound;
    private Clip scoreSound;
    private Clip hitSound;
    private Clip selectSound;
    private Clip speedUpSound;
    private boolean soundsEnabled = true;
    
    // High score file
    private static final String HIGH_SCORE_FILE = "flappybird_highscore.dat";
    
    // ========== NEW: Background Object Classes ==========
    private class Cloud {
        int x, y;
        int width, height;
        int speed;
        
        Cloud(int x, int y, int width, int height, int speed) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.speed = speed;
        }
    }
    
    private class Star {
        int x, y;
        int size;
        float brightness;
        
        Star(int x, int y, int size, float brightness) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.brightness = brightness;
        }
    }
    // ====================================================

    public FlappyBird() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addKeyListener(this);
        
        timer = new Timer(16, this);
        random = new Random();
        
        // ========== NEW: Initialize background objects ==========
        clouds = new ArrayList<>();
        stars = new ArrayList<>();
        initializeClouds();
        initializeStars();
        // ========================================================
        
        loadHighScore();
        loadSounds();
        resetGame();
    }
    
    // ========== NEW: Initialize Clouds ==========
    private void initializeClouds() {
        clouds.clear();
        for (int i = 0; i < 8; i++) {
            int x = random.nextInt(WIDTH * 2);
            int y = random.nextInt(HEIGHT / 3);
            int width = 60 + random.nextInt(80);
            int height = 20 + random.nextInt(30);
            int speed = 1 + random.nextInt(3);
            clouds.add(new Cloud(x, y, width, height, speed));
        }
    }
    
    // ========== NEW: Initialize Stars ==========
    private void initializeStars() {
        stars.clear();
        if (nightMode) {
            for (int i = 0; i < 100; i++) {
                int x = random.nextInt(WIDTH);
                int y = random.nextInt(HEIGHT / 2);
                int size = 1 + random.nextInt(3);
                float brightness = 0.5f + random.nextFloat() * 0.5f;
                stars.add(new Star(x, y, size, brightness));
            }
        }
    }
    // ===========================================
    
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
            jumpSound = loadSoundFromFile("jump.wav");
            scoreSound = loadSoundFromFile("score.wav");
            hitSound = loadSoundFromFile("hit.wav");
            selectSound = loadSoundFromFile("select.wav");
            // ========== NEW: Speed up sound ==========
            speedUpSound = loadSoundFromFile("speedup.wav");
            // ==========================================
            
            if (jumpSound == null) jumpSound = createTone(800, 100, 0.3f);
            if (scoreSound == null) scoreSound = createTone(1200, 150, 0.3f);
            if (hitSound == null) hitSound = createTone(300, 500, 0.5f);
            if (selectSound == null) selectSound = createTone(600, 100, 0.2f);
            // ========== NEW: Fallback speed sound ==========
            if (speedUpSound == null) speedUpSound = createTone(1500, 200, 0.4f);
            // ================================================
            
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
            Toolkit.getDefaultToolkit().beep();
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
        cloudOffset = 0;
        starOffset = 0;
        
        // ========== NEW: Reset speed level ==========
        speedLevel = 1;
        // ===========================================
        
        // Create initial pipes
        for (int i = 0; i < 3; i++) {
            addPipe(WIDTH + i * PIPE_SPACING);
        }
        
        // ========== NEW: Reinitialize background objects ==========
        initializeClouds();
        initializeStars();
        // ========================================================
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
            // Enhanced night background with gradient
            GradientPaint nightGradient = new GradientPaint(
                0, 0, new Color(10, 10, 40), 
                0, HEIGHT/2, new Color(15, 15, 60)
            );
            g2d.setPaint(nightGradient);
            g2d.fillRect(0, 0, WIDTH, HEIGHT/2);
            
            // Draw stars with varying brightness
            for (Star star : stars) {
                int alpha = (int)(255 * star.brightness);
                g2d.setColor(new Color(255, 255, 255, alpha));
                g2d.fillOval(star.x, star.y, star.size, star.size);
                
                // Make some stars twinkle
                if (random.nextInt(100) < 5) {
                    g2d.setColor(new Color(255, 255, 255, alpha + 50));
                    g2d.fillOval(star.x - 1, star.y - 1, star.size + 2, star.size + 2);
                }
            }
            
            // Enhanced moon with craters
            g2d.setColor(new Color(230, 230, 230));
            g2d.fillOval(650, 50, 70, 70);
            g2d.setColor(new Color(210, 210, 210));
            g2d.fillOval(660, 65, 15, 15);
            g2d.fillOval(680, 80, 10, 10);
            g2d.fillOval(665, 90, 8, 8);
            
        } else {
            // Enhanced day background with gradient sky
            GradientPaint skyGradient = new GradientPaint(
                0, 0, new Color(100, 180, 255), 
                0, HEIGHT/2, new Color(176, 226, 255)
            );
            g2d.setPaint(skyGradient);
            g2d.fillRect(0, 0, WIDTH, HEIGHT/2);
            
            // Draw moving clouds
            for (Cloud cloud : clouds) {
                // Main cloud
                g2d.setColor(new Color(255, 255, 255, 200));
                g2d.fillOval(cloud.x, cloud.y, cloud.width, cloud.height);
                g2d.fillOval(cloud.x + cloud.width/3, cloud.y - cloud.height/3, 
                            cloud.width * 2/3, cloud.height);
                g2d.fillOval(cloud.x + cloud.width * 2/3, cloud.y, 
                            cloud.width/2, cloud.height);
                
                // Cloud shadow
                g2d.setColor(new Color(255, 255, 255, 100));
                g2d.fillOval(cloud.x + 5, cloud.y + 5, cloud.width, cloud.height);
            }
            
            // Sun
            g2d.setColor(new Color(255, 255, 200));
            g2d.fillOval(700, 30, 60, 60);
            g2d.setColor(Color.YELLOW);
            g2d.fillOval(705, 35, 50, 50);
        }
        
        // Enhanced forest with varying tree colors
        for (int i = 0; i < 25; i++) {
            int x = (i * 100) % WIDTH;
            int height = 60 + random.nextInt(140);
            int width = 30 + random.nextInt(20);
            
            // Tree trunk
            Color trunkColor = nightMode ? 
                new Color(80 + random.nextInt(40), 50 + random.nextInt(30), 20 + random.nextInt(20)) :
                new Color(101 + random.nextInt(50), 67 + random.nextInt(40), 33 + random.nextInt(20));
            g2d.setColor(trunkColor);
            g2d.fillRect(x + width/2 - 5, HEIGHT/2 - height, 10, height);
            
            // Tree leaves
            Color leafColor = nightMode ?
                new Color(0, 60 + random.nextInt(40), 0) :
                new Color(30 + random.nextInt(40), 120 + random.nextInt(50), 30 + random.nextInt(40));
            g2d.setColor(leafColor);
            g2d.fillOval(x, HEIGHT/2 - height - 30, width, 60);
            g2d.fillOval(x - 10, HEIGHT/2 - height - 10, width + 20, 50);
        }
    }
    
    private void drawPipe(Graphics2D g2d, Pipe pipe) {
        // Pipe color based on speed level
        Color pipeColor;
        switch(speedLevel) {
            case 1: // Slow - Green
                pipeColor = nightMode ? new Color(0, 100, 0) : new Color(0, 180, 0);
                break;
            case 2: // Medium - Yellow/Orange
                pipeColor = nightMode ? new Color(150, 120, 0) : new Color(220, 160, 0);
                break;
            case 3: // Fast - Red
                pipeColor = nightMode ? new Color(150, 0, 0) : new Color(220, 0, 0);
                break;
            default:
                pipeColor = new Color(0, 180, 0);
        }
        
        Color pipeCapColor = new Color(
            Math.max(0, pipeColor.getRed() - 40),
            Math.max(0, pipeColor.getGreen() - 40),
            Math.max(0, pipeColor.getBlue() - 40)
        );
        
        // Top pipe with speed indicator pattern
        g2d.setColor(pipeColor);
        g2d.fillRect(pipe.x, 0, PIPE_WIDTH, pipe.height);
        
        // Draw speed stripes on pipes
        g2d.setColor(new Color(255, 255, 255, 100));
        for (int i = 0; i < pipe.height; i += 20) {
            g2d.fillRect(pipe.x, i, 10, 10);
        }
        
        // Bottom pipe
        int bottomPipeY = pipe.height + (hardMode ? PIPE_GAP - 50 : PIPE_GAP);
        g2d.setColor(pipeColor);
        g2d.fillRect(pipe.x, bottomPipeY, PIPE_WIDTH, HEIGHT - bottomPipeY - GROUND_HEIGHT);
        
        // Speed stripes on bottom pipe
        g2d.setColor(new Color(255, 255, 255, 100));
        for (int i = bottomPipeY; i < HEIGHT - GROUND_HEIGHT; i += 20) {
            g2d.fillRect(pipe.x, i, 10, 10);
        }
        
        // Pipe caps
        g2d.setColor(pipeCapColor);
        g2d.fillRect(pipe.x - 5, pipe.height - 20, PIPE_WIDTH + 10, 20);
        g2d.fillRect(pipe.x - 5, bottomPipeY, PIPE_WIDTH + 10, 20);
        
        // Speed level indicator on pipes
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        String speedText = "SPEED " + speedLevel;
        g2d.drawString(speedText, pipe.x + 15, pipe.height - 5);
        g2d.drawString(speedText, pipe.x + 15, bottomPipeY + 15);
    }
    
    private void drawGround(Graphics2D g2d) {
        // Ground with gradient
        GradientPaint groundGradient = new GradientPaint(
            0, HEIGHT - GROUND_HEIGHT, new Color(120, 60, 20),
            0, HEIGHT, new Color(160, 100, 50)
        );
        g2d.setPaint(groundGradient);
        g2d.fillRect(0, HEIGHT - GROUND_HEIGHT, WIDTH, GROUND_HEIGHT);
        
        // Grass with pattern
        g2d.setColor(nightMode ? new Color(0, 80, 0) : new Color(40, 160, 40));
        for (int i = 0; i < WIDTH; i += 10) {
            int height = 5 + random.nextInt(10);
            g2d.fillRect(i, HEIGHT - GROUND_HEIGHT, 3, height);
        }
        
        // Ground details
        g2d.setColor(new Color(140, 90, 40));
        for (int i = 0; i < WIDTH; i += 15) {
            g2d.fillOval((i + backgroundOffset) % WIDTH, HEIGHT - GROUND_HEIGHT + 20, 8, 4);
        }
    }
    
    private void drawBird(Graphics2D g2d) {
        int birdX = WIDTH / 4 - BIRD_WIDTH / 2;
        
        // Calculate rotation based on velocity
        double rotation = Math.toRadians(Math.min(30, Math.max(-90, birdVelocity * 3)));
        
        AffineTransform oldTransform = g2d.getTransform();
        g2d.rotate(rotation, birdX + BIRD_WIDTH / 2, birdY + BIRD_HEIGHT / 2);
        
        // Bird body color based on speed
        Color birdColor;
        switch(speedLevel) {
            case 1: // Slow - Yellow
                birdColor = Color.YELLOW;
                break;
            case 2: // Medium - Orange
                birdColor = Color.ORANGE;
                break;
            case 3: // Fast - Red
                birdColor = Color.RED;
                break;
            default:
                birdColor = Color.YELLOW;
        }
        
        g2d.setColor(birdColor);
        g2d.fillOval(birdX, (int)birdY - BIRD_HEIGHT / 2, BIRD_WIDTH, BIRD_HEIGHT);
        
        // Wing with faster animation at higher speeds
        g2d.setColor(new Color(200, 100, 0));
        int wingSpeedMultiplier = speedLevel; // Faster wing flap at higher speeds
        int wingOffset = (birdAnimationFrame * wingSpeedMultiplier) % 3;
        wingOffset = wingOffset == 1 ? 3 : (wingOffset == 2 ? -3 : 0);
        g2d.fillOval(birdX + 10, (int)birdY - BIRD_HEIGHT / 2 + 10 + wingOffset, 15, 10);
        
        // Eye
        g2d.setColor(Color.BLACK);
        g2d.fillOval(birdX + BIRD_WIDTH - 15, (int)birdY - BIRD_HEIGHT / 2 + 10, 8, 8);
        g2d.setColor(Color.WHITE);
        g2d.fillOval(birdX + BIRD_WIDTH - 14, (int)birdY - BIRD_HEIGHT / 2 + 11, 3, 3);
        
        // Beak
        g2d.setColor(new Color(255, 140, 0));
        int[] xPoints = {birdX + BIRD_WIDTH - 5, birdX + BIRD_WIDTH + 5, birdX + BIRD_WIDTH - 5};
        int[] yPoints = {(int)birdY, (int)birdY + 4, (int)birdY + 8};
        g2d.fillPolygon(xPoints, yPoints, 3);
        
        // Speed trail effect
        if (speedLevel > 1) {
            g2d.setColor(new Color(255, 255, 0, 100));
            for (int i = 0; i < speedLevel * 2; i++) {
                g2d.fillOval(birdX - i * 5 - 10, (int)birdY - BIRD_HEIGHT / 4, 
                           BIRD_WIDTH/2, BIRD_HEIGHT/2);
            }
        }
        
        g2d.setTransform(oldTransform);
    }
    
    private void drawUI(Graphics2D g2d) {
        // Score with glowing effect
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 40));
        String scoreText = "" + score;
        
        // Score shadow
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.drawString(scoreText, WIDTH / 2 - g2d.getFontMetrics().stringWidth(scoreText) / 2 + 2, 52);
        
        // Main score
        g2d.setColor(Color.WHITE);
        g2d.drawString(scoreText, WIDTH / 2 - g2d.getFontMetrics().stringWidth(scoreText) / 2, 50);
        
        // High score
        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        g2d.drawString("High Score: " + highScore, 20, 30);
        
        // Sound indicator
        g2d.setColor(soundsEnabled ? Color.GREEN : Color.RED);
        g2d.drawString("Sound: " + (soundsEnabled ? "ON" : "OFF"), 20, 50);
        
        // ========== NEW: Speed Level Indicator ==========
        String speedText = "SPEED: ";
        switch(speedLevel) {
            case 1:
                speedText += "SLOW";
                g2d.setColor(Color.GREEN);
                break;
            case 2:
                speedText += "MEDIUM";
                g2d.setColor(Color.ORANGE);
                break;
            case 3:
                speedText += "FAST";
                g2d.setColor(Color.RED);
                break;
        }
        g2d.drawString(speedText, 20, 70);
        // ================================================
        
        // Mode indicators
        if (hardMode) {
            g2d.setColor(Color.RED);
            g2d.drawString("HARD MODE", WIDTH - 120, 30);
        }
        if (nightMode) {
            g2d.setColor(Color.BLUE);
            g2d.drawString("NIGHT MODE", WIDTH - 120, 50);
        }
        
        // Speed level bar
        g2d.setColor(Color.GRAY);
        g2d.fillRect(WIDTH - 150, 70, 100, 10);
        g2d.setColor(speedLevel >= 1 ? Color.GREEN : Color.DARK_GRAY);
        g2d.fillRect(WIDTH - 150, 70, 33, 10);
        g2d.setColor(speedLevel >= 2 ? Color.ORANGE : Color.DARK_GRAY);
        g2d.fillRect(WIDTH - 117, 70, 33, 10);
        g2d.setColor(speedLevel >= 3 ? Color.RED : Color.DARK_GRAY);
        g2d.fillRect(WIDTH - 84, 70, 33, 10);
        
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
        // Enhanced semi-transparent overlay with gradient
        GradientPaint overlayGradient = new GradientPaint(
            0, 0, new Color(0, 0, 0, 180),
            0, HEIGHT, new Color(0, 0, 0, 100)
        );
        g2d.setPaint(overlayGradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
        
        // Title with shadow
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 52));
        String title = "FLAPPY BIRD PRO";
        g2d.drawString(title, WIDTH / 2 - g2d.getFontMetrics().stringWidth(title) / 2 + 3, 153);
        
        g2d.setColor(Color.YELLOW);
        g2d.drawString(title, WIDTH / 2 - g2d.getFontMetrics().stringWidth(title) / 2, 150);
        
        // Menu options with better styling
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        
        int menuY = 250;
        g2d.drawString("1. START GAME", WIDTH / 2 - 100, menuY);
        g2d.drawString("2. HARD MODE: " + (hardMode ? "ON" : "OFF"), WIDTH / 2 - 100, menuY + 40);
        g2d.drawString("3. NIGHT MODE: " + (nightMode ? "ON" : "OFF"), WIDTH / 2 - 100, menuY + 80);
        g2d.drawString("4. SOUND: " + (soundsEnabled ? "ON" : "OFF"), WIDTH / 2 - 100, menuY + 120);
        
        // ========== NEW: Speed System Explanation ==========
        g2d.setColor(Color.CYAN);
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.drawString("SPEED INCREASES WITH SCORE!", WIDTH / 2 - 140, menuY + 160);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        g2d.drawString("Score 5+ : Medium Speed", WIDTH / 2 - 90, menuY + 190);
        g2d.drawString("Score 15+: Fast Speed", WIDTH / 2 - 90, menuY + 210);
        // ===================================================
        
        // Instructions
        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        g2d.drawString("Press SPACE to jump", WIDTH / 2 - 80, menuY + 250);
        g2d.drawString("Press P to pause", WIDTH / 2 - 60, menuY + 280);
        g2d.drawString("Press R to restart", WIDTH / 2 - 65, menuY + 310);
        
        // Draw demo bird with speed colors
        int demoBirdY = 150 + (int)(Math.sin(System.currentTimeMillis() * 0.005) * 20);
        drawDemoBird(g2d, WIDTH / 2 - 150, demoBirdY);
    }
    
    private void drawDemoBird(Graphics2D g2d, int x, int y) {
        // Demo bird showing speed colors
        for (int i = 0; i < 3; i++) {
            Color demoColor;
            switch(i) {
                case 0: demoColor = Color.GREEN; break;
                case 1: demoColor = Color.ORANGE; break;
                case 2: demoColor = Color.RED; break;
                default: demoColor = Color.YELLOW;
            }
            
            g2d.setColor(demoColor);
            g2d.fillOval(x + i * 30, y, BIRD_WIDTH, BIRD_HEIGHT);
            
            g2d.setColor(Color.BLACK);
            g2d.fillOval(x + i * 30 + BIRD_WIDTH - 15, y + 10, 6, 6);
        }
    }
    
    private void drawPauseScreen(Graphics2D g2d) {
        // Enhanced pause screen
        GradientPaint pauseGradient = new GradientPaint(
            0, 0, new Color(0, 0, 0, 200),
            0, HEIGHT, new Color(0, 0, 50, 150)
        );
        g2d.setPaint(pauseGradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
        
        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        String paused = "GAME PAUSED";
        g2d.drawString(paused, WIDTH / 2 - g2d.getFontMetrics().stringWidth(paused) / 2, HEIGHT / 2 - 50);
        
        // Current speed display
        g2d.setColor(Color.CYAN);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        String speedStatus = "Current Speed: ";
        switch(speedLevel) {
            case 1: speedStatus += "SLOW"; break;
            case 2: speedStatus += "MEDIUM"; break;
            case 3: speedStatus += "FAST"; break;
        }
        g2d.drawString(speedStatus, WIDTH / 2 - g2d.getFontMetrics().stringWidth(speedStatus) / 2, HEIGHT / 2);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        g2d.drawString("Press P to resume", WIDTH / 2 - 100, HEIGHT / 2 + 50);
        g2d.drawString("Press R to restart", WIDTH / 2 - 100, HEIGHT / 2 + 90);
        g2d.drawString("Press M for menu", WIDTH / 2 - 100, HEIGHT / 2 + 130);
    }
    
    private void drawGameOverScreen(Graphics2D g2d) {
        // Enhanced game over screen
        GradientPaint gameOverGradient = new GradientPaint(
            0, 0, new Color(100, 0, 0, 200),
            0, HEIGHT, new Color(50, 0, 0, 150)
        );
        g2d.setPaint(gameOverGradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
        
        g2d.setColor(Color.RED);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        String gameOverText = "GAME OVER";
        g2d.drawString(gameOverText, WIDTH / 2 - g2d.getFontMetrics().stringWidth(gameOverText) / 2, HEIGHT / 2 - 80);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        g2d.drawString("Score: " + score, WIDTH / 2 - 70, HEIGHT / 2 - 20);
        g2d.drawString("High Score: " + highScore, WIDTH / 2 - 100, HEIGHT / 2 + 20);
        
        // Speed achieved
        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        String speedAchieved = "Maximum Speed Reached: ";
        switch(speedLevel) {
            case 1: speedAchieved += "SLOW"; break;
            case 2: speedAchieved += "MEDIUM"; break;
            case 3: speedAchieved += "FAST"; break;
        }
        g2d.drawString(speedAchieved, WIDTH / 2 - g2d.getFontMetrics().stringWidth(speedAchieved) / 2, HEIGHT / 2 + 60);
        
        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        g2d.drawString("Press R to play again", WIDTH / 2 - 100, HEIGHT / 2 + 100);
        g2d.drawString("Press M for main menu", WIDTH / 2 - 110, HEIGHT / 2 + 130);
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
        
        // ========== NEW: Update clouds ==========
        for (Cloud cloud : clouds) {
            cloud.x -= cloud.speed;
            if (cloud.x + cloud.width < 0) {
                cloud.x = WIDTH;
                cloud.y = random.nextInt(HEIGHT / 3);
            }
        }
        // ========================================
        
        // ========== NEW: Check and update speed level ==========
        int oldSpeedLevel = speedLevel;
        if (score >= SPEED_THRESHOLDS[1]) {
            speedLevel = 3; // Fast
        } else if (score >= SPEED_THRESHOLDS[0]) {
            speedLevel = 2; // Medium
        } else {
            speedLevel = 1; // Slow
        }
        
        // Play speed up sound when speed level increases
        if (oldSpeedLevel < speedLevel) {
            playSound(speedUpSound);
        }
        // ========================================================
        
        // Update bird physics with speed modifier
        birdVelocity += GRAVITY * (hardMode ? 1.2 : 1.0);
        birdY += birdVelocity;
        
        // Update pipes with variable speed
        for (int i = 0; i < pipes.size(); i++) {
            Pipe pipe = pipes.get(i);
            
            // ========== NEW: Variable pipe speed based on speed level ==========
            double currentPipeSpeed = BIRD_SPEEDS[speedLevel - 1] * (hardMode ? 1.3 : 1.0);
            pipe.x -= currentPipeSpeed;
            // ================================================================
            
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
                    // ========== NEW: Variable jump strength ==========
                    birdVelocity = JUMP_STRENGTH * JUMP_MODIFIERS[speedLevel - 1] * (hardMode ? 0.9 : 1.0);
                    // =================================================
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
                    initializeStars(); // Reinitialize stars for night mode
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
            JFrame frame = new JFrame("Flappy Bird - SPEED EDITION");
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
