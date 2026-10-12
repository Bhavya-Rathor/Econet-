import java.io.IOException;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * WeatherReducer:
 * Adds up all partial sums for each key (month or overall) and computes:
 * - Average Temperature
 * - Average Rainfall
 * - Average Humidity
 * - Average Pressure
 * - Average Wind Speed
 * - Mean Wind Direction (circular mean from summed sin/cos components)
 *
 * Output line: MONTH_2026-04<TAB>days=30,temp=..,rainfall=..,humidity=..,pressure=..,wind_speed=..,wind_dir=..
 */
public class WeatherReducer extends Reducer<Text, Text, Text, Text> {

    private Text resultValue = new Text();

    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {

        double[] acc = new double[WeatherAggregate.FIELDS];
        for (Text val : values) {
            WeatherAggregate.addInto(acc, WeatherAggregate.parse(val.toString()));
        }

        if (acc[0] > 0) {
            resultValue.set(WeatherAggregate.toAverages(acc));
            context.write(key, resultValue);
        }
    }
}
