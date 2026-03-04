package proyecto2so.core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonScenarioLoader {

    public static JsonScenario loadFromFile(String path) throws IOException {
        String json = readAll(path);

        String testId = extractString(json, "\"test_id\"\\s*:\\s*\"([^\"]+)\"");
        int initialHead = extractInt(json, "\"initial_head\"\\s*:\\s*(\\d+)");

        Request[] requests = parseRequests(json);
        SystemFileSeed[] systemFiles = parseSystemFiles(json);

        return new JsonScenario(testId, initialHead, requests, systemFiles);
    }

    private static String readAll(String path) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(path));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line).append("\n");
        br.close();
        return sb.toString();
    }

    private static String extractString(String json, String regex) {
        Pattern p = Pattern.compile(regex, Pattern.DOTALL);
        Matcher m = p.matcher(json);
        if (!m.find()) throw new IllegalArgumentException("Missing field for regex: " + regex);
        return m.group(1);
    }

    private static int extractInt(String json, String regex) {
        String s = extractString(json, regex);
        return Integer.parseInt(s);
    }

    private static Request[] parseRequests(String json) {
        Pattern p = Pattern.compile("\\{\\s*\"pos\"\\s*:\\s*(\\d+)\\s*,\\s*\"op\"\\s*:\\s*\"(READ|UPDATE|DELETE)\"\\s*\\}", Pattern.DOTALL);
        Matcher m = p.matcher(json);

        int count = 0;
        while (m.find()) count++;

        Request[] arr = new Request[count];
        m.reset();

        int i = 0;
        while (m.find()) {
            int pos = Integer.parseInt(m.group(1));
            RequestOp op = RequestOp.fromString(m.group(2));
            arr[i++] = new Request(pos, op);
        }

        return arr;
    }

    private static SystemFileSeed[] parseSystemFiles(String json) {
        Pattern p = Pattern.compile("\"(\\d+)\"\\s*:\\s*\\{\\s*\"name\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"blocks\"\\s*:\\s*(\\d+)\\s*\\}", Pattern.DOTALL);
        Matcher m = p.matcher(json);

        int count = 0;
        while (m.find()) count++;

        SystemFileSeed[] arr = new SystemFileSeed[count];
        m.reset();

        int i = 0;
        while (m.find()) {
            int pos = Integer.parseInt(m.group(1));
            String name = m.group(2);
            int blocks = Integer.parseInt(m.group(3));
            arr[i++] = new SystemFileSeed(pos, name, blocks);
        }

        return arr;
    }
}