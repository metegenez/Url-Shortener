package com.example.urlshorthener;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.apache.commons.lang3.RandomStringUtils;
import io.github.cdimascio.dotenv.Dotenv;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;


@RestController
public class Controller {
    Table table;
    Controller() {
        Dotenv dotenv = Dotenv.load();
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(dotenv.get("AWS_ACCESS_KEY"), dotenv.get("AWS_SECRET_KEY"));
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCreds)).withRegion(dotenv.get("AWS_REGION")).build();
        DynamoDB dynamoDB = new DynamoDB(client);
        table = dynamoDB.getTable("shortner");
    }


    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<Object> redirectToExternalUrl(@PathVariable("id") String id) throws URISyntaxException {
        Item item = table.getItem("prefix", id);
        if (item == null){ // if prefix does not exists, 404.
            HttpHeaders httpHeaders = new HttpHeaders();
            return new ResponseEntity<>(httpHeaders, HttpStatus.NOT_FOUND);
        }else {
            URI uri = new URI(item.get("Url").toString());

            // Increment count by 1 for prefix
            Map<String,String> expressionAttributeNames = new HashMap<String,String>();
            expressionAttributeNames.put("#p", "Count");
            Map<String,Object> expressionAttributeValues = new HashMap<String,Object>();
            expressionAttributeValues.put(":val", 1);
            table.updateItem(
                    "prefix", id,
                    "set #p = #p + :val",
                    expressionAttributeNames,
                    expressionAttributeValues);
            //
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setLocation(uri); // redirect
            return new ResponseEntity<>(httpHeaders, HttpStatus.SEE_OTHER);
        }
    }
    @RequestMapping(value = "/info", method = RequestMethod.POST)
    public ResponseEntity<String> shorthener(@RequestParam String url, @RequestParam(required = false) String prefix) {
        if (prefix == null){ // If prefix does not provided, create one.
            prefix = RandomStringUtils.randomAlphabetic(7).toLowerCase(Locale.ROOT);

        } else if (prefix.length() != 7){ // If prefix char length control.
            return new ResponseEntity<String>("Prefix should be 7 chars.", HttpStatus.BAD_REQUEST);
        } else {
            Item temp_item = table.getItem("prefix", prefix);
            if (temp_item != null) { // If prefix exists, it is a duplicate. Do not override the count.
                return new ResponseEntity<String>("Duplicate request.", HttpStatus.BAD_REQUEST);
            }
        }
        String user_hash = RandomStringUtils.randomAlphabetic(15).toLowerCase(Locale.ROOT);
        Item item = new Item()
                .withPrimaryKey("prefix", prefix)
                .withString("Url", url)
                .withString("UserHash", user_hash) // for authentication the requests.
                .withInt("Count", 0);

        // Write the item to the table
        table.putItem(item);

        return new ResponseEntity<String>("User Hash : "
                + user_hash + " Prefix: " + prefix, HttpStatus.OK);
    }
    @RequestMapping(value = "/info", method = RequestMethod.DELETE)
    public ResponseEntity<String> shorthenerDelete(@RequestParam String user_hash, @RequestParam String prefix) {
        Item item = table.getItem("prefix", prefix);
        if (item == null){
            return new ResponseEntity<String>("No url.", HttpStatus.NO_CONTENT);
        }else {
            String hash_at_table = item.get("UserHash").toString();
            if (hash_at_table.equals(user_hash)){ //checks that user provided correct string.
                table.deleteItem("prefix", prefix);
                return new ResponseEntity<String>("Url is deleted.", HttpStatus.OK);
            }else {
                return new ResponseEntity<String>("Unauthorized.", HttpStatus.UNAUTHORIZED);
            }
        }
    }
    @RequestMapping(value = "/info", method = RequestMethod.GET)
    public ResponseEntity<String> shorthenerInfo(@RequestParam String user_hash, @RequestParam String prefix) {
        Item item = table.getItem("prefix", prefix);
        if (item == null){
            return new ResponseEntity<String>("No url.", HttpStatus.NO_CONTENT);
        }else {
            String hash_at_table = item.get("UserHash").toString();
            if (hash_at_table.equals(user_hash)){ //checks that user provided correct string.
                String count = item.get("Count").toString();
                return new ResponseEntity<String>("Count: " + count, HttpStatus.OK);
            }else {
                return new ResponseEntity<String>("Unauthorized.", HttpStatus.UNAUTHORIZED);
            }
        }
    }


}
