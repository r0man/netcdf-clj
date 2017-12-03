package netcdf.hadoop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import org.apache.hadoop.mapred.JobConf;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GridDatasetConfigurationTest {

    static String MODEL = "akw";
    static String URL = new File("test-resources/akw-htsgwsfc-2014-01-14T00.nc").getAbsolutePath();

    String AKW = "http://nomads.ncep.noaa.gov:9090/dods/wave/akw/akw20110331/akw20110331_00z";
    String NWW3 = "http://nomads.ncep.noaa.gov:9090/dods/wave/nww3/nww320110331/nww320110331_00z";

    private JobConf job;
    private GridDatasetConfiguration configuration;

    @Before
    public void setUp() throws Exception {
        job = new JobConf();
        configuration = new GridDatasetConfiguration(job);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testConfigure() {
        configuration.configure(MODEL, URL);
        assertEquals(MODEL, configuration.getModels()[0]);
        assertEquals(URL, configuration.getUrls()[0]);
        String[][] datatypes = configuration.getDatatypes();
        assertEquals(1, datatypes.length);
        assertEquals(1, datatypes[0].length);
        assertEquals("htsgwsfc", datatypes[0][0]);
        DateTime[][] timestamps = configuration.getTimestamps();
        assertEquals(1, timestamps.length);
        assertEquals(61, timestamps[0].length);
    }

    @Test
    public void testAddModel() {
        configuration.addModel("akw");
        assertEquals("akw", configuration.job.get(GridDatasetConfiguration.MODELS));
        configuration.addModel("nww3");
        assertEquals("akw,nww3", configuration.job.get(GridDatasetConfiguration.MODELS));
    }

    @Test
    public void testAddDatatypes() {
        String datatypes[] = {"windsfc", "htsgwsfc"};
        configuration.addDatatypes(datatypes);
        assertEquals("windsfc#htsgwsfc", configuration.job.get(GridDatasetConfiguration.DATATYPES));
        String datatypes2[] = {"wdirsfc", "dirswsfc"};
        configuration.addDatatypes(datatypes2);
        assertEquals("windsfc#htsgwsfc,wdirsfc#dirswsfc", configuration.job.get(GridDatasetConfiguration.DATATYPES));
    }

    @Test
    public void testAddUrl() {
        configuration.addUrl(URL);
        assertEquals(URL, configuration.job.get(GridDatasetConfiguration.URLS));
        configuration.addUrl(URL + "/other");
        assertEquals(URL + "," + URL + "/other", configuration.job.get(GridDatasetConfiguration.URLS));
    }

    @Test
    public void testAddTimestamps() {
        DateTime timestamps[] = {new DateTime("2011-08-05T00:00:00Z"), new DateTime("2011-08-05T03:00:00Z")};
        configuration.addTimestamps(timestamps);
        assertEquals("2011-08-05T00:00:00Z#2011-08-05T03:00:00Z", configuration.job.get(GridDatasetConfiguration.TIMESTAMPS));
        DateTime timestamps2[] = {new DateTime("2011-08-06T00:00:00Z"), new DateTime("2011-08-06T03:00:00Z")};
        configuration.addTimestamps(timestamps2);
        assertEquals("2011-08-05T00:00:00Z#2011-08-05T03:00:00Z,2011-08-06T00:00:00Z#2011-08-06T03:00:00Z",
                     configuration.job.get(GridDatasetConfiguration.TIMESTAMPS));
    }

    @Test
    public void testGetModels() {
        assertNull(configuration.getModels());
        configuration.job.set(GridDatasetConfiguration.MODELS, "akw");
        assertEquals("akw", configuration.getModels()[0]);
        configuration.job.set(GridDatasetConfiguration.MODELS, "akw,nww3");
        assertEquals("nww3", configuration.getModels()[1]);
    }

    @Test
    public void testGetDatatypes() {
        String datatypes[][] = configuration.getDatatypes();
        assertEquals(0, datatypes.length);
        configuration.job.set(GridDatasetConfiguration.DATATYPES, "windsfc#htsgwsfc,wdirsfc#dirswsfc");
        datatypes = configuration.getDatatypes();
        assertEquals("windsfc", datatypes[0][0]);
        assertEquals("htsgwsfc", datatypes[0][1]);
        assertEquals("wdirsfc", datatypes[1][0]);
        assertEquals("dirswsfc", datatypes[1][1]);
    }

    @Test
    public void testGetTimestamps() {
        DateTime timestamps[][] = configuration.getTimestamps();
        assertEquals(0, timestamps.length);
        configuration.job.set(GridDatasetConfiguration.TIMESTAMPS, "2011-08-05T02:00:00+02:00#2011-08-05T05:00:00+02:00,2011-08-06T02:00:00+02:00#2011-08-06T05:00:00+02:00");
        timestamps = configuration.getTimestamps();
        assertEquals(new DateTime("2011-08-05T02:00:00+02:00"), timestamps[0][0]);
        assertEquals(new DateTime("2011-08-05T05:00:00+02:00"), timestamps[0][1]);
        assertEquals(new DateTime("2011-08-06T02:00:00+02:00"), timestamps[1][0]);
        assertEquals(new DateTime("2011-08-06T05:00:00+02:00"), timestamps[1][1]);
    }

    @Test
    public void testGetUrls() {
        assertNull(configuration.getUrls());
        configuration.job.set(GridDatasetConfiguration.URLS, URL);
        assertEquals(URL, configuration.getUrls()[0]);
        configuration.job.set(GridDatasetConfiguration.URLS, URL + "," + URL + "/other");
        assertEquals(URL, configuration.getUrls()[0]);
        assertEquals(URL + "/other", configuration.getUrls()[1]);
    }

    @Test
    public void testSetDatatypes() {
        String datatypes[][] = {{"windsfc", "htsgwsfc"}};
        configuration.setDatatypes(datatypes);
        assertEquals("windsfc#htsgwsfc", configuration.job.get(GridDatasetConfiguration.DATATYPES));
        String datatypes2[][] = {{"windsfc", "htsgwsfc"}, {"wdirsfc", "dirswsfc"}};
        configuration.setDatatypes(datatypes2);
        assertEquals("windsfc#htsgwsfc,wdirsfc#dirswsfc", configuration.job.get(GridDatasetConfiguration.DATATYPES));
    }

    @Test
    public void testSetModels() {
        String models[] = {"akw"};
        configuration.setModels(models);
        assertEquals("akw", job.get(GridDatasetConfiguration.MODELS));
        String models2[] = {"akw", "nww3"};
        configuration.setModels(models2);
        assertEquals("akw,nww3", job.get(GridDatasetConfiguration.MODELS));
    }

    @Test
    public void testSetTimestamps() {
        DateTime timestamps[][] = {{new DateTime("2011-08-05T00:00:00Z"), new DateTime("2011-08-05T03:00:00Z")}};
        configuration.setTimestamps(timestamps);
        assertEquals("2011-08-05T00:00:00Z#2011-08-05T03:00:00Z", configuration.job.get(GridDatasetConfiguration.TIMESTAMPS));
        DateTime timestamps2[][] = {
            {new DateTime("2011-08-05T00:00:00Z"), new DateTime("2011-08-05T03:00:00Z")},
            {new DateTime("2011-08-06T00:00:00Z"), new DateTime("2011-08-06T03:00:00Z")}
        };
        configuration.setTimestamps(timestamps2);
        assertEquals("2011-08-05T00:00:00Z#2011-08-05T03:00:00Z,2011-08-06T00:00:00Z#2011-08-06T03:00:00Z",
                     configuration.job.get(GridDatasetConfiguration.TIMESTAMPS));
    }

    @Test
    public void testSetUrl() {
        String urls[] = {URL};
        configuration.setUrls(urls);
        assertEquals(URL, job.get(GridDatasetConfiguration.URLS));
        String urls2[] = {URL, URL};
        configuration.setUrls(urls2);
        assertEquals(URL + "," + URL, job.get(GridDatasetConfiguration.URLS));
    }

    @Test
    public void testGridDatasetConfiguration() {
        GridDatasetConfiguration configuration = new GridDatasetConfiguration(job);
        assert(GridDatasetConfiguration.class.isInstance(configuration));
    }

}
