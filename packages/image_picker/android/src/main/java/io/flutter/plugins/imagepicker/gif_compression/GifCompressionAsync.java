package io.flutter.plugins.imagepicker.gif_compression;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.flutter.plugins.imagepicker.gif_compression.gif_decoder.GifDecoder;
import io.flutter.plugins.imagepicker.gif_compression.gif_encoder.AnimatedGifEncoder;
import io.flutter.plugins.imagepicker.util.AppUtils;

/**
 * Gif compression async to perform compression task in background
 * to avoid ANR
 */
public class GifCompressionAsync extends AsyncTask<Void, Void, String> {

    private byte[] fileData;
    private int height = 0, width = 0, imageQuality = 0;
    private File externalFilesDirectory;
    private String convertedGifPath = "";
    private List<Bitmap> bitmaps = new ArrayList<>();
    private GifDecoder gifDecoder = new GifDecoder();
    private Bitmap bmpImg;
    private AnimatedGifEncoder animatedGifEncoder = new AnimatedGifEncoder();

    public GifCompressionAsync(byte[] fileData, int height, int width, int imageQuality, File externalFilesDirectory) {
        this.fileData = fileData;
        this.height = height;
        this.width = width;
        this.imageQuality = imageQuality;
        this.externalFilesDirectory = externalFilesDirectory;
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();
//        AppUtils.showProgressDialog("Getting GIF");
    }

    @Override
    protected String doInBackground(Void... voids) {
        try {
            gifDecoder.read(fileData);
            int avgDelay = 0;
            Bitmap tempBitmap;
            int frames = 0;
            while (frames < gifDecoder.getFrameCount()) {
                gifDecoder.advance();

                tempBitmap = gifDecoder.getNextFrame();
                bmpImg = Bitmap.createScaledBitmap(tempBitmap, width, height, false);
                boolean isPng = tempBitmap.hasAlpha();
                if (imageQuality > 0 && imageQuality < 100)
                    bmpImg.compress(isPng ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, imageQuality, new ByteArrayOutputStream());
                bitmaps.add(bmpImg);
                avgDelay += gifDecoder.getDelay(frames);
                frames++;
            }

            avgDelay = avgDelay / gifDecoder.getFrameCount();

            convertedGifPath = externalFilesDirectory + "/scaled_test";
            animatedGifEncoder.start(convertedGifPath);
            animatedGifEncoder.setDelay(avgDelay);
            animatedGifEncoder.setRepeat(0);
            animatedGifEncoder.setTransparent(Color.WHITE);
            for (Bitmap b : bitmaps) {
                animatedGifEncoder.addFrame(b);
            }


            animatedGifEncoder.finish();
        } catch (Exception e) {
            convertedGifPath = "";
        }
        return convertedGifPath;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
//        AppUtils.cancelProgressDialog();
    }
}
