package me.falu.runbenchmark;

import com.google.gson.*;
import com.redlimerl.speedrunigt.option.SpeedRunOptions;
import com.redlimerl.speedrunigt.timer.InGameTimer;
import com.redlimerl.speedrunigt.timer.category.RunCategories;
import me.falu.runbenchmark.gui.AverageOverlay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import org.apache.logging.log4j.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class RunBenchmark implements ClientModInitializer {
    public static final ModContainer MOD_CONTAINER = FabricLoader.getInstance().getModContainer("runbenchmark").orElseThrow(RuntimeException::new);
    public static final String MOD_NAME = MOD_CONTAINER.getMetadata().getName();
    public static final String MOD_VERSION = String.valueOf(MOD_CONTAINER.getMetadata().getVersion());
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final File FOLDER = new File(System.getProperty("user.home")).toPath().resolve(MOD_NAME).toFile();
    public static final File FILE = FOLDER.toPath().resolve("timelines.json").toFile();
    public static long LAST_TIMESTAMP = 0L;
    public static final List<BenchmarkTypes> COMPLETED_TYPES = new ArrayList<>();
    public static SpeedRunOptions.TimerDecimals TIMER_DECIMALS = SpeedRunOptions.TimerDecimals.THREE;

    private static void createFiles() throws IOException {
        if (!FOLDER.exists()) { boolean ignored = FOLDER.mkdirs(); }
        if (!FILE.exists()) { boolean ignored = FILE.createNewFile(); }
    }

    public static JsonObject getTimelines() {
        try {
            createFiles();

            FileReader reader = new FileReader(FILE);
            JsonParser parser = new JsonParser();

            Object obj = parser.parse(reader);
            reader.close();

            return obj == null || obj.equals(JsonNull.INSTANCE) ? new JsonObject() : (JsonObject) obj;
        } catch (IOException ignored) {}

        return new JsonObject();
    }

    public static void addTime(BenchmarkTypes timeline) {
        if (COMPLETED_TYPES.contains(timeline) || (!timeline.equals(BenchmarkTypes.END) && InGameTimer.getInstance().isCompleted()) || !InGameTimer.getInstance().getCategory().equals(RunCategories.ANY)) { return; }
        try {
            JsonObject timelines = getTimelines();

            JsonArray times;
            if (!timelines.has(timeline.key)) { times = new JsonArray(); }
            else { times = timelines.get(timeline.key).getAsJsonArray(); }
            long time = InGameTimer.getInstance().getInGameTime(false);
            log(getTimeFormat(LAST_TIMESTAMP));
            log(getTimeFormat(time));
            log(getTimeFormat(time - LAST_TIMESTAMP));
            times.add(time - LAST_TIMESTAMP);
            LAST_TIMESTAMP = time;
            log(getTimeFormat(LAST_TIMESTAMP));
            timelines.add(timeline.key, times);
            COMPLETED_TYPES.add(timeline);

            FileWriter writer = new FileWriter(FILE);
            writer.write(GSON.toJson(timelines));
            writer.flush();
            writer.close();

            log("Added new time to timeline '" + timeline + "'.");

            AverageOverlay.getInstance().setAverages(getAverageTimes(timelines));
        } catch (IOException ignored) {}
    }

    public static Map<String, Long> getAverageTimes() {
        return getAverageTimes(getTimelines());
    }

    public static Map<String, Long> getAverageTimes(JsonObject timelines) {
        Map<String, Long> result = new HashMap<>();
        for (BenchmarkTypes type : BenchmarkTypes.values()) {
            if (timelines.has(type.key)) {
                JsonElement element = timelines.get(type.key);
                if (element instanceof JsonArray) {
                    JsonArray times = element.getAsJsonArray();
                    if (times.size() == 0) {
                        result.put(type.key, -1L);
                        continue;
                    }
                    long total = 0L;
                    int index = 0;
                    for (JsonElement element1 : times) {
                        if (element1.isJsonPrimitive()) {
                            total += element1.getAsLong();
                            index++;
                        }
                    }
                    long average = total / index;
                    result.put(type.key, average);
                }
            }
        }
        return result;
    }

    private static void addDefaultTimelines(JsonObject timelines) {
        try {
            boolean changed = false;
            for (BenchmarkTypes type : BenchmarkTypes.values()) {
                if (!timelines.has(type.key)) {
                    timelines.add(type.key, new JsonArray());
                    changed = true;
                }
            }
            if (changed) {
                FileWriter writer = new FileWriter(FILE);
                writer.write(GSON.toJson(timelines));
                writer.flush();
                writer.close();
            }
        } catch (IOException ignored) {}
    }

    private static String getNameFromId(String id) {
        StringBuilder text = new StringBuilder();
        boolean shouldCapitalise = true;
        for (Character c : id.toCharArray()) {
            if (shouldCapitalise) {
                text.append(c.toString().toUpperCase(Locale.ROOT));
                shouldCapitalise = false;
            } else if (c.equals('_')) {
                text.append(" ");
                shouldCapitalise = true;
            } else {
                text.append(c.toString().toLowerCase(Locale.ROOT));
            }
        }
        return text.toString();
    }

    public static String getTimeFormat(long time) {
        String millsString = String.format("%03d", time % 1000).substring(0, TIMER_DECIMALS.getNumber());
        int seconds = ((int) (time / 1000)) % 60;
        int minutes = ((int) (time / 1000)) / 60;
        if (minutes > 59) {
            int hours = minutes / 60;
            minutes = minutes % 60;
            if (TIMER_DECIMALS == SpeedRunOptions.TimerDecimals.NONE) {
                return String.format("%d:%02d:%02d", hours, minutes, seconds);
            }
            return String.format("%d:%02d:%02d.%s", hours, minutes, seconds, millsString);
        } else {
            if (TIMER_DECIMALS == SpeedRunOptions.TimerDecimals.NONE) {
                return String.format("%02d:%02d", minutes, seconds);
            }
            return String.format("%02d:%02d.%s", minutes, seconds, millsString);
        }
    }

    public static void log(Object msg) {
        LOGGER.log(Level.INFO, msg);
    }

    @Override
    public void onInitializeClient() {
        log("Using " + MOD_NAME + " v" + MOD_VERSION);

        JsonObject timelines = getTimelines();
        addDefaultTimelines(timelines);
        AverageOverlay.getInstance().setAverages(getAverageTimes(timelines));

        InGameTimer.onComplete(timer -> {
            if (timer.getCategory().equals(RunCategories.ANY)) {
                RunBenchmark.addTime(RunBenchmark.BenchmarkTypes.END);
            }
        });
    }

    public enum BenchmarkTypes {
        OVERWORLD(),
        NETHER(),
        POST_BLIND(),
        END();

        public final String key;
        public final String name;

        BenchmarkTypes() {
            this.key = this.name().toLowerCase(Locale.ROOT);
            this.name = getNameFromId(this.key);
        }
    }
}
