package netcdf.cascading;

import netcdf.hadoop.GridDatasetInputFormat;
import cascading.flow.FlowProcess;
import cascading.scheme.Scheme;
import cascading.scheme.SinkCall;
import cascading.scheme.SourceCall;
import cascading.tap.Tap;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;
import java.io.IOException;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.RecordReader;
import org.joda.time.DateTime;

@SuppressWarnings("serial")
public class GridDatasetScheme extends Scheme<JobConf, RecordReader, OutputCollector, Object[], Object[]> {

    private String model;
    private String url;
    private String[] datatypes;
    private DateTime[] timestamps;

    public GridDatasetScheme(String model, String url, String[] datatypes) {
    	this(model, url, datatypes, null);
    }

    public GridDatasetScheme(String model, String url, String[] datatypes, DateTime[] timestamps) {
        super(new Fields("model", "datatype", "timestamp", "latitude", "longitude", "value", "unit"));
        this.model = model;
        this.url = url;
        this.datatypes = datatypes;
        this.timestamps = timestamps;
    }

    public String getModel() {
	return model;
    }

    public String getUrl() {
	return url;
    }

    public String[] getDatatypes() {
	return datatypes;
    }

    public DateTime[] getTimestamps() {
	return timestamps;
    }

    // @Override
    // public void sourceInit(Tap tap, JobConf job) throws IOException {
    //     // a hack for MultiInputFormat to see that there is a child format
    //     FileInputFormat.setInputPaths(job, url);
    //     GridDatasetInputFormat.setInput(job, model, url, datatypes, timestamps);
    // }

    // @Override
    // public void sinkInit(Tap tap, JobConf job) throws IOException {
    //     throw new UnsupportedOperationException("Cannot be used as a sink");
    // }

    // @Override
    // public Tuple source(Object key, Object value) {
    //     return ((TupleWrapper) value).tuple;
    // }

    // @SuppressWarnings("rawtypes")
    // @Override
    // public void sink(TupleEntry entry, OutputCollector collector) throws IOException {
    //     throw new UnsupportedOperationException("Cannot be used as a sink.");
    // }

    @Override
    public boolean source(FlowProcess<JobConf> flowProcess, SourceCall<Object[], RecordReader> sourceCall) throws IOException {
	System.out.println("SOURCE");
	// TupleEntry entry =  sourceCall.getInput();
	return false;
    }

    @Override
    public void sourceConfInit(FlowProcess<JobConf> flowProcess, Tap<JobConf, RecordReader, OutputCollector> tap, JobConf conf) {
	System.out.println("SOURCE CONF INIT");
        // a hack for MultiInputFormat to see that there is a child format
        FileInputFormat.setInputPaths(conf, url);
        GridDatasetInputFormat.setInput(conf, model, url, datatypes, timestamps);
    }

    @Override
    public void sink(FlowProcess<JobConf> flowProcess, SinkCall<Object[], OutputCollector> sinkCall) throws IOException {
        throw new UnsupportedOperationException("Cannot be used as a sink.");
    }

    @Override
    public void sinkConfInit(FlowProcess<JobConf> flowProcess, Tap<JobConf, RecordReader, OutputCollector> tap, JobConf conf) {
        throw new UnsupportedOperationException("Cannot be used as a sink.");
    }

}
