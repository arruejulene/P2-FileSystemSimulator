package proyecto2so.json;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import proyecto2so.core.Block;
import proyecto2so.core.DirectoryNode;
import proyecto2so.core.FileNode;
import proyecto2so.core.FileSystemService;
import proyecto2so.core.VirtualDisk;

public class FileSystemJsonManager {
    public void save(String filePath, FileSystemService fs) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("La ruta de guardado no puede ser nula o vacía.");
        }

        if (fs == null) {
            throw new IllegalArgumentException("FileSystemService no puede ser null.");
        }

        StringBuilder json = new StringBuilder();

        json.append("{\n");
        json.append("  \"totalBlocks\": ").append(fs.getDisk().getTotalBlocks()).append(",\n");
        json.append("  \"directories\": [\n");
        appendDirectoriesJson(fs.getRoot(), json, new boolean[]{true});
        json.append("  ],\n");
        json.append("  \"files\": [\n");
        appendFilesJson(fs, json);
        json.append("  ],\n");
        json.append("  \"blocks\": [\n");
        appendBlocksJson(fs.getDisk(), json);
        json.append("  ]\n");
        json.append("}\n");

        writeTextFile(filePath, json.toString());
    }

    public void load(String filePath, FileSystemService fs) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("La ruta de carga no puede ser nula o vacía.");
        }

        if (fs == null) {
            throw new IllegalArgumentException("FileSystemService no puede ser null.");
        }

        String json = readTextFile(filePath);

        try {
            int totalBlocks = SimpleJsonParser.extractIntValue(json, "totalBlocks");
            fs.initialize(totalBlocks);

            String directoriesSection = SimpleJsonParser.extractArraySection(json, "directories");
            String filesSection = SimpleJsonParser.extractArraySection(json, "files");
            String blocksSection = SimpleJsonParser.extractArraySection(json, "blocks");

            String[] directoryObjects = SimpleJsonParser.splitObjects(directoriesSection);
            for (int i = 0; i < directoryObjects.length; i++) {
                String path = SimpleJsonParser.extractStringValue(directoryObjects[i], "path");
                String owner = SimpleJsonParser.extractStringValue(directoryObjects[i], "owner");
                createDirectoryByFullPath(fs, path, owner);
            }

            String[] fileObjects = SimpleJsonParser.splitObjects(filesSection);
            int[] savedFirstBlockIds = new int[fileObjects.length];

            for (int i = 0; i < fileObjects.length; i++) {
                String path = SimpleJsonParser.extractStringValue(fileObjects[i], "path");
                String owner = SimpleJsonParser.extractStringValue(fileObjects[i], "owner");
                int sizeInBlocks = SimpleJsonParser.extractIntValue(fileObjects[i], "sizeInBlocks");
                int firstBlockId = SimpleJsonParser.extractIntValue(fileObjects[i], "firstBlockId");

                savedFirstBlockIds[i] = firstBlockId;
                createFileLogicallyThroughService(fs, path, owner, sizeInBlocks);
            }

            FileNode[] files = fs.getFileIndex();
            for (int i = 0; i < files.length; i++) {
                if (files[i].getFirstBlockId() >= 0) {
                    fs.getDisk().freeChain(files[i].getFirstBlockId());
                }
            }

            String[] blockObjects = SimpleJsonParser.splitObjects(blocksSection);
            for (int i = 0; i < blockObjects.length; i++) {
                int id = SimpleJsonParser.extractIntValue(blockObjects[i], "id");
                boolean free = SimpleJsonParser.extractBooleanValue(blockObjects[i], "free");

                if (!free) {
                    String fileName = SimpleJsonParser.extractNullableStringValue(blockObjects[i], "fileName");
                    int nextBlockId = SimpleJsonParser.extractIntValue(blockObjects[i], "nextBlockId");
                    fs.getDisk().occupyBlock(id, fileName, nextBlockId);
                }
            }

            FileNode[] restoredFiles = fs.getFileIndex();
            for (int i = 0; i < restoredFiles.length; i++) {
                restoredFiles[i].setFirstBlockId(savedFirstBlockIds[i]);
            }
        } catch (RuntimeException e) {
            throw new IllegalStateException("JSON inválido o estado inconsistente.");
        }
    }

    private void createDirectoryByFullPath(FileSystemService fs, String fullPath, String owner) {
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

        fs.createDirectory(parentPath, directoryName, owner);
    }

    private void createFileLogicallyThroughService(FileSystemService fs, String fullPath, String owner, int sizeInBlocks) {
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

        fs.createFile(parentPath, fileName, owner, sizeInBlocks);
    }

    private void appendDirectoriesJson(DirectoryNode directory, StringBuilder json, boolean[] firstEntryRef) {
        DirectoryNode[] subdirs = directory.getSubdirectories();

        for (int i = 0; i < subdirs.length; i++) {
            if (!firstEntryRef[0]) {
                json.append(",\n");
            }

            json.append("    {\"path\":\"")
                .append(SimpleJsonParser.escapeJson(subdirs[i].getPath()))
                .append("\",\"owner\":\"")
                .append(SimpleJsonParser.escapeJson(subdirs[i].getOwner()))
                .append("\"}");

            firstEntryRef[0] = false;
            appendDirectoriesJson(subdirs[i], json, firstEntryRef);
        }

        if (!firstEntryRef[0] && directory == null) {
            json.append("\n");
        }
    }

    private void appendFilesJson(FileSystemService fs, StringBuilder json) {
        FileNode[] files = fs.getFileIndex();

        for (int i = 0; i < files.length; i++) {
            if (i > 0) {
                json.append(",\n");
            }

            json.append("    {\"path\":\"")
                .append(SimpleJsonParser.escapeJson(files[i].getPath()))
                .append("\",\"owner\":\"")
                .append(SimpleJsonParser.escapeJson(files[i].getOwner()))
                .append("\",\"sizeInBlocks\":")
                .append(files[i].getSizeInBlocks())
                .append(",\"firstBlockId\":")
                .append(files[i].getFirstBlockId())
                .append("}");
        }

        if (files.length > 0) {
            json.append("\n");
        }
    }

    private void appendBlocksJson(VirtualDisk disk, StringBuilder json) {
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
                json.append("\"")
                    .append(SimpleJsonParser.escapeJson(blocks[i].getFileName()))
                    .append("\"");
            }

            json.append(",\"nextBlockId\":")
                .append(blocks[i].getNextBlockId())
                .append("}");
        }

        if (blocks.length > 0) {
            json.append("\n");
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
}