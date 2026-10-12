import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

/**
 * WeatherLocalSimulator:
 * Runs the MapReduce logic locally without needing a Hadoop installation.
 * It simulates:
 * 1. Read input (.dat file)
 * 2. Mapper phase: emit (Key, partial sums)
 * 3. Shuffle & Sort phase: group values by Key
 * 4. Reducer phase: add up sums, compute averages
 *
 * Writes the same files a real Hadoop run produces in the output directory:
 *   <outputDir>/part-r-00000   (machine-readable, imported into PostgreSQL by EcoNet)
 *   <outputDir>/report.txt     (human-readable table)
 */
public class WeatherLocalSimulator {

    public static void main(String[] args) {
        String inputFile = args.length >= 1 ? args[0] : "data/weather_data.dat";
        String outputDir = args.length >= 2 ? args[1] : "mapreduce_output";

        System.out.println("==================================================================");
        System.out.println("  Weather Analytics - Local MapReduce Execution Engine");
        System.out.println("==================================================================");
        System.out.println("Reading input dataset: " + inputFile);

        // Map + Shuffle phase storage: Key -> running partial sums
        Map<String, double[]> grouped = new TreeMap<>();
        int totalInputRecords = 0;

        // 1. MAPPER SIMULATION
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.toLowerCase().startsWith("date")) {
                    continue; // Skip header or blank lines
                }

                String[] tokens = line.split(",");
                if (tokens.length < 7) {
                    continue;
                }

                try {
                    String date = tokens[0].trim();
                    double windRad = Math.toRadians(Double.parseDouble(tokens[6].trim()));
                    double[] sums = new double[]{
                            1,
                            Double.parseDouble(tokens[1].trim()),
                            Double.parseDouble(tokens[2].trim()),
                            Double.parseDouble(tokens[3].trim()),
                            Double.parseDouble(tokens[4].trim()),
                            Double.parseDouble(tokens[5].trim()),
                            Math.sin(windRad),
                            Math.cos(windRad)
                    };
                    totalInputRecords++;

                    WeatherAggregate.addInto(grouped.computeIfAbsent("MONTH_" + date.substring(0, 7), k -> new double[WeatherAggregate.FIELDS]), sums);
                    WeatherAggregate.addInto(grouped.computeIfAbsent("OVERALL", k -> new double[WeatherAggregate.FIELDS]), sums);

                } catch (NumberFormatException ex) {
                    // Skip malformed records
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading input file: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        System.out.println("Mapper Phase completed: Processed " + totalInputRecords + " daily records.");
        System.out.println("Shuffle & Sort Phase: Grouped into " + grouped.size() + " distinct keys.");
        System.out.println("Reducer Phase: Computing averages and writing to: " + outputDir);

        // 2. REDUCER SIMULATION & OUTPUT
        new File(outputDir).mkdirs();
        try (BufferedWriter part = new BufferedWriter(new FileWriter(new File(outputDir, "part-r-00000")));
             BufferedWriter report = new BufferedWriter(new FileWriter(new File(outputDir, "report.txt")))) {

            report.write("========================================================================================================================\n");
            report.write("HADOOP MAPREDUCE OUTPUT: WEATHER ATTRIBUTE AVERAGES (VELLORE, OPEN-METEO DATA)\n");
            report.write("========================================================================================================================\n");
            report.write(String.format("%-18s | %-6s | %-12s | %-16s | %-14s | %-14s | %-16s | %-16s\n",
                    "GROUP KEY", "DAYS", "AVG TEMP", "AVG RAINFALL", "AVG HUMIDITY", "AVG PRESSURE", "AVG WIND SPEED", "MEAN WIND DIR"));
            report.write("------------------------------------------------------------------------------------------------------------------------\n");

            for (Map.Entry<String, double[]> entry : grouped.entrySet()) {
                String key = entry.getKey();
                double[] s = entry.getValue();
                double n = s[0];

                part.write(key + "\t" + WeatherAggregate.toAverages(s) + "\n");

                String row = String.format(Locale.ROOT, "%-18s | %-6d | %-10.2f C | %-13.2f mm | %-13.2f%% | %-10.2f hPa | %-12.2f km/h | %-12.2f deg\n",
                        key, (long) n, s[1] / n, s[2] / n, s[3] / n, s[4] / n, s[5] / n,
                        WeatherAggregate.meanWindDirection(s[6], s[7]));
                report.write(row);
                System.out.print(row);
            }
            report.write("========================================================================================================================\n");
        } catch (Exception e) {
            System.err.println("Error writing output: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        System.out.println("\nSuccessfully generated MapReduce output in: " + outputDir);
    }
}
