UID_VALUE=1682069645648
USER_ID=1112223334445

# invalid acctId -- test
#ACCT_ID=9999
#DOMAIN_KEY=dummy_domain
#HOMEPAGE_URL=https://dummy.test.com

# demo-siv07
ACCT_ID=7704
DOMAIN_KEY=demo_siv07
HOMEPAGE_URL=https://demo.siv07.com

# testing: pacific_supply_mindcurv
#ACCT_ID=6475
#DOMAIN_KEY=pacific_supply_mindcurv
#HOMEPAGE_URL=https://pacific-supply-mindcurv.bloomreach.com

# title: domain_key
TITLE=$DOMAIN_KEY

# pid, prod_names for siv07
# pid-list, prod-name-list must have same length
PID_LIST=(
    11008364 
    11014974 
    50117SO0265
)
PROD_NAMES=(
    "LESCO Crosscheck Plus Liquid Insecticide 1 gal"  
    "LESCO Flash Fungicide 2 gal" 
    "LESCO Three-Way LO Broadleaf Post Emergent Herbicide"
)

# ---------- NO CHANGE NEEDED BELOW -----
COUNT=`echo ${#PID_LIST[@]}`
COUNT=$((COUNT - 1))

function perform_pageview {
    echo 'Pageview'

    prod_id=$1
    prod_name_encoded=$2

    PTYPE=product
    TYPE=pageview
    URL=${HOMEPAGE_URL}/products/$prod_id

    curl -v -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=$ACCT_ID&cookie2=uid%3D${UID_VALUE}%3Av%3D16.0%3Ats%3D1708556649535%3Ahc%3D5&sid=undefined&ref=$HOMEPAGE_URL&tzo=480&rand=0.9708529849018466&title=$prod_name_encoded&user_id=$USER_ID&domain_key=$DOMAIN_KEY&tms=gtm&test_data=false&ptype=$PTYPE&is_conversion=0&prod_id=$prod_id&prod_name=$prod_name_encoded&type=$TYPE&lang=en-US&url=$URL&version=16.0&debug=false"
}

function perform_add_to_cart {
    echo 'Add To Cart'

    prod_id=$1
    prod_name_encoded=$2

    PTYPE=event
    TYPE=event
    URL=${HOMEPAGE_URL}/products/$prod_id

    curl -v -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=$ACCT_ID&cookie2=uid%3D${UID_VALUE}%3Av%3D16.0%3Ats%3D1708556649535%3Ahc%3D5&sid=undefined&ref=$HOMEPAGE_URL&tzo=480&rand=0.19258222548721405&title=$prod_name_encoded&user_id=$USER_ID&domain_key=$DOMAIN_KEY&tms=gtm&test_data=false&ptype=$PTYPE&is_conversion=0&prod_id=$prod_id&sku=$prod_id&prod_name=$prod_name_encoded&group=cart&type=$TYPE&etype=click-add&e_prod_id=$prod_id&lang=en-US&url=$URL&version=16.0&debug=false"
}

function perform_conversion {
    echo 'Conversion'
    prod_id=$1
    prod_name_encoded=$2

    ORDER_ID=`uuidgen`
    echo order_id = $ORDER_ID

    PTYPE=other
    TYPE=pageview
    URL=${HOMEPAGE_URL}/orders/$ORDER_ID

    BASKET=!i$prod_id%27s$prod_id%27n$prod_name_encoded%27p25.00%27q2
    echo $BASKET

    BASKET_VALUE=50.00

    curl -v -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=$ACCT_ID&cookie2=uid%3D${UID_VALUE}%3Av%3D16.0%3Ats%3D1708556649535%3Ahc%3D5&sid=undefined&is_conversion=1&ref=$HOMEPAGE_URL&tzo=480&rand=0.05988073179999631&title=Order%20Detail%20%7C%20Site%20One&user_id=$USER_ID&domain_key=$DOMAIN_KEY&tms=gtm&test_data=false&ptype=$PTYPE&prod_id=$prod_id&prod_name=$prod_name_encoded&type=$TYPE&lang=en-US&url=$URL&version=16.0&basket=$BASKET&basket_value=$BASKET_VALUE&order_id=$ORDER_ID&debug=false"
}

for i in $(eval echo {0..$COUNT} )
do
    prod_id=${PID_LIST[$i]}
    prod_name=${PROD_NAMES[$i]}
    echo $prod_id
    echo $prod_name

    prod_name_encoded=`echo $prod_name | jq -Rr @uri` 

    perform_pageview $prod_id $prod_name_encoded

    perform_add_to_cart $prod_id $prod_name_encoded

    perform_conversion $prod_id $prod_name_encoded

    echo '------'
done

