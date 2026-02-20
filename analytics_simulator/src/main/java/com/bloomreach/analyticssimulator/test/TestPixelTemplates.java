package com.bloomreach.analyticssimulator.test;

import com.bloomreach.proto.Aggregation.PixelLog;
import com.bloomreach.proto.Aggregation.PixelLog.Builder;

import com.bloomreach.analyticssimulator.templates.*;

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
        PixelLog.Builder pixelLogBuilder;
        PixelLog pixelLog;

        pt = new PixelTemplates (); 
        pt.setTemplatesDir (TEMPLATE_DIR);

        // pixelLogBuilder = pt.loadProductPixelTemplate ();
        // pixelLogBuilder = pt.loadAddToCartPixelTemplate ();
        // pixelLogBuilder = pt.loadConversionPixelTemplate ();
        pixelLogBuilder = pt.loadSearchResultPagePixelTemplate ();
        if (pixelLogBuilder != null) {
            pixelLog = pixelLogBuilder.build ();
            System.out.println ("pixelLog: ");
            System.out.println (pixelLog.toString ());
        } 
    }

}
