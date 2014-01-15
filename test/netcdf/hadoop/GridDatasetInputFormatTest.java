package netcdf.hadoop;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GridDatasetInputFormatTest {

    public static String MODEL = GridDatasetConfigurationTest.MODEL;
    public static String URL = GridDatasetConfigurationTest.URL;

    private JobConf job;
    private GridDatasetConfiguration configuration;

    private String datatypes[];
    private DateTime timestamps[];

    @Before
    public void setUp() throws Exception {
        job = new JobConf();
        configuration = new GridDatasetConfiguration(job);
        configuration.configure(MODEL, URL);
        datatypes = GridDatasetConfiguration.datatypes(URL);
        timestamps = GridDatasetConfiguration.timestamps(URL);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGridDatasetInputFormat() {
        GridDatasetInputFormat input = new GridDatasetInputFormat();
        assert(GridDatasetInputFormat.class.isInstance(input));
    }

    @Test
    public void testSetInput() {
        GridDatasetInputFormat.setInput(job, MODEL, URL);
        GridDatasetConfiguration configuration = new GridDatasetConfiguration(job);
        assertEquals(MODEL, configuration.getModels()[0]);
        assertEquals(URL, configuration.getUrls()[0]);
        assertArrayEquals(datatypes, configuration.getDatatypes()[0]);
        assertArrayEquals(timestamps, configuration.getTimestamps()[0]);
    }

    @Test
    public void testSetInputWithDatatypes() {
        GridDatasetInputFormat.setInput(job, MODEL, URL, datatypes);
        GridDatasetConfiguration configuration = new GridDatasetConfiguration(job);
        assertEquals(MODEL, configuration.getModels()[0]);
        assertEquals(URL, configuration.getUrls()[0]);
        assertArrayEquals(datatypes, configuration.getDatatypes()[0]);
        assertArrayEquals(timestamps, configuration.getTimestamps()[0]);
    }

    @Test
    public void testGetSplits() throws IOException {
        GridDatasetInputFormat input = new GridDatasetInputFormat();
        InputSplit[] splits = input.getSplits(job, -1);
        assertEquals(configuration.getDatatypes()[0].length, splits.length);
    }

}
