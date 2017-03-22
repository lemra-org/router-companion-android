package org.rm3l.router_companion.utils;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Created by rm3l on 21/12/2016.
 */
public class FontUtils {

  private static final Cache<String, Typeface> FONTS_CACHE =
      CacheBuilder.newBuilder().maximumSize(5).weakValues().build();

  public static Typeface getTypeface(@NonNull final Context context, @NonNull final String fontName,
      @Nullable final FontType fontType) {
    Typeface typeface = FONTS_CACHE.getIfPresent(fontName);
    if (typeface == null) {
      typeface = Typeface.createFromAsset(context.getAssets(),
          "fonts/" + fontName + (fontType == null ? "" : fontType.extension));
      FONTS_CACHE.put(fontName, typeface);
    }
    return typeface;
  }

  public enum FontType {
    TTF(".ttf"), OTF(".otf");

    private final String extension;

    FontType(String extension) {
      this.extension = extension;
    }
  }
}
