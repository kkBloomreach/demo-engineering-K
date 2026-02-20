USER_ID=1112223334445
ORDER_ID=`uuidgen`
echo order_id = $ORDER_ID

# visit page
curl -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=7451&cookie2=&sid=undefined&ref=&tzo=480&rand=0.2751437695964227&title=Product%20Details%20%7C%20Haus&user_id=$USER_ID&domain_key=&tms=gtm&customer_profile=industrial&test_data=false&ptype=product&prod_id=61873&prod_name=Graue%20Jute%20gestreifte%20Sahaj%20Tab%20Top%20Vorh%C3%A4nge%2C%20Set%20von%202.&sku=526693&is_conversion=0&cat_id=119563&cat=Curtains&type=pageview&lang=en-US&url=https%3A%2F%2Fpacific.bloomreach.com%2Fhaus%2Fproducts%2F61873___526693&version=16.0"

# add to cart
curl -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=7451&cookie2=&sid=undefined&ref=&tzo=480&rand=0.2520452213480633&user_id=$USER_ID&domain_key=&tms=gtm&customer_profile=industrial&test_data=false&ptype=event&prod_id=61873&prod_name=Graue%20Jute%20gestreifte%20Sahaj%20Tab%20Top%20Vorh%C3%A4nge%2C%20Set%20von%202.&sku=526693&is_conversion=0&cat_id=119563&cat=Curtains&group=cart&type=event&etype=click-add&e_prod_id=61873&e_sku=526693&lang=en-US&url=https%3A%2F%2Fpacific.bloomreach.com%2Fhaus%2Fproducts%2F61873___526693&version=16.0"

# convert
curl -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=7451&cookie2=&sid=undefined&is_conversion=1&basket=!i61873%27s526693%27nGraue%20Jute%20gestreifte%20Sahaj%20Tab%20Top%20Vorh%C3%A4nge%2C%20Set%20von%202.%27p39.98%27q3&order_id=$ORDER_ID&basket_value=119.94&ref=&tzo=480&rand=0.02247251667466421&title=Order%20Detail%20%7C%20Haus&user_id=$USER_ID&domain_key=&tms=gtm&customer_profile=industrial&test_data=false&ptype=other&prod_id=61873&prod_name=Graue%20Jute%20gestreifte%20Sahaj%20Tab%20Top%20Vorh%C3%A4nge%2C%20Set%20von%202.&sku=526693&cat_id=119563&cat=Curtains&type=pageview&lang=en-US&url=https%3A%2F%2Fpacific.bloomreach.com%2Fhaus%2Forders%2F8c2drjvn-503c-4545-a2d2-3fda6e0a5e8a&version=16.0"


