USER_ID=1112223334445
ORDER_ID=`uuidgen`
echo order_id = $ORDER_ID

# visit product
curl -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=6413&cookie2=uid%3D1682069645648%3Av%3D16.0%3Ats%3D1708556649535%3Ahc%3D5&sid=undefined&ref=&tzo=480&rand=0.4871884631482326&title=Product%20Details%20%7C%20Pacific%20Home&user_id=$USER_ID&domain_key=&tms=gtm&customer_profile=industrial&test_data=false&ptype=product&prod_id=88977&prod_name=Large%20Whitewash%20Wood%20Draped%20Bead%204%20Light%20Chandelier&sku=574247&is_conversion=0&cat_id=116869&cat=Pendants%20%26%20Chandeliers&type=pageview&lang=en-US&cdp_segments=63a1ede75322418784c619f1%3A63a21063733ffea05e1a29f0&url=https%3A%2F%2Fpacific.bloomreach.com%2Fhome%2Fproducts%2F88977___574247&version=16.0"

# add-to-cart
curl -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=6413&cookie2=uid%3D1682069645648%3Av%3D16.0%3Ats%3D1708556649535%3Ahc%3D5&sid=undefined&ref=&tzo=480&rand=0.7396931019070929&user_id=$USER_ID&domain_key=&tms=gtm&customer_profile=industrial&test_data=false&ptype=event&prod_id=88977&prod_name=Large%20Whitewash%20Wood%20Draped%20Bead%204%20Light%20Chandelier&sku=574247&is_conversion=0&cat_id=116869&cat=Pendants%20%26%20Chandeliers&group=cart&type=event&etype=click-add&e_prod_id=88977&e_sku=574247&lang=en-US&cdp_segments=63a1ede75322418784c619f1%3A63a21063733ffea05e1a29f0&url=https%3A%2F%2Fpacific.bloomreach.com%2Fhome%2Fproducts%2F88977___574247&version=16.0"

# convert
curl -o ./pix.gif "https://p.brsrvr.com/pix.gif?acct_id=6413&cookie2=uid%3D1682069645648%3Av%3D16.0%3Ats%3D1708556649535%3Ahc%3D5&sid=undefined&is_conversion=1&basket=!i88977%27s574247%27nLarge%20Whitewash%20Wood%20Draped%20Bead%204%20Light%20Chandelier%27p349.99%27q5&order_id=$ORDER_ID&basket_value=1749.95&ref=&tzo=480&rand=0.08987400277448243&title=Order%20Detail%20%7C%20Pacific%20Home&user_id=$USER_ID&domain_key=&tms=gtm&customer_profile=industrial&test_data=false&ptype=other&prod_id=88977&prod_name=Large%20Whitewash%20Wood%20Draped%20Bead%204%20Light%20Chandelier&sku=574247&cat_id=116869&cat=Pendants%20%26%20Chandeliers&type=pageview&lang=en-US&cdp_segments=63a1ede75322418784c619f1%3A63a21063733ffea05e1a29ee&url=https%3A%2F%2Fpacific.bloomreach.com%2Fhome%2Forders%2Fx9a0fydk-503c-4545-a2d2-3fda6e0a5e8a&version=16.0"


