package netcdf.hadoop;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.hadoop.util.StringUtils;

import ucar.unidata.util.StringUtil;

public class Helper {

    public static String[] append(String[] array, String element) {
        String[] result;
        if (array == null) {
            result = new String[1];
        } else {
            result = Arrays.copyOf(array, array.length + 1);
            System.arraycopy(array, 0, result, 0, array.length);
        }
        result[result.length - 1] = element;
        return result;
    }

    public static Object[][] append(Object[][] arrays, Object[] array) {
        Object[][] result;
        if (array == null) {
            result = new Object[1][];
        } else {
            result = Arrays.copyOf(arrays, arrays.length + 1);
            System.arraycopy(arrays, 0, result, 0, arrays.length);
        }
        result[result.length - 1] = array;
        return result;
    }

    public static String join(String[][] datatypes) {
        ArrayList<String> array = new ArrayList<String>();
        for (String[] nested: datatypes) {
            array.add(StringUtils.join("#", Arrays.asList(nested)));
        }
        return StringUtils.join(",", array);
    }

    public static String[][] split(String string) {
        ArrayList<String[]> array = new ArrayList<String[]>();
        for (String substring: StringUtil.split(string)) {
            array.add(StringUtils.split(substring, '\\', '#'));
        }
        return array.toArray(new String[0][]);
    }

}
