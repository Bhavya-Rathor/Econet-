import java.io.IOException;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * WeatherCombiner:
 * Runs on the mapper side and adds up partial sums for each key before the shuffle,
 * so only one record per key per mapper crosses the network instead of one per day.
 * Input and output have the same format, so Hadoop may run it zero, one or many times.
 */
public class WeatherCombiner extends Reducer<Text, Text, Text, Text> {

    private Text outValue = new Text();

    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {

        double[] acc = new double[WeatherAggregate.FIELDS];
        for (Text val : values) {
            WeatherAggregate.addInto(acc, WeatherAggregate.parse(val.toString()));
        }
        outValue.set(WeatherAggregate.format(acc));
        context.write(key, outValue);
    }
}
