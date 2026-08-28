// StepCounter.java
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;

public class StepCounter {
    private static final String DATA_FILE = "steps.json";
    private static final int DEFAULT_GOAL = 10000;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type DATA_TYPE = new TypeToken<Data>(){}.getType();

    @Parameter(names = "--add")
    private Integer addSteps;
    @Parameter(names = "--show")
    private boolean show;
    @Parameter(names = "--goal")
    private Integer goal;
    @Parameter(names = "--history")
    private boolean history;
    @Parameter(names = "--reset")
    private boolean reset;
    @Parameter(names = "--export-csv")
    private String exportCsv;

    static class Data {
        int goal = DEFAULT_GOAL;
        List<HistoryEntry> history = new ArrayList<>();
    }

    static class HistoryEntry {
        String date;
        int steps;
    }

    private Data data = new Data();

    private void load() {
        try {
            String json = new String(Files.readAllBytes(Paths.get(DATA_FILE)));
            data = GSON.fromJson(json, DATA_TYPE);
        } catch (Exception e) {
            data = new Data();
        }
        if (data.history == null) data.history = new ArrayList<>();
    }

    private void save() {
        try {
            Files.write(Paths.get(DATA_FILE), GSON.toJson(data).getBytes());
        } catch (IOException e) {
            System.err.println("Ошибка сохранения: " + e.getMessage());
        }
    }

    private HistoryEntry getToday() {
        String today = LocalDate.now().toString();
        for (HistoryEntry e : data.history) {
            if (e.date.equals(today)) return e;
        }
        return null;
    }

    private void addSteps(int steps) {
        HistoryEntry entry = getToday();
        if (entry != null) {
            entry.steps += steps;
        } else {
            entry = new HistoryEntry();
            entry.date = LocalDate.now().toString();
            entry.steps = steps;
            data.history.add(entry);
        }
        save();
        System.out.println("\u001B[32mДобавлено " + steps + " шагов. Всего сегодня: " + entry.steps + "\u001B[0m");
    }

    private void show() {
        HistoryEntry entry = getToday();
        int stepsToday = entry != null ? entry.steps : 0;
        int progress = Math.min(100, stepsToday * 100 / data.goal);
        int barLen = 20;
        int filled = progress * barLen / 100;
        String bar = "█".repeat(filled) + "░".repeat(barLen - filled);
        System.out.println("\u001B[36m📊 Сегодня: " + stepsToday + " шагов\u001B[0m");
        System.out.println("\u001B[33mЦель: " + data.goal + " шагов\u001B[0m");
        System.out.println("Прогресс: \u001B[32m" + bar + "\u001B[0m " + progress + "%");
        System.out.println("\u001B[35mИстория за 7 дней:\u001B[0m");
        LocalDate now = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = now.minusDays(i);
            String dateStr = d.toString();
            HistoryEntry found = null;
            for (HistoryEntry e : data.history) {
                if (e.date.equals(dateStr)) { found = e; break; }
            }
            System.out.println("  " + dateStr + ": " + (found != null ? found.steps : 0));
        }
    }

    private void setGoal(int goal) {
        data.goal = goal;
        save();
        System.out.println("\u001B[32mЦель установлена: " + goal + " шагов\u001B[0m");
    }

    private void history() {
        System.out.println("\u001B[36mИстория (все записи):\u001B[0m");
        data.history.sort(Comparator.comparing(e -> e.date));
        for (HistoryEntry e : data.history) {
            System.out.println("  " + e.date + ": " + e.steps);
        }
    }

    private void reset() {
        data.history.clear();
        save();
        System.out.println("\u001B[32mВсе данные сброшены.\u001B[0m");
    }

    private void exportCsv(String filename) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            pw.println("date,steps");
            for (HistoryEntry e : data.history) {
                pw.println(e.date + "," + e.steps);
            }
        }
        System.out.println("\u001B[32mЭкспортировано в " + filename + " (CSV)\u001B[0m");
    }

    public void run() throws Exception {
        load();
        if (addSteps != null) {
            addSteps(addSteps);
        } else if (show) {
            show();
        } else if (goal != null) {
            setGoal(goal);
        } else if (history) {
            history();
        } else if (reset) {
            reset();
        } else if (exportCsv != null) {
            exportCsv(exportCsv);
        } else {
            System.out.println("Используйте --help для справки.");
        }
    }

    public static void main(String[] args) throws Exception {
        StepCounter counter = new StepCounter();
        JCommander.newBuilder().addObject(counter).build().parse(args);
        counter.run();
    }
}
