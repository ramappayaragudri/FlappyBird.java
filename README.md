# 🐦 Flappy Bird - Java Edition

## 🎮 Features

### ✨ Core Gameplay
- **Smooth Physics**: Realistic bird movement with gravity and jumping mechanics
- **Animated Bird**: 3-frame wing animation with rotation based on velocity
- **Collision Detection**: Precise hit detection with pipes and boundaries
- **Score System**: Real-time scoring with visual feedback

### 🎯 Game Modes
- **Normal Mode**: Classic Flappy Bird experience
- **Hard Mode**: Faster pipes, smaller gaps, increased gravity
- **Night Mode**: Dark theme with stars, moon, and adjusted visuals

### 🎨 Visual Features
- **Dynamic Backgrounds**: 
  - Day mode with clouds and blue sky
  - Night mode with stars and moon
- **Animated Elements**:
  - Bird wing flapping animation
  - Scrolling background and ground
  - Smooth pipe movement
- **Professional UI**:
  - Clean score display
  - Mode indicators
  - Interactive pause button

### 🔊 Audio Features
- **Sound Effects**:
  - Jump sound
  - Score sound
  - Collision sound
  - Menu selection sounds

### 💾 Data Persistence
- **High Score Saving**: Automatically saves and loads best score
- **File Storage**: Uses local file system for score persistence

## 🚀 How to Run

### Prerequisites
- Java JDK 8 or higher
- Git (optional)

### Installation & Execution

1. **Clone the repository**:
   ```bash
   git clone https://github.com/yourusername/flappy-bird-java.git
   cd flappy-bird-java
   ```

2. **Compile the game**:
   ```bash
   javac FlappyBird.java
   ```

3. **Run the game**:
   ```bash
   java FlappyBird
   ```

### Alternative: Download and Run
1. Download `FlappyBird.java` file
2. Open terminal/command prompt in the download directory
3. Compile: `javac FlappyBird.java`
4. Run: `java FlappyBird`

## 🎯 Controls

| Key | Action |
|-----|--------|
| `SPACE` | Jump / Start game |
| `P` | Pause/Resume game |
| `R` | Restart game |
| `M` | Return to main menu |
| `1` | Start normal game |
| `2` | Toggle hard mode |
| `3` | Toggle night mode |

## 🏆 Game Modes Explained

### Normal Mode
- Standard pipe speed and gap size
- Regular gravity physics
- Classic Flappy Bird experience

### Hard Mode ⚡
- **30% faster pipe movement**
- **25% smaller pipe gaps** (150px vs 200px)
- **20% increased gravity**
- **10% reduced jump strength**
- Visual "HARD MODE" indicators on pipes

### Night Mode 🌙
- Dark blue gradient sky background
- Twinkling stars and moon
- Adjusted pipe and ground colors
- Softer visual experience

## 🛠️ Technical Details

### Architecture
- **Pure Java**: No external dependencies
- **Swing Framework**: For GUI and rendering
- **Object-Oriented Design**: Clean class structure
- **Event-Driven**: Mouse and keyboard input handling

### Key Components
- `FlappyBird` - Main game class extending JPanel
- `Timer` - Game loop and animation controller
- `Pipe` - Inner class for pipe management
- `Graphics2D` - Advanced rendering with anti-aliasing

### Performance
- **60 FPS** game loop
- Efficient collision detection
- Optimized rendering pipeline
- Memory-efficient object management

## 📁 Project Structure

```
flappy-bird-java/
│
├── FlappyBird.java          # Main game source code
├── README.md               # Project documentation
├── flappybird_highscore.dat # Auto-generated high score file
└── assets/                 # (Optional) Game assets directory
```

## 🎨 Customization

### Easy Modifications
You can easily customize the game by modifying these constants at the top of the `FlappyBird` class:

```java
// Game difficulty
private static final int PIPE_GAP = 200;      // Change gap size
private static final double GRAVITY = 0.5;    // Adjust physics
private static final int PIPE_SPEED = 5;      // Change game speed

// Visual settings
private static final int BIRD_SIZE = 30;      // Change bird size
private static final Color BIRD_COLOR = Color.YELLOW; // Change colors
```

### Adding New Features
The modular code structure makes it easy to add:
- New power-ups
- Additional game modes
- Custom backgrounds
- New sound effects

## 🤝 Contributing

Contributions are welcome! Here's how you can help:

1. Fork the project
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Areas for Improvement
- [ ] Add power-ups system
- [ ] Implement level progression
- [ ] Add multiplayer support
- [ ] Create custom skin system
- [ ] Add particle effects

## 🐛 Troubleshooting

### Common Issues

**Game won't compile:**
- Ensure you have Java JDK installed
- Check that `javac` is in your PATH
- Verify file encoding is UTF-8

**No sound:**
- System beeps require functioning audio output
- Some systems may have beep disabled

**Performance issues:**
- Close other applications
- Ensure sufficient system resources
- Update Java to latest version

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Inspired by the original Flappy Bird game by Dong Nguyen
- Built with Java Swing framework
- Thanks to the Java community for excellent documentation

## 📞 Support

If you encounter any issues or have questions:

1. Check the [Troubleshooting](#🐛-troubleshooting) section
2. Search existing [GitHub Issues](../../issues)
3. Create a new issue with details about your problem

## 🎯 Future Plans

- [ ] Mobile version (Android)
- [ ] Web version (Java Applet/WebStart)
- [ ] Level editor
- [ ] Online leaderboards
- [ ] Custom bird skins

---

<div align="center">

**Enjoy playing!** 🎮

*If you like this project, don't forget to give it a ⭐!*

</div>
