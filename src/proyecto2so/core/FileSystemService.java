package proyecto2so.core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileSystemService {
    private DirectoryNode root;
    private VirtualDisk disk;
    private FileNode[] fileIndex;
    private int fileCount;

    public FileSystemService() {
        this.root = null;
        this.disk = null;
        this.fileIndex = new FileNode[10];
        this.fileCount = 0;
    }

    public void initialize(int totalBlocks) {
        this.root = new DirectoryNode("root", "admin", null);
        this.disk = new VirtualDisk(totalBlocks);
        this.fileIndex = new FileNode[10];
        this.fileCount = 0;
    }

    public void createDirectory(String parentPath, String directoryName, String owner) {
        ensureInitialized();

        DirectoryNode parentDirectory = resolveDirectory(parentPath);

        if (parentDirectory == null) {
            throw new IllegalStateException("La ruta padre no existe.");
        }

        DirectoryNode newDirectory = new DirectoryNode(directoryName, owner, parentDirectory);
        parentDirectory.addSubdirectory(newDirectory);
    }

    public void createFile(String parentPath, String fileName, String owner, int sizeInBlocks) {
        ensureInitialized();

        if (sizeInBlocks <= 0) {
            throw new IllegalArgumentException("El tamaño en bloques debe ser mayor que 0.");
        }

        DirectoryNode parentDirectory = resolveDirectory(parentPath);

        if (parentDirectory == null) {
            throw new IllegalStateException("La ruta padre no existe.");
        }

        int[] allocatedBlocks = disk.findFreeBlocks(sizeInBlocks);

        for (int i = 0; i < allocatedBlocks.length; i++) {
            int nextBlockId = (i == allocatedBlocks.length - 1) ? -1 : allocatedBlocks[i + 1];
            disk.occupyBlock(allocatedBlocks[i], fileName, nextBlockId);
        }

        FileNode newFile = new FileNode(fileName, owner, parentDirectory, sizeInBlocks, allocatedBlocks[0]);
        parentDirectory.addFile(newFile);
        addToFileIndex(newFile);
    }

    public FileNode getFileByPath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("La ruta del archivo no puede ser nula o vacía.");
        }

        if (!filePath.startsWith("/")) {
            throw new IllegalArgumentException("La ruta debe empezar con '/'.");
        }

        if ("/".equals(filePath)) {
            return null;
        }

        int lastSlash = filePath.lastIndexOf('/');

        String parentPath;
        String fileName;

        if (lastSlash == 0) {
            parentPath = "/";
            fileName = filePath.substring(1);
        } else {
            parentPath = filePath.substring(0, lastSlash);
            fileName = filePath.substring(lastSlash + 1);
        }

        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("La ruta del archivo no es válida.");
        }

        DirectoryNode parentDirectory = resolveDirectory(parentPath);

        if (parentDirectory == null) {
            return null;
        }

        FileNode[] files = parentDirectory.getFiles();

        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().equals(fileName)) {
                return files[i];
            }
        }

        return null;
    }

    public int[] getFileBlockChain(String filePath) {
        FileNode file = getFileByPath(filePath);

        if (file == null) {
            throw new IllegalStateException("El archivo no existe.");
        }

        if (file.getFirstBlockId() < 0) {
            throw new IllegalStateException("El archivo no tiene bloques asignados.");
        }

        return disk.traverseChain(file.getFirstBlockId());
    }

    public void deleteFile(String filePath) {
        ensureInitialized();

        FileNode file = getFileByPath(filePath);

        if (file == null) {
            throw new IllegalStateException("El archivo no existe.");
        }

        if (file.getFirstBlockId() >= 0) {
            disk.freeChain(file.getFirstBlockId());
        }

        DirectoryNode parent = file.getParent();

        if (parent == null) {
            throw new IllegalStateException("El archivo no tiene directorio padre.");
        }

        parent.removeFileByName(file.getName());
        removeFromFileIndex(file);
    }

    public void deleteDirectoryRecursive(String directoryPath) {
        ensureInitialized();

        if ("/".equals(directoryPath)) {
            throw new IllegalStateException("No se puede eliminar el directorio raíz.");
        }

        DirectoryNode target = resolveDirectory(directoryPath);

        if (target == null) {
            throw new IllegalStateException("El directorio no existe.");
        }

        deleteDirectoryContents(target);

        DirectoryNode parent = target.getParent();

        if (parent == null) {
            throw new IllegalStateException("El directorio no tiene padre.");
        }

        parent.removeSubdirectoryByName(target.getName());
    }

    public void renameFile(String filePath, String newName) {
        ensureInitialized();

        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("El nuevo nombre no puede ser nulo o vacío.");
        }

        FileNode file = getFileByPath(filePath);

        if (file == null) {
            throw new IllegalStateException("El archivo no existe.");
        }

        DirectoryNode parent = file.getParent();

        if (parent == null) {
            throw new IllegalStateException("El archivo no tiene directorio padre.");
        }

        if (!file.getName().equals(newName) && parent.containsName(newName)) {
            throw new IllegalStateException("Ya existe un nodo con ese nombre en el directorio padre.");
        }

        file.setName(newName);
    }

    public void renameDirectory(String directoryPath, String newName) {
        ensureInitialized();

        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("El nuevo nombre no puede ser nulo o vacío.");
        }

        if ("/".equals(directoryPath)) {
            throw new IllegalStateException("No se puede renombrar la raíz.");
        }

        DirectoryNode directory = resolveDirectory(directoryPath);

        if (directory == null) {
            throw new IllegalStateException("El directorio no existe.");
        }

        DirectoryNode parent = directory.getParent();

        if (parent == null) {
            throw new IllegalStateException("El directorio no tiene padre.");
        }

        if (!directory.getName().equals(newName) && parent.containsName(newName)) {
            throw new IllegalStateException("Ya existe un nodo con ese nombre en el directorio padre.");
        }

        directory.setName(newName);
    }

    public AllocationEntry[] getAllocationTable() {
        AllocationEntry[] table = new AllocationEntry[fileCount];

        for (int i = 0; i < fileCount; i++) {
            FileNode file = fileIndex[i];
            table[i] = new AllocationEntry(file.getName(), file.getSizeInBlocks(), file.getFirstBlockId());
        }

        return table;
    }

    public void saveToJson(String filePath) {
        ensureInitialized();

        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("La ruta de guardado no puede ser nula o vacía.");
        }

        StringBuilder json = new StringBuilder();

        json.append("{\n");
        json.append("  \"totalBlocks\": ").append(disk.getTotalBlocks()).append(",\n");
        json.append("  \"directories\": [\n");
        appendDirectoriesJson(root, json, new boolean[]{true});
        json.append("  ],\n");
        json.append("  \"files\": [\n");
        appendFilesJson(json);
        json.append("  ],\n");
        json.append("  \"blocks\": [\n");
        appendBlocksJson(json);
        json.append("  ]\n");
        json.append("}\n");

        writeTextFile(filePath, json.toString());
    }

    public void loadFromJson(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("La ruta de carga no puede ser nula o vacía.");
        }

        String json = readTextFile(filePath);

        try {
            int totalBlocks = extractIntValue(json, "totalBlocks");
            initialize(totalBlocks);

            String directoriesSection = extractArraySection(json, "directories");
            String filesSection = extractArraySection(json, "files");
            String blocksSection = extractArraySection(json, "blocks");

            String[] directoryObjects = splitObjects(directoriesSection);
            for (int i = 0; i < directoryObjects.length; i++) {
                String path = extractStringValue(directoryObjects[i], "path");
                String owner = extractStringValue(directoryObjects[i], "owner");
                createDirectoryByFullPath(path, owner);
            }

            String[] blockObjects = splitObjects(blocksSection);
            for (int i = 0; i < blockObjects.length; i++) {
                int id = extractIntValue(blockObjects[i], "id");
                boolean free = extractBooleanValue(blockObjects[i], "free");

                if (!free) {
                    String fileName = extractNullableStringValue(blockObjects[i], "fileName");
                    int nextBlockId = extractIntValue(blockObjects[i], "nextBlockId");
                    disk.occupyBlock(id, fileName, nextBlockId);
                }
            }

            String[] fileObjects = splitObjects(filesSection);
            for (int i = 0; i < fileObjects.length; i++) {
                String path = extractStringValue(fileObjects[i], "path");
                String owner = extractStringValue(fileObjects[i], "owner");
                int sizeInBlocks = extractIntValue(fileObjects[i], "sizeInBlocks");
                int firstBlockId = extractIntValue(fileObjects[i], "firstBlockId");
                createFileNodeOnly(path, owner, sizeInBlocks, firstBlockId);
            }
        } catch (RuntimeException e) {
            throw new IllegalStateException("JSON inválido o estado inconsistente.");
        }
    }

    private void createDirectoryByFullPath(String fullPath, String owner) {
        if (fullPath == null || fullPath.trim().isEmpty() || "/".equals(fullPath)) {
            return;
        }

        int lastSlash = fullPath.lastIndexOf('/');
        String parentPath;
        String directoryName;

        if (lastSlash == 0) {
            parentPath = "/";
            directoryName = fullPath.substring(1);
        } else {
            parentPath = fullPath.substring(0, lastSlash);
            directoryName = fullPath.substring(lastSlash + 1);
        }

        createDirectory(parentPath, directoryName, owner);
    }

    private void createFileNodeOnly(String fullPath, String owner, int sizeInBlocks, int firstBlockId) {
        int lastSlash = fullPath.lastIndexOf('/');

        String parentPath;
        String fileName;

        if (lastSlash == 0) {
            parentPath = "/";
            fileName = fullPath.substring(1);
        } else {
            parentPath = fullPath.substring(0, lastSlash);
            fileName = fullPath.substring(lastSlash + 1);
        }

        DirectoryNode parentDirectory = resolveDirectory(parentPath);

        if (parentDirectory == null) {
            throw new IllegalStateException("La ruta padre no existe al reconstruir archivo.");
        }

        FileNode file = new FileNode(fileName, owner, parentDirectory, sizeInBlocks, firstBlockId);
        parentDirectory.addFile(file);
        addToFileIndex(file);
    }

    private void appendDirectoriesJson(DirectoryNode directory, StringBuilder json, boolean[] firstEntryRef) {
        DirectoryNode[] subdirs = directory.getSubdirectories();

        for (int i = 0; i < subdirs.length; i++) {
            if (!firstEntryRef[0]) {
                json.append(",\n");
            }

            json.append("    {\"path\":\"")
                .append(escapeJson(subdirs[i].getPath()))
                .append("\",\"owner\":\"")
                .append(escapeJson(subdirs[i].getOwner()))
                .append("\"}");

            firstEntryRef[0] = false;
            appendDirectoriesJson(subdirs[i], json, firstEntryRef);
        }
    }

    private void appendFilesJson(StringBuilder json) {
        for (int i = 0; i < fileCount; i++) {
            if (i > 0) {
                json.append(",\n");
            }

            FileNode file = fileIndex[i];

            json.append("    {\"path\":\"")
                .append(escapeJson(file.getPath()))
                .append("\",\"owner\":\"")
                .append(escapeJson(file.getOwner()))
                .append("\",\"sizeInBlocks\":")
                .append(file.getSizeInBlocks())
                .append(",\"firstBlockId\":")
                .append(file.getFirstBlockId())
                .append("}");
        }

        if (fileCount > 0) {
            json.append("\n");
        }
    }

    private void appendBlocksJson(StringBuilder json) {
        Block[] blocks = disk.getBlocks();

        for (int i = 0; i < blocks.length; i++) {
            if (i > 0) {
                json.append(",\n");
            }

            json.append("    {\"id\":")
                .append(blocks[i].getId())
                .append(",\"free\":")
                .append(blocks[i].isFree())
                .append(",\"fileName\":");

            if (blocks[i].getFileName() == null) {
                json.append("null");
            } else {
                json.append("\"").append(escapeJson(blocks[i].getFileName())).append("\"");
            }

            json.append(",\"nextBlockId\":")
                .append(blocks[i].getNextBlockId())
                .append("}");
        }

        if (blocks.length > 0) {
            json.append("\n");
        }
    }

    private void deleteDirectoryContents(DirectoryNode directory) {
        FileNode[] files = directory.getFiles();

        while (files.length > 0) {
            deleteFile(files[0].getPath());
            files = directory.getFiles();
        }

        DirectoryNode[] subdirs = directory.getSubdirectories();

        while (subdirs.length > 0) {
            deleteDirectoryRecursive(subdirs[0].getPath());
            subdirs = directory.getSubdirectories();
        }
    }

    private void removeFromFileIndex(FileNode file) {
        int foundIndex = -1;

        for (int i = 0; i < fileCount; i++) {
            if (fileIndex[i] == file) {
                foundIndex = i;
                break;
            }
        }

        if (foundIndex == -1) {
            throw new IllegalStateException("El archivo no existe en el índice global.");
        }

        for (int i = foundIndex; i < fileCount - 1; i++) {
            fileIndex[i] = fileIndex[i + 1];
        }

        fileIndex[fileCount - 1] = null;
        fileCount--;
    }

    private void addToFileIndex(FileNode file) {
        if (fileCount >= fileIndex.length) {
            FileNode[] newArray = new FileNode[fileIndex.length * 2];

            for (int i = 0; i < fileIndex.length; i++) {
                newArray[i] = fileIndex[i];
            }

            fileIndex = newArray;
        }

        fileIndex[fileCount] = file;
        fileCount++;
    }

    private DirectoryNode resolveDirectory(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("La ruta no puede ser nula o vacía.");
        }

        if (root == null) {
            return null;
        }

        if ("/".equals(path)) {
            return root;
        }

        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("La ruta debe empezar con '/'.");
        }

        String[] parts = path.split("/");
        DirectoryNode current = root;

        for (int i = 1; i < parts.length; i++) {
            if (parts[i] == null || parts[i].isEmpty()) {
                continue;
            }

            DirectoryNode next = findSubdirectoryByName(current, parts[i]);

            if (next == null) {
                return null;
            }

            current = next;
        }

        return current;
    }

    private DirectoryNode findSubdirectoryByName(DirectoryNode parent, String name) {
        DirectoryNode[] subdirs = parent.getSubdirectories();

        for (int i = 0; i < subdirs.length; i++) {
            if (subdirs[i].getName().equals(name)) {
                return subdirs[i];
            }
        }

        return null;
    }

    private void ensureInitialized() {
        if (root == null || disk == null) {
            throw new IllegalStateException("El sistema de archivos no ha sido inicializado.");
        }
    }

    private void writeTextFile(String filePath, String content) {
        try {
            FileWriter writer = new FileWriter(filePath);
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el archivo JSON.");
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
            throw new IllegalStateException("No se pudo leer el archivo JSON.");
        }
    }

    private String extractArraySection(String json, String key) {
        String token = "\"" + key + "\"";
        int keyIndex = json.indexOf(token);

        if (keyIndex < 0) {
            throw new IllegalStateException("No se encontró la sección " + key + ".");
        }

        int start = json.indexOf('[', keyIndex);

        if (start < 0) {
            throw new IllegalStateException("No se encontró el inicio de array para " + key + ".");
        }

        int depth = 0;

        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);

            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;

                if (depth == 0) {
                    return json.substring(start + 1, i);
                }
            }
        }

        throw new IllegalStateException("No se encontró el cierre de array para " + key + ".");
    }

    private String[] splitObjects(String arrayContent) {
        String trimmed = arrayContent.trim();

        if (trimmed.isEmpty()) {
            return new String[0];
        }

        String[] temp = new String[10];
        int count = 0;
        int depth = 0;
        int objectStart = -1;

        for (int i = 0; i < arrayContent.length(); i++) {
            char c = arrayContent.charAt(i);

            if (c == '{') {
                if (depth == 0) {
                    objectStart = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;

                if (depth == 0 && objectStart >= 0) {
                    if (count >= temp.length) {
                        String[] newArray = new String[temp.length * 2];
                        for (int j = 0; j < temp.length; j++) {
                            newArray[j] = temp[j];
                        }
                        temp = newArray;
                    }

                    temp[count] = arrayContent.substring(objectStart, i + 1);
                    count++;
                    objectStart = -1;
                }
            }
        }

        String[] result = new String[count];
        for (int i = 0; i < count; i++) {
            result[i] = temp[i];
        }

        return result;
    }

    private int extractIntValue(String json, String key) {
        String raw = extractRawValue(json, key);
        return Integer.parseInt(raw);
    }

    private boolean extractBooleanValue(String json, String key) {
        String raw = extractRawValue(json, key);
        return "true".equals(raw);
    }

    private String extractStringValue(String json, String key) {
        String raw = extractRawValue(json, key);

        if ("null".equals(raw)) {
            throw new IllegalStateException("El valor " + key + " no puede ser null.");
        }

        if (raw.length() < 2 || raw.charAt(0) != '"' || raw.charAt(raw.length() - 1) != '"') {
            throw new IllegalStateException("El valor " + key + " no es un string válido.");
        }

        return unescapeJson(raw.substring(1, raw.length() - 1));
    }

    private String extractNullableStringValue(String json, String key) {
        String raw = extractRawValue(json, key);

        if ("null".equals(raw)) {
            return null;
        }

        if (raw.length() < 2 || raw.charAt(0) != '"' || raw.charAt(raw.length() - 1) != '"') {
            throw new IllegalStateException("El valor " + key + " no es un string válido.");
        }

        return unescapeJson(raw.substring(1, raw.length() - 1));
    }

    private String extractRawValue(String json, String key) {
        String token = "\"" + key + "\"";
        int keyIndex = json.indexOf(token);

        if (keyIndex < 0) {
            throw new IllegalStateException("No se encontró la llave " + key + ".");
        }

        int colonIndex = json.indexOf(':', keyIndex);

        if (colonIndex < 0) {
            throw new IllegalStateException("No se encontró el separador para " + key + ".");
        }

        int index = colonIndex + 1;

        while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
            index++;
        }

        if (index >= json.length()) {
            throw new IllegalStateException("No hay valor para " + key + ".");
        }

        if (json.charAt(index) == '"') {
            StringBuilder value = new StringBuilder();
            value.append('"');
            index++;

            boolean escaped = false;

            while (index < json.length()) {
                char c = json.charAt(index);
                value.append(c);

                if (c == '"' && !escaped) {
                    break;
                }

                if (c == '\\' && !escaped) {
                    escaped = true;
                } else {
                    escaped = false;
                }

                index++;
            }

            return value.toString();
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

    private String escapeJson(String value) {
        String escaped = value.replace("\\", "\\\\");
        escaped = escaped.replace("\"", "\\\"");
        return escaped;
    }

    private String unescapeJson(String value) {
        String unescaped = value.replace("\\\"", "\"");
        unescaped = unescaped.replace("\\\\", "\\");
        return unescaped;
    }

    public DirectoryNode getRoot() {
        return root;
    }

    public VirtualDisk getDisk() {
        return disk;
    }

    public FileNode[] getFileIndex() {
        FileNode[] result = new FileNode[fileCount];

        for (int i = 0; i < fileCount; i++) {
            result[i] = fileIndex[i];
        }

        return result;
    }

    public int getFileCount() {
        return fileCount;
    }
}