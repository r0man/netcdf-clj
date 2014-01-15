package netcdf.cascading;

import cascading.tuple.Tuple;

public class TupleWrapper {

    public Tuple tuple;

    public TupleWrapper() {
    }

    public TupleWrapper(Tuple tuple) {
	this.tuple = tuple;
    }

}
