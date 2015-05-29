import com.couchbase.beersample.CouchbaseService;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.FileReader;

import static org.junit.Assert.assertEquals;


public class TestCreateBeer
{
    private
    CouchbaseService couchbaseService;

    @Autowired
    @Before
    public void BeersController(CouchbaseService couchbaseService)
    {
        this.couchbaseService = couchbaseService;
    }


    @Test
    public void testCreateOneBeer()
    {
        String id = "JUNIT";
        boolean isok = true;

        try
        {
            // read the json file
            FileReader reader = new FileReader("beer.json");

            JSONParser jsonParser = new JSONParser();
            JSONObject beer = (JSONObject) jsonParser.parse(reader);

            if( !beer.get("name").equals("test-joan"))
            {
                isok = false;
            }
        }
        catch (Exception e) {
            isok = false;
        }

        assertEquals("A JSON beer document has NOT already been created.", isok, false);
    }
}