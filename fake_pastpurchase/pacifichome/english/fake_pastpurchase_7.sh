USER_ID=1112223334445
ORDER_ID=`uuidgen`
echo order_id = $ORDER_ID

# visit
curl -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=6413&cookie2=uid%3D1682069645648%3Av%3D16.0%3Ats%3D1708556649535%3Ahc%3D5&sid=undefined&ref=&tzo=480&rand=0.3598439991542526&title=Product%20Details%20%7C%20Pacific%20Home&user_id=$USER_ID&domain_key=&tms=gtm&customer_profile=industrial&test_data=false&ptype=product&is_conversion=0&cat_id=119561&cat=One%20Of%20A%20Kind%20Rugs&prod_id=94965&prod_name=Revival%20Rugs%20Brown%20Striped%20Vlatko%20Vintage%20Area%20Rug&sku=57003800&type=pageview&lang=en-US&cdp_segments=63a1ede75322418784c619f1%3A63a21063733ffea05e1a29ee&url=https%3A%2F%2Fpacific.bloomreach.com%2Fhome%2Fproducts%2F94965___57003800&version=16.0"

# add-to-cart
curl -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=6413&cookie2=uid%3D1682069645648%3Av%3D16.0%3Ats%3D1708556649535%3Ahc%3D5&sid=undefined&ref=&tzo=480&rand=0.203880088337008&user_id=$USER_ID&domain_key=&tms=gtm&customer_profile=industrial&test_data=false&ptype=event&is_conversion=0&cat_id=119561&cat=One%20Of%20A%20Kind%20Rugs&prod_id=94965&prod_name=Revival%20Rugs%20Brown%20Striped%20Vlatko%20Vintage%20Area%20Rug&sku=57003800&group=cart&type=event&etype=click-add&e_prod_id=94965&e_sku=57003800&lang=en-US&cdp_segments=63a1ede75322418784c619f1%3A63a21063733ffea05e1a29ee&url=https%3A%2F%2Fpacific.bloomreach.com%2Fhome%2Fproducts%2F94965___57003800&version=16.0"

#convert
curl -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=6413&cookie2=uid%3D1682069645648%3Av%3D16.0%3Ats%3D1708556649535%3Ahc%3D5&sid=undefined&is_conversion=1&basket=!i94965%27s57003800%27nRevival%20Rugs%20Brown%20Striped%20Vlatko%20Vintage%20Area%20Rug%27p799.99%27q3&order_id=$ORDER_ID&basket_value=2399.9700000000003&ref=&tzo=480&rand=0.7681860139623937&title=Order%20Detail%20%7C%20Pacific%20Home&user_id=$USER_ID&domain_key=&tms=gtm&customer_profile=industrial&test_data=false&ptype=other&cat_id=119561&cat=One%20Of%20A%20Kind%20Rugs&prod_id=94965&prod_name=Revival%20Rugs%20Brown%20Striped%20Vlatko%20Vintage%20Area%20Rug&sku=57003800&type=pageview&lang=en-US&cdp_segments=63a1ede75322418784c619f1%3A63a21063733ffea05e1a29ee&url=https%3A%2F%2Fpacific.bloomreach.com%2Fhome%2Forders%2F1al4vont-503c-4545-a2d2-3fda6e0a5e8a&version=16.0"



