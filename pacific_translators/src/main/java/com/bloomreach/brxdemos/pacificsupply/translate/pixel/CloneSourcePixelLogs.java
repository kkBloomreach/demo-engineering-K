package com.bloomreach.brxdemos.pacificsupply.translate.pixel;

// given a list of pixelLogs from a source file, clone each and return list of clone'd pixelLogs
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.bloomreach.proto.Aggregation.PixelLog;
import com.bloomreach.proto.Aggregation.PixelLog.Builder;

public class CloneSourcePixelLogs {

    ProcessedFeed processedFeed;
    UidToViewIdMap uidViewIdMap;

    public CloneSourcePixelLogs (ProcessedFeed processedFeed, UidToViewIdMap uidViewIdMap) {
        this.processedFeed = processedFeed;
        this.uidViewIdMap = uidViewIdMap;
    }

    public ArrayList <PixelLog> clone (ArrayList<PixelLog> srcPixelLogs) {
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
            cloneStatus = ClonePixelConstants.CLONE_STATUS_ERROR;   // init 

            isConversion = pixelLog.getIsConversion ();
            if (isConversion == 1) {
                CloneConversionPixel cloneConversionPixel;

                cloneConversionPixel = new CloneConversionPixel ();
                cloneStatus = cloneConversionPixel.clonePixel (clonePixelLogBuilder, uidViewIdMap, processedFeed);
            } else {
                pType = pixelLog.getPtype ();
                if (StringUtils.isEmpty (pType) == true) {
                    System.out.println ("Error. No ptype in source pixel log");
                    continue;
                } else {
                    if (pType.equals ("homepage") == true) {
                        CloneHomePagePixel cloneHomePagePixel;

                        cloneHomePagePixel = new CloneHomePagePixel ();
                        cloneStatus = cloneHomePagePixel.clonePixel (clonePixelLogBuilder, uidViewIdMap, processedFeed);
                    } else if (pType.equals ("product") == true) {
                        CloneProductPagePixel cloneProductPagePixel;

                        cloneProductPagePixel = new CloneProductPagePixel ();
                        cloneStatus = cloneProductPagePixel.clonePixel (clonePixelLogBuilder, uidViewIdMap, processedFeed);
                    } else if (pType.equals ("category") == true) {
                        CloneCategoryPagePixel cloneCategoryPagePixel;

                        cloneCategoryPagePixel = new CloneCategoryPagePixel ();
                        cloneStatus = cloneCategoryPagePixel.clonePixel (clonePixelLogBuilder, uidViewIdMap, processedFeed);
                    } else if (pType.equals ("search") == true) {
                        CloneSearchPagePixel cloneSearchPagePixel;

                        cloneSearchPagePixel = new CloneSearchPagePixel ();
                        cloneStatus = cloneSearchPagePixel.clonePixel (clonePixelLogBuilder, uidViewIdMap, processedFeed);
                    } else if (pType.equals ("other") == true) {
                        CloneOtherPagePixel cloneOtherPagePixel;

                        cloneOtherPagePixel = new CloneOtherPagePixel ();
                        cloneStatus = cloneOtherPagePixel.clonePixel (clonePixelLogBuilder, uidViewIdMap, processedFeed);
                    } else if (pType.equals ("content") == true) {
                        CloneContentPagePixel cloneContentPagePixel;

                        cloneContentPagePixel = new CloneContentPagePixel ();
                        cloneStatus = cloneContentPagePixel.clonePixel (clonePixelLogBuilder, uidViewIdMap, processedFeed);
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
                            cloneStatus = cloneAddToCartPixel.clonePixel (clonePixelLogBuilder, uidViewIdMap, processedFeed);
                        } else if (eType.equals ("submit") && (group.equals ("suggest"))) {
                            // search event pixel
                            CloneSearchEventPixel cloneSearchEventPixel;

                            cloneSearchEventPixel = new CloneSearchEventPixel ();
                            cloneStatus = cloneSearchEventPixel.clonePixel (clonePixelLogBuilder, uidViewIdMap, processedFeed);
                        } else if (eType.equals ("click") && (group.equals ("suggest"))) {
                            CloneSuggestEventPixel cloneSuggestEventPixel;

                            cloneSuggestEventPixel = new CloneSuggestEventPixel ();
                            cloneStatus = cloneSuggestEventPixel.clonePixel (clonePixelLogBuilder, uidViewIdMap, processedFeed);
                        } else {
                            System.out.println ("Unknown event. eType = " + eType + ", group = " + group);
                            continue;
                        }
                    }

                }

            }

            // finally, if cloneStatus for this pixelLog is OK, build a pixelLog
            // object from the logBuilder and add it to the return-list
            if (cloneStatus == ClonePixelConstants.CLONE_STATUS_OK) {
                clonePixelLog = clonePixelLogBuilder.build ();
                clonePixelLogList.add (clonePixelLog);
            }

        }

        return (clonePixelLogList);
    }

    // first obtain entire list of params in the pixel. Then look for specified key's value
    // Return null if not found
    protected String getPixelLogParam (PixelLog.Builder pixelLogBuilder, String paramKey) {
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

