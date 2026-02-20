package com.bloomreach.brxdemos.pacifichome.translate.pixel.clone;

// abstract base class for all clonePixel classes
import java.util.List;
import java.net.URLEncoder;
import java.net.URLDecoder;

import org.apache.commons.lang.StringUtils;

import com.bloomreach.proto.Aggregation.PixelLog;
import com.bloomreach.proto.Aggregation.PixelLog.PixelLogParam;
import com.bloomreach.proto.Aggregation.PixelLog.Builder;

import com.bloomreach.brxdemos.pacifichome.translate.pixel.feed.*;
import com.bloomreach.brxdemos.pacifichome.translate.pixel.CloneConstants;
import com.bloomreach.brxdemos.pacifichome.translate.pixel.urlmap.ProductURLPidMapReader;
import com.bloomreach.brxdemos.pacifichome.translate.pixel.urlmap.CategoryURLCrumbMapReader;

public abstract class ClonePixelLogBase {


    protected ClonePixelLogBase () {
    }

    // common fields to be cloned in ALL pixels
    protected int cloneCommonFields (Builder pixelLogBuilder, ProcessedFeed processedFeed, ProductURLPidMapReader productUrlPidMapReader, CategoryURLCrumbMapReader catUrlCrumbMapReader) {
        String refUrl;

        // domain
        pixelLogBuilder.setDomain (CloneConstants.PACIFICHOME_DOMAIN);

        // acct id
        pixelLogBuilder.setAcctId (CloneConstants.PACIFICHOME_ACCOUNT_ID);

        // ref_url. There are too many variations of ref_url in the source pixels
        // As a catchAll, if an exception occurs, set ref_url = HOMEPAGE
        try {
            refUrl = buildRefUrl (pixelLogBuilder, processedFeed, productUrlPidMapReader, catUrlCrumbMapReader);
        } catch (Exception e) {
            refUrl = CloneConstants.HOMEPAGE_URL;
        }
        pixelLogBuilder.setRefUrl (refUrl);
        replacePixelLogParam (pixelLogBuilder, "ref", refUrl);

        // param acctId
        replacePixelLogParam (pixelLogBuilder, "acct_id", CloneConstants.PACIFICHOME_ACCOUNT_ID);

        // refQuery
        String refQuery;
        refQuery = pixelLogBuilder.getRefQuery ();
        if (StringUtils.isNotEmpty (refQuery) == true) {
            pixelLogBuilder.setRefQuery ("");
        }

        // add param domain_key. If one exists already, replace it with 'pacifichome'
        if (doesPixelLogParamExist (pixelLogBuilder, "domain_key") == true) {
            replacePixelLogParam (pixelLogBuilder, "domain_key", 
                                  CloneConstants.PACIFICHOME_DOMAIN_KEY);
        } else {
            addPixelLogParam (pixelLogBuilder, "domain_key", 
                                  CloneConstants.PACIFICHOME_DOMAIN_KEY);
        }

        // pixelLogBuilder.setDomain (CloneConstants.PACIFICHOME_PIXEL_DOMAIN_KEY);

        // if there is a df_domain_key param, remove that param as well
        // Note that the original pixel source (worldmarket) did not have
        // multiple domains. Therefore "df_domain_key" not in source pixels
        if (doesPixelLogParamExist (pixelLogBuilder, "df_domain_key") == true) {
            removePixelLogParam (pixelLogBuilder, "df_domain_key");
        }

        // if there is a df_prod_name param, remove that param as well
        if (doesPixelLogParamExist (pixelLogBuilder, "df_prod_name") == true) {
            removePixelLogParam (pixelLogBuilder, "df_prod_name");
        }

        // update title if it contains "World Market"
        String srcTitle = pixelLogBuilder.getTitle ();
        if (StringUtils.isNotEmpty (srcTitle)) {
            String srcTitle_lowerCase = srcTitle.toLowerCase ();
            int indx = srcTitle_lowerCase.indexOf ("world");
            if (indx >= 0) {
                String modTitle;
                modTitle = srcTitle.substring (0, indx);
                pixelLogBuilder.setTitle (modTitle);
                replacePixelLogParam (pixelLogBuilder, "title", modTitle);
            }
        }

        // remove/replace rel_canonical_url, can_url if they exist
        pixelLogBuilder.setRelCanonicalUrl (CloneConstants.HOMEPAGE_URL);
        if (doesPixelLogParamExist (pixelLogBuilder, "can_url") == true) {
            removePixelLogParam (pixelLogBuilder, "can_url");
        }

        // remove/replace link
        pixelLogBuilder.setLink (CloneConstants.HOMEPAGE_URL);
        if (doesPixelLogParamExist (pixelLogBuilder, "link") == true) {
            replacePixelLogParam (pixelLogBuilder, "link", CloneConstants.HOMEPAGE_URL);
        }

        // in some pixels, even if it is not a product-page pixel, there
        // is 'prod_name' field which also contains "world market"
        String prodName = pixelLogBuilder.getProdName ();
        if (StringUtils.isNotEmpty (prodName) == true) {
            if (prodName.indexOf ("World Market") >= 0) {
                // in many cases, the prod_name in input has a registered trademark symbol
                // Therefore, replace "world market" with just a blank, not "pacifichome"
                // Otherwise it will look as if "pacifichome" is registered-trademark as well 
                prodName = prodName.replaceAll ("World Market", "");
                pixelLogBuilder.setProdName (prodName);
                replacePixelLogParam (pixelLogBuilder, "prod_name", prodName);
            }
        }

        return (CloneConstants.CLONE_STATUS_OK);
    }

    /**
     * Add PixelLogParam corresponding to the passed parameter key-value pair to the pixel builder.
     * @param pixelBuilder
     * @param paramKey
     * @param paramValue
     */
    protected void addPixelLogParam (PixelLog.Builder pixelLogBuilder, String paramKey, String paramValue) {

        PixelLogParam.Builder paramBuilder = PixelLogParam.newBuilder();
        paramBuilder.setKey(paramKey);
        paramBuilder.setValue(paramValue);
        pixelLogBuilder.addParams(paramBuilder.build());
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

    // replace an existing parameter's value
    // first obtain entire list of params in the pixel. Then look for specified key's index 
    // and use that to replace value. Looks like the "setParams" method does not have a
    // overloaded method where it takes a paramKey as an argument
    protected boolean replacePixelLogParam (PixelLog.Builder pixelLogBuilder, String paramKey, String paramValue) {
        List<PixelLog.PixelLogParam> paramsList;

        paramsList = pixelLogBuilder.getParamsList ();
        for (int i = 0; i < paramsList.size (); i++) {
            PixelLogParam aParam;

            aParam = paramsList.get (i);
            if (aParam.getKey ().equals (paramKey)) {
                PixelLogParam.Builder paramBuilder = PixelLogParam.newBuilder();
                paramBuilder.setKey(paramKey);
                paramBuilder.setValue(paramValue);
                PixelLogParam newPixelLogParam = paramBuilder.build();

                pixelLogBuilder.setParams(i, newPixelLogParam);

                return (true);
            }
        }

        // specified paramKey not found in current paramsList
        return (false);
    }

    // remove an existing parameter
    // first obtain entire list of params in the pixel. Then look for specified key's index 
    // and use that to remove it. Looks like the "removeParams" method does not have a
    // overloaded method where it takes a paramKey as an argument
    protected boolean removePixelLogParam (PixelLog.Builder pixelLogBuilder, String paramKey) {
        List<PixelLog.PixelLogParam> paramsList;

        paramsList = pixelLogBuilder.getParamsList ();
        for (int i = 0; i < paramsList.size (); i++) {
            PixelLogParam aParam;

            aParam = paramsList.get (i);
            if (aParam.getKey ().equals (paramKey)) {
                pixelLogBuilder.removeParams(i);
                return (true);
            }
        }

        // specified paramKey not found in current paramsList
        return (false);
    }

    // check if given paramKey exists. This is needed for params such as "df_domain_key"
    // which exist only if it contains "df_*" params
    protected boolean doesPixelLogParamExist (PixelLog.Builder pixelLogBuilder, String paramKey) {
        List<PixelLog.PixelLogParam> paramsList;

        paramsList = pixelLogBuilder.getParamsList ();
        for (int i = 0; i < paramsList.size (); i++) {
            PixelLogParam aParam;

            aParam = paramsList.get (i);
            if (aParam.getKey ().equals (paramKey)) {
                return (true);
            }
        }

        // specified paramKey not found in current paramsList
        return (false);
    }

    // generate cloned productId in exactly the same way it is done when feed is preProcessed
    // For pacificHome, pid remains unchanged 
    protected String generateUniqPid  (String srcPid) {
        return (srcPid);
    }

    protected String generateProductUrl (String newPid) {
        return (CloneConstants.PRODUCT_URL_PREFIX  + newPid + "___" + newPid);
    }

    protected String generateSEOFriendlyCrumbLeaf (String crumbLeaf) {
        String crumbLeafLowerCase = crumbLeaf.toLowerCase ();
        String tmpTxt1 = crumbLeafLowerCase.replaceAll ("[^a-zA-Z0-9\\-\\+\\s]", "");
        String tmpTxt2 = tmpTxt1.replaceAll ("[\\s]", "-");

        return (tmpTxt2);
    }

    // crumbInFeed example: "Kitchen>Kitchen Tools>Cutting Boards"
    protected String generateCategoriesUrl (FeedCrumbData crumbData) {
        return (CloneConstants.CATEGORY_URL_PREFIX  + crumbData.getLeafCrumbId ()); // eg: .../categories/23123
    }

    protected String generateSearchPageUrl (String queryTerm) {
        String searchResultPageUrl;
        String encodedQuery;

        // encodedQuery = URLEncoder.encode (query);
        encodedQuery = queryTerm;   // assume queryTerm is already encoded in source pixel
        searchResultPageUrl = CloneConstants.SEARCH_PAGE_URL_PREFIX;
        searchResultPageUrl = searchResultPageUrl + encodedQuery; 
        return (searchResultPageUrl);
    }

    // Building refUrl from the value in original pixel is complicated
    // The algo builds one based on type-of-pixel, source-ref-url, ...
    private String buildRefUrl (PixelLog.Builder pixelLogBuilder, ProcessedFeed processedFeed, ProductURLPidMapReader productUrlPidMapReader, CategoryURLCrumbMapReader catUrlCrumbMapReader) {
        String refUrl;
        String pType;

        refUrl = pixelLogBuilder.getRefUrl ();
        if (refUrl == null) 
            return CloneConstants.HOMEPAGE_URL; // Default...

        // many refUrls in the source start off from google / costplus / ...
        // replace those with 'homepage' url
        if ((refUrl.startsWith ("https://www.worldmarket.com") == false) && 
            (refUrl.startsWith ("http://www.worldmarket.com") == false) ) {
            return (CloneConstants.HOMEPAGE_URL);
        }

        // refUrl for a ATC pixel. 
        // effectively remains same as the value in source pixel (but translated to PacificHome format)

        // if refUrl is a search-resullt-page url, need to use queryTerm in 
        // url itself. This is needed by BR's analytics. WorldMarket search urls
        // are of this form: 
        // ref_url: "https://www.worldmarket.com/search.do?query=Side+table"
        if (refUrl.indexOf ("search.do") > 0) {
            int eqIndx; // index of '='
    
            eqIndx = refUrl.lastIndexOf ('='); // see if there are queryParams
            if (eqIndx > 0) {
                String queryTerm;

                queryTerm = refUrl.substring (eqIndx+1);
                refUrl = generateSearchPageUrl (queryTerm);
            }
            return (refUrl);
        } 

        // if refUrl = product page, use urlPidMapReader. 
        // The refUrls with /product/ form have different format in source pixels
        // as compared to pacifichome
        if (refUrl.indexOf ("/www.worldmarket.com/product/") > 0) {
            int slashIndx;
            int doIndx; // indx of '.do'
            String prodId;
            String refUrlHead;

            // Some refUrls have no ".do". 
            // Some refUrls have a '/' AFTER .do. So, first get the 'head' (ie, 0 -> .do)
            // then find lastIndxOf '/' in that head-string
            doIndx = refUrl.lastIndexOf (".do");
            if (doIndx > 0)
                refUrlHead = refUrl.substring (0, doIndx);
            else
                refUrlHead = refUrl;

            prodId = productUrlPidMapReader.getPid (refUrlHead);
            if (prodId != null) {
                refUrl = generateProductUrl (prodId);
                refUrl = refUrl.replace ("worldmarket", CloneConstants.HOMEPAGE_TITLE.toLowerCase());
            } else {
                System.out.println ("DEBUG refUrl not in urlPidMal: " + refUrlHead);
                refUrl = CloneConstants.HOMEPAGE_URL;
            }

            return (refUrl);
        }

        // if refUrl = category page, use catUrlCrumbMapReader
        // sample refUrl: https://www.worldmarket.com/category/furniture/living-room.do
        if (refUrl.indexOf ("/www.worldmarket.com/category/") > 0) {
            int doIndx; // index of '.do'
            int qIndx; // index of '?'
            int slashIndx; // index of '/'
            String refUrlLeaf;
            FeedCrumbData crumbInFeed;

            // some category refUrls DON'T have .do
            doIndx = refUrl.lastIndexOf (".do");
            if (doIndx > 0)
                refUrl = refUrl.substring (0, doIndx);

            // some category refUrls have trailing ?
            qIndx = refUrl.lastIndexOf ('?');
            if (qIndx > 0)
                refUrl = refUrl.substring (0, qIndx);

            // extract leaf - it is encoded
            slashIndx = refUrl.lastIndexOf ('/');
            if (slashIndx > 0)
                refUrlLeaf = refUrl.substring (slashIndx+1);
            else
                refUrlLeaf = refUrl;

            // leaf may be URLEncoded OR may have embedded '-'
            refUrlLeaf = URLDecoder.decode (refUrlLeaf);
            refUrlLeaf = refUrlLeaf.replaceAll ("-", " ");
            crumbInFeed = catUrlCrumbMapReader.getFeedCrumbData (refUrlLeaf);

            if (crumbInFeed != null)
                refUrl = generateCategoriesUrl (crumbInFeed);
            else
                refUrl = CloneConstants.HOMEPAGE_URL;

            return (refUrl);
        }

        // if any other type of refUrl, return homepage itself
        refUrl = CloneConstants.HOMEPAGE_URL;
        return (refUrl);
    }

}

/*******
//             // replace world*market, case-insensitive
//             // refUrlLeaf = refUrlLeaf.replaceAll ("(?i)world.*market*[^-]", CloneConstants.HOMEPAGE_TITLE.toLowerCase());
//         pType = pixelLogBuilder.getPtype ();
//         if (pType.equals ("event") == true) {
//             String eType;
//             String group;
// 
//             // if this is a ATC event, url, refUrl = productPage url
//             eType = getPixelLogParam (pixelLogBuilder, "etype");
//             group = getPixelLogParam (pixelLogBuilder, "group");
//             if (eType.equals ("click-add") && group.equals ("cart")) {
//                 // String prodId;
// 
//                 // prodId = pixelLogBuilder.getProdId ();
//                 // prodId = generateUniqPid (prodId);
//                 // if (processedFeed.isProductInFeed (prodId) == true)
// 
//                 return (refUrl);
//             }
//         }
******/
