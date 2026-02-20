for filepath in $(ls ./data/images/*.png);
do
    echo $filepath
    filename=`basename $filepath .png`
    echo $filename 
    sh ./data/images/aws_image_upload.sh $filename
done

