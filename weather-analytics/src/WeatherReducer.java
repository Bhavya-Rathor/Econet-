import java.io.IOException;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * WeatherReducer:
 * Aggregates all records for each key (month or overall) and computes the average:
 * - Average Temperature
 * - Average Rainfall
 * - Average Humidity
 * - Average Pressure
 * - Average Wind Speed
 * - Average Wind Direction
 */
public class WeatherReducer extends Reducer<Text, Text, Text, Text> {

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
