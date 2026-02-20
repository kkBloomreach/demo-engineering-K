package com.bloomreach.analyticsdatagenerator;

import java.net.URLEncoder;
import java.io.File;
import java.util.ArrayList;

import com.bloomreach.analyticsdatagenerator.input.*;
import com.bloomreach.analyticsdatagenerator.generate.*;

public class DataGenerator {

    private final static String VERSION = "1.0.0";

    public static void main (String[] args) {
        GeneratorCommandLine commandLine;
        MessageLogger messageLogger;
        DataGenerator generator;

        System.out.println  ("Analytics DataGenerator, version: " + VERSION);

        commandLine = new GeneratorCommandLine ();
        if (commandLine.parse (args) == false) {
            // help message already shown
            System.exit (-1);
        }

        try {
            messageLogger = new MessageLogger ();
            messageLogger.init (commandLine);
        } catch (Exception e) {
            e.printStackTrace ();
            System.out.println ("Exception in creating messageLogger: " + e.getMessage ());
            // don't exit even if this fails
        }

        generator = new DataGenerator ();
        try {
            generator.doGenerate (commandLine);
        } catch (Exception e) {
            e.printStackTrace ();
            MessageLogger.logFatal ("Exception in generator doGenerate: " + e.getMessage ());
        }

        System.exit (0);
    }

    private DataGenerator () {
    }

    private void doGenerate (GeneratorCommandLine commandLine) throws Exception {
        String rootDirPath;
        GeneratorInputData inputData;
        ArrayList<CategoryAndRefinements> categoryAndRefinementsList = null;
        ArrayList<SearchQueryAndRefinements> searchQueryAndRefinementsList = null;

        inputData = readGeneratorInput (commandLine);
        if (inputData == null)
            return; // error already reported

        // generate needed data
        // generate Uid Map (uid, view, segment)
        MessageLogger.logInfo ("Generate UID map");
        generateUidMap (commandLine, inputData);

        // generate search query to pid map
        MessageLogger.logInfo ("Generate SearchQueryToPid map");
        searchQueryAndRefinementsList = generateSearchQueryToPidMap (commandLine, inputData);

        // generate category query to pid map
        // That methods returns dynamically-created {primary_cat->its refinements} map. It is
        // then used to write refinedJourney for categories
        MessageLogger.logInfo ("Generate CategoryQueryToPid map");
        categoryAndRefinementsList = generateCategoryQueryToPidMap (commandLine, inputData);

        // generate refined query map
        // IMPORTANT - call this method only after generateCategoryQueryToPidMap
        MessageLogger.logInfo ("Generate RefinedJourney map");
        generateRefinedJourneyMap (commandLine, inputData, searchQueryAndRefinementsList, categoryAndRefinementsList);

        // generate refUrlPool map
        MessageLogger.logInfo ("Generate RefUrlPool map");
        generateRefUrlPoolMap (commandLine, inputData);

        // generate zeroResult query map
        MessageLogger.logInfo ("Generate ZeroResultQuery map");
        generateZeroResultQueryMap (commandLine, inputData);

        MessageLogger.logInfo ("Finish");
    }

    private GeneratorInputData readGeneratorInput (GeneratorCommandLine commandLine) throws Exception {
        String rootDirPath;
        String inputDirPath;
        File inputDir;
        File inputFile;
        GeneratorInputDataReader inputDataReader;
        GeneratorInputData inputData;

        // inputFile: -d<dir>/source/generator/config/pacifichome.json
        rootDirPath = commandLine.getRootSourceDataDir ();
        inputDir = new File (rootDirPath, GeneratorConstants.INPUT_DIR_PATH);
        inputFile = new File (inputDir, commandLine.getAccountName() + ".json");
        if (inputFile.exists () == false) {
            MessageLogger.logError ("Generator input file does not exist: " + inputFile.getPath());
            return null;
        }

        inputDataReader = new GeneratorInputDataReader ();
        inputData = inputDataReader.read (inputFile);
        if (inputData == null) {
            MessageLogger.logError ("Error in reading simulation input");
            return null;
        }

        return inputData;
    }

    private void generateUidMap (GeneratorCommandLine commandLine, GeneratorInputData inputData) throws Exception {
        String outDirPath;
        File outputDir;
        File outputFile;
        UidMapGenerator uidMapGenerator;

        outDirPath = commandLine.getOutputDir ();
        outputDir = new File (outDirPath, GeneratorConstants.OUTPUT_DIR_PATH.replace ("$ACCOUNTNAME", commandLine.getAccountName ())); 
        outputDir.mkdirs ();
        outputFile = new File (outputDir, GeneratorConstants.FILENAME_UID_MAP_OUTPUT);
 
        uidMapGenerator = new UidMapGenerator ();
        uidMapGenerator.start (outputFile);
        uidMapGenerator.write (inputData);
        uidMapGenerator.close ();
    }

    private ArrayList <SearchQueryAndRefinements> generateSearchQueryToPidMap (GeneratorCommandLine commandLine, GeneratorInputData inputData) throws Exception {
        String outDirPath;
        File outputDir;
        File outputFile;
        SearchQueryToPidMapGenerator queryToPidMapGenerator;
        ArrayList<SearchQueryAndRefinements> searchQueryAndRefinementsList;

        outDirPath = commandLine.getOutputDir ();
        outputDir = new File (outDirPath, GeneratorConstants.OUTPUT_DIR_PATH.replace ("$ACCOUNTNAME", commandLine.getAccountName ())); 
        outputDir.mkdirs ();
        outputFile = new File (outputDir, GeneratorConstants.FILENAME_SEARCH_QUERY_TO_PID_MAP_OUTPUT);
 
        queryToPidMapGenerator = new SearchQueryToPidMapGenerator ();
        queryToPidMapGenerator.start (outputFile);
        searchQueryAndRefinementsList = queryToPidMapGenerator.write (inputData);
        queryToPidMapGenerator.close ();

        return searchQueryAndRefinementsList;
    }

    private ArrayList <CategoryAndRefinements> generateCategoryQueryToPidMap (GeneratorCommandLine commandLine, GeneratorInputData inputData) throws Exception {
        String outDirPath;
        File outputDir;
        File outputFile;
        CategoryQueryToPidMapGenerator queryToPidMapGenerator;
        ArrayList<CategoryAndRefinements> categoryAndRefinementsList;

        outDirPath = commandLine.getOutputDir ();
        outputDir = new File (outDirPath, GeneratorConstants.OUTPUT_DIR_PATH.replace ("$ACCOUNTNAME", commandLine.getAccountName ())); 
        outputDir.mkdirs ();
        outputFile = new File (outputDir, GeneratorConstants.FILENAME_CATEGORY_QUERY_TO_PID_MAP_OUTPUT);
 
        queryToPidMapGenerator = new CategoryQueryToPidMapGenerator ();
        queryToPidMapGenerator.start (outputFile);
        categoryAndRefinementsList = queryToPidMapGenerator.write (inputData);
        queryToPidMapGenerator.close ();

        return categoryAndRefinementsList;
    }

    private void generateRefinedJourneyMap (GeneratorCommandLine commandLine, GeneratorInputData inputData,
                                            ArrayList<SearchQueryAndRefinements> searchQueryAndRefinementsList,
                                            ArrayList<CategoryAndRefinements> categoryAndRefinementsList) throws Exception {
        String outDirPath;
        File outputDir;
        File outputFile;
        RefinedJourneyMapGenerator refinedJourneyMapGenerator;

        outDirPath = commandLine.getOutputDir ();
        outputDir = new File (outDirPath, GeneratorConstants.OUTPUT_DIR_PATH.replace ("$ACCOUNTNAME", commandLine.getAccountName ())); 
        outputDir.mkdirs ();
        outputFile = new File (outputDir, GeneratorConstants.FILENAME_REFINED_QUERY_MAP_OUTPUT);
 
        refinedJourneyMapGenerator = new RefinedJourneyMapGenerator ();
        refinedJourneyMapGenerator.start (outputFile);
        refinedJourneyMapGenerator.write (inputData, searchQueryAndRefinementsList, categoryAndRefinementsList);
        refinedJourneyMapGenerator.close ();
    }

    private void generateRefUrlPoolMap (GeneratorCommandLine commandLine, GeneratorInputData inputData) throws Exception {
        String outDirPath;
        File outputDir;
        File outputFile;
        RefUrlPoolMapGenerator refUrlPoolMapGenerator;

        outDirPath = commandLine.getOutputDir ();
        outputDir = new File (outDirPath, GeneratorConstants.OUTPUT_DIR_PATH.replace ("$ACCOUNTNAME", commandLine.getAccountName ())); 
        outputDir.mkdirs ();
        outputFile = new File (outputDir, GeneratorConstants.FILENAME_REFURL_POOL_MAP_OUTPUT);
 
        refUrlPoolMapGenerator = new RefUrlPoolMapGenerator ();
        refUrlPoolMapGenerator.start (outputFile);
        refUrlPoolMapGenerator.write (inputData);
        refUrlPoolMapGenerator.close ();
    }

    private void generateZeroResultQueryMap (GeneratorCommandLine commandLine, GeneratorInputData inputData) throws Exception {
        String outDirPath;
        File outputDir;
        File outputFile;
        ZeroResultQueryMapGenerator zeroResultQueryMapGenerator;

        outDirPath = commandLine.getOutputDir ();
        outputDir = new File (outDirPath, GeneratorConstants.OUTPUT_DIR_PATH.replace ("$ACCOUNTNAME", commandLine.getAccountName ())); 
        outputDir.mkdirs ();
        outputFile = new File (outputDir, GeneratorConstants.FILENAME_ZERO_RESULT_QUERY_MAP_OUTPUT);
 
        zeroResultQueryMapGenerator = new ZeroResultQueryMapGenerator ();
        zeroResultQueryMapGenerator.start (outputFile);
        zeroResultQueryMapGenerator.write (inputData);
        zeroResultQueryMapGenerator.close ();
    }

}

