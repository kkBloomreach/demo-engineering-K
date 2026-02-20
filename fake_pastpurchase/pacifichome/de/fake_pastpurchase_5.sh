USER_ID=1112223334445
ORDER_ID=`uuidgen`
echo order_id = $ORDER_ID

# visit page
curl -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=7451&cookie2=&sid=undefined&ref=&tzo=480&rand=0.7468968944291312&title=Product%20Details%20%7C%20Haus&user_id=$USER_ID&domain_key=&tms=gtm&customer_profile=industrial&test_data=false&ptype=product&prod_id=95916&prod_name=Multicolor%20Hallo%20Sonnenschein%20Regenbogen%20Kokosfu%C3%9Fmatte&sku=589398&is_conversion=0&cat_id=119582&cat=Doormats&type=pageview&lang=en-US&url=https%3A%2F%2Fpacific.bloomreach.com%2Fhaus%2Fproducts%2F95916___589398&version=16.0"

# add to cart
curl -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=7451&cookie2=&sid=undefined&ref=&tzo=480&rand=0.8081820913997328&user_id=$USER_ID&domain_key=&tms=gtm&customer_profile=industrial&test_data=false&ptype=event&prod_id=95916&prod_name=Multicolor%20Hallo%20Sonnenschein%20Regenbogen%20Kokosfu%C3%9Fmatte&sku=589398&is_conversion=0&cat_id=119582&cat=Doormats&group=cart&type=event&etype=click-add&e_prod_id=95916&e_sku=589398&lang=en-US&url=https%3A%2F%2Fpacific.bloomreach.com%2Fhaus%2Fproducts%2F95916___589398&version=16.0"

# convert
curl -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=7451&cookie2=&sid=undefined&is_conversion=1&basket=!i95916%27s589398%27nMulticolor%20Hallo%20Sonnenschein%20Regenbogen%20Kokosfu%C3%9Fmatte%27p16.99%27q3&order_id=$ORDER_ID&basket_value=50.97&ref=&tzo=480&rand=0.31089500195358877&title=Order%20Detail%20%7C%20Haus&user_id=$USER_ID&domain_key=&tms=gtm&customer_profile=industrial&test_data=false&ptype=other&prod_id=95916&prod_name=Multicolor%20Hallo%20Sonnenschein%20Regenbogen%20Kokosfu%C3%9Fmatte&sku=589398&cat_id=119582&cat=Doormats&type=pageview&lang=en-US&url=https%3A%2F%2Fpacific.bloomreach.com%2Fhaus%2Forders%2Fskwut8ds-503c-4545-a2d2-3fda6e0a5e8a&version=16.0"



