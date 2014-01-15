package netcdf.cascading;

import cascading.tap.hadoop.io.HadoopTupleEntrySchemeIterator;
import cascading.flow.FlowProcess;
import cascading.tap.Tap;
import cascading.tuple.TupleEntryCollector;
import cascading.tuple.TupleEntryIterator;
import java.io.IOException;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.OutputCollector;
import org.joda.time.DateTime;

@SuppressWarnings("serial")
public class GridDatasetTap extends Tap<JobConf, RecordReader, OutputCollector> {

    String url;
    GridDatasetScheme scheme;

    public GridDatasetTap(String model, String url, String[] datatypes) {
    	this(model, url, datatypes, null);
    }

    public GridDatasetTap(String model, String url, String[] datatypes, DateTime[] timestamps) {
	this.scheme = new GridDatasetScheme(model, url, datatypes, timestamps);
        this.url = url;
        setScheme(scheme);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        GridDatasetTap other = (GridDatasetTap) obj;
        if (url == null) {
            if (other.url != null)
                return false;
        } else if (!url.equals(other.url))
            return false;
        return true;
    }

    // @Override
    // public boolean makeDirs(JobConf jc) throws IOException {
    //     return true;
    // }

    // @Override
    // public boolean deletePath(JobConf jc) throws IOException {
    //     return false;
    // }

    // @Override
    // public boolean pathExists(JobConf jc) throws IOException {
    //     return true;
    // }

    // @Override
    // public long getPathModified(JobConf jc) throws IOException {
    //     return System.currentTimeMillis();
    // }

    public Path getPath() {
        return new Path(url);
    }

    public GridDatasetScheme getScheme() {
        return scheme;
    }

    @Override
    public TupleEntryIterator openForRead(FlowProcess<JobConf> flowProcess, RecordReader input) throws IOException {
        // return new TupleEntryIterator(getSourceFields(), new TapIterator(this, conf));
    	// throw new UnsupportedOperationException("Not supported.");
	return new HadoopTupleEntrySchemeIterator(flowProcess, this, input);
    }

    @Override
    public TupleEntryCollector openForWrite(FlowProcess<JobConf> flowProcess, OutputCollector output) throws IOException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean createResource(JobConf conf) throws IOException {
    	return true;
    }

    @Override
    public boolean deleteResource(JobConf conf) throws IOException {
    	return false;
    }

    @Override
    public String getIdentifier() {
	return getPath().toString();
    }

    @Override
    public long getModifiedTime(JobConf conf) throws IOException {
    	return System.currentTimeMillis();
    }

    @Override
    public boolean resourceExists(JobConf conf) throws IOException {
    	return true;
    }

}
