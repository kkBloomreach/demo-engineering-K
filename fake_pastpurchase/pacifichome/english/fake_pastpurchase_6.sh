USER_ID=1112223334445
ORDER_ID=`uuidgen`
echo order_id = $ORDER_ID

# visit
curl -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=6413&cookie2=uid%3D1682069645648%3Av%3D16.0%3Ats%3D1708556649535%3Ahc%3D5&sid=undefined&ref=&tzo=480&rand=0.9708529849018466&title=Product%20Details%20%7C%20Pacific%20Home&user_id=$USER_ID&domain_key=&tms=gtm&customer_profile=industrial&test_data=false&ptype=product&is_conversion=0&cat_id=116815&cat=Outdoor&prod_id=92197&prod_name=White%20Square%20Scallop%209%20Ft%20Tilting%20Outdoor%20Umbrella&sku=57003203&type=pageview&lang=en-US&cdp_segments=63a1ede75322418784c619f1%3A63a21063733ffea05e1a29ee&url=https%3A%2F%2Fpacific.bloomreach.com%2Fhome%2Fproducts%2F92197___57003203&version=16.0"

# add to cart

curl -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=6413&cookie2=uid%3D1682069645648%3Av%3D16.0%3Ats%3D1708556649535%3Ahc%3D5&sid=undefined&ref=&tzo=480&rand=0.19258222548721404&user_id=$USER_ID&domain_key=&tms=gtm&customer_profile=industrial&test_data=false&ptype=event&is_conversion=0&cat_id=116815&cat=Outdoor&prod_id=92197&prod_name=White%20Square%20Scallop%209%20Ft%20Tilting%20Outdoor%20Umbrella&sku=57003203&group=cart&type=event&etype=click-add&e_prod_id=92197&e_sku=57003203&lang=en-US&cdp_segments=63a1ede75322418784c619f1%3A63a21063733ffea05e1a29ee&url=https%3A%2F%2Fpacific.bloomreach.com%2Fhome%2Fproducts%2F92197___57003203&version=16.0"

# convert
curl -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=6413&cookie2=uid%3D1682069645648%3Av%3D16.0%3Ats%3D1708556649535%3Ahc%3D5&sid=undefined&is_conversion=1&basket=!i92197%27s57003203%27nWhite%20Square%20Scallop%209%20Ft%20Tilting%20Outdoor%20Umbrella%27p229.99%27q5&order_id=$ORDER_ID&basket_value=1149.95&ref=&tzo=480&rand=0.05988073179999631&title=Order%20Detail%20%7C%20Pacific%20Home&user_id=$USER_ID&domain_key=&tms=gtm&customer_profile=industrial&test_data=false&ptype=other&cat_id=116815&cat=Outdoor&prod_id=92197&prod_name=White%20Square%20Scallop%209%20Ft%20Tilting%20Outdoor%20Umbrella&sku=57003203&type=pageview&lang=en-US&cdp_segments=63a1ede75322418784c619f1%3A63a21063733ffea05e1a29ee&url=https%3A%2F%2Fpacific.bloomreach.com%2Fhome%2Forders%2F6tsspsfr-503c-4545-a2d2-3fda6e0a5e8a&version=16.0"



