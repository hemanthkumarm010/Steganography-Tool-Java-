import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;

// Keep your current imports and add:

import java.nio.file.Files;

public class SteganographyTool extends JFrame {

    private JTextArea messageArea;
    private JButton loadButton, encodeButton, decodeButton, saveButton;
    private JButton hideFileButton, extractFileButton;
    private JLabel imageLabel;
    private BufferedImage originalImage, encodedImage;

    public SteganographyTool() {
        setTitle("Java Steganography Tool");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top Panel
        JPanel topPanel = new JPanel();
        loadButton = new JButton("Load Image");
        encodeButton = new JButton("Encode Message");
        decodeButton = new JButton("Decode Message");
        saveButton = new JButton("Save Image");
        hideFileButton = new JButton("Hide File");
        extractFileButton = new JButton("Extract File");

        topPanel.add(loadButton);
        topPanel.add(encodeButton);
        topPanel.add(decodeButton);
        topPanel.add(hideFileButton);
        topPanel.add(extractFileButton);
        topPanel.add(saveButton);
        add(topPanel, BorderLayout.NORTH);

        // Center Panel
        imageLabel = new JLabel("", SwingConstants.CENTER);
        JScrollPane imageScroll = new JScrollPane(imageLabel);
        add(imageScroll, BorderLayout.CENTER);

        // Bottom Panel
        JPanel bottomPanel = new JPanel(new BorderLayout());
        messageArea = new JTextArea(5, 40);
        JScrollPane scrollPane = new JScrollPane(messageArea);
        bottomPanel.add(new JLabel("Secret Message:"), BorderLayout.NORTH);
        bottomPanel.add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        setupActions();
    }

    private void setupActions() {
        loadButton.addActionListener(e -> loadImage());
        encodeButton.addActionListener(e -> encodeMessage());
        decodeButton.addActionListener(e -> decodeMessage());
        saveButton.addActionListener(e -> saveImage());
        hideFileButton.addActionListener(e -> hideFile());
        extractFileButton.addActionListener(e -> extractFile());
    }

    private void loadImage() {
        JFileChooser chooser = new JFileChooser();
        int option = chooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            try {
                File file = chooser.getSelectedFile();
                if (!file.getName().endsWith(".png") && !file.getName().endsWith(".bmp")) {
                    showError("Only .png or .bmp images are supported.");
                    return;
                }
                originalImage = ImageIO.read(file);
                encodedImage = null;
                imageLabel.setIcon(new ImageIcon(originalImage));
            } catch (Exception ex) {
                showError("Failed to load image.");
            }
        }
    }

    private void encodeMessage() {
        if (originalImage == null) {
            showError("Please load an image first.");
            return;
        }
        String message = messageArea.getText();
        if (message.isEmpty()) {
            showError("Message is empty.");
            return;
        }

        try {
            encodedImage = encodeLSB(originalImage, message + "#");
            imageLabel.setIcon(new ImageIcon(encodedImage));
            showInfo("Message encoded successfully.");
        } catch (Exception ex) {
            showError("Encoding failed: " + ex.getMessage());
        }
    }

    private void decodeMessage() {
        if (originalImage == null && encodedImage == null) {
            showError("Please load or encode an image first.");
            return;
        }
        try {
            String message = decodeLSB(encodedImage != null ? encodedImage : originalImage);
            if (message.isEmpty()) {
                showError("No message found.");
            } else {
                messageArea.setText(message);
                showInfo("Message decoded.");
            }
        } catch (Exception ex) {
            showError("Decoding failed: " + ex.getMessage());
        }
    }

    private void saveImage() {
        if (encodedImage == null) {
            showError("No encoded image to save.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        int option = chooser.showSaveDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            try {
                File file = chooser.getSelectedFile();
                if (!file.getName().endsWith(".png")) {
                    file = new File(file.getAbsolutePath() + ".png");
                }
                ImageIO.write(encodedImage, "png", file);
                showInfo("Image saved successfully.");
            } catch (Exception ex) {
                showError("Failed to save image.");
            }
        }
    }

    // 🔐 LSB Encode Message
    private BufferedImage encodeLSB(BufferedImage image, String msg) {
        BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        int msgIndex = 0;

        outer: for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                int red = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int blue = pixel & 0xff;

                if (msgIndex < msg.length()) {
                    char ch = msg.charAt(msgIndex++);
                    int ascii = ch;
                    blue = (blue & 0xF0) | ((ascii >> 4) & 0x0F);
                    green = (green & 0xF0) | (ascii & 0x0F);
                }

                int newPixel = (red << 16) | (green << 8) | blue;
                newImage.setRGB(x, y, newPixel);

                if (msgIndex >= msg.length()) {
                    for (int y2 = y; y2 < image.getHeight(); y2++) {
                        for (int x2 = x + 1; x2 < image.getWidth(); x2++) {
                            newImage.setRGB(x2, y2, image.getRGB(x2, y2));
                        }
                        x = -1;
                    }
                    break outer;
                }
            }
        }
        return newImage;
    }

    // 🔓 LSB Decode Message
    private String decodeLSB(BufferedImage image) {
        StringBuilder message = new StringBuilder();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                int green = (pixel >> 8) & 0xff;
                int blue = pixel & 0xff;

                int upper = blue & 0x0F;
                int lower = green & 0x0F;
                char ch = (char) ((upper << 4) | lower);

                if (ch == '#')
                    return message.toString();
                message.append(ch);
            }
        }
        return message.toString();
    }

    // 📁 Hide File
    private void hideFile() {
        if (originalImage == null) {
            showError("Please load an image first.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        int option = chooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            try {
                File file = chooser.getSelectedFile();
                byte[] fileBytes = Files.readAllBytes(file.toPath());

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                baos.write("FILEDATA".getBytes()); // signature
                baos.write(intToBytes(file.getName().length()));
                baos.write(file.getName().getBytes());
                baos.write(intToBytes(fileBytes.length));
                baos.write(fileBytes);

                encodedImage = embedBytes(originalImage, baos.toByteArray());
                imageLabel.setIcon(new ImageIcon(encodedImage));
                showInfo("File hidden successfully.");
            } catch (Exception ex) {
                showError("Failed to hide file: " + ex.getMessage());
            }
        }
    }

    // 📤 Extract File
    private void extractFile() {
        if (originalImage == null && encodedImage == null) {
            showError("Please load or encode an image first.");
            return;
        }

        try {
            byte[] data = extractBytes(encodedImage != null ? encodedImage : originalImage);
            ByteArrayInputStream bais = new ByteArrayInputStream(data);

            byte[] sig = bais.readNBytes(8);
            if (!new String(sig).equals("FILEDATA")) {
                showError("No file data found.");
                return;
            }

            int nameLen = bytesToInt(bais.readNBytes(4));
            String fileName = new String(bais.readNBytes(nameLen));
            int fileLen = bytesToInt(bais.readNBytes(4));
            byte[] fileBytes = bais.readNBytes(fileLen);

            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File(fileName));
            int option = chooser.showSaveDialog(this);
            if (option == JFileChooser.APPROVE_OPTION) {
                Files.write(chooser.getSelectedFile().toPath(), fileBytes);
                showInfo("File extracted and saved.");
            }
        } catch (Exception ex) {
            showError("Failed to extract file: " + ex.getMessage());
        }
    }

    // 🔧 Embed bytes using LSB
    private BufferedImage embedBytes(BufferedImage image, byte[] data) throws Exception {
        BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        int dataIndex = 0, bitIndex = 0;

        outer: for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                int red = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int blue = pixel & 0xff;

                for (int c = 0; c < 2; c++) { // green and blue
                    if (dataIndex < data.length) {
                        int byteVal = data[dataIndex];
                        int bit = (byteVal >> (7 - bitIndex)) & 1;
                        if (c == 0)
                            green = (green & 0xFE) | bit;
                        else
                            blue = (blue & 0xFE) | bit;

                        bitIndex++;
                        if (bitIndex == 8) {
                            bitIndex = 0;
                            dataIndex++;
                        }
                    }
                }

                int newPixel = (red << 16) | (green << 8) | blue;
                newImage.setRGB(x, y, newPixel);

                if (dataIndex >= data.length) {
                    for (int y2 = y; y2 < image.getHeight(); y2++) {
                        for (int x2 = x + 1; x2 < image.getWidth(); x2++) {
                            newImage.setRGB(x2, y2, image.getRGB(x2, y2));
                        }
                        x = -1;
                    }
                    break outer;
                }
            }
        }
        return newImage;
    }

    private byte[] extractBytes(BufferedImage image) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int bitIndex = 0, byteVal = 0;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                int green = (pixel >> 8) & 0xff;
                int blue = pixel & 0xff;

                for (int val : new int[] { green, blue }) {
                    int bit = val & 1;
                    byteVal = (byteVal << 1) | bit;
                    bitIndex++;
                    if (bitIndex == 8) {
                        baos.write(byteVal);
                        byteVal = 0;
                        bitIndex = 0;
                    }
                }
            }
        }
        return baos.toByteArray();
    }

    private byte[] intToBytes(int value) {
        return new byte[] { (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value };
    }

    private int bytesToInt(byte[] bytes) {
        return ((bytes[0] & 0xff) << 24) | ((bytes[1] & 0xff) << 16)
                | ((bytes[2] & 0xff) << 8) | (bytes[3] & 0xff);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SteganographyTool().setVisible(true));
    }
}
