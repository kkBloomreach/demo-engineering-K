package com.bloomreach.trafficgenerator;

// Change log 
// 0.1.0.1: Initial
// 0.2.0.1: added 'timeslice'
// 0.3.0.1: changed data directory structure, input filenames
// 0.4.0.1: select pid from actual api response
// 0.5.0.1: trigger pixel as-and-when it is built (ie, not 'collected' then dispatched)
// 0.6.0.1: added all session types
// 0.7.0.1: include campaigns, daily feedpublisher 
// 0.8.0.1: added boost parameter in api calls if campaign valid
// 0.9.0.1: changed to log4j2 (from log4j1), removed analytics engr, hadoop dependencies
// 1.0.0.1: Introduced visitor and site, and their threads
// 1.1.0.1: Code restructured
// 1.1.1.0: Added 'exclude categories' support
// 1.1.1.1: Even if feed publish fails, continue (log error)
// 1.2.0.1: Added suggest session
// 1.2.0.2: Small code fix
// 1.3.0.0: Introduce trafficSteps, random session generator (WIP)
// 1.4.0.0: Implement Step* classes
// 1.4.1.0: Code modifications, added home-page pixel
// 1.5.0.0: Changed predefined session generator to use Steps*
// 1.5.0.1: Add warnings if stepResult data not as expected
// 1.5.0.2: Added StepResultInvalidData to note incorrect data in a step
// 1.5.0.3: Predefined sessions, MVP
// 1.5.1.0: Added 'stepInvalidData' as one more step
// 1.5.2.0: Added step duration values
// 1.5.2.1: Replaced browseHome/browseOther -> StepStartUrl
// 1.5.2.2: Remove excluded products in apiResponse
// 1.5.3.0: Added support for Views
// 1.5.4.0: Extended search, suggest terms and campaign products
// 1.6.0.0: Moved gendata package to journeydata
// 1.6.0.1: Changed log file name (include hour and minute in filename)
// 1.6.0.2: Added sku_thumb_image in fl list
// 1.6.1.0: Added EnvironmentConfig
// 1.7.0.0: Changed site config json format
// 1.7.1.0: Changed product url in dailyFeed to match site config
// 1.7.2.0: Added pixel-debugger api end-point
// 1.7.2.1: Added 'aq' parameter in suggest event pixel
// 1.7.3.0: Added '*' (star) support in searchTerm to replicate
// 1.7.4.0: Added special-visitor-id for pastpurchase data (etc)
// 1.7.5.0: Updated url/refUrl flow in openUrl method
// 1.7.6.0: Special visitor info now provided via siteconfig
// 1.7.6.1: set InvalidData.url, refUrl 
// 1.7.6.2: predefined journey, after each session, check url, refUrl 
// 1.7.6.3: predefined journey, after each session, revert url, refUrl if stepResult invalid
// 1.7.6.4: slight fix 
// 1.7.6.5: check for null search API response
// 1.7.6.6: added more visitor-creator log messages
// 1.7.6.7: changed site log information (user/journey/session/steps)
// 1.7.6.8: some fixes
// 1.7.6.9: if dailyFeed file does not exist, copy original -> daily
// 1.7.7.0: Added browse pid, atc pid, conversion info to stepLog
// 1.7.7.1: small fix
// 1.7.8.0: DailyLogs are written out after N userLogs, to reduce in-memory footprint
// 1.8.0.0: Added rts segment evaluator to dynamically set userSegment
// 1.8.1.0: Change campaigns parser
// 1.9.0.0: new campaigns parser MVP
// 1.9.1.0: add 'include', 'exclude' options in campaign
// 1.9.2.0: add serarch term parser to include refinements. Also added Traffic DataGenerator class
// 1.9.3.0: few cleanups
// 1.9.3.1: Add user segment in log record
// 1.9.3.2: search term file-not-found -> stop
// 1.9.3.3: suggest->category bugfix
// 1.9.3.4: added specific log for 'select-suggest-term'
// 1.9.3.5: fixed suggest-select-product flow, randomJourney
// 1.9.3.6: changed pixelApi endpoint for event-mgr (no loner uses host = p-debugger)
// 1.9.4.0: add category collector
// 1.9.4.1: remove category extraction from product feed + associated cleanup; use categoryCollector instead
// 1.9.4.2: StepSuggetSelectProduct -> replaced with StepBrowsePDP
// 1.9.4.3: Removed zero2search, zero2category journeys in Predefined flows
// 1.9.4.4: Add 'phantom' search after zero search
// 1.9.4.5: Set different userAgent based on deviceType
// 1.9.5.0: Change zero-query session logic
// 1.9.5.1: Removed some dispatcher debug logMessage to keep log file size small
// 1.9.5.2: small log fix related to zero-query session termination
// 1.9.5.3: add skuid in suggest-select-product
// 1.9.6.0: for Coview (aka Refinement), ATC and Covert first product then do second product
// 1.9.6.1: added 'User-Agent' in api call header; used for device type detection
// 1.9.7.0: set startRefUrl = search-engine/social/home/...
// 1.10.0.0: change RTS-related algo (details in PACIFICAPPAREL-29)
// 1.10.0.1: small fix for RTS-related algo
// 1.10.1.0: increase traffic by increasing visitors/day
// 1.10.1.1: small fix - add 'suggest-select-term' to stepLog
// 1.10.2.0: added thread-lock synchronization in dailyLog 
// 1.11.0.0: session log bug fix
// 1.12.0.0: added partial s2s/s2c/c2s/c2c predefined flows
// 1.12.0.1: updated error/warning messages for clarification
// 1.12.0.2: updated error/warning messages for clarification
// 1.12.0.3: increase max-rows in API response to 100
// 1.12.0.4: add 'stepStart' time in log file
// 1.12.0.5: debug root cause(s) where ref = url 
// 1.12.0.6: set startRef = <blank> instead of <home> -- trial check
// 1.12.0.7: for search, collect ALL 'numFound' products using multiple api calls as needed
// 1.12.0.8: debug ref = url case, added more info along with stack trace
// 1.12.1.0: updated suggestEvent pixel to include both aq and q
// 1.12.2.0: updated buildPixel api params to include ref and url 
// 1.12.3.0: modified suggest-select-product generator flow
// 1.12.4.0: slight rearrangement of Dispatcher internal calls
// 1.12.4.1: set url = home in startStep for some cases (otherwise api call has exception)
// 1.12.4.2: to select random search term (or catId), avoid re-selection of current term (or catId)
// 1.12.4.3: check currentUrl != selected term/cat page url
// 1.12.4.4: additional code change for check currentUrl != selected term/cat page url
// 1.12.4.5: additional code change for check currentUrl != selected term/cat page url - selectZeroResultSearchTerm
// 1.12.4.6: adjusted endTime values (so that time-based-sort is deterministic)
// 1.12.4.7: more fixes to avoid ref == url
// 1.12.4.8: more fixes to avoid ref == url
// 1.12.4.9: more fixes to avoid ref == url
// 1.12.4.9: more fixes to avoid ref == url
// 1.12.5.0: added check to avoid simultaneous duplicate visitorIds
// 1.12.5.1: updated duplicate visitorIds algo
// 1.12.6.0: improve queryExecutor, make multiple api calls as needed
// 1.12.6.1: check q not blank in search/suggest api call
// 1.12.6.2: use br_psugg in PDP pixel if selected from suggest response
// 1.12.6.3: fixed visitorId creation algo
// 1.12.6.4: more fixes to avoid ref == url
// 1.12.6.5: correct pixel endpoint for EU region
// 1.12.6.6: added log message in feed processing
// 1.12.6.7: ensure RTS segment for home/apparel
// 1.12.6.8: deployed pacifichome, update site.json 
// 1.12.6.9: ref = url, corner case fix
// 1.12.7.0: increase total user pool to 10M
// 1.12.7.1: added site visitor monitor to control active visitors
// 1.12.7.2: added debug msg to find why running out of threads with large pool
// 1.12.8.0: if no dataConnect access key, don't index
// 1.12.8.1: if 'pid' field not in catalog, use product->tail as pid
// 1.12.8.2: dont exceed startRow > 10000 to collect API response, BR imposed limit 
// 1.13.0.0: include widget traffic (only on home page and PDP since current SPA has only those)
// 1.13.0.1: corrected urls for 'other' and 'post-conversion' pages
// 1.13.0.2: clode cleanups (removed unused imports etc...)
// 1.13.0.3: added widget-specific stepLog
// 1.13.0.4: fixed itemId in atc, click pixel. Tested for pacificApparel widgets
// 2.0.0.0:  Bumped version to 2.0.0.X
// 2.0.0.1:  Changed quantity-in-cart algo to reduce conversion amount
// 2.0.0.2:  Changed predefined traffic to 60% from 70%
// 2.0.0.3:  Removed few debug stmts
// 2.0.0.4:  Commented out 'api' logs in widgetLog file
// 2.1.0.0:  Changed ATC, Conversion formula to control conversions/visit ratio (predefined journey)
// 2.1.0.1:  Log totalVisitors/atc#/conversion# to monitor conversion ratio
// 2.1.0.2:  'category' facet may be missing in search API response - check if it exists 
// 2.1.0.3:  Added warning message if widget api response has zero results
// 2.1.0.4:  Changed some "info" messages to 'debug' to reduce log size
// 2.1.1.0:  Reduce widget traffic to avoid high-QPS problem
// 2.2.0.0:  Collect pixel counts across all users to calculate QPS/PageViews/Second, ...)
// 2.3.0.0:  add widgetLog to collect pdp/homepage/atc/... counts to monitor qps
// 2.3.1.0:  for widgetAPI, get only one the first response (not repeat)
// 2.3.2.0:  added apiCount log to monitor api QPS
// 2.3.2.1:  fixed categoryApiCount bug. Rolling log policy: increased size 1MB->5MB
// 2.3.2.2:  experiment different delay factors to reduce QPS (end goal, QPS < 10)
// 2.3.2.3:  use logInfo instead of logDebug in some cases, to be reverted to logDebug
// 2.3.2.4:  Testing mean-time-between-visitor to 5 sec (instead of 10)
// 2.3.2.5:  After testing, finally set visitor MTBF = 5
// 2.3.3.0:  Support low-perf-category feature, requested by SCs
// 2.3.4.0:  if no 'pid' attribute, create one from 'path' value
// 2.3.5.0:  add/not-add sku value in PDP url. Pacific* need sku in url; shopify does not
// 2.3.5.1:  added ForcedSessionExit exception message in VisitorHandler thread
// 2.3.6.0:  when forceSessionExit, leave site entirely (previously only session terminated)
// 2.3.6.1:  check for 'allCategoriesInfoList != null' in SearchCategories.java
// 2.3.6.2:  reverted ForcedExit -> ForcedSessionExit (ie, visitor bounces but does not exit site)
// 2.3.7.0:  added customJourney feature (customJourney configs, exceptions, ...)
// 2.3.7.1:  code cleanup after customjourney unit tests
// 2.3.7.2:  RandomJourney -> customJourney, exit session entirely
// 2.3.7.3:  Add variant_id (aka sku_id) in log file (for variant-slicing feature debug)
// 2.3.8.0:  Changed dataconnect API endpoint to their version V3
// 2.3.8.1:  added check for refurl == search for suggest and search events
// 2.3.8.2:  fix feed parser code for catalogs with views
// 2.3.8.3:  support views (as needed by pacific_supply)
// 2.4.0.0:  added RTS support using dynamic attribute 'style'
// 2.4.0.1:  set pixel event version = 17 (from 15)

public class GeneratorVersion {
    public final static String VERSION = "2.4.0.1-X"; // Make sure to also change pom.xml->"version"

    private GeneratorVersion () {
    }
}
