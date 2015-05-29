/**
 * Copyright (C) 2014 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */
package com.couchbase.beersample.beers;

import com.couchbase.beersample.CouchbaseService;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.DocumentAlreadyExistsException;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.couchbase.client.java.view.AsyncViewResult;
import com.couchbase.client.java.view.ViewResult;
import com.couchbase.client.java.view.ViewRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rx.functions.Func1;

import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * REST CRUD Controller for beers
 *
 * @author Simon Baslé
 * @since 1.0
 */
@RestController
@RequestMapping("/beer")
public class BeersController {

    private final CouchbaseService couchbaseService;
    private boolean isStarted=false;

    @Autowired
    public BeersController(CouchbaseService couchbaseService) {
        this.couchbaseService = couchbaseService;
    }



    /**
     * Start method to enable auto creation of beer entities to a couchbase bucket
     *
     * @param number
     * @return json confirmation
     */
    @RequestMapping(method = RequestMethod.GET, value = "/start/{number}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> startBeersCreation(@PathVariable String number)
    {
        try
        {
            if( !isStarted )
            {
                isStarted = true;

                String id = "";
                String name = "";
                String abv = "";
                int beersnumber = Integer.parseInt(number);
                JsonDocument doc = null;
                GregorianCalendar cal = null;
                String[] types = {"ale","beer","brewing","pale","pale ale","lager","pilsen","malt","black","melt"};
                int i = 1;

                while( isStarted && i<=beersnumber )
                {
                    cal = new GregorianCalendar();
                    long timestamp = cal.getTimeInMillis();
                    JsonObject beer = JsonObject.create();
                    id = "beer-id-" + timestamp;
                    beer.put("id", id);
                    name = "beer" + timestamp;
                    beer.put("name", name);
                    beer.put("type", types[this.getRandomNum(1,10)]);
                    abv = Integer.toString( this.getRandomNum(1,10));
                    beer.put("abv", abv);
                    doc = CouchbaseService.createDocument(id, beer);
                    couchbaseService.create(doc);
                    i++;

                    // Time wait 1 second
                    Thread.sleep(1000);
                }

                isStarted = false;

                // Response OK
                JsonObject docrsp = JsonObject.create();
                docrsp.put("status", "OK");
                doc = CouchbaseService.createDocument("response", docrsp);
                return new ResponseEntity<String>(doc.content().toString(), HttpStatus.OK);
            }
            else
            {
                // Response ERROR
                JsonObject docrsp = JsonObject.create();
                docrsp.put("status", "Process already started.");
                JsonDocument doc = CouchbaseService.createDocument("response", docrsp);
                return new ResponseEntity<String>(doc.content().toString(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        catch(Exception e)
        {
            isStarted = false;

            // Response ERROR
            JsonObject docrsp = JsonObject.create();
            docrsp.put("status", "ERROR");
            JsonDocument doc = CouchbaseService.createDocument("response", docrsp);
            return new ResponseEntity<String>(doc.content().toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



    private int getRandomNum(int x, int y)
    {
        return ThreadLocalRandom.current().nextInt(x,y);
    }


    /**
     * Start method to stop the auto creation process of beer entities to a couchbase bucket,
     * just in case there is a process currently creating beer docs into Database.
     *
     * @return json confirmation
     */
    @RequestMapping(method = RequestMethod.GET, value = "/stop", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> stopBeersCreation()
    {
        JsonDocument doc = null;

        try
        {
            if( isStarted )
            {
                isStarted = false;

                // Response OK
                JsonObject docrsp = JsonObject.create();
                docrsp.put("status", "OK");
                doc = CouchbaseService.createDocument("response", docrsp);
                return new ResponseEntity<String>(doc.content().toString(), HttpStatus.OK);
            }
            else
            {
                // Response ERROR
                JsonObject docrsp = JsonObject.create();
                docrsp.put("status", "Process already stopped.");
                doc = CouchbaseService.createDocument("response", docrsp);
                return new ResponseEntity<String>(doc.content().toString(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        catch(Exception e)
        {
            isStarted = false;

            // Response ERROR
            JsonObject docrsp = JsonObject.create();
            docrsp.put("status", "ERROR");
            doc = CouchbaseService.createDocument("response", docrsp);
            return new ResponseEntity<String>(doc.content().toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }






    @RequestMapping(method = RequestMethod.GET, value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getBeer(@PathVariable String id)
    {
        JsonDocument doc = couchbaseService.read(id);
        if (doc != null) {
            return new ResponseEntity<String>(doc.content().toString(), HttpStatus.OK);
        } else {
            return new ResponseEntity<String>(HttpStatus.NOT_FOUND);
        }
    }





    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createBeer(@RequestBody Map<String, Object> beerData) {
        String id = "";
        try {
            JsonObject beer = parseBeer(beerData);
            id = "beer-" + beer.getString("name");
            JsonDocument doc = CouchbaseService.createDocument(id, beer);
            couchbaseService.create(doc);

            return new ResponseEntity<String>(id, HttpStatus.CREATED);
        }
        catch (IllegalArgumentException e) {
            return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
        } catch (DocumentAlreadyExistsException e) {
            return new ResponseEntity<String>("Id " + id + " already exist", HttpStatus.CONFLICT);
        } catch (Exception e) {
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }




    @RequestMapping(method = RequestMethod.DELETE, value = "/{beerId}")
    public ResponseEntity<String> deleteBeer(@PathVariable String beerId)
    {
        JsonDocument deleted = couchbaseService.delete(beerId);
        return new ResponseEntity<String>(""+deleted.cas(), HttpStatus.OK);
    }





    @RequestMapping(value = "/{beerId}", consumes = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.PUT)
    public ResponseEntity<String> updateBeer(@PathVariable String beerId, @RequestBody Map<String, Object> beerData) {
        try {
            JsonObject beer = parseBeer(beerData);
            couchbaseService.update(CouchbaseService.createDocument(beerId, beer));
            return new ResponseEntity<String>(beerId, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
        } catch (DocumentDoesNotExistException e) {
            return new ResponseEntity<String>("Id " + beerId + " does not exist", HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }





    private JsonObject parseBeer(Map<String, Object> beerData)
    {
        String type = (String) beerData.get("type");
        String name = (String) beerData.get("name");
        if (type == null || name == null || type.isEmpty() || name.isEmpty()) {
           throw new IllegalArgumentException();
        } else {
            JsonObject beer = JsonObject.create();
            for (Map.Entry<String, Object> entry : beerData.entrySet()) {
                beer.put(entry.getKey(), entry.getValue());
            }
            return beer;
        }
    }




    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> listBeers(@RequestParam(required = false) Integer offset, @RequestParam(required = false) Integer limit)
    {
        ViewResult result = couchbaseService.findAllBeers(offset, limit);
        if (!result.success()) {
            //TODO maybe detect type of error and change error code accordingly
            return new ResponseEntity<String>(result.error().toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        else
        {
            JsonArray keys = JsonArray.create();
            Iterator<ViewRow> iter = result.rows();
            while(iter.hasNext())
            {
                ViewRow row = iter.next();
                JsonObject beer = JsonObject.create();
                beer.put("name", row.key());
                beer.put("id", row.id());
                keys.add(beer);
            }
            return new ResponseEntity<String>(keys.toString(), HttpStatus.OK);
        }
    }




    @RequestMapping(method = RequestMethod.GET, value = "/search/{token}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> searchBeer(@PathVariable final String token) {
        //we'll get all beers asynchronously and compose on the stream to extract those that match
        AsyncViewResult viewResult = couchbaseService.findAllBeersAsync().toBlocking().single();
        if (viewResult.success()) {
            return couchbaseService.searchBeer(viewResult.rows(), token)
                    //transform the array into a ResponseEntity with correct status
                    .map(new Func1<JsonArray, ResponseEntity<String>>() {
                        @Override
                        public ResponseEntity<String> call(JsonArray objects) {
                            return new ResponseEntity<String>(objects.toString(), HttpStatus.OK);
                        }
                    })
                    //in case of errors during this processing, return a ERROR 500 response with detail
                    .onErrorReturn(new Func1<Throwable, ResponseEntity<String>>() {
                        @Override
                        public ResponseEntity<String> call(Throwable throwable) {
                            return new ResponseEntity<String>("Error while parsing results - " + throwable,
                                    HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                    })
                    //block and send back the response
                   .toBlocking().single();
        } else {
            return new ResponseEntity<String>("Error while searching - " + viewResult.error(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * Delete all beers with type brewery
     *
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = "/deleteBreweries", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> deleteBreweries()
    {
        ViewResult result = couchbaseService.findAllBreweries(null, null);
        if (!result.success()) {
            return new ResponseEntity<String>(result.error().toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        else
        {
            JsonArray keys = JsonArray.create();
            Iterator<ViewRow> iter = result.rows();
            while(iter.hasNext())
            {
                // Collect the id
                ViewRow row = iter.next();
                JsonObject beer = JsonObject.create();
                beer.put("name", row.key());
                beer.put("id", row.id());
                keys.add(beer);

                // Get the beer doc
                JsonDocument doc = couchbaseService.read(row.id());

                // Delete just in case is a beer
                String type = doc.content().getString("type");
                if ( type.equalsIgnoreCase("brewery") ){
                    couchbaseService.delete(row.id());
                }
            }
            return new ResponseEntity<String>(keys.toString(), HttpStatus.OK);
        }

    }



    /**
     * Delete all beers with type beer
     *
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = "/deleteBeers", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> deleteBeers()
    {
        ViewResult result = couchbaseService.findAllBeers(null, null);
        if (!result.success()) {
            return new ResponseEntity<String>(result.error().toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        else
        {
            JsonArray keys = JsonArray.create();
            Iterator<ViewRow> iter = result.rows();
            while(iter.hasNext())
            {
                // Collect the id
                ViewRow row = iter.next();
                JsonObject beer = JsonObject.create();
                beer.put("name", row.key());
                beer.put("id", row.id());
                keys.add(beer);

                // Get the beer doc
                JsonDocument doc = couchbaseService.read(row.id());

                // Delete just in case is a beer
                String type = doc.content().getString("type");
                if ( type.equalsIgnoreCase("beer") ){
                    couchbaseService.delete(row.id());
                }
            }
            return new ResponseEntity<String>(keys.toString(), HttpStatus.OK);
        }

    }


}