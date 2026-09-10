package keystrokesmod.utility;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import keystrokesmod.utility.question.QuestionEntry;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class QuestionUtility {
    private static final String QUESTION_RESOURCE_PATH = "assets/keystrokesmod/question/";
    private static final Map<String, List<QuestionEntry>> QUESTIONS = loadQuestions();

    public static List<QuestionEntry> find(String message) {
        for (String line : message.split("\\r?\\n")) {
            String normalizedLine = normalizeQuestion(line);
            if (!normalizedLine.isEmpty()) {
                List<QuestionEntry> exact = QUESTIONS.get(normalizedLine);
                if (exact != null) {
                    return exact;
                }
            }
        }
        return Collections.emptyList();
    }

    private static Map<String, List<QuestionEntry>> loadQuestions() {
        Map<String, List<QuestionEntry>> questions = new LinkedHashMap<>();
        Set<String> loadedFiles = new HashSet<>();
        try {
            Enumeration<URL> resources = QuestionUtility.class.getClassLoader().getResources(QUESTION_RESOURCE_PATH);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if ("file".equals(resource.getProtocol())) {
                    loadDirectory(new File(resource.toURI()), questions, loadedFiles);
                }
                else if ("jar".equals(resource.getProtocol())) {
                    JarURLConnection connection = (JarURLConnection) resource.openConnection();
                    connection.setUseCaches(false);
                    try (JarFile jarFile = connection.getJarFile()) {
                        loadJar(jarFile, questions, loadedFiles);
                    }
                }
            }
        }
        catch (Exception ignored) {
        }

        if (questions.isEmpty()) {
            loadFromCodeSource(questions, loadedFiles);
        }
        for (Map.Entry<String, List<QuestionEntry>> entry : questions.entrySet()) {
            entry.setValue(Collections.unmodifiableList(entry.getValue()));
        }
        return Collections.unmodifiableMap(questions);
    }

    private static void loadFromCodeSource(Map<String, List<QuestionEntry>> questions, Set<String> loadedFiles) {
        try {
            CodeSource codeSource = QuestionUtility.class.getProtectionDomain().getCodeSource();
            if (codeSource == null || codeSource.getLocation() == null) {
                return;
            }
            URI location = codeSource.getLocation().toURI();
            File source = new File(location);
            if (source.isDirectory()) {
                loadDirectory(new File(source, QUESTION_RESOURCE_PATH), questions, loadedFiles);
            }
            else if (source.isFile()) {
                try (JarFile jarFile = new JarFile(source)) {
                    loadJar(jarFile, questions, loadedFiles);
                }
            }
        }
        catch (Exception ignored) {
        }
    }

    private static void loadDirectory(File directory, Map<String, List<QuestionEntry>> questions, Set<String> loadedFiles) {
        File[] files = directory.listFiles((file, name) -> name.endsWith(".json"));
        if (files == null) {
            return;
        }
        Arrays.sort(files, Comparator.comparing(File::getName));
        for (File file : files) {
            if (!loadedFiles.contains(file.getName())) {
                try (InputStream inputStream = new FileInputStream(file)) {
                    loadQuestion(inputStream, file.getName(), questions, loadedFiles);
                }
                catch (Exception ignored) {
                }
            }
        }
    }

    private static void loadJar(JarFile jarFile, Map<String, List<QuestionEntry>> questions, Set<String> loadedFiles) {
        List<JarEntry> entries = new ArrayList<>();
        Enumeration<JarEntry> jarEntries = jarFile.entries();
        while (jarEntries.hasMoreElements()) {
            JarEntry entry = jarEntries.nextElement();
            if (!entry.isDirectory() && entry.getName().startsWith(QUESTION_RESOURCE_PATH) && entry.getName().endsWith(".json")) {
                entries.add(entry);
            }
        }
        entries.sort(Comparator.comparing(JarEntry::getName));
        for (JarEntry entry : entries) {
            String fileName = entry.getName().substring(QUESTION_RESOURCE_PATH.length());
            if (loadedFiles.contains(fileName)) {
                continue;
            }
            try (InputStream inputStream = jarFile.getInputStream(entry)) {
                loadQuestion(inputStream, fileName, questions, loadedFiles);
            }
            catch (Exception ignored) {
            }
        }
    }

    private static void loadQuestion(InputStream inputStream, String fileName, Map<String, List<QuestionEntry>> questions, Set<String> loadedFiles) {
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            JsonElement element = new JsonParser().parse(reader);
            if (!element.isJsonObject()) {
                return;
            }
            JsonObject object = element.getAsJsonObject();
            String question = getString(object, "question");
            String type = getString(object, "type");
            String answer = getString(object, "answer");
            if (question.isEmpty() || answer.isEmpty() || !("click".equalsIgnoreCase(type) || "chat".equalsIgnoreCase(type) || "send".equalsIgnoreCase(type))) {
                return;
            }
            String key = normalizeQuestion(question);
            if (key.isEmpty()) {
                return;
            }
            questions.computeIfAbsent(key, ignored -> new ArrayList<>()).add(new QuestionEntry(type, answer));
            loadedFiles.add(fileName);
        }
        catch (Exception ignored) {
        }
    }

    private static String getString(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        try {
            return object.get(key).getAsString().trim();
        }
        catch (Exception ignored) {
            return "";
        }
    }

    private static String normalizeQuestion(String value) {
        String normalized = value.replaceAll("§.", "")
                .replaceAll("\\[[^\\]]*题\\]", "")
                .replaceAll("<[^>]*>", "")
                .replaceAll("[？?。！!，,：:；;]", "")
                .replaceAll("\\s+", "")
                .trim();
        int singleChoice = normalized.indexOf("单选题");
        int shortAnswer = normalized.indexOf("简答题");
        int typeIndex = singleChoice < 0 ? shortAnswer : shortAnswer < 0 ? singleChoice : Math.min(singleChoice, shortAnswer);
        if (typeIndex >= 0) {
            normalized = normalized.substring(0, typeIndex);
        }
        int separator = Math.max(normalized.lastIndexOf("»"), normalized.lastIndexOf(">>"));
        if (separator >= 0) {
            normalized = normalized.substring(separator + (normalized.charAt(separator) == '»' ? 1 : 2));
        }
        return normalized.trim().toLowerCase(java.util.Locale.ROOT);
    }

}
