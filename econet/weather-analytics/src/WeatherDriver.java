import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/**
 * WeatherDriver:
 * Configures and launches the Hadoop MapReduce job.
 * Uses ToolRunner so generic options work, e.g. run without HDFS daemons:
 *   hadoop jar weather.jar WeatherDriver -D fs.defaultFS=file:/// -D mapreduce.framework.name=local in.dat out_dir
 */
public class WeatherDriver extends Configured implements Tool {

    @Override
    public int run(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: hadoop jar weather.jar WeatherDriver [generic options] <input path> <output path>");
            return -1;
        }

        Job job = Job.getInstance(getConf(), "Weather Averages Analyzer");

        job.setJarByClass(WeatherDriver.class);
        job.setMapperClass(WeatherMapper.class);
        job.setCombinerClass(WeatherCombiner.class);
        job.setReducerClass(WeatherReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        return job.waitForCompletion(true) ? 0 : 1;
    }

    public static void main(String[] args) throws Exception {
        System.exit(ToolRunner.run(new Configuration(), new WeatherDriver(), args));
    }
}
