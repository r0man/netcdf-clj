package netcdf.hadoop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ucar.unidata.geoloc.LatLonPoint;
import netcdf.cascading.TupleWrapper;
import netcdf.hadoop.GridDatasetInputFormat.GridDatasetRecordReader;
import cascading.tuple.Tuple;

public class GridDatasetRecordReaderTest {

    private JobConf job;
    private GridDatasetConfiguration configuration;
    private GridDatasetRecordReader reader;
    private GridDatasetInputFormat inputFormat;

    NullWritable key = NullWritable.get();
    TupleWrapper value = new TupleWrapper(new Tuple());

    @Before
    public void setUp() throws Exception {
	//    	PropertyConfigurator.configure("resources/log4j.properties");
        job = new JobConf();
        job.setInputFormat(GridDatasetInputFormat.class);
        configuration = new GridDatasetConfiguration(job);
        configuration.configure(GridDatasetInputFormatTest.MODEL, GridDatasetInputFormatTest.URL);
        inputFormat = (GridDatasetInputFormat) job.getInputFormat();
        InputSplit[] splits = inputFormat.getSplits(job, -1);
        reader = (GridDatasetRecordReader) inputFormat.getRecordReader(splits[0], job, null);
    }

    @After
    public void tearDown() throws Exception {
    	reader.close();
    }

    @Test
    public void testGetHeight() throws IOException {
        assertEquals(123, reader.getHeight());
    }

    @Test
    public void testGetLocation() throws IOException {
        LatLonPoint location = reader.getLocation(0);
        assertEquals(44.75, location.getLatitude(), 0);
        assertEquals(159.5, location.getLongitude(), 0);
    }

    @Test
    public void testGetPos() throws IOException {
        assertEquals(0, reader.getPos());
    }

    @Test
    public void testGetTimestamp() throws IOException {
        assertNull(reader.getTimestamp());
        reader.next(key, value);
        assertEquals(new DateTime("2014-01-16T01:00:00.000+01:00"), reader.getTimestamp());
    }

    @Test
    public void testGetWidth() throws IOException {
        assertEquals(155, reader.getWidth());
    }

    @Test
    public void testGetX() throws IOException {
        assertEquals(0, reader.getX(0));
        assertEquals(1, reader.getX(1));
    }
    @Test
    public void testGetY() throws IOException {
        assertEquals(0, reader.getY(0));
        assertEquals(1, reader.getY(reader.getWidth()));
    }

    @Test
    public void testNext() throws IOException {
    	reader.next(key, value);
    	assertEquals("akw", value.tuple.getString(0));
    	assertEquals("htsgwsfc", value.tuple.getString(1));
    	assertEquals(1389830400000L, value.tuple.getLong(2));
    	assertEquals(44.75, value.tuple.getDouble(3), 0);
    	assertEquals(159.5, value.tuple.getDouble(4), 0);
    	assertEquals(Double.NaN, value.tuple.getDouble(5), 0);
    	reader.next(key, value);
    	assertEquals("akw", value.tuple.getString(0));
    	assertEquals("htsgwsfc", value.tuple.getString(1));
    	assertEquals(1389830400000L, value.tuple.getLong(2));
    	assertEquals(44.75, value.tuple.getDouble(3), 0);
    	assertEquals(160, value.tuple.getDouble(4), 0);
    	assertEquals(Double.NaN, value.tuple.getDouble(5), 0);
    }

    @Test
    public void testBenchmark() throws IOException {
    	while (reader.next(key, value)) {
	    // System.out.println(reader.getProgress());
    	};
    }

}
