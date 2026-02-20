USER_ID=1112223334445
ORDER_ID=`uuidgen`
echo order_id = $ORDER_ID

# visit
curl -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=6413&cookie2=uid%3D4827027657798%3Av%3D16.0%3Ats%3D1707256326478%3Ahc%3D9%3Acdp_segments%3DNjNhMWVkZTc1MzIyNDE4Nzg0YzYxOWYxOjYzYTIxMDYzNzMzZmZlYTA1ZTFhMjllZQ&sid=undefined&ref=&tzo=480&rand=0.8955984068497072&title=Product%20Details%20%7C%20Pacific%20Home&user_id=$USER_ID&domain_key=&tms=gtm&test_data=false&ptype=product&prod_id=84793&prod_name=Metal%20Succulent%20Plant%20Wall%20Decor&sku=566516&is_conversion=0&type=pageview&lang=en-US&cdp_segments=63a1ede75322418784c619f1%3A63a21063733ffea05e1a29f0&url=https%3A%2F%2Fpacific.bloomreach.com%2Fhome%2Fproducts%2F84793___566516&version=16.0"

# add to cart
curl -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=6413&cookie2=uid%3D4827027657798%3Av%3D16.0%3Ats%3D1707256326478%3Ahc%3D9%3Acdp_segments%3DNjNhMWVkZTc1MzIyNDE4Nzg0YzYxOWYxOjYzYTIxMDYzNzMzZmZlYTA1ZTFhMjllZQ&sid=undefined&ref=&tzo=480&rand=0.18322287953589766&user_id=$USER_ID&domain_key=&tms=gtm&test_data=false&ptype=event&prod_id=84793&prod_name=Metal%20Succulent%20Plant%20Wall%20Decor&sku=566516&is_conversion=0&group=cart&type=event&etype=click-add&e_prod_id=84793&e_sku=566516&lang=en-US&cdp_segments=63a1ede75322418784c619f1%3A63a21063733ffea05e1a29f0&url=https%3A%2F%2Fpacific.bloomreach.com%2Fhome%2Fproducts%2F84793___566516&version=16.0"

# conversion
curl -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=6413&cookie2=uid%3D4827027657798%3Av%3D16.0%3Ats%3D1707256326478%3Ahc%3D9%3Acdp_segments%3DNjNhMWVkZTc1MzIyNDE4Nzg0YzYxOWYxOjYzYTIxMDYzNzMzZmZlYTA1ZTFhMjllZQ&sid=undefined&is_conversion=1&basket=!i84793%27s566516%27nMetal%20Succulent%20Plant%20Wall%20Decor%27p59.99%27q2&order_id=$ORDER_ID&basket_value=119.98&ref=&tzo=480&rand=0.2616338190922427&title=Order%20Detail%20%7C%20Pacific%20Home&user_id=$USER_ID&domain_key=&tms=gtm&customer_profile=industrial&test_data=false&ptype=other&prod_id=84793&prod_name=Metal%20Succulent%20Plant%20Wall%20Decor&sku=566516&type=pageview&lang=en-US&cdp_segments=63a1ede75322418784c619f1%3A63a21063733ffea05e1a29f0&url=https%3A%2F%2Fpacific.bloomreach.com%2Fhome%2Forders%2Fm7cri6s9-503c-4545-a2d2-3fda6e0a5e8a&version=16.0"

