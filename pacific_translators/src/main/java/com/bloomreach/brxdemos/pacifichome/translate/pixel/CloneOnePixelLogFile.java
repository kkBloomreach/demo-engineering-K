package com.bloomreach.brxdemos.pacifichome.translate.pixel;

// Read input sequence file(s), get pixeLogs from them, clone each pixelLog and write the clone'd logs to output file(s)
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.Reader;
import org.apache.hadoop.io.SequenceFile.Writer;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;

import com.bloomreach.proto.PwfPixelLog;
import com.bloomreach.proto.Aggregation.PixelLog;

import com.bloomreach.brxdemos.pacifichome.translate.pixel.feed.*;
import com.bloomreach.brxdemos.pacifichome.translate.pixel.clone.*;
import com.bloomreach.brxdemos.pacifichome.translate.pixel.seqfile.*;
import com.bloomreach.brxdemos.pacifichome.translate.pixel.urlmap.*;

public class CloneOnePixelLogFile {

    private ProcessedFeed processedFeed = null;
    private SeqFileReader seqFileReader = null;
    private SeqFileWriter seqFileWriter = null;
    private OrderIdGenerator orderIdGenerator = null;
    private ProductURLPidMapReader productUrlPidMapReader = null;
    private CategoryURLCrumbMapReader catUrlCrumbMapReader = null;

    public CloneOnePixelLogFile () {
        this.seqFileReader = new SeqFileReader ();
        this.seqFileWriter = new SeqFileWriter ();
    }

    public void setProcessedFeed (ProcessedFeed processedFeed) {
        this.processedFeed = processedFeed;
    }

    public void setOrderIdGenerator (OrderIdGenerator orderIdGenerator) {
        this.orderIdGenerator = orderIdGenerator;
    }

    public void setProductUrlPidMapReader (ProductURLPidMapReader productUrlPidMapReader) {
        this.productUrlPidMapReader = productUrlPidMapReader;
    }

    public void setCategoryUrlCrumbMapReader (CategoryURLCrumbMapReader catUrlCrumbMapReader) {
        this.catUrlCrumbMapReader = catUrlCrumbMapReader;
    }

    public void doClone (String srcPath, String outputPath) throws Exception {
        ArrayList <PixelLog> srcPixelLogs;
        ArrayList <PixelLog> clonedPixelLogsList;

        srcPixelLogs = this.seqFileReader.readSourceFile (srcPath);
        if (srcPixelLogs.size () == 0) {
            System.out.println ("No pixelLogs in source file: " + srcPath);
            return;
        }

        clonedPixelLogsList = clone (srcPixelLogs, orderIdGenerator, productUrlPidMapReader, catUrlCrumbMapReader);
        if ((clonedPixelLogsList == null) || (clonedPixelLogsList.size () == 0)) {
            System.out.println ("clone pixelLogs unsuccessful for " + srcPath);
            return;
        }

        this.seqFileWriter.writeOutputFile (clonedPixelLogsList, outputPath);
    }

    private ArrayList <PixelLog> clone (ArrayList<PixelLog> srcPixelLogs, OrderIdGenerator orderIdGenerator,
                                        ProductURLPidMapReader productUrlPidMapReader,
                                        CategoryURLCrumbMapReader catUrlCrumbMapReader) {
        ArrayList <PixelLog> clonePixelLogList;

        // list of clone'd pixelLogs, returned from this method
        clonePixelLogList = new ArrayList <PixelLog> ();

        for (PixelLog pixelLog : srcPixelLogs) {
            int isConversion;
            String pType;
            PixelLog.Builder clonePixelLogBuilder;
            PixelLog clonePixelLog;
            int cloneStatus;

            clonePixelLogBuilder = PixelLog.newBuilder (pixelLog);
            cloneStatus = CloneConstants.CLONE_STATUS_ERROR;   // init 

            isConversion = pixelLog.getIsConversion ();
            if (isConversion == 1) {
                CloneConversionPixel cloneConversionPixel;

                cloneConversionPixel = new CloneConversionPixel ();
                cloneStatus = cloneConversionPixel.clonePixel (clonePixelLogBuilder, processedFeed, 
                                                               orderIdGenerator, productUrlPidMapReader,
                                                               catUrlCrumbMapReader);
            } else {
                pType = pixelLog.getPtype ();
                if (StringUtils.isEmpty (pType) == true) {
                    System.out.println ("Error. No ptype in source pixel log");
                    continue;
                } else {
                    if (pType.equals ("homepage") == true) {
                        CloneHomePagePixel cloneHomePagePixel;

                        cloneHomePagePixel = new CloneHomePagePixel ();
                        cloneStatus = cloneHomePagePixel.clonePixel (clonePixelLogBuilder, processedFeed, productUrlPidMapReader, catUrlCrumbMapReader);
                    } else if (pType.equals ("product") == true) {
                        CloneProductPagePixel cloneProductPagePixel;

                        cloneProductPagePixel = new CloneProductPagePixel ();
                        cloneStatus = cloneProductPagePixel.clonePixel (clonePixelLogBuilder, processedFeed, productUrlPidMapReader, catUrlCrumbMapReader);
                    } else if (pType.equals ("category") == true) {
                        CloneCategoryPagePixel cloneCategoryPagePixel;

                        cloneCategoryPagePixel = new CloneCategoryPagePixel ();
                        cloneStatus = cloneCategoryPagePixel.clonePixel (clonePixelLogBuilder, processedFeed, productUrlPidMapReader, catUrlCrumbMapReader);
                    } else if (pType.equals ("search") == true) {
                        CloneSearchResultPagePixel cloneSearchResultPagePixel;

                        cloneSearchResultPagePixel = new CloneSearchResultPagePixel ();
                        cloneStatus = cloneSearchResultPagePixel.clonePixel (clonePixelLogBuilder, processedFeed, productUrlPidMapReader, catUrlCrumbMapReader);
                    } else if (pType.equals ("other") == true) {
                        CloneOtherPagePixel cloneOtherPagePixel;

                        cloneOtherPagePixel = new CloneOtherPagePixel ();
                        cloneStatus = cloneOtherPagePixel.clonePixel (clonePixelLogBuilder, processedFeed, productUrlPidMapReader, catUrlCrumbMapReader);
                    } else if (pType.equals ("content") == true) {
                        CloneContentPagePixel cloneContentPagePixel;

                        cloneContentPagePixel = new CloneContentPagePixel ();
                        cloneStatus = cloneContentPagePixel.clonePixel (clonePixelLogBuilder, processedFeed, productUrlPidMapReader, catUrlCrumbMapReader);
                    } else if (pType.equals ("event") == true) {
                        // event pixel additional fields. 
                        // Looks like pixelLogBuilder.getEventType()/getEventAction() return blank values although the corresponding
                        // setEventType/setEventAction are used when parsing pixelLogs (PixelLogParser.java in br/work/src/....)
                        String eType;
                        String group;

                        eType = getPixelLogParam (clonePixelLogBuilder, "etype");
                        group = getPixelLogParam (clonePixelLogBuilder, "group");
 
                        if (eType.equals ("click-add") && group.equals ("cart")) {
                            // addtocart pixel
                            CloneAddToCartPixel cloneAddToCartPixel;

                            cloneAddToCartPixel = new CloneAddToCartPixel ();
                            cloneStatus = cloneAddToCartPixel.clonePixel (clonePixelLogBuilder, processedFeed, productUrlPidMapReader, catUrlCrumbMapReader);
                        } else if (eType.equals ("submit") && (group.equals ("suggest"))) {
                            // search event pixel
                            CloneSearchEventPixel cloneSearchEventPixel;

                            cloneSearchEventPixel = new CloneSearchEventPixel ();
                            cloneStatus = cloneSearchEventPixel.clonePixel (clonePixelLogBuilder, processedFeed, productUrlPidMapReader, catUrlCrumbMapReader);
                        } else if (eType.equals ("click") && (group.equals ("suggest"))) {
                            CloneSuggestEventPixel cloneSuggestEventPixel;

                            cloneSuggestEventPixel = new CloneSuggestEventPixel ();
                            cloneStatus = cloneSuggestEventPixel.clonePixel (clonePixelLogBuilder, processedFeed, productUrlPidMapReader, catUrlCrumbMapReader);
                        } else {
                            System.out.println ("Unknown event. eType = " + eType + ", group = " + group);
                            continue;
                        }
                    }

                }

            }

            // finally, if cloneStatus for this pixelLog is OK, build a pixelLog
            // object from the logBuilder and add it to the return-list
            if (cloneStatus == CloneConstants.CLONE_STATUS_OK) {
                clonePixelLog = clonePixelLogBuilder.build ();
                clonePixelLogList.add (clonePixelLog);
            }

        }

        return (clonePixelLogList);
    }

    // This method is similar to the one in clone/*Base class
    // first obtain entire list of params in the pixel. Then look for specified key's value
    // Return null if not found
    private String getPixelLogParam (PixelLog.Builder pixelLogBuilder, String paramKey) {
        List<PixelLog.PixelLogParam> paramsList;

        paramsList = pixelLogBuilder.getParamsList ();
        for (int i = 0; i < paramsList.size (); i++) {
            PixelLog.PixelLogParam aParam;

            aParam = paramsList.get (i);
            if (aParam.getKey ().equals (paramKey)) {
                return (aParam.getValue ());
            }
        }

        return (null);
    }
}


