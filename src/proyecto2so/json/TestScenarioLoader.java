package proyecto2so.json;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class TestScenarioLoader {
    public TestScenario load(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("La ruta no puede ser nula o vacía.");
        }

        String json = readTextFile(filePath);

        try {
            String testId = SimpleJsonParser.extractStringValue(json, "test_id");
            int initialHead = SimpleJsonParser.extractIntValue(json, "initial_head");

            DiskRequest[] requests = parseRequests(SimpleJsonParser.extractArraySection(json, "requests"));
            SystemFileSeed[] systemFiles = parseSystemFiles(SimpleJsonParser.extractObjectSection(json, "system_files"));

            return new TestScenario(testId, initialHead, requests, systemFiles);
        } catch (RuntimeException e) {
            throw new IllegalStateException("El JSON de escenario es inválido.");
        }
    }

    private DiskRequest[] parseRequests(String requestsSection) {
        String[] requestObjects = SimpleJsonParser.splitObjects(requestsSection);
        DiskRequest[] requests = new DiskRequest[requestObjects.length];

        for (int i = 0; i < requestObjects.length; i++) {
            int pos = SimpleJsonParser.extractIntValue(requestObjects[i], "pos");
            String op = SimpleJsonParser.extractStringValue(requestObjects[i], "op");
            requests[i] = new DiskRequest(pos, op);
        }

        return requests;
    }

    private SystemFileSeed[] parseSystemFiles(String objectSection) {
        String trimmed = objectSection.trim();

        if (trimmed.isEmpty()) {
            return new SystemFileSeed[0];
        }

        String[] tempKeys = new String[10];
        String[] tempObjects = new String[10];
        int count = 0;

        int i = 0;
        while (i < objectSection.length()) {
            while (i < objectSection.length() && (Character.isWhitespace(objectSection.charAt(i)) || objectSection.charAt(i) == ',')) {
                i++;
            }

            if (i >= objectSection.length()) {
                break;
            }

            if (objectSection.charAt(i) != '"') {
                throw new IllegalStateException("Clave inválida en system_files.");
            }

            int keyStart = i + 1;
            int keyEnd = objectSection.indexOf('"', keyStart);

            if (keyEnd < 0) {
                throw new IllegalStateException("Clave sin cierre en system_files.");
            }

            String key = objectSection.substring(keyStart, keyEnd);
            i = keyEnd + 1;

            while (i < objectSection.length() && Character.isWhitespace(objectSection.charAt(i))) {
                i++;
            }

            if (i >= objectSection.length() || objectSection.charAt(i) != ':') {
                throw new IllegalStateException("Falta ':' en system_files.");
            }

            i++;

            while (i < objectSection.length() && Character.isWhitespace(objectSection.charAt(i))) {
                i++;
            }

            if (i >= objectSection.length() || objectSection.charAt(i) != '{') {
                throw new IllegalStateException("Falta objeto en system_files.");
            }

            int startObject = i;
            int depth = 0;

            while (i < objectSection.length()) {
                char c = objectSection.charAt(i);

                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;

                    if (depth == 0) {
                        break;
                    }
                }

                i++;
            }

            if (i >= objectSection.length()) {
                throw new IllegalStateException("Objeto sin cerrar en system_files.");
            }

            String obj = objectSection.substring(startObject, i + 1);

            if (count >= tempKeys.length) {
                String[] newKeys = new String[tempKeys.length * 2];
                String[] newObjects = new String[tempObjects.length * 2];

                for (int j = 0; j < tempKeys.length; j++) {
                    newKeys[j] = tempKeys[j];
                    newObjects[j] = tempObjects[j];
                }

                tempKeys = newKeys;
                tempObjects = newObjects;
            }

            tempKeys[count] = key;
            tempObjects[count] = obj;
            count++;

            i++;
        }

        SystemFileSeed[] result = new SystemFileSeed[count];

        for (int j = 0; j < count; j++) {
            int position = Integer.parseInt(tempKeys[j]);
            String name = SimpleJsonParser.extractStringValue(tempObjects[j], "name");
            int blocks = SimpleJsonParser.extractIntValue(tempObjects[j], "blocks");
            result[j] = new SystemFileSeed(position, name, blocks);
        }

        return result;
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