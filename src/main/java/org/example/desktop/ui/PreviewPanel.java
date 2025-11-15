package org.example.desktop.ui;

import org.example.desktop.api.RemoteStorageClient;
import org.example.desktop.model.RemoteFileMetadata;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class PreviewPanel extends JPanel {
    private final RemoteStorageClient client;
    private final JTextArea text = new JTextArea();
    private final JScrollPane scrollText = new JScrollPane(text);
    private final JLabel image = new JLabel("No preview", SwingConstants.CENTER);

    public PreviewPanel(RemoteStorageClient client) {
        super(new BorderLayout());
        this.client = client;
        text.setEditable(false);
        text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        add(scrollText, BorderLayout.CENTER);
    }

    public void showPreview(RemoteFileMetadata meta) {
        if (meta == null) {
            setNoPreview();
            return;
        }
        String ext = meta.getExtensionLower();

        if ("kt".equals(ext)) {
            try (InputStream is = client.download(meta.getId())) {
                byte[] bytes = is.readAllBytes();
                text.setText(new String(bytes, StandardCharsets.UTF_8));
                text.setCaretPosition(0); 
                removeAll(); add(scrollText, BorderLayout.CENTER); revalidate(); repaint();
            } catch (IOException ex) {
                setError(ex);
            }
        } else if ("jpg".equals(ext) || "jpeg".equals(ext)) {
            try (InputStream is = client.download(meta.getId())) {
                BufferedImage img = ImageIO.read(is);
                image.setIcon(img == null ? null : new ImageIcon(img));
                image.setText(null);
                removeAll(); add(new JScrollPane(image), BorderLayout.CENTER); revalidate(); repaint();
            } catch (IOException ex) {
                setError(ex);
            }
        } else {
            setNoPreview();
        }
    }

    private void setNoPreview() {
        image.setIcon(null);
        image.setText("No preview available for this file type");
        removeAll(); add(image, BorderLayout.CENTER); revalidate(); repaint();
    }

    private void setError(Exception ex) {
        text.setText("Error loading preview: " + ex.getMessage());
        removeAll(); add(scrollText, BorderLayout.CENTER); revalidate(); repaint();
    }
}