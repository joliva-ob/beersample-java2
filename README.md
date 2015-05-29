beersample-java2
================

A sample application for the Java SDK 2.0 and Couchbase Server 3.0

*Note: this is a rewrite of the previous SDK 2.0 tutorial material, for 2.1+ releases.
You can find the previous material on the __oldtutorial__ branch, see it*
[here on github](https://github.com/couchbaselabs/beersample-java2/tree/oldtutorial).

## What's needed?
 - The beersample sample bucket
 - The beer/brewery_beers view (built in in beersample sample)
 - An additional view beer/by_name with the following map function (you should copy the beer designdoc to dev in order
 to edit it and add this view):
 - Couchbase server
 - couchbase plugin for elasticsearch replication
 - elasticsearch server
 - head elasticsearch plugin for web monitoring
 - kibana elasticsearch plugin for online live dashboard


```
    function (doc, meta) {
       if (doc.type == "beer") {
         emit(doc.name, doc.brewery_id)
       }
     }
```

## Building and running
To install a couchbase server with the beers bucket, its connection to elasticsearch by auto-replication and finally to configure a kibana dashboard in order to monitor the changes on live as follows:

![kibana exmample dashboard](https://github.com/joliva-ob/beersample-java2/blob/master/kibana-snapshot-beer.png?raw=true) 

- http://es.slideshare.net/Couchbase/using-elasticsearch-and-couchbase-together-to-build-large-scale-applications
- https://www.digitalocean.com/community/tutorials/how-to-install-elasticsearch-logstash-and-kibana-4-on-ubuntu-14-04
- https://www.elastic.co/guide/en/watcher/current/getting-started.html?ultron=watcher-annoucement&blade=instruction&hulk=email&mkt_tok=3RkMMJWWfF9wsRouuK%2FBZKXonjHpfsX97ekrX6CzlMI%2F0ER3fOvrPUfGjI4ESMNmI%2BSLDwEYGJlv6SgFQrHGMa1h17gOUhM%3D
- http://www.couchbase.com/nosql-databases/downloads

Correctly configure the application for your couchbase installation by editing **`src/main/resources/application.yml`**.

To build a self-contained jar of the application, run the following Maven command:

    mvn clean package
    
To start couchbase
    
    Just start from the already installed App (Mac: Couchbase Server.app)
    
To start elasticsearch
    
    Type /bin/elasticsearch & from the root elasticsearch folder
    
To start kibana

    Just type /bin/kibana & from the root kibana installation directory

Finally in order to run the application and expose the REST API on `localhost:8080`, run the following command:

    java -jar target/beersample2-1.0-SNAPSHOT.jar

## REST API
The REST API is deployed on port 8080 and has the following routes:

## Beer Routes
 * `GET /beer/start/{number}` : Starts the creation of {number} of beers with a random beer type
 * `GET /beer/stop` : Just stop the beers creation process if already started
 * `GET /beer/deleteBeers` : Delete all inputs with type = 'beer'
 * `GET /beer/{id}`: retrieve the Beer with id {id} (one json object representing the beer)
 * `POST /beer`: with a jsonObject in body representing the beer data, creates a new beer
 * `PUT /beer/{id}`: with a jsonObject in body representing the updated beer data, updates a beer of id {id}
 * `DELETE /beer/{id}`: deletes the beer of id {id}
 * `GET /beer`: list all the beers, just outputting the beers `id` and `name` in an array of JSON objects
 * `GET /beer/search/{partOfName}`: list all the beers which name's contains {partOfName} (ignoring case). Each returned
 beer is represented as a JSON object with the beer's `id` and `name` and the whoe beer details under `detail`.

## Test the application
- Start the beers auto-creation process.
- Check it out the changes at kibana on live.