package proyecto2so.gui;

import java.awt.Color;

public final class FileColorPalette {
    private FileColorPalette() {
    }

    public static Color colorForFile(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return new Color(200, 95, 95);
        }

        int hash = fileName.hashCode();
        float hue = ((hash & 0x7fffffff) % 360) / 360.0f;
        // High-contrast palette for dark UI.
        return Color.getHSBColor(hue, 0.78f, 0.92f);
    }
}
