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
        setBackground(new Color(247, 249, 252));
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
            g2.setColor(Color.DARK_GRAY);
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
            Color fill = b.isFree() ? new Color(191, 242, 195) : new Color(255, 204, 188);
            if (i == headPos) {
                fill = new Color(255, 235, 120);
            }

            g2.setColor(fill);
            g2.fillRect(x, y, cellW - 2, cellH);
            g2.setColor(new Color(70, 70, 70));
            g2.drawRect(x, y, cellW - 2, cellH);

            String label = String.valueOf(i);
            g2.drawString(label, x + 3, y + 14);
        }

        int legendY = y0 + ((blocks.length / columns) + 2) * (cellH + 4);
        g2.setColor(new Color(191, 242, 195));
        g2.fillRect(10, legendY, 14, 14);
        g2.setColor(Color.BLACK);
        g2.drawRect(10, legendY, 14, 14);
        g2.drawString("Libre", 30, legendY + 12);

        g2.setColor(new Color(255, 204, 188));
        g2.fillRect(90, legendY, 14, 14);
        g2.setColor(Color.BLACK);
        g2.drawRect(90, legendY, 14, 14);
        g2.drawString("Ocupado", 110, legendY + 12);

        g2.setColor(new Color(255, 235, 120));
        g2.fillRect(190, legendY, 14, 14);
        g2.setColor(Color.BLACK);
        g2.drawRect(190, legendY, 14, 14);
        g2.drawString("Cabezal", 210, legendY + 12);
    }
}
