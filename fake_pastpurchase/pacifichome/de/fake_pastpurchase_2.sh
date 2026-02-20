USER_ID=1112223334445
ORDER_ID=`uuidgen`
echo order_id = $ORDER_ID

# visit page
curl -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=7451&cookie2=&sid=undefined&ref=&tzo=480&rand=0.4593296946372636&title=Product%20Details%20%7C%20Haus&user_id=$USER_ID&domain_key=&tms=gtm&customer_profile=industrial&test_data=false&ptype=product&prod_id=84146&prod_name=Mondstrahl%20Patchwork%20Leder%20Wurfkissen&sku=566364&is_conversion=0&type=pageview&lang=en-US&url=https%3A%2F%2Fpacific.bloomreach.com%2Fhaus%2Fproducts%2F84146___566364&version=16.0"

# add to cart
curl -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=7451&cookie2=&sid=undefined&ref=&tzo=480&rand=0.1482028790685146&user_id=$USER_ID&domain_key=&tms=gtm&customer_profile=industrial&test_data=false&ptype=event&prod_id=84146&prod_name=Mondstrahl%20Patchwork%20Leder%20Wurfkissen&sku=566364&is_conversion=0&group=cart&type=event&etype=click-add&e_prod_id=84146&e_sku=566364&lang=en-US&url=https%3A%2F%2Fpacific.bloomreach.com%2Fhaus%2Fproducts%2F84146___566364&version=16.0"

# convert
curl -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=7451&cookie2=&sid=undefined&is_conversion=1&basket=!i84146%27s566364%27nMondstrahl%20Patchwork%20Leder%20Wurfkissen%27p49.98%27q3&order_id=$ORDER_ID&basket_value=149.94&ref=&tzo=480&rand=0.5437392597507289&title=Order%20Detail%20%7C%20Haus&user_id=$USER_ID&domain_key=&tms=gtm&customer_profile=industrial&test_data=false&ptype=other&prod_id=84146&prod_name=Mondstrahl%20Patchwork%20Leder%20Wurfkissen&sku=566364&type=pageview&lang=en-US&url=https%3A%2F%2Fpacific.bloomreach.com%2Fhaus%2Forders%2Fhickp2bo-503c-4545-a2d2-3fda6e0a5e8a&version=16.0"



