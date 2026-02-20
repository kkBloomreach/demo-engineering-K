import logging
from PIL import Image

def _convert_webp_to_png (local_image_path_webp: str, local_image_path_png: str) -> str:
    try:
        image = Image.open (local_image_path_webp)
        image.save (local_image_path_png, "png")
    except Exception as e:
        logging.warning ('Exception in converting webp to png: %s' % local_image_path_webp)
    return

if __name__ == '__main__':
    webp_image_path = './output.webp'
    png_image_path = './output.png'
    _convert_webp_to_png (webp_image_path, png_image_path)


