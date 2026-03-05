import type { Area } from 'react-easy-crop';

const MAX_OUTPUT_SIZE = 400;
const JPEG_QUALITY = 0.8;

/**
 * Creates an image element from a source URL
 */
const createImage = (url: string): Promise<HTMLImageElement> =>
  new Promise((resolve, reject) => {
    const image = new Image();
    image.addEventListener('load', () => resolve(image));
    image.addEventListener('error', () => reject(new Error('Failed to load image')));
    image.src = url;
  });

/**
 * Extracts the cropped region from an image and returns it as a base64 JPEG.
 * The output is scaled to fit within MAX_OUTPUT_SIZE while maintaining aspect ratio.
 */
export async function getCroppedImg(
  imageSrc: string,
  pixelCrop: Area
): Promise<string> {
  const image = await createImage(imageSrc);
  const canvas = document.createElement('canvas');
  const ctx = canvas.getContext('2d');

  if (!ctx) {
    throw new Error('Failed to get canvas context');
  }

  // Calculate output size (maintain square for avatar, max 400px)
  const outputSize = Math.min(pixelCrop.width, pixelCrop.height, MAX_OUTPUT_SIZE);

  canvas.width = outputSize;
  canvas.height = outputSize;

  // Draw the cropped region scaled to fit the output canvas
  ctx.drawImage(
    image,
    pixelCrop.x,
    pixelCrop.y,
    pixelCrop.width,
    pixelCrop.height,
    0,
    0,
    outputSize,
    outputSize
  );

  // Return as base64 JPEG
  return canvas.toDataURL('image/jpeg', JPEG_QUALITY);
}
