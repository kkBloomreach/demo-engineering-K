USER_ID=1112223334445
ORDER_ID=`uuidgen`
echo order_id = $ORDER_ID

# visit page
curl -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=7451&cookie2=&sid=undefined&ref=&tzo=480&rand=0.7349018752024936&title=Product%20Details%20%7C%20Haus&user_id=$USER_ID&domain_key=&tms=gtm&customer_profile=industrial&test_data=false&ptype=product&prod_id=65940&prod_name=Gold%20Deko-F%C3%A4cher-Drum-Tischlampenschirm&sku=534684_0&is_conversion=0&cat_id=116871&cat=Table%20Lamps&type=pageview&lang=en-US&url=https%3A%2F%2Fpacific.bloomreach.com%2Fhaus%2Fproducts%2F65940___534684_0&version=16.0"

# add to cart
curl -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=7451&cookie2=&sid=undefined&ref=&tzo=480&rand=0.44653436824165404&user_id=$USER_ID&domain_key=&tms=gtm&customer_profile=industrial&test_data=false&ptype=event&prod_id=65940&prod_name=Gold%20Deko-F%C3%A4cher-Drum-Tischlampenschirm&sku=534684_0&is_conversion=0&cat_id=116871&cat=Table%20Lamps&group=cart&type=event&etype=click-add&e_prod_id=65940&e_sku=534684_0&lang=en-US&url=https%3A%2F%2Fpacific.bloomreach.com%2Fhaus%2Fproducts%2F65940___534684_0&version=16.0"

# convert
curl -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=7451&cookie2=&sid=undefined&is_conversion=1&basket=!i65940%27s534684_0%27nGold%20Deko-F%C3%A4cher-Drum-Tischlampenschirm%27p26.99%27q6&order_id=$ORDER_ID&basket_value=161.94&ref=&tzo=480&rand=0.3638271804148756&title=Order%20Detail%20%7C%20Haus&user_id=$USER_ID&domain_key=&tms=gtm&customer_profile=industrial&test_data=false&ptype=other&prod_id=65940&prod_name=Gold%20Deko-F%C3%A4cher-Drum-Tischlampenschirm&sku=534684_0&cat_id=116871&cat=Table%20Lamps&type=pageview&lang=en-US&url=https%3A%2F%2Fpacific.bloomreach.com%2Fhaus%2Forders%2Fgtrly76m-503c-4545-a2d2-3fda6e0a5e8a&version=16.0"



