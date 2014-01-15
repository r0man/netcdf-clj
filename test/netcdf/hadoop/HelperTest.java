package netcdf.hadoop;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class HelperTest {

    @Test
    public void testJoin() {
        String datatypes[][] = {{"windsfc", "htsgwsfc"}, {"wdirsfc", "dirswsfc"}};
        assertEquals("windsfc#htsgwsfc,wdirsfc#dirswsfc", Helper.join(datatypes));
    }

    @Test
    public void testSplit() {
        String[][] datatypes = Helper.split("windsfc#htsgwsfc,wdirsfc#dirswsfc");
        assertEquals("windsfc", datatypes[0][0]);
        assertEquals("htsgwsfc", datatypes[0][1]);
        assertEquals("wdirsfc", datatypes[1][0]);
        assertEquals("dirswsfc", datatypes[1][1]);
    }

}
