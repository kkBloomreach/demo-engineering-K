// complementary class to the "Builder"
// During translation, this class is used to lookup a category urlLeaf and return
// corresponding crumb. 
package com.bloomreach.brxdemos.pacifichome.translate.pixel.urlmap;

import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import com.bloomreach.brxdemos.pacifichome.translate.pixel.feed.FeedCrumbData;

public class CategoryURLCrumbMapReader {

    private HashMap <String, FeedCrumbData> urlLeafCrumbDataMap;

    public CategoryURLCrumbMapReader () {
    }

    // filePath for .tsv file
    public void load (String urlLeafCrumbDataMapFilePath) throws Exception {
        File urlLeafCrumbDataMapFile;

        urlLeafCrumbDataMapFile = new File (urlLeafCrumbDataMapFilePath);
        if (urlLeafCrumbDataMapFile.exists () == false)
            throw new Exception ("URL-crumb-map file not found");

        urlLeafCrumbDataMap = parseUrlLeafCrumbDataMap (urlLeafCrumbDataMapFile);
    }

    public FeedCrumbData getFeedCrumbData (String urlLeaf) {
        if (urlLeafCrumbDataMap != null)
            return urlLeafCrumbDataMap.get (urlLeaf); // returns null if urlLeaf does not exist

        return null;
    }

    private HashMap <String, FeedCrumbData> parseUrlLeafCrumbDataMap (File urlLeafCrumbDataMapFile) throws Exception {
        FileReader srcReader;
        BufferedReader srcBufferedReader = null;
        String srcLine;
        int lineNum = 0;
        HashMap <String, FeedCrumbData> parsedMap;

        parsedMap = new HashMap <String, FeedCrumbData> (); 
        try {
            srcBufferedReader = new BufferedReader (new FileReader (urlLeafCrumbDataMapFile));
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
                if (tokens.length > 2) {
                    FeedCrumbData crumbData;
                    String fullCrumb;
                    String leafCrumb;
                    String leafCrumbId;

                    leafCrumb = tokens [0];
                    fullCrumb = tokens [1];
                    leafCrumbId = tokens [2];
                    crumbData = new FeedCrumbData (fullCrumb, leafCrumb, leafCrumbId);
                    parsedMap.put (leafCrumb, crumbData);
                }
            }
        } catch (Exception e) {
            e.printStackTrace ();
            throw new Exception ("Exception in parsing urlLeafCrumbDataMap file");
        } finally {
            if (srcBufferedReader != null)
                srcBufferedReader.close ();
        }

        return  (parsedMap);
    }

}

