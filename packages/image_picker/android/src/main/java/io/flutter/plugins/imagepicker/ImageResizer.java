// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.imagepicker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import io.flutter.plugins.imagepicker.gif_compression.GifCompressionAsync;
import io.flutter.plugins.imagepicker.gif_compression.gif_decoder.GifDecoder;

import io.flutter.plugins.imagepicker.gif_compression.gif_encoder.AnimatedGifEncoder;
import io.flutter.plugins.imagepicker.util.AppUtils;

class ImageResizer {
    private final File externalFilesDirectory;
    private final ExifDataCopier exifDataCopier;

    ImageResizer(File externalFilesDirectory, ExifDataCopier exifDataCopier) {
        this.externalFilesDirectory = externalFilesDirectory;
        this.exifDataCopier = exifDataCopier;
    }

    /**
     * If necessary, resizes the image located in imagePath and then returns the path for the scaled
     * image.
     *
     * <p>If no resizing is needed, returns the path for the original image.
     */
    String resizeImageIfNeeded(
            String imagePath, Double maxWidth, Double maxHeight, int imageQuality) {
        boolean shouldScale =
                maxWidth != null || maxHeight != null || (imageQuality > -1 && imageQuality < 101);

        if (!shouldScale) {
            return imagePath;
        }

        try {
            File scaledImage = resizedImage(imagePath, maxWidth, maxHeight, imageQuality);
            exifDataCopier.copyExif(imagePath, scaledImage.getPath());

            return scaledImage.getPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private File resizedImage(String path, Double maxWidth, Double maxHeight, int imageQuality)
            throws IOException {
        Bitmap bmp = BitmapFactory.decodeFile(path);
        double originalWidth = bmp.getWidth() * 1.0;
        double originalHeight = bmp.getHeight() * 1.0;

        if (imageQuality < 0 || imageQuality > 100) {
            imageQuality = 100;
        }

        boolean hasMaxWidth = maxWidth != null;
        boolean hasMaxHeight = maxHeight != null;

        Double width = hasMaxWidth ? Math.min(originalWidth, maxWidth) : originalWidth;
        Double height = hasMaxHeight ? Math.min(originalHeight, maxHeight) : originalHeight;

        boolean shouldDownscaleWidth = hasMaxWidth && maxWidth < originalWidth;
        boolean shouldDownscaleHeight = hasMaxHeight && maxHeight < originalHeight;
        boolean shouldDownscale = shouldDownscaleWidth || shouldDownscaleHeight;

        if (shouldDownscale) {
            double downscaledWidth = (height / originalHeight) * originalWidth;
            double downscaledHeight = (width / originalWidth) * originalHeight;

            if (width < height) {
                if (!hasMaxWidth) {
                    width = downscaledWidth;
                } else {
                    height = downscaledHeight;
                }
            } else if (height < width) {
                if (!hasMaxHeight) {
                    height = downscaledHeight;
                } else {
                    width = downscaledWidth;
                }
            } else {
                if (originalWidth < originalHeight) {
                    width = downscaledWidth;
                } else if (originalHeight < originalWidth) {
                    height = downscaledHeight;
                }
            }
        }

        ////

        String fileExtension = "";
        try {
            if (path != null && path.lastIndexOf(".") != -1) {
                fileExtension = path.substring(path.lastIndexOf(".") + 1);
            }
        } catch (Exception e) {
            fileExtension = "";
        }

        if (fileExtension.isEmpty()) {
            //default extension for matches the previous behavior of the plugin
            fileExtension = "jpg";
        }

        if (fileExtension.equalsIgnoreCase("gif")) {

            String compressedGifPath = null;
            try {
                AppUtils.showProgressDialog("Getting GIF");
                compressedGifPath = new GifCompressionAsync(getFileData(path), height.intValue(), width.intValue(), imageQuality, externalFilesDirectory).execute().get();
                AppUtils.cancelProgressDialog();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            String[] pathParts = path.split("/");
            String imageName = pathParts[pathParts.length - 1];

            File imageFile = new File(externalFilesDirectory, "/scaled_" + imageName);
            FileOutputStream fileOutput = new FileOutputStream(imageFile);

            if (compressedGifPath == null || compressedGifPath.equalsIgnoreCase("")) {
                Log.d("compressedGifPath", "resizedImage: compressedGifPath is empty or null");
                Bitmap scaledBmp = Bitmap.createScaledBitmap(bmp, width.intValue(), height.intValue(), false);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                boolean saveAsPNG = bmp.hasAlpha();
                if (saveAsPNG) {
                    Log.d(
                            "ImageResizer",
                            "image_picker: compressing is not supported for type PNG. Returning the image with original quality");
                }
                scaledBmp.compress(
                        saveAsPNG ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG,
                        100,
                        outputStream);

                fileOutput.write(outputStream.toByteArray());
            } else {
                fileOutput.write(getFileData(compressedGifPath));
            }

            fileOutput.close();
            return imageFile;

        } else {
            Bitmap scaledBmp = Bitmap.createScaledBitmap(bmp, width.intValue(), height.intValue(), false);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            boolean saveAsPNG = bmp.hasAlpha();
            if (saveAsPNG) {
                Log.d(
                        "ImageResizer",
                        "image_picker: compressing is not supported for type PNG. Returning the image with original quality");
            }
            scaledBmp.compress(
                    saveAsPNG ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG,
                    imageQuality,
                    outputStream);

            String[] pathParts = path.split("/");
            String imageName = pathParts[pathParts.length - 1];

            File imageFile = new File(externalFilesDirectory, "/scaled_" + imageName);
            FileOutputStream fileOutput = new FileOutputStream(imageFile);
            fileOutput.write(outputStream.toByteArray());
            fileOutput.close();
            return imageFile;

        }
    }

    /**
     * Get byte[] from file path
     *
     * @param path
     * @return
     */
    private byte[] getFileData(String path) {
        File file = new File(path);
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }


    private static byte[] readBytesFromFile(String filePath) {
        FileInputStream fileInputStream = null;
        byte[] bytesArray = null;
        try {
            File file = new File(filePath);
            bytesArray = new byte[(int) file.length()];

            //read file into bytes[]
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(bytesArray);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return bytesArray;
    }
}