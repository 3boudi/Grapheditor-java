import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * A modern graph/shape editor application using Java Swing
 */
public class graph extends JFrame {
    // Constants
    private static final int DEFAULT_WIDTH = 800;
    private static final int DEFAULT_HEIGHT = 600;
    private static final Color DEFAULT_BG_COLOR = Color.WHITE;

    // Canvas and drawing elements
    private DrawingCanvas canvas;
    private BufferedImage image;
    private Graphics2D g2d;

    // Shape properties
    private Shape currentShape;
    private Color currentColor = Color.BLACK;
    private boolean fillShape = false;
    private float strokeWidth = 2.0f;

    // Shape history for undo functionality
    private List<DrawnShape> shapes = new ArrayList<>();
    private DrawnShape activeShape;

    // Tracking mouse positions
    private Point startPoint;

    /**
     * Constructor: Sets up the graph editor application
     */
    public graph() {
        setTitle("Modern Graph Editor");
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center on screen

        // Initialize the drawing canvas
        canvas = new DrawingCanvas();
        canvas.setBackground(DEFAULT_BG_COLOR);

        // Set up event listeners
        canvas.addMouseListener(new ShapeMouseListener());
        canvas.addMouseMotionListener(new ShapeMotionListener());

        // Create the drawing image
        createDrawingImage();

        // Set up the UI components
        setupUI();

        // Make the frame visible
        setVisible(true);
    }

    /**
     * Sets up the user interface including toolbar and status bar
     */
    private void setupUI() {
        // Use a border layout for the main frame
        setLayout(new BorderLayout());

        // Add the canvas in the center
        add(new JScrollPane(canvas), BorderLayout.CENTER);

        // Create toolbar with buttons
        JToolBar toolBar = createToolBar();
        add(toolBar, BorderLayout.NORTH);

        // Create status bar
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusBar.setBorder(BorderFactory.createEtchedBorder());
        JLabel statusLabel = new JLabel("Ready");
        statusBar.add(statusLabel);
        add(statusBar, BorderLayout.SOUTH);
    }

    /**
     * Creates and initializes the drawing image buffer
     */
    private void createDrawingImage() {
        image = new BufferedImage(DEFAULT_WIDTH, DEFAULT_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setColor(DEFAULT_BG_COLOR);
        g2d.fillRect(0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * Creates the toolbar with drawing tools
     */
    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        // Shape buttons
        JButton rectangleButton = createToolButton("Rectangle", "/icons/rectangle.png",
                e -> setCurrentShape(new Rectangle2D.Double()));

        JButton ellipseButton = createToolButton("Ellipse", "/icons/ellipse.png",
                e -> setCurrentShape(new Ellipse2D.Double()));

        // Color selection button
        JButton colorButton = createToolButton("Color", "/icons/color.png", e -> changeColor());

        // Fill checkbox
        JCheckBox fillCheckBox = new JCheckBox("Fill Shape");
        fillCheckBox.addActionListener(e -> fillShape = fillCheckBox.isSelected());

        // Stroke width selector
        JComboBox<Float> strokeWidthCombo = new JComboBox<>(new Float[] {1.0f, 2.0f, 3.0f, 5.0f, 8.0f});
        strokeWidthCombo.setSelectedItem(strokeWidth);
        strokeWidthCombo.addActionListener(e -> strokeWidth = (Float) strokeWidthCombo.getSelectedItem());

        // Editing buttons
        JButton undoButton = createToolButton("Undo", "/icons/undo.png", e -> undoLastShape());
        JButton clearButton = createToolButton("Clear", "/icons/clear.png", e -> clearCanvas());

        // Add components to toolbar
        toolBar.add(rectangleButton);
        toolBar.add(ellipseButton);
        toolBar.addSeparator();
        toolBar.add(colorButton);
        toolBar.add(new JLabel(" Stroke: "));
        toolBar.add(strokeWidthCombo);
        toolBar.add(fillCheckBox);
        toolBar.addSeparator();
        toolBar.add(undoButton);
        toolBar.add(clearButton);

        return toolBar;
    }

    /**
     * Helper method to create toolbar buttons
     */
    private JButton createToolButton(String text, String iconPath, ActionListener action) {
        JButton button = new JButton(text);
        try {
            // In a real app, you would load actual icons
            // ImageIcon icon = new ImageIcon(getClass().getResource(iconPath));
            // button.setIcon(icon);
            // button.setText("");
            // button.setToolTipText(text);
        } catch (Exception e) {
            // Use text if icon fails to load
        }
        button.addActionListener(action);
        return button;
    }

    /**
     * Sets the current shape type to draw
     */
    private void setCurrentShape(Shape shape) {
        currentShape = shape;
    }

    /**
     * Opens a color chooser dialog to select drawing color
     */
    private void changeColor() {
        Color newColor = JColorChooser.showDialog(this, "Choose Drawing Color", currentColor);
        if (newColor != null) {
            currentColor = newColor;
        }
    }

    /**
     * Removes the last drawn shape (undo operation)
     */
    private void undoLastShape() {
        if (!shapes.isEmpty()) {
            shapes.remove(shapes.size() - 1);
            redrawShapes();
        }
    }

    /**
     * Clears the entire canvas
     */
    private void clearCanvas() {
        shapes.clear();
        g2d.setColor(canvas.getBackground());
        g2d.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.repaint();
    }

    /**
     * Redraws all shapes on the canvas from the shapes list
     */
    private void redrawShapes() {
        // Clear the canvas
        g2d.setColor(canvas.getBackground());
        g2d.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Redraw all shapes
        for (DrawnShape shape : shapes) {
            g2d.setColor(shape.color);
            g2d.setStroke(new BasicStroke(shape.strokeWidth));

            if (shape.filled) {
                g2d.fill(shape.shape);
            } else {
                g2d.draw(shape.shape);
            }
        }

        canvas.repaint();
    }

    /**
     * Creates a shape based on the current selection and coordinates
     */
    private Shape createShape(int x, int y, int width, int height) {
        // Ensure proper sizing with negative width/height
        int x1 = x;
        int y1 = y;
        int w = width;
        int h = height;

        if (width < 0) {
            x1 = x + width;
            w = -width;
        }

        if (height < 0) {
            y1 = y + height;
            h = -height;
        }

        if (currentShape instanceof Rectangle2D) {
            return new Rectangle2D.Double(x1, y1, w, h);
        } else if (currentShape instanceof Ellipse2D) {
            return new Ellipse2D.Double(x1, y1, w, h);
        }

        // Default to rectangle if something went wrong
        return new Rectangle2D.Double(x1, y1, w, h);
    }

    /**
     * Canvas component for drawing shapes
     */
    private class DrawingCanvas extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            // Enable anti-aliasing for smoother shapes
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw the persistent image with all completed shapes
            g2.drawImage(image, 0, 0, this);

            // Draw the active shape if one is being drawn
            if (activeShape != null) {
                g2.setColor(activeShape.color);
                g2.setStroke(new BasicStroke(activeShape.strokeWidth));

                if (activeShape.filled) {
                    g2.fill(activeShape.shape);
                } else {
                    g2.draw(activeShape.shape);
                }
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        }
    }

    /**
     * Mouse listener for shape creation
     */
    private class ShapeMouseListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            if (currentShape != null) {
                startPoint = e.getPoint();
                activeShape = new DrawnShape(
                        createShape(startPoint.x, startPoint.y, 0, 0),
                        currentColor,
                        fillShape,
                        strokeWidth
                );
                canvas.repaint();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (activeShape != null) {
                // Add the completed shape to our list
                shapes.add(activeShape);

                // Draw the shape to the persistent image
                g2d.setColor(activeShape.color);
                g2d.setStroke(new BasicStroke(activeShape.strokeWidth));

                if (activeShape.filled) {
                    g2d.fill(activeShape.shape);
                } else {
                    g2d.draw(activeShape.shape);
                }

                // Reset the active shape
                activeShape = null;
                canvas.repaint();
            }
        }
    }

    /**
     * Mouse motion listener for shape resizing during dragging
     */
    private class ShapeMotionListener extends MouseMotionAdapter {
        @Override
        public void mouseDragged(MouseEvent e) {
            if (activeShape != null && startPoint != null) {
                int width = e.getX() - startPoint.x;
                int height = e.getY() - startPoint.y;

                activeShape.shape = createShape(startPoint.x, startPoint.y, width, height);
                canvas.repaint();
            }
        }
    }

    /**
     * Class to store information about a drawn shape
     */
    private static class DrawnShape {
        Shape shape;
        Color color;
        boolean filled;
        float strokeWidth;

        public DrawnShape(Shape shape, Color color, boolean filled, float strokeWidth) {
            this.shape = shape;
            this.color = color;
            this.filled = filled;
            this.strokeWidth = strokeWidth;
        }
    }

    /**
     * Main method to run the application
     */
    public static void main(String[] args) {
        // Use system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Launch the application on the EDT
        SwingUtilities.invokeLater(() -> new graph());
    }
}