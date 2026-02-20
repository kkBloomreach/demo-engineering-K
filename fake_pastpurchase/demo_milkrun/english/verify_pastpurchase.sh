USER_ID=1112223334445
BR_UID=4827027657798

# MILKRUN
# past purchase, user_id = 1112223334445
echo curl -v -X GET "http://pathways.dxpapi.com/api/v2/widgets/personalized/2jn40gg9?account_id=7526&domain_key=demo_milkrun&_br_uid_2=uid%3D$BR_UID%3Av%3D11.8%3Ats%3D1585222161176%3Ahc%3D30&url=demo.milkrun.com/home&query=*&user_id=$USER_ID&ref_url=demo.milkrun.com&request_id=123456789" -H "accept: application/json" -H "auth-key: fefzq66ovo3ooro9"
echo "----"

curl -v -X GET "http://pathways.dxpapi.com/api/v2/widgets/personalized/2jn40gg9?account_id=7526&domain_key=demo_milkrun&_br_uid_2=uid%3D$BR_UID%3Av%3D11.8%3Ats%3D1585222161176%3Ahc%3D30&url=demo.milkrun.com/home&query=*&user_id=$USER_ID&ref_url=demo.milkrun.com&request_id=123456789" -H "accept: application/json" -H "auth-key: fefzq66ovo3ooro9"


