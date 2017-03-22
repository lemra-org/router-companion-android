package com.nextstagesearch.design;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v7.graphics.Palette;
import com.squareup.picasso.Transformation;

/**
 * Created by oded on 9/15/15.
 * Watermark Transformation for the Picasso image loading library (https://github.com/square/picasso).
 * The transformation will add the text you provide in the constructor to the image.
 * This was created to be implemented in http://wheredatapp.com, android's greatest search engine.
 */
public class WatermarkTransformation implements Transformation {

  private static final int PADDING = 8;
  private String waterMark;

  public WatermarkTransformation(String waterMark) {
    this.waterMark = waterMark;
  }

  @Override public Bitmap transform(Bitmap source) {
    //choose the color of the text based on the color contents of the image
    Palette palette = Palette.generate(source);

    Bitmap workingBitmap = Bitmap.createBitmap(source);
    Bitmap mutableBitmap = workingBitmap.copy(Bitmap.Config.ARGB_8888, true);
    Canvas canvas = new Canvas(mutableBitmap);

    Paint paint2 = new Paint();
    paint2.setColor(palette.getVibrantColor(0xdd03a9f4));
    paint2.setTextSize(24);
    paint2.setTextAlign(Paint.Align.RIGHT);
    paint2.setAntiAlias(true);
    Rect textBounds = new Rect();
    paint2.getTextBounds(waterMark, 0, waterMark.length(), textBounds);
    int x = source.getWidth() - PADDING;
    int y = source.getHeight() - PADDING;

    canvas.drawText(waterMark, x, y, paint2);
    source.recycle();

    return mutableBitmap;
  }

  @Override public String key() {
    return "WaterMarkTransformation-" + waterMark;
  }
}