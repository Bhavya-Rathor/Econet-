import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

/**
 * WeatherLocalSimulator:
 * Runs the MapReduce logic locally without needing a Hadoop cluster or HDFS daemons.
 * It simulates:
 * 1. Read input (.dat file)
 * 2. Mapper phase: emit (Key, Value)
 * 3. Shuffle & Sort phase: group values by Key
 * 4. Reducer phase: compute averages and write to output file
 */
public class WeatherLocalSimulator {

    public static void main(String[] args) {
        String inputFile = "data/weather_data.dat";
        String outputFile = "mapreduce_output.txt";

        if (args.length >= 1) {
            inputFile = args[0];
        }
        if (args.length >= 2) {
            outputFile = args[1];
        }

        System.out.println("==================================================================");
        System.out.println("  Weather Analytics - Local MapReduce Execution Engine");
        System.out.println("==================================================================");
        System.out.println("Reading input dataset: " + inputFile);

        // Map phase storage: Key -> List of Values
        Map<String, List<double[]>> intermediateData = new TreeMap<>();

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
                    double temp = Double.parseDouble(tokens[1].trim());
                    double rainfall = Double.parseDouble(tokens[2].trim());
                    double humidity = Double.parseDouble(tokens[3].trim());
                    double pressure = Double.parseDouble(tokens[4].trim());
                    double windSpeed = Double.parseDouble(tokens[5].trim());
                    double windDir = Double.parseDouble(tokens[6].trim());

                    double[] attributes = new double[]{temp, rainfall, humidity, pressure, windSpeed, windDir};
                    totalInputRecords++;

                    // Emit by Month
                    String monthKey = "MONTH_" + date.substring(0, 7);
                    intermediateData.computeIfAbsent(monthKey, k -> new ArrayList<>()).add(attributes);

                    // Emit Overall
                    intermediateData.computeIfAbsent("OVERALL_6_MONTHS", k -> new ArrayList<>()).add(attributes);

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
        System.out.println("Shuffle & Sort Phase: Grouped into " + intermediateData.size() + " distinct keys.");
        System.out.println("Reducer Phase: Computing averages and writing to: " + outputFile);

        // 2. REDUCER SIMULATION & OUTPUT
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write("========================================================================================================================\n");
            writer.write("HADOOP MAPREDUCE OUTPUT: 6-MONTH WEATHER ATTRIBUTE AVERAGES\n");
            writer.write("========================================================================================================================\n");
            writer.write(String.format("%-18s | %-6s | %-12s | %-16s | %-14s | %-14s | %-16s | %-16s\n",
                    "GROUP KEY", "DAYS", "AVG TEMP", "AVG RAINFALL", "AVG HUMIDITY", "AVG PRESSURE", "AVG WIND SPEED", "AVG WIND DIR"));
            writer.write("------------------------------------------------------------------------------------------------------------------------\n");

            for (Map.Entry<String, List<double[]>> entry : intermediateData.entrySet()) {
                String key = entry.getKey();
                List<double[]> list = entry.getValue();

                int count = list.size();
                double sumTemp = 0.0;
                double sumRainfall = 0.0;
                double sumHumidity = 0.0;
                double sumPressure = 0.0;
                double sumWindSpeed = 0.0;
                double sumWindDir = 0.0;

                for (double[] d : list) {
                    sumTemp += d[0];
                    sumRainfall += d[1];
                    sumHumidity += d[2];
                    sumPressure += d[3];
                    sumWindSpeed += d[4];
                    sumWindDir += d[5];
                }

                double avgTemp = sumTemp / count;
                double avgRainfall = sumRainfall / count;
                double avgHumidity = sumHumidity / count;
                double avgPressure = sumPressure / count;
                double avgWindSpeed = sumWindSpeed / count;
                double avgWindDir = sumWindDir / count;

                String row = String.format("%-18s | %-6d | %-10.2f C | %-13.2f mm | %-13.2f%% | %-10.2f hPa | %-12.2f km/h | %-12.2f deg\n",
                        key, count, avgTemp, avgRainfall, avgHumidity, avgPressure, avgWindSpeed, avgWindDir);

                writer.write(row);
                System.out.print(row);
            }
            writer.write("========================================================================================================================\n");
        } catch (Exception e) {
            System.err.println("Error writing output file: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        System.out.println("\nSuccessfully generated MapReduce output file: " + outputFile);
    }
}
