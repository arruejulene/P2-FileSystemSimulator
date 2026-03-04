package proyecto2so.json;

import proyecto2so.core.FileNode;
import proyecto2so.core.FileSystemService;
import proyecto2so.core.JsonScenario;
import proyecto2so.core.Request;
import proyecto2so.core.RequestOp;
import proyecto2so.core.SystemFileSeed;

public class ScenarioRequestExecutor {
    private int currentHead;
    private int totalHeadMovement;

    private int[] positions;
    private String[] currentNames;
    private boolean[] deleted;

    public void execute(FileSystemService fs, JsonScenario scenario) {
        if (fs == null) {
            throw new IllegalArgumentException("FileSystemService no puede ser null.");
        }

        if (scenario == null) {
            throw new IllegalArgumentException("JsonScenario no puede ser null.");
        }

        initializeTracking(scenario);

        Request[] requests = scenario.getRequests();

        for (int i = 0; i < requests.length; i++) {
            Request request = requests[i];
            moveHeadTo(request.getPos());

            int index = findIndexByPos(request.getPos());

            if (index == -1) {
                throw new IllegalStateException("No existe system_file para la posición " + request.getPos() + ".");
            }

            if (deleted[index]) {
                throw new IllegalStateException("El archivo en posición " + request.getPos() + " ya fue eliminado.");
            }

            String currentPath = "/system/" + currentNames[index];

            if (request.getOp() == RequestOp.READ) {
                executeRead(fs, currentPath);
            } else if (request.getOp() == RequestOp.UPDATE) {
                executeUpdate(fs, currentPath, index);
            } else if (request.getOp() == RequestOp.DELETE) {
                executeDelete(fs, currentPath, index);
            } else {
                throw new IllegalStateException("Operación no soportada.");
            }
        }
    }

    public int getCurrentHead() {
        return currentHead;
    }

    public int getTotalHeadMovement() {
        return totalHeadMovement;
    }

    public String resolveCurrentPath(int pos) {
        int index = findIndexByPos(pos);

        if (index == -1 || deleted[index]) {
            return null;
        }

        return "/system/" + currentNames[index];
    }

    public boolean wasDeleted(int pos) {
        int index = findIndexByPos(pos);

        if (index == -1) {
            return false;
        }

        return deleted[index];
    }

    private void initializeTracking(JsonScenario scenario) {
        SystemFileSeed[] seeds = scenario.getSystemFiles();

        positions = new int[seeds.length];
        currentNames = new String[seeds.length];
        deleted = new boolean[seeds.length];

        for (int i = 0; i < seeds.length; i++) {
            positions[i] = seeds[i].getPos();
            currentNames[i] = seeds[i].getName();
            deleted[i] = false;
        }

        currentHead = scenario.getInitialHead();
        totalHeadMovement = 0;
    }

    private void moveHeadTo(int target) {
        totalHeadMovement += Math.abs(currentHead - target);
        currentHead = target;
    }

    private void executeRead(FileSystemService fs, String path) {
        FileNode file = fs.getFileByPath(path);

        if (file == null) {
            throw new IllegalStateException("READ no encontró el archivo " + path + ".");
        }

        int[] chain = fs.getFileBlockChain(path);

        if (chain.length != file.getSizeInBlocks()) {
            throw new IllegalStateException("READ detectó una cadena inconsistente en " + path + ".");
        }
    }

    private void executeUpdate(FileSystemService fs, String oldPath, int index) {
        FileNode before = fs.getFileByPath(oldPath);

        if (before == null) {
            throw new IllegalStateException("UPDATE no encontró el archivo " + oldPath + ".");
        }

        int oldHead = before.getFirstBlockId();
        int oldSize = before.getSizeInBlocks();

        String newName = buildUpdatedName(currentNames[index]);
        fs.renameFile(oldPath, newName);

        String newPath = "/system/" + newName;
        FileNode after = fs.getFileByPath(newPath);

        if (after == null) {
            throw new IllegalStateException("UPDATE no renombró correctamente el archivo " + oldPath + ".");
        }

        if (after.getFirstBlockId() != oldHead) {
            throw new IllegalStateException("UPDATE cambió la asignación física de " + newPath + ".");
        }

        if (after.getSizeInBlocks() != oldSize) {
            throw new IllegalStateException("UPDATE cambió el tamaño de " + newPath + ".");
        }

        currentNames[index] = newName;
    }

    private void executeDelete(FileSystemService fs, String path, int index) {
        FileNode file = fs.getFileByPath(path);

        if (file == null) {
            throw new IllegalStateException("DELETE no encontró el archivo " + path + ".");
        }

        fs.deleteFile(path);

        if (fs.getFileByPath(path) != null) {
            throw new IllegalStateException("DELETE no eliminó correctamente " + path + ".");
        }

        deleted[index] = true;
    }

    private String buildUpdatedName(String currentName) {
        return currentName + "_upd";
    }

    private int findIndexByPos(int pos) {
        for (int i = 0; i < positions.length; i++) {
            if (positions[i] == pos) {
                return i;
            }
        }

        return -1;
    }
}