USER_ID=1112223334445
ORDER_ID=`uuidgen`
echo order_id = $ORDER_ID

# visit
curl -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=6413&cookie2=uid%3D4827027657798%3Av%3D16.0%3Ats%3D1707256326478%3Ahc%3D9%3Acdp_segments%3DNjNhMWVkZTc1MzIyNDE4Nzg0YzYxOWYxOjYzYTIxMDYzNzMzZmZlYTA1ZTFhMjllZQ&sid=undefined&ref=&tzo=480&rand=0.48950220635622377&title=Product%20Details%20%7C%20Pacific%20Home&user_id=$USER_ID&domain_key=&tms=gtm&test_data=false&ptype=product&prod_id=89093&prod_name=Multicolor%20Jaipur%20Floral%20Coir%20Doormat&sku=573805&is_conversion=0&type=pageview&lang=en-US&cdp_segments=63a1ede75322418784c619f1%3A63a21063733ffea05e1a29f0&url=https%3A%2F%2Fpacific.bloomreach.com%2Fhome%2Fproducts%2F89093___573805&version=16.0"

# add-to-cart
curl -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=6413&cookie2=uid%3D4827027657798%3Av%3D16.0%3Ats%3D1707256326478%3Ahc%3D9%3Acdp_segments%3DNjNhMWVkZTc1MzIyNDE4Nzg0YzYxOWYxOjYzYTIxMDYzNzMzZmZlYTA1ZTFhMjllZQ&sid=undefined&ref=&tzo=480&rand=0.6902137455403088&user_id=$USER_ID&domain_key=&tms=gtm&test_data=false&ptype=event&prod_id=89093&prod_name=Multicolor%20Jaipur%20Floral%20Coir%20Doormat&sku=573805&is_conversion=0&group=cart&type=event&etype=click-add&e_prod_id=89093&e_sku=573805&lang=en-US&cdp_segments=63a1ede75322418784c619f1%3A63a21063733ffea05e1a29f0&url=https%3A%2F%2Fpacific.bloomreach.com%2Fhome%2Fproducts%2F89093___573805&version=16.0"

# convert
curl -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=6413&cookie2=uid%3D4827027657798%3Av%3D16.0%3Ats%3D1707256326478%3Ahc%3D9%3Acdp_segments%3DNjNhMWVkZTc1MzIyNDE4Nzg0YzYxOWYxOjYzYTIxMDYzNzMzZmZlYTA1ZTFhMjllZQ&sid=undefined&is_conversion=1&basket=!i89093%27s573805%27nMulticolor%20Jaipur%20Floral%20Coir%20Doormat%27p16.99%27q6&order_id=$ORDER_ID&basket_value=101.94&ref=&tzo=480&rand=0.846920575624625&title=Order%20Detail%20%7C%20Pacific%20Home&user_id=$USER_ID&domain_key=&tms=gtm&customer_profile=industrial&test_data=false&ptype=other&prod_id=89093&prod_name=Multicolor%20Jaipur%20Floral%20Coir%20Doormat&sku=573805&type=pageview&lang=en-US&cdp_segments=63a1ede75322418784c619f1%3A63a21063733ffea05e1a29f0&url=https%3A%2F%2Fpacific.bloomreach.com%2Fhome%2Forders%2F0dazpowk-503c-4545-a2d2-3fda6e0a5e8a&version=16.0"

