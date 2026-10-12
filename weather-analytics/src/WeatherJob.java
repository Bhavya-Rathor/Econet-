import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/**
 * WeatherJob: Hadoop MapReduce program to process 6-month daily weather data.
 * Attributes: Date, Temp, Rainfall, Humidity, Pressure, Wind Speed, Wind Direction.
 * Calculates average temperature, rainfall, and all weather attributes.
 */
public class WeatherJob extends Configured implements Tool {

    // ==========================================
    // MAPPER CLASS
    // ==========================================
    public static class WeatherMapper extends Mapper<LongWritable, Text, Text, Text> {

        private Text outKeyMonth = new Text();
        private Text outKeyOverall = new Text("OVERALL_6_MONTHS");
        private Text outValue = new Text();

        @Override
        protected void map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException {

            String line = value.toString().trim();

            // Skip empty lines or CSV header row
            if (line.isEmpty() || line.toLowerCase().startsWith("date")) {
                return;
            }

            String[] tokens = line.split(",");
            if (tokens.length < 7) {
                return; // Skip malformed lines
            }

            try {
                String date = tokens[0].trim();
                double temp = Double.parseDouble(tokens[1].trim());
                double rainfall = Double.parseDouble(tokens[2].trim());
                double humidity = Double.parseDouble(tokens[3].trim());
                double pressure = Double.parseDouble(tokens[4].trim());
                double windSpeed = Double.parseDouble(tokens[5].trim());
                double windDir = Double.parseDouble(tokens[6].trim());

                // Package all 6 attributes into comma-separated value string
                String valStr = String.format("%.2f,%.2f,%.2f,%.2f,%.2f,%.2f",
                        temp, rainfall, humidity, pressure, windSpeed, windDir);
                outValue.set(valStr);

                // 1. Emit grouped by Month (e.g. "MONTH_2026-04")
                if (date.length() >= 7) {
                    String month = date.substring(0, 7);
                    outKeyMonth.set("MONTH_" + month);
                    context.write(outKeyMonth, outValue);
                }

                // 2. Emit for Overall 6-Month calculation
                context.write(outKeyOverall, outValue);

            } catch (NumberFormatException e) {
                // Ignore lines with parsing errors
            }
        }
    }

    // ==========================================
    // REDUCER CLASS
    // ==========================================
    public static class WeatherReducer extends Reducer<Text, Text, Text, Text> {

        private Text resultValue = new Text();

        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context)
                throws IOException, InterruptedException {

            int count = 0;
            double sumTemp = 0.0;
            double sumRainfall = 0.0;
            double sumHumidity = 0.0;
            double sumPressure = 0.0;
            double sumWindSpeed = 0.0;
            double sumWindDir = 0.0;

            for (Text val : values) {
                String[] parts = val.toString().split(",");
                if (parts.length == 6) {
                    sumTemp += Double.parseDouble(parts[0]);
                    sumRainfall += Double.parseDouble(parts[1]);
                    sumHumidity += Double.parseDouble(parts[2]);
                    sumPressure += Double.parseDouble(parts[3]);
                    sumWindSpeed += Double.parseDouble(parts[4]);
                    sumWindDir += Double.parseDouble(parts[5]);
                    count++;
                }
            }

            if (count > 0) {
                double avgTemp = sumTemp / count;
                double avgRainfall = sumRainfall / count;
                double avgHumidity = sumHumidity / count;
                double avgPressure = sumPressure / count;
                double avgWindSpeed = sumWindSpeed / count;
                double avgWindDir = sumWindDir / count;

                String formattedOutput = String.format(
                        "Days=%d | Avg Temp=%.2f C | Avg Rainfall=%.2f mm | Avg Humidity=%.2f%% | Avg Pressure=%.2f hPa | Avg Wind Speed=%.2f km/h | Avg Wind Dir=%.2f deg",
                        count, avgTemp, avgRainfall, avgHumidity, avgPressure, avgWindSpeed, avgWindDir
                );

                resultValue.set(formattedOutput);
                context.write(key, resultValue);
            }
        }
    }

    // ==========================================
    // TOOL RUNNER IMPLEMENTATION
    // ==========================================
    @Override
    public int run(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: WeatherJob <input path> <output path>");
            return -1;
        }

        Configuration conf = getConf();
        
        // If paths are local or if local flag is given, ensure local file system is used
        if (args.length >= 3 && args[2].equalsIgnoreCase("local")) {
            conf.set("fs.defaultFS", "file:///");
            conf.set("mapreduce.framework.name", "local");
        }
        
        Job job = Job.getInstance(conf, "Weather Averages Analyzer");

        job.setJarByClass(WeatherJob.class);
        job.setMapperClass(WeatherMapper.class);
        job.setReducerClass(WeatherReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        return job.waitForCompletion(true) ? 0 : 1;
    }

    // ==========================================
    // MAIN METHOD
    // ==========================================
    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        int res = ToolRunner.run(conf, new WeatherJob(), args);
        System.exit(res);
    }
}
