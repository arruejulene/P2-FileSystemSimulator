package proyecto2so.json;

public class SimpleJsonParser {
    public static String extractArraySection(String json, String key) {
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

    public static String extractObjectSection(String json, String key) {
        String token = "\"" + key + "\"";
        int keyIndex = json.indexOf(token);

        if (keyIndex < 0) {
            throw new IllegalStateException("No se encontró la sección " + key + ".");
        }

        int start = json.indexOf('{', keyIndex);

        if (start < 0) {
            throw new IllegalStateException("No se encontró el inicio de objeto para " + key + ".");
        }

        int depth = 0;

        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);

            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;

                if (depth == 0) {
                    return json.substring(start + 1, i);
                }
            }
        }

        throw new IllegalStateException("No se encontró el cierre de objeto para " + key + ".");
    }

    public static String[] splitObjects(String arrayContent) {
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

    public static int extractIntValue(String json, String key) {
        String raw = extractRawValue(json, key);
        return Integer.parseInt(raw);
    }

    public static boolean extractBooleanValue(String json, String key) {
        String raw = extractRawValue(json, key);
        return "true".equals(raw);
    }

    public static String extractStringValue(String json, String key) {
        String raw = extractRawValue(json, key);

        if ("null".equals(raw)) {
            throw new IllegalStateException("El valor " + key + " no puede ser null.");
        }

        if (raw.length() < 2 || raw.charAt(0) != '"' || raw.charAt(raw.length() - 1) != '"') {
            throw new IllegalStateException("El valor " + key + " no es un string válido.");
        }

        return unescapeJson(raw.substring(1, raw.length() - 1));
    }

    public static String extractNullableStringValue(String json, String key) {
        String raw = extractRawValue(json, key);

        if ("null".equals(raw)) {
            return null;
        }

        if (raw.length() < 2 || raw.charAt(0) != '"' || raw.charAt(raw.length() - 1) != '"') {
            throw new IllegalStateException("El valor " + key + " no es un string válido.");
        }

        return unescapeJson(raw.substring(1, raw.length() - 1));
    }

    public static String extractRawValue(String json, String key) {
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

    public static String escapeJson(String value) {
        String escaped = value.replace("\\", "\\\\");
        escaped = escaped.replace("\"", "\\\"");
        return escaped;
    }

    public static String unescapeJson(String value) {
        String unescaped = value.replace("\\\"", "\"");
        unescaped = unescaped.replace("\\\\", "\\");
        return unescaped;
    }
}