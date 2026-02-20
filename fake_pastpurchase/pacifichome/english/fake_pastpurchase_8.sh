USER_ID=1112223334445
ORDER_ID=`uuidgen`
echo order_id = $ORDER_ID

# visit
curl -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=6413&cookie2=uid%3D1682069645648%3Av%3D16.0%3Ats%3D1708556649535%3Ahc%3D5&sid=undefined&ref=&tzo=480&rand=0.3900837402223867&title=Product%20Details%20%7C%20Pacific%20Home&user_id=$USER_ID&domain_key=&tms=gtm&test_data=false&ptype=product&prod_id=95555&prod_name=Small%20Distressed%20Metal%20and%20Wood%20Amador%20Bookshelf&sku=589814&is_conversion=0&type=pageview&lang=en-US&cdp_segments=63a1ede75322418784c619f1%3A63a21063733ffea05e1a29f0&url=https%3A%2F%2Fpacific.bloomreach.com%2Fhome%2Fproducts%2F95555___589814&version=16.0"

# add-to-cart
curl -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=6413&cookie2=uid%3D1682069645648%3Av%3D16.0%3Ats%3D1708556649535%3Ahc%3D5&sid=undefined&ref=&tzo=480&rand=0.49255975240888605&user_id=$USER_ID&domain_key=&tms=gtm&test_data=false&ptype=event&prod_id=95555&prod_name=Small%20Distressed%20Metal%20and%20Wood%20Amador%20Bookshelf&sku=589814&is_conversion=0&group=cart&type=event&etype=click-add&e_prod_id=95555&e_sku=589814&lang=en-US&cdp_segments=63a1ede75322418784c619f1%3A63a21063733ffea05e1a29f0&url=https%3A%2F%2Fpacific.bloomreach.com%2Fhome%2Fproducts%2F95555___589814&version=16.0"

# convert
curl -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=6413&cookie2=uid%3D1682069645648%3Av%3D16.0%3Ats%3D1708556649535%3Ahc%3D5&sid=undefined&is_conversion=1&basket=!i95555%27s589814%27nSmall%20Distressed%20Metal%20and%20Wood%20Amador%20Bookshelf%27p159.99%27q1&order_id=$ORDER_ID&basket_value=159.99&ref=&tzo=480&rand=0.12084057115094549&title=Order%20Detail%20%7C%20Pacific%20Home&user_id=$USER_ID&domain_key=&tms=gtm&customer_profile=industrial&test_data=false&ptype=other&prod_id=95555&prod_name=Small%20Distressed%20Metal%20and%20Wood%20Amador%20Bookshelf&sku=589814&type=pageview&lang=en-US&cdp_segments=63a1ede75322418784c619f1%3A63a21063733ffea05e1a29f0&url=https%3A%2F%2Fpacific.bloomreach.com%2Fhome%2Forders%2F8edwoglh-503c-4545-a2d2-3fda6e0a5e8a&version=16.0"





