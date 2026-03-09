package proyecto2so.json;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import proyecto2so.journal.DeletedFileSnapshot;
import proyecto2so.journal.DeletedDirectorySnapshot;
import proyecto2so.journal.JournalEntry;
import proyecto2so.journal.JournalOperation;
import proyecto2so.journal.JournalStatus;

public class JournalJsonManager {
    public void save(String filePath, JournalEntry[] entries) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("La ruta de guardado no puede ser nula o vacía.");
        }
        if (entries == null) {
            throw new IllegalArgumentException("entries no puede ser null.");
        }

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"entries\": [\n");

        for (int i = 0; i < entries.length; i++) {
            JournalEntry e = entries[i];
            if (i > 0) {
                json.append(",\n");
            }

            json.append("    {");
            json.append("\"id\":").append(e.getId()).append(",");
            json.append("\"createdAtMillis\":").append(e.getCreatedAtMillis()).append(",");
            json.append("\"operation\":\"").append(e.getOperation().name()).append("\",");
            json.append("\"status\":\"").append(e.getStatus().name()).append("\",");
            json.append("\"primaryPath\":\"").append(SimpleJsonParser.escapeJson(e.getPrimaryPath())).append("\",");

            if (e.getSecondaryPath() == null) {
                json.append("\"secondaryPath\":null,");
            } else {
                json.append("\"secondaryPath\":\"")
                        .append(SimpleJsonParser.escapeJson(e.getSecondaryPath()))
                        .append("\",");
            }

            json.append("\"deletedSnapshot\":");
            appendDeletedSnapshot(json, e.getDeletedFileSnapshot());
            json.append(",");
            json.append("\"deletedDirectorySnapshot\":");
            appendDeletedDirectorySnapshot(json, e.getDeletedDirectorySnapshot());
            json.append("}");
        }

        if (entries.length > 0) {
            json.append("\n");
        }

        json.append("  ]\n");
        json.append("}\n");

        writeTextFile(filePath, json.toString());
    }

    public JournalEntry[] load(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("La ruta de carga no puede ser nula o vacía.");
        }

        String json = readTextFile(filePath);

        try {
            String entriesSection = SimpleJsonParser.extractArraySection(json, "entries");
            String[] entryObjects = SimpleJsonParser.splitObjects(entriesSection);
            JournalEntry[] entries = new JournalEntry[entryObjects.length];

            for (int i = 0; i < entryObjects.length; i++) {
                entries[i] = parseEntry(entryObjects[i]);
            }

            return entries;
        } catch (RuntimeException e) {
            throw new IllegalStateException("JSON de journal inválido.");
        }
    }

    private JournalEntry parseEntry(String jsonEntry) {
        long id = Long.parseLong(SimpleJsonParser.extractRawValue(jsonEntry, "id"));
        long createdAtMillis = Long.parseLong(SimpleJsonParser.extractRawValue(jsonEntry, "createdAtMillis"));
        String opRaw = SimpleJsonParser.extractStringValue(jsonEntry, "operation");
        String statusRaw = SimpleJsonParser.extractStringValue(jsonEntry, "status");
        String primaryPath = SimpleJsonParser.extractStringValue(jsonEntry, "primaryPath");
        String secondaryPath = SimpleJsonParser.extractNullableStringValue(jsonEntry, "secondaryPath");
        DeletedFileSnapshot snapshot = parseDeletedSnapshot(safeExtractRawValue(jsonEntry, "deletedSnapshot"));
        DeletedDirectorySnapshot deletedDirectorySnapshot = parseDeletedDirectorySnapshot(
                safeExtractRawValue(jsonEntry, "deletedDirectorySnapshot")
        );

        return new JournalEntry(
                id,
                createdAtMillis,
                JournalOperation.valueOf(opRaw),
                JournalStatus.valueOf(statusRaw),
                primaryPath,
                secondaryPath,
                snapshot,
                deletedDirectorySnapshot
        );
    }

    private DeletedFileSnapshot parseDeletedSnapshot(String rawSnapshot) {
        if (rawSnapshot == null) {
            return null;
        }

        String trimmed = rawSnapshot.trim();
        if ("null".equals(trimmed)) {
            return null;
        }

        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            throw new IllegalStateException("deletedSnapshot inválido.");
        }

        String obj = trimmed;
        String fullPath = SimpleJsonParser.extractStringValue(obj, "fullPath");
        String parentPath = SimpleJsonParser.extractStringValue(obj, "parentPath");
        String fileName = SimpleJsonParser.extractStringValue(obj, "fileName");
        String owner = SimpleJsonParser.extractStringValue(obj, "owner");
        int sizeInBlocks = SimpleJsonParser.extractIntValue(obj, "sizeInBlocks");
        int[] chain = parseIntArray(SimpleJsonParser.extractArraySection(obj, "chainBlockIds"));

        return new DeletedFileSnapshot(fullPath, parentPath, fileName, owner, sizeInBlocks, chain);
    }

    private int[] parseIntArray(String arraySection) {
        String trimmed = arraySection.trim();
        if (trimmed.isEmpty()) {
            return new int[0];
        }

        String[] parts = trimmed.split(",");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Integer.parseInt(parts[i].trim());
        }
        return result;
    }

    private void appendDeletedSnapshot(StringBuilder json, DeletedFileSnapshot snapshot) {
        if (snapshot == null) {
            json.append("null");
            return;
        }

        json.append("{");
        json.append("\"fullPath\":\"").append(SimpleJsonParser.escapeJson(snapshot.getFullPath())).append("\",");
        json.append("\"parentPath\":\"").append(SimpleJsonParser.escapeJson(snapshot.getParentPath())).append("\",");
        json.append("\"fileName\":\"").append(SimpleJsonParser.escapeJson(snapshot.getFileName())).append("\",");
        json.append("\"owner\":\"").append(SimpleJsonParser.escapeJson(snapshot.getOwner())).append("\",");
        json.append("\"sizeInBlocks\":").append(snapshot.getSizeInBlocks()).append(",");
        json.append("\"chainBlockIds\":[");

        int[] chain = snapshot.getChainBlockIds();
        for (int i = 0; i < chain.length; i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append(chain[i]);
        }

        json.append("]");
        json.append("}");
    }

    private DeletedDirectorySnapshot parseDeletedDirectorySnapshot(String rawSnapshot) {
        if (rawSnapshot == null) {
            return null;
        }

        String trimmed = rawSnapshot.trim();
        if ("null".equals(trimmed)) {
            return null;
        }

        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            throw new IllegalStateException("deletedDirectorySnapshot inválido.");
        }

        String obj = trimmed;
        String fullPath = SimpleJsonParser.extractStringValue(obj, "fullPath");
        String parentPath = SimpleJsonParser.extractStringValue(obj, "parentPath");
        String directoryName = SimpleJsonParser.extractStringValue(obj, "directoryName");
        String owner = SimpleJsonParser.extractStringValue(obj, "owner");

        String filesSection = SimpleJsonParser.extractArraySection(obj, "files");
        String[] fileObjects = SimpleJsonParser.splitObjects(filesSection);
        DeletedFileSnapshot[] files = new DeletedFileSnapshot[fileObjects.length];
        for (int i = 0; i < fileObjects.length; i++) {
            files[i] = parseDeletedSnapshot(fileObjects[i]);
        }

        String subdirsSection = SimpleJsonParser.extractArraySection(obj, "subdirectories");
        String[] subdirObjects = SimpleJsonParser.splitObjects(subdirsSection);
        DeletedDirectorySnapshot[] subdirs = new DeletedDirectorySnapshot[subdirObjects.length];
        for (int i = 0; i < subdirObjects.length; i++) {
            subdirs[i] = parseDeletedDirectorySnapshot(subdirObjects[i]);
        }

        return new DeletedDirectorySnapshot(fullPath, parentPath, directoryName, owner, files, subdirs);
    }

    private String safeExtractRawValue(String json, String key) {
        try {
            return extractRawJsonValue(json, key);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String extractRawJsonValue(String json, String key) {
        String token = "\"" + key + "\"";
        int keyIndex = json.indexOf(token);

        if (keyIndex < 0) {
            throw new IllegalStateException("No se encontró la llave " + key + ".");
        }

        int colonIndex = json.indexOf(':', keyIndex);
        if (colonIndex < 0) {
            throw new IllegalStateException("No se encontró ':' para " + key + ".");
        }

        int index = colonIndex + 1;
        while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
            index++;
        }

        if (index >= json.length()) {
            throw new IllegalStateException("No hay valor para " + key + ".");
        }

        char start = json.charAt(index);

        if (start == '"') {
            int end = findStringEnd(json, index);
            return json.substring(index, end + 1);
        }

        if (start == '{') {
            int end = findMatching(json, index, '{', '}');
            return json.substring(index, end + 1);
        }

        if (start == '[') {
            int end = findMatching(json, index, '[', ']');
            return json.substring(index, end + 1);
        }

        int end = index;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == ',' || c == '}' || c == ']' || Character.isWhitespace(c)) {
                break;
            }
            end++;
        }

        return json.substring(index, end).trim();
    }

    private int findMatching(String text, int startIndex, char open, char close) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = startIndex; i < text.length(); i++) {
            char c = text.charAt(i);

            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }

            if (c == '"') {
                inString = true;
                continue;
            }

            if (c == open) {
                depth++;
                continue;
            }

            if (c == close) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }

        throw new IllegalStateException("Valor JSON sin cierre.");
    }

    private int findStringEnd(String text, int startQuote) {
        boolean escaped = false;

        for (int i = startQuote + 1; i < text.length(); i++) {
            char c = text.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                continue;
            }

            if (c == '"') {
                return i;
            }
        }

        throw new IllegalStateException("String JSON sin cierre.");
    }

    private void appendDeletedDirectorySnapshot(StringBuilder json, DeletedDirectorySnapshot snapshot) {
        if (snapshot == null) {
            json.append("null");
            return;
        }

        json.append("{");
        json.append("\"fullPath\":\"").append(SimpleJsonParser.escapeJson(snapshot.getFullPath())).append("\",");
        json.append("\"parentPath\":\"").append(SimpleJsonParser.escapeJson(snapshot.getParentPath())).append("\",");
        json.append("\"directoryName\":\"").append(SimpleJsonParser.escapeJson(snapshot.getDirectoryName())).append("\",");
        json.append("\"owner\":\"").append(SimpleJsonParser.escapeJson(snapshot.getOwner())).append("\",");
        json.append("\"files\":[");

        DeletedFileSnapshot[] files = snapshot.getFiles();
        for (int i = 0; i < files.length; i++) {
            if (i > 0) {
                json.append(",");
            }
            appendDeletedSnapshot(json, files[i]);
        }

        json.append("],");
        json.append("\"subdirectories\":[");

        DeletedDirectorySnapshot[] subdirs = snapshot.getSubdirectories();
        for (int i = 0; i < subdirs.length; i++) {
            if (i > 0) {
                json.append(",");
            }
            appendDeletedDirectorySnapshot(json, subdirs[i]);
        }

        json.append("]");
        json.append("}");
    }

    private void writeTextFile(String filePath, String content) {
        try {
            FileWriter writer = new FileWriter(filePath);
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el archivo JSON de journal.");
        }
    }

    private String readTextFile(String filePath) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            StringBuilder content = new StringBuilder();
            String line = reader.readLine();

            while (line != null) {
                content.append(line).append("\n");
                line = reader.readLine();
            }

            reader.close();
            return content.toString();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el archivo JSON de journal.");
        }
    }
}
