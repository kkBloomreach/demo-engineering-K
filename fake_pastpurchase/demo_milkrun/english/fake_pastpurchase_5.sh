ACCT_ID=7526
BR_UID=4827027657798
USER_ID=1112223334445
REF_URL=https%3A%2F%2Fdemo.milkrun.com
#DOMAIN_KEY=demo_milkrun
DOMAIN_KEY=

ORDER_ID=`uuidgen`

PROD_ID=187440
PROD_NAME=Bell%20Farms%20Crunchy%20Chips%201kg%20Bell%20Farms%20Crunchy%20Chips%20Frozen%201kg
PROD_URL=https%3A%2F%2Fdemo.milkrun.com%2Fproducts/$PROD_ID
PROD_PRICE=10.00

ATC_URL=https%3A%2F%2Fdemo.milkrun.com%2Faddtocart.html
CONV_URL=https%3A%2F%2Fdemo.milkrun.com%2Fthankyou

# visit product
#echo "https://p-eu.brsrvr.com/pix.gif?acct_id=$ACCT_ID&cookie2=uid%3D$BR_UID%3Av%3D16.0%3Ats%3D1707256326478%3Ahc%3D9&sid=undefined&ref=$REF_URL&tzo=480&rand=0.8966476736468367&title=$PROD_NAME&user_id=$USER_ID&domain_key=$DOMAIN_KEY&test_data=false&ptype=product&is_conversion=0&prod_id=$PROD_ID&prod_name=$PROD_NAME&type=pageview&lang=en-US&url=$PROD_URL&version=16.0"
curl -v -o ./pix.gif "https://p-eu.brsrvr.com/pix.gif?acct_id=$ACCT_ID&cookie2=uid%3D$BR_UID%3Av%3D16.0%3Ats%3D1707256326478%3Ahc%3D9&sid=undefined&ref=$REF_URL&tzo=480&rand=0.8966476736468367&title=$PROD_NAME&user_id=$USER_ID&domain_key=$DOMAIN_KEY&test_data=false&ptype=product&is_conversion=0&prod_id=$PROD_ID&prod_name=$PROD_NAME&type=pageview&lang=en-US&url=$PROD_URL&version=16.0"
echo "---"

# add to cart
#echo "https://p-eu.brsrvr.com/pix.gif?acct_id=$ACCT_ID&cookie2=uid%3D$BR_UID%3Av%3D16.0%3Ats%3D1707256326478%3Ahc%3D9&sid=undefined&ref=$PROD_URL&tzo=480&rand=0.37693448811431685&user_id=$USER_ID&domain_key=$DOMAIN_KEY&tms=gtm&test_data=false&ptype=event&is_conversion=0&prod_id=$PROD_ID&prod_name=$PROD_NAME&group=cart&type=event&etype=click-add&e_prod_id=$PROD_ID&e_sku=&lang=en-US&url=$ATC_URL&version=16.0"
curl -v -o ./pix.gif "https://p-eu.brsrvr.com/pix.gif?acct_id=$ACCT_ID&cookie2=uid%3D$BR_UID%3Av%3D16.0%3Ats%3D1707256326478%3Ahc%3D9&sid=undefined&ref=$PROD_URL&tzo=480&rand=0.37693448811431685&user_id=$USER_ID&domain_key=$DOMAIN_KEY&tms=gtm&test_data=false&ptype=event&is_conversion=0&prod_id=$PROD_ID&prod_name=$PROD_NAME&group=cart&type=event&etype=click-add&e_prod_id=$PROD_ID&e_sku=&lang=en-US&url=$ATC_URL&version=16.0"
echo "---"

# convert
#echo "https://p-eu.brsrvr.com/pix.gif?acct_id=$ACCT_ID&cookie2=uid%3D$BR_UID%3Av%3D16.0%3Ats%3D1707256326478%3Ahc%3D9&sid=undefined&is_conversion=1&basket=!i$PROD_ID%27n$PROD_NAME%27p$PROD_PRICE%27q1&order_id=$ORDER_ID&basket_value=$PROD_PRICE&ref=$ATC_URL&tzo=480&rand=0.4247554795413315&title=Order%20Details&user_id=$USER_ID&domain_key=$DOMAIN_KEY&tms=gtm&test_data=false&ptype=other&prod_id=$PROD_ID&prod_name=$PROD_NAME&type=pageview&lang=en-US&url=$CONV_URL&version=16.0"
curl -v -o ./pix.gif "https://p-eu.brsrvr.com/pix.gif?acct_id=$ACCT_ID&cookie2=uid%3D$BR_UID%3Av%3D16.0%3Ats%3D1707256326478%3Ahc%3D9&sid=undefined&is_conversion=1&basket=!i$PROD_ID%27n$PROD_NAME%27p$PROD_PRICE%27q1&order_id=$ORDER_ID&basket_value=$PROD_PRICE&ref=$ATC_URL&tzo=480&rand=0.4247554795413315&title=Order%20Details&user_id=$USER_ID&domain_key=$DOMAIN_KEY&tms=gtm&test_data=false&ptype=other&prod_id=$PROD_ID&prod_name=$PROD_NAME&type=pageview&lang=en-US&url=$CONV_URL&version=16.0"


