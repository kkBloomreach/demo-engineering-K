import logging
from PIL import Image

def _convert_png_to_webp (local_image_path_png: str, local_image_path_webp: str) -> str:
    try:
        image = Image.open (local_image_path_png)
        image.save (local_image_path_webp, "webp")
    except Exception as e:
        logging.warning ('Exception in converting png to webp: %s' % local_image_path_png)
    return

if __name__ == '__main__':
    png_image_path = './output.png'
    webp_image_path = './output.webp'
    _convert_png_to_webp (png_image_path, webp_image_path)


