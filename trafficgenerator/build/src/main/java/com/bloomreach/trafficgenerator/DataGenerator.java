// generate traffic data
// This is executed ONLY when needed. It reads searchTerms, uses openAI to
// to generate refined-terms and saves in 'data' sub folder
// Path to 'data' subfolder is provided as an argument to the application
package com.bloomreach.trafficgenerator;

import java.util.ArrayList;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;
import org.json.JSONArray;


public class DataGenerator {

    private final static String DATA_GENERATOR_VERSION = "0.5.0.0";

    public DataGenerator () {
    }

    public static void main (String[] args) {
        GeneratorCommandLine commandLine;
        DataGenerator dataGenerator;
        MessageLogger messageLogger;

        System.out.printf ("Traffic DataGenerator, version: %s\n", DATA_GENERATOR_VERSION);

        commandLine = new GeneratorCommandLine ();
        if (commandLine.parse (args) == false) {
            // help message already shown
            System.exit (-1);
        }

        try {
            String logDirPath;

            // per-account log directory
            logDirPath = String.format ("%s/%s/%s", commandLine.getDataDirPath (), 
                                                    commandLine.getAccountName ().trim(),
                                                    GeneratorConstants.OUTPUT_LOG_DIR);
            messageLogger = new MessageLogger ();
            messageLogger.init (logDirPath, commandLine.getMessageLevel());
            MessageLogger.logInfo (String.format ("Traffic DataGenerator, version: %s\n", DATA_GENERATOR_VERSION));
        } catch (Exception e) {
            e.printStackTrace ();
            System.out.println ("Exception in creating messageLogger: " + e.getMessage ());
            // don't exit even if this fails
        }

        dataGenerator = new DataGenerator ();

        // generate traffic data
        try {
            dataGenerator.generateData (commandLine);
        } catch (Exception e) {
            e.printStackTrace ();
            MessageLogger.logFatal ("Exception in generating traffic data: " + e);
            MessageLogger.close ();
            System.exit (-1);
        }

        MessageLogger.logInfo ("DataGenerator finished");
        MessageLogger.close ();
        System.exit (0);
    }

    private void generateData (GeneratorCommandLine commandLine) throws Exception {
        String rootDirPath;
        String accountName;

        rootDirPath = commandLine.getDataDirPath ();  // provided in command line as "-d <arg>"
        accountName = commandLine.getAccountName ().trim();

        // generate search terms with refinements data (uses OpenAI)
        generateSearchTermsWithRefinements (rootDirPath, accountName);
        return;
    }

    private void generateSearchTermsWithRefinements (String rootDirPath, String accountName) throws Exception {
        File searchTermsFile;
        File acctDir;
        File searchTermsWithRefinementsFile;
        ArrayList<String> primarySearchTerms;
        BufferedWriter writer;
        String headerLine;

        acctDir = new File (rootDirPath, accountName);  // eg, ./data/sandbox-xxx
        searchTermsFile = new File (acctDir, GeneratorConstants.INPUT_SEARCH_TERM_PATH);
        searchTermsWithRefinementsFile = new File (acctDir, GeneratorConstants.INPUT_SEARCH_TERM_WITH_REFINEMENTS_PATH);

        if (searchTermsFile.exists () == false) {
            MessageLogger.logError (String.format ("Cannot find searchTerms file: %s", searchTermsFile.getPath()));
            return;
        }

        // refinement file does not exist OR is older than the primary search-terms file
        if ((searchTermsWithRefinementsFile.exists () == false) || 
            (searchTermsFile.lastModified () > searchTermsWithRefinementsFile.lastModified())) {

            // read primary search terms
            primarySearchTerms = loadPrimarySearchTerms (searchTermsFile);
            if ((primarySearchTerms == null) || (primarySearchTerms.size () == 0)) {
                MessageLogger.logWarning ("There are no primary search terms");
                return;
            }

            // for each primary term, generate refinements using openAI
            // and write data to output file
            writer = new BufferedWriter (new FileWriter (searchTermsWithRefinementsFile));

            // header line
            headerLine = String.format ("%s\t%s\t%s\n", "primary term", "importance", "refinements...");
            writer.write (headerLine);

            for (String primaryTerm : primarySearchTerms) {
                String refinedTermsLine;    // tab-separated list of refined terms
    
                refinedTermsLine = generateRefinedTerms (primaryTerm); // tab-separated list of terms
                if (refinedTermsLine != null) {
                    String totalStr;

                    // "1" -- importance
                    totalStr = String.format ("%s\t%s\t%s\n", primaryTerm, "1", refinedTermsLine);
                    writer.write (totalStr);
                }
            }

            writer.flush ();
            writer.close ();
        }
    }

    private ArrayList<String> loadPrimarySearchTerms (File searchTermsFile) throws Exception { 
        FileReader srcReader = null;
        BufferedReader srcBufferedReader = null;
        String srcLine;
        boolean headerLine;
        ArrayList<String> primarySearchTerms;

        primarySearchTerms = new ArrayList <String> ();
        try {       
            srcReader = new FileReader (searchTermsFile);
            srcBufferedReader = new BufferedReader (srcReader);

            headerLine = true;
            while ((srcLine = srcBufferedReader.readLine ()) != null) {
                String[] tokens;

                // skip header line
                if (headerLine == true) {
                    headerLine = false;
                    continue;
                }

                if (srcLine.length () == 0) // blank line
                    continue;

                tokens = srcLine.split ("\t");
                // expected tokens: search_term
                if ((tokens != null) && (tokens.length == 1)) {
                    // column 0: search term
                    if ((tokens[0] != null) && (tokens[0].length() > 0)) {
                        String inputTerm = tokens [0];

                        // first add the term to 'default' list, 
                        primarySearchTerms.add (inputTerm);
                    }
                }
            }
            srcReader.close ();
            srcBufferedReader.close ();
        } catch (Exception e) {
            MessageLogger.logError ("Exception reading primary search terms: " + e.getMessage ());
        }

        if (srcReader != null)
        {
            try {
                srcReader.close ();
            }
            catch (Exception e)
            {
                MessageLogger.logError ("Src reader close exception: " + e.getMessage ());
            }
        }
        return primarySearchTerms;
    }

    private String generateRefinedTerms (String primaryTerm) throws Exception {
        String prompt;
        String yourRole;
        HttpsURLConnection connection = null;
        StringBuffer response = null;

        prompt = String.format ("%s %s", "generate unnumbered keyword search list of 10 queries similar to %s maximum 2 words ", primaryTerm);
        yourRole = "You are generating marketing campaign using popular search keywords";
 
        try {
            URL openaiUrl = new URL (GeneratorConstants.OPENAPI_URL);
            connection = (HttpsURLConnection) openaiUrl.openConnection ();
            connection.setRequestMethod ("POST");
            connection.setRequestProperty ("Authorization", "Bearer " + GeneratorConstants.OPENAPI_KEY);
            connection.setRequestProperty ("Content-Type", "application/json");
            connection.setDoOutput (true);

            //String body = "{\"model\": \"" + GeneratorConstants.OPENAPI_MODEL + "\", \"messages\": [ {\"role\": \"user\", \"content\": \"" + prompt + "\"}, {\"role\": \"system\", \"content\": \"You are helpful assistant\"} ], \"temperature\": 0 }";

            String body = "{\"model\": \"" + GeneratorConstants.OPENAPI_MODEL + "\", \"messages\": [ {\"role\": \"user\", \"content\": \"" + prompt + "\"}, {\"role\": \"system\", \"content\": \"" + yourRole + "\"} ], \"temperature\": 0 }";

            MessageLogger.logDebug (String.format ("Request body = %s", body));

            OutputStreamWriter writer = new OutputStreamWriter (connection.getOutputStream());
            writer.write (body);
            writer.flush ();
            writer.close ();
        } catch (Exception e) {
            MessageLogger.logError (String.format ("%s: %s", "Got exception sending", e.getMessage()));
            return null;
        }

        try {
            BufferedReader br = new BufferedReader (new InputStreamReader (connection.getInputStream ()));
            String line;
            response = new StringBuffer ();

            while ((line = br.readLine()) != null) {
                response.append (line);
            }
            br.close ();
        } catch (Exception e) {
            MessageLogger.logDebug (String.format ("%s: %s", "Got exception receiving", e.getMessage()));
        } 

        if (response != null) {
            JSONObject responseObj;

            // json response format: {... "choices": [ { "index":..., "message": {"content": "<list>" } } ] ... }
            responseObj = new JSONObject (response.toString());
            if (responseObj.has ("choices")) {
                JSONArray responseChoices;
                JSONObject responseChoice0;

                responseChoices = responseObj.getJSONArray ("choices");
                responseChoice0 = (JSONObject) responseChoices.get (0);
                if (responseChoice0.has ("message")) {
                    JSONObject messageObj;

                    messageObj = responseChoice0.getJSONObject ("message");
                    if (messageObj.has ("content")) {
                        String messageContent;

                        messageContent = messageObj.getString ("content");
                        if ((messageContent != null) && (messageContent.length () > 0)) {

                            // clean up to construct a line in .tsv format
                            // In some cases, the string contains '%s ' - but not always
                            if (messageContent.indexOf ("%s ") >= 0) {
                                messageContent = messageContent.replace ("%s ", "");
                            }
                            messageContent = messageContent.replace ("\n- ", "\t");
                            messageContent = messageContent.replace ("- ", "");
                            return messageContent;
                        }
                    }
                }
            }
        }

        return null;
    }
}

