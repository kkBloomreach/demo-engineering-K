USER_ID=1112223334445
ORDER_ID=`uuidgen`
echo order_id = $ORDER_ID

# visit page
curl -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=7451&cookie2=&sid=undefined&ref=&tzo=480&rand=0.8177134396072598&user_id=$USER_ID&domain_key=&tms=gtm&customer_profile=industrial&test_data=false&ptype=event&is_conversion=0&cat_id=116746&cat=Dining%20Room%20Furniture&prod_id=28187&prod_name=Doppelseitiges%20Holbrook%20Sideboard&sku=467078&group=widget&type=event&etype=widget-view&wrid=25a3e688-2f1d-4bd8-bf86-281d20afa81a&wq=&wid=p9p12gy9&wty=co_bought&e_wrid=25a3e688-2f1d-4bd8-bf86-281d20afa81a&e_wq=&e_wid=p9p12gy9&e_wty=co_bought&lang=en-US&url=https%3A%2F%2Fpacific.bloomreach.com%2Fhaus%2Fproducts%2F28187___467078&version=16.0"

# add-to-cart
curl -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=7451&cookie2=&sid=undefined&ref=&tzo=480&rand=0.728260072490021&user_id=$USER_ID&domain_key=&tms=gtm&customer_profile=industrial&test_data=false&ptype=event&is_conversion=0&cat_id=116746&cat=Dining%20Room%20Furniture&prod_id=28187&prod_name=Doppelseitiges%20Holbrook%20Sideboard&sku=467078&group=cart&type=event&etype=click-add&e_prod_id=28187&e_sku=467078&lang=en-US&url=https%3A%2F%2Fpacific.bloomreach.com%2Fhaus%2Fproducts%2F28187___467078&version=16.0"

# convert
curl -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=7451&cookie2=&sid=undefined&is_conversion=1&basket=!i28187%27s467078%27nDoppelseitiges%20Holbrook%20Sideboard%27p499.99%27q3&order_id=$ORDER_ID&basket_value=1499.97&ref=&tzo=480&rand=0.4874253791371983&title=Order%20Detail%20%7C%20Haus&user_id=$USER_ID&domain_key=&tms=gtm&customer_profile=industrial&test_data=false&ptype=other&cat_id=116746&cat=Dining%20Room%20Furniture&prod_id=28187&prod_name=Doppelseitiges%20Holbrook%20Sideboard&sku=467078&type=pageview&lang=en-US&url=https%3A%2F%2Fpacific.bloomreach.com%2Fhaus%2Forders%2Fuhbe5kmk-503c-4545-a2d2-3fda6e0a5e8a&version=16.0"

