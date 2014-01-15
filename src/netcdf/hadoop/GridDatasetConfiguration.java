package netcdf.hadoop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.mapred.JobConf;
import org.joda.time.DateTime;

import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;

public class GridDatasetConfiguration {

    public static final String MODELS = "netcdf.model";
    public static final String URLS = "netcdf.url";
    public static final String DATATYPES = "netcdf.datatypes";
    public static final String TIMESTAMPS = "netcdf.timestamps";

    public JobConf job;

    public GridDatasetConfiguration(JobConf job) {
        this.job = job;
    }

    public void addDatatypes(String... datatypes) {
    	setDatatypes((String[][]) Helper.append(getDatatypes(), datatypes));
    }

    public void addModel(String model) {
    	setModels((String[]) Helper.append(getModels(), model));
    }

    public void addTimestamps(DateTime... timestamps) {
    	setTimestamps((DateTime[][]) Helper.append(getTimestamps(), timestamps));
    }

    public void addUrl(String url) {
    	setUrls((String[]) Helper.append(getUrls(), url));
    }

    public void configure(String model, String url) {
    	this.configure(model, url, null, null);
    }

    public void configure(String model, String url, String[] datatypes) {
    	this.configure(model, url, datatypes, null);
    }

    public void configure(String model, String url, String[] datatypes, DateTime[] timestamps) {
    	addModel(model);
    	addUrl(url);
    	if (datatypes == null || datatypes.length == 0) {
            addDatatypes(GridDatasetConfiguration.datatypes(url));
    	} else {
            addDatatypes(datatypes);
    	}
    	if (timestamps == null || timestamps.length == 0) {
            addTimestamps(GridDatasetConfiguration.timestamps(url));
    	} else {
            addTimestamps(timestamps);
    	}
    }

    public String[][] getDatatypes() {
    	return Helper.split(job.get(DATATYPES));
    }

    public String[] getModels() {
    	return job.getStrings(MODELS);
    }

    public DateTime[][] getTimestamps() {
    	return TimeHelper.split(job.get(TIMESTAMPS));
    }

    public String[] getUrls() {
    	return job.getStrings(URLS);
    }

    public void setDatatypes(String[][] datatypes) {
    	job.setStrings(DATATYPES, Helper.join(datatypes));
    }

    public void setModels(String... models) {
    	job.setStrings(MODELS, models);
    }

    public void setTimestamps(DateTime[][] timestamps) {
    	job.setStrings(TIMESTAMPS, TimeHelper.join(timestamps));
    }

    public void setUrls(String... urls) {
    	job.setStrings(URLS, urls);
    }

    public static String[] datatypes(String url) {
        GridDataset dataset = null;
        try {
            dataset = GridDataset.open(url);
            List<GridDatatype> datatypes = dataset.getGrids();
            ArrayList<String> names = new ArrayList<String>();
            for (int n = 0; n < datatypes.size(); ++n)
                names.add(datatypes.get(n).getName());
            return names.toArray(new String[0]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            closeDataset(dataset);
        }
    }

    public static DateTime[] timestamps(String url) {
        GridDataset dataset = null;
        try {
            dataset = GridDataset.open(url);
            GridCoordSystem coordinates = dataset.getGrids().get(0).getCoordinateSystem();
            return TimeHelper.toDateTimes(coordinates.getTimeAxis1D().getTimeDates());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            closeDataset(dataset);
        }

    }

    private static void closeDataset(GridDataset dataset) {
        try {
            if (dataset != null) dataset.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
