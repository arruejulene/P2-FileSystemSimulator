package proyecto2so.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;
import proyecto2so.core.Block;
import proyecto2so.core.VirtualDisk;

public class DiskViewPanel extends JPanel {
    private VirtualDisk disk;
    private int headPos;

    public DiskViewPanel() {
        this.disk = null;
        this.headPos = 0;
        setBackground(new Color(42, 42, 45));
        setPreferredSize(new Dimension(640, 220));
    }

    public void setState(VirtualDisk disk, int headPos) {
        this.disk = disk;
        this.headPos = headPos;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        if (disk == null) {
            g2.setColor(new Color(220, 220, 225));
            g2.drawString("Sin disco inicializado", 16, 24);
            return;
        }

        Block[] blocks = disk.getBlocks();
        int columns = 20;
        int cellW = Math.max(22, (getWidth() - 20) / columns);
        int cellH = 20;
        int x0 = 10;
        int y0 = 10;

        g2.setFont(new Font("Monospaced", Font.PLAIN, 10));

        for (int i = 0; i < blocks.length; i++) {
            int row = i / columns;
            int col = i % columns;
            int x = x0 + col * cellW;
            int y = y0 + row * (cellH + 4);

            Block b = blocks[i];
            Color fill;
            if (b.isFree()) {
                fill = new Color(86, 94, 106);
            } else {
                fill = FileColorPalette.colorForFile(b.getFileName());
            }

            g2.setColor(fill);
            g2.fillRect(x, y, cellW - 2, cellH);
            g2.setColor(new Color(132, 132, 142));
            g2.drawRect(x, y, cellW - 2, cellH);

            if (i == headPos) {
                g2.setColor(new Color(255, 219, 88));
                g2.drawRect(x - 1, y - 1, cellW, cellH + 1);
                g2.drawRect(x - 2, y - 2, cellW + 2, cellH + 3);
            }

            g2.setColor(labelColorFor(fill));
            String label = String.valueOf(i);
            g2.drawString(label, x + 3, y + 14);
        }

    }

    private Color labelColorFor(Color bg) {
        int luminance = (bg.getRed() * 299 + bg.getGreen() * 587 + bg.getBlue() * 114) / 1000;
        return luminance < 140 ? new Color(245, 245, 248) : new Color(18, 18, 20);
    }

}
