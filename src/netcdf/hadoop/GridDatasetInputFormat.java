package netcdf.hadoop;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.util.StringUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.unidata.geoloc.LatLonPoint;
import netcdf.cascading.TupleWrapper;
import cascading.tuple.Tuple;

public class GridDatasetInputFormat implements InputFormat<NullWritable, TupleWrapper> {

    private static final Logger LOG = Logger.getLogger(GridDatasetInputFormat.class);

    public static class GridDatasetRecordReader implements RecordReader<NullWritable, TupleWrapper> {

        private GridCoordSystem coords;
        private GridDataset dataset;
        private GridDatasetInputSplit split;
        private GridDatatype datatype;

        private Array slice;
        private IndexIterator iterator;
        private DateTime splitStartedAt;
        private DateTime timestamp;
        private Iterator<DateTime> timestamps;

        private String unit;
        private String model;
        private String variable;

        private long pos = 0;
        private long total = 0;

	private PeriodFormatter formatter;
	private DateTime sliceStartedAt;

        protected GridDatasetRecordReader(GridDatasetInputSplit split, JobConf job) throws IOException {
            LOG.info("NetCDF Dataset: " + split.getUrl());
            this.dataset = GridDataset.open(split.getUrl());
            this.datatype = dataset.findGridDatatype(split.getDatatype());
            this.split = split;
            this.splitStartedAt = new DateTime();
            this.timestamps = Arrays.asList(split.getTimestamps()).iterator();
            this.unit = datatype.getUnitsString();
            this.model = split.getModel();
            this.variable = split.getDatatype();
            this.coords = datatype.getCoordinateSystem();
            this.formatter = new PeriodFormatterBuilder().
		appendMinutes().appendSuffix(" min, ").
		appendSeconds().appendSuffix(" sec, ").
		appendMillis().appendSuffix(" ms").
		printZeroRarelyFirst().
		toFormatter();
            LOG.info("      Datatype: " + datatype.getName());
            LOG.info("   Description: " + datatype.getDescription());
            LOG.info("   Coordinates: " + datatype.getCoordinateSystem());
        }

        @Override
        public void close() throws IOException {
            this.slice = null;
            this.iterator = null;
            this.dataset.close();
            LOG.info("   Total Bytes: " + StringUtils.byteDesc(total) + ", " + pos + " records");
            LOG.info("Total Duration: " + formatter.print(new Period(splitStartedAt, new DateTime())));
            LOG.info("NetCDF dataset " + split.getUrl() + " closed.");
        }

        @Override
        public NullWritable createKey() {
            return NullWritable.get();
        }

        @Override
        public TupleWrapper createValue() {
            return new TupleWrapper(new Tuple());
        }

        @Override
        public long getPos() throws IOException {
            return pos;
        }

        @Override
        public float getProgress() throws IOException {
            return slice == null ? 0 : (pos / (float) (slice.getSize() * split.getTimestamps().length));
        }

        public LatLonPoint getLocation(long pos) {
            return coords.getLatLon(getX(pos), getY(pos));
        }

        public long getHeight() {
            return coords.getYHorizAxis().getSize();
        }

        public DateTime getTimestamp() {
            return timestamp;
        }

        public long getWidth() {
            return coords.getXHorizAxis().getSize();
        }

        public int getX(long pos) {
            return (int) (pos % this.getWidth());
        }

        public int getY(long pos) {
            return (int) ((pos / this.getWidth()) % getHeight());
        }

        @Override
        public boolean next(NullWritable key, TupleWrapper value) throws IOException {

            if (iterator == null || !iterator.hasNext()) {

            	iterator = null;

                if (!timestamps.hasNext()) {
                    return false;
                }

                if (sliceStartedAt != null) {
		    LOG.info("      Duration: " + formatter.print(new Period(sliceStartedAt, new DateTime())));
                }

                sliceStartedAt = new DateTime();
                slice = readDataSlice(timestamp = timestamps.next());
                iterator = slice.getIndexIterator();

            }

            LatLonPoint location = this.getLocation(pos);

            value.tuple = new Tuple();
            value.tuple.add(model);
            value.tuple.add(variable);
            value.tuple.add(timestamp.getMillis());
            value.tuple.add(location.getLatitude());
            value.tuple.add(location.getLongitude());
            value.tuple.add(iterator.next());
            value.tuple.add(unit);

            pos++;
            return true;

        }

        Array readDataSlice(DateTime timestamp) throws IOException {
            int timeIndex = coords.getTimeAxis1D().findTimeIndexFromDate(timestamp.toDate());
            Array slice = datatype.readDataSlice(timeIndex, -1, -1, -1);
	    //            Array slice = datatype.readVolumeData(timeIndex);
            LOG.info("     Timestamp: " + timestamp);
            LOG.info("         Bytes: " + StringUtils.byteDesc(slice.getSizeBytes()) + ", " + slice.getSize() + " records");
            total += slice.getSizeBytes();
            return slice;
        }

    }

    protected static class GridDatasetInputSplit implements InputSplit {

    	private String model;
    	private String url;
    	private String datatype;
    	private DateTime[] timestamps;

        public GridDatasetInputSplit() {
        }

        public GridDatasetInputSplit(String model, String dataset, String datatype, DateTime... timestamps) {
            this.model = model;
            this.url = dataset;
            this.datatype = datatype;
            this.timestamps = timestamps;
        }

        public String getDatatype() {
            return datatype;
        }

        @Override
        public long getLength() throws IOException {
            return 1;
        }

        @Override
        public String[] getLocations() throws IOException {
            return new String[]{};
        }

        public String getModel() {
            return model;
        }

        public DateTime[] getTimestamps() {
            return timestamps;
        }

        public String getUrl() {
            return url;
        }

        @Override
        public void readFields(DataInput input) throws IOException {
            model = WritableUtils.readString(input);
            url = WritableUtils.readString(input);
            datatype = WritableUtils.readString(input);
            timestamps = TimeHelper.parseTimes(WritableUtils.readStringArray(input));
        }

        @Override
        public void write(DataOutput output) throws IOException {
            WritableUtils.writeString(output, model);
            WritableUtils.writeString(output, url);
            WritableUtils.writeString(output, datatype);
            WritableUtils.writeStringArray(output, TimeHelper.formatTimes(timestamps));
        }

    }

    @Override
    public RecordReader<NullWritable, TupleWrapper> getRecordReader(InputSplit split, JobConf job, Reporter reporter) throws IOException {
        return new GridDatasetRecordReader((GridDatasetInputSplit) split, job);
    }

    @Override
    public InputSplit[] getSplits(JobConf job, int ignored) throws IOException {

        GridDatasetConfiguration configuration = new GridDatasetConfiguration(job);
        ArrayList<InputSplit> splits = new ArrayList<InputSplit>();

        for (int i = 0; i < configuration.getModels().length; ++i) {
            for (String datatype: configuration.getDatatypes()[i]) {
                String model = configuration.getModels()[i];
                String url = configuration.getUrls()[i];
                DateTime[] timestamps = configuration.getTimestamps()[i];
                splits.add(new GridDatasetInputSplit(model, url, datatype, timestamps));
            }
        }

        // for (int i = 0; i < configuration.getModels().length; ++i) {
        //     for (String datatype: configuration.getDatatypes()[i]) {
        //     	for (DateTime timestamp: configuration.getTimestamps()[i]) {
        //             String model = configuration.getModels()[i];
        //             String url = configuration.getUrls()[i];
        //             DateTime[] timestamps = {timestamp};
        //             splits.add(new GridDatasetInputSplit(model, url, datatype, timestamps));

        //     	}
        //     }
        // }

        return splits.toArray(new InputSplit[0]);

    }

    public static void setInput(JobConf job, String model, String url) {
        setInput(job, model, url, null);
    }

    public static void setInput(JobConf job, String model, String url, String[] datatypes) {
        setInput(job, model, url, datatypes, null);
    }

    public static void setInput(JobConf job, String model, String url, String[] datatypes, DateTime[] timestamps) {
        job.setInputFormat(GridDatasetInputFormat.class);
        GridDatasetConfiguration configuration = new GridDatasetConfiguration(job);
        configuration.configure(model, url, datatypes, timestamps);
    }

}
