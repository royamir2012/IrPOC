package com.robotdreams.api;

import com.wolfram.alpha.WAEngine;
import com.wolfram.alpha.WAException;
import com.wolfram.alpha.WAPlainText;
import com.wolfram.alpha.WAPod;
import com.wolfram.alpha.WAQuery;
import com.wolfram.alpha.WAQueryResult;
import com.wolfram.alpha.WASubpod;


/*
 * A simple example program demonstrating the WolframAlpha.jar library. The program
 * performs a query given on the command line and prints out the resulting pod titles
 * and plaintext content.
 * 
 * You will need to insert your appid into the code. To compile or run this program
 * you will need the following dependent libraries on your classpath (including 
 * WolframAlpha.jar, of course):
 * 
 *     commons-codec-1.3.jar
 *     httpclient-4.0.1.jar
 *     httpcore-4.0.1.jar
 *     commons-logging.jar
 *     
 * These libraries are widely available on the Internet. You can probably use other version
 * numbers than these, although these are the versions I used.
 * 
 * To launch from the command line, do the following (these classpath specifications assume
 * that the WolframAlpha.jar file and the four other dependent jars listed above are in the same
 * directory as AlphaAPISample.class):
 * 
 *     Windows:
 *     
 *       java -classpath .;* AlphaAPISample "sin x"
 *     
 *     Linux, Mac OSX:
 *     
 *       java -classpath .:* AlphaAPISample "sin x"
 */

public class WolframAlphaAPI {

    // PUT YOUR APPID HERE:
    private static String appid = "RRP8E3-UGWHE56UUQ";


    public String sendRequest(String input) throws WAException {

        // The WAEngine is a factory for creating WAQuery objects,
        // and it also used to perform those queries. You can set properties of
        // the WAEngine (such as the desired API output format types) that will
        // be inherited by all WAQuery objects created from it. Most applications
        // will only need to crete one WAEngine object, which is used throughout
        // the life of the application.
        WAEngine engine = new WAEngine();

        // These properties will be set in all the WAQuery objects created from this WAEngine.
        engine.setAppID(appid);
        engine.addFormat("plaintext");

        // Create the query.
        WAQuery query = engine.createQuery();

        // Set properties of the query.
        query.setInput(input);

        // For educational purposes, print out the URL we are about to send:
        System.out.println("Query URL:");
        System.out.println(engine.toURL(query));
        System.out.println("");

        // This sends the URL to the Wolfram|Alpha server, gets the XML result
        // and parses it into an object hierarchy held by the WAQueryResult object.
        WAQueryResult queryResult = engine.performQuery(query);

        if (queryResult.isError()) {
            System.out.println("Query error." + "  error code: " + queryResult.getErrorCode() + "  error message: " + queryResult.getErrorMessage());
            return "Something ain't right";
        } else if (!queryResult.isSuccess()) {
            System.out.println("Query was not understood; no results available.");
            return "What was that?";
        } else {
            // Got a result.
            StringBuilder longResponse = new StringBuilder();
            StringBuilder shortResponse = new StringBuilder();
            System.out.println("Successful query. Pods follow:\n");
            for (WAPod pod : queryResult.getPods()) {
                if (!pod.isError()) {
                    longResponse.append(pod.getTitle()).append("\n");
                    longResponse.append("------------").append("\n");
                    for (WASubpod subpod : pod.getSubpods()) {
                        for (Object element : subpod.getContents()) {
                            if (element instanceof WAPlainText) {
                                if (shortResponse.length() == 0) {
                                    shortResponse.append(((WAPlainText) element).getText());
                                }
                                longResponse.append(((WAPlainText) element).getText()).append("\n");
                                longResponse.append("").append("\n");
                            }
                        }
                    }
                    longResponse.append("").append("\n");
                }
            }

            System.out.println(longResponse.toString());

            return longResponse.toString();
            // We ignored many other types of Wolfram|Alpha output, such as warnings, assumptions, etc.
            // These can be obtained by methods of WAQueryResult or objects deeper in the hierarchy.
        }
    }


}
