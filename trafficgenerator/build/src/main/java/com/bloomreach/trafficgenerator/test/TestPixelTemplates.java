package com.bloomreach.trafficgenerator.test;

import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.site.journeydata.templates.*;

public class TestPixelTemplates {

    private final static String TEMPLATE_DIR = "./data/resources";

    public static void main (String[] args) {

        TestPixelTemplates test;

        test = new TestPixelTemplates ();
        try {
            test.doTest ();
        } catch (Exception e) {
            e.printStackTrace ();
            System.out.println ("template tester exception");
            System.exit (-1);
        }
    }

    private void doTest () throws Exception {
        PixelTemplates pt;
        PixelBRData pixelData;

        pt = new PixelTemplates (); 
        // pt.setTemplatesDir (TEMPLATE_DIR);

        // pixelLogBuilder = pt.loadProductPixelTemplate ();
        // pixelLogBuilder = pt.loadAddToCartPixelTemplate ();
        // pixelLogBuilder = pt.loadConversionPixelTemplate ();
        pixelData = pt.loadPixelTemplate (GeneratorConstants.DEVICE_TYPE_DESKTOP);
        if (pixelData != null) {
            System.out.println ("pixelData: ");
            System.out.println (pixelData.toString ());
        } 
    }

}
