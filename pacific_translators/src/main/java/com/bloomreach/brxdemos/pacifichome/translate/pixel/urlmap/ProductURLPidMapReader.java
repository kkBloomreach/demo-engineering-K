// complementary class to the "Builder"
// During translation, this class is used to lookup a product url and return
// corresponding pid. 
package com.bloomreach.brxdemos.pacifichome.translate.pixel.urlmap;

import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ProductURLPidMapReader {

    private HashMap <String, String> urlPidMap;

    public ProductURLPidMapReader () {
    }

    public void load (String urlPidMapFilePath) throws Exception {
        File urlPidMapFile;

        urlPidMapFile = new File (urlPidMapFilePath);
        if (urlPidMapFile.exists () == false)
            throw new Exception ("URL-pid-map file not found");

        urlPidMap = parseUrlPidMap (urlPidMapFile);
    }

    public String getPid (String url) {
        if (urlPidMap != null)
            return urlPidMap.get (url); // returns null if url does not exist

        return null;
    }

    private HashMap <String, String> parseUrlPidMap (File urlPidMapFile) throws Exception {
        FileReader srcReader;
        BufferedReader srcBufferedReader = null;
        String srcLine;
        int lineNum = 0;
        HashMap <String, String> parsedMap;

        parsedMap = new HashMap <String, String> (); 
        try {
            srcBufferedReader = new BufferedReader (new FileReader (urlPidMapFile));
            while ((srcLine = srcBufferedReader.readLine ()) != null) {
                String[] tokens;

                // skip header
                if (lineNum == 0) {
                    lineNum = lineNum + 1;
                    continue;
                }

                if (srcLine.length () == 0) // should not really happen...
                    continue;

                tokens = srcLine.split ("\t");
                if (tokens.length > 1)
                    parsedMap.put (tokens[0], tokens[1]);
            }
        } catch (Exception e) {
            e.printStackTrace ();
            throw new Exception ("Exception in parsing urlPidMap file");
        } finally {
            if (srcBufferedReader != null)
                srcBufferedReader.close ();
        }

        return  (parsedMap);
    }

}

