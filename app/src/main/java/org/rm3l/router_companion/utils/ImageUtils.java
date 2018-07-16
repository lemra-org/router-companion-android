package org.rm3l.router_companion.utils;

import static org.rm3l.router_companion.utils.Utils.guessAppropriateEncoding;
import static org.rm3l.router_companion.utils.Utils.reportException;

import android.app.Activity;
import android.app.Notification;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RemoteViews;
import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.crashlytics.android.Crashlytics;
import com.google.common.base.Strings;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;
import com.squareup.picasso.Transformation;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.exceptions.DDWRTCompanionException;
import org.rm3l.router_companion.main.DDWRTMainActivity;
import org.rm3l.router_companion.resources.conn.Router;

/**
 * Created by rm3l on 07/11/15.
 */
public final class ImageUtils {

    public static class DownloadImageException extends DDWRTCompanionException {

        public DownloadImageException() {
        }

        public DownloadImageException(@Nullable String detailMessage) {
            super(detailMessage);
        }

        public DownloadImageException(@Nullable String detailMessage, @Nullable Throwable throwable) {
            super(detailMessage, throwable);
        }

        public DownloadImageException(@Nullable Throwable throwable) {
            super(throwable);
        }
    }

    public static class MissingRouterModelImageException extends DownloadImageException {

        public MissingRouterModelImageException(@Nullable String routerModel) {
            super(routerModel);
        }
    }

    public static class SuccessfulRouterModelImageDownloadNotice extends DownloadImageException {

        public SuccessfulRouterModelImageDownloadNotice(@Nullable String routerModel) {
            super(routerModel);
        }
    }

    private static final String TAG = ImageUtils.class.getSimpleName();

    private static final int WHITE = 0xFFFFFFFF;

    private static final int BLACK = 0xFF000000;

    private static TextDrawable.IBuilder TEXTDRAWABLE_BUILDER =
            TextDrawable.builder().beginConfig().withBorder(4).toUpperCase().endConfig().roundRect(15);

    public static void downloadImageFromUrl(@Nullable Context context, @Nullable final Uri url,
            @Nullable final ImageView imageView, @Nullable final Integer placeHolderDrawable,
            @Nullable final Integer errorPlaceHolderDrawable, @Nullable final Callback callback) {
        downloadImageFromUrl(context, url, imageView, null, placeHolderDrawable,
                errorPlaceHolderDrawable, callback);
    }

    public static void downloadImageFromUrl(@Nullable Context context, @Nullable final String url,
            @Nullable final ImageView imageView, @Nullable final Integer placeHolderDrawable,
            @Nullable final Integer errorPlaceHolderDrawable, @Nullable final Callback callback) {
        downloadImageFromUrl(context, url != null ? Uri.parse(url) : null, imageView, null, placeHolderDrawable,
                errorPlaceHolderDrawable, callback);
    }

    public static void downloadImageFromUrl(@Nullable Context context, @Nullable final Uri url,
            @Nullable final ImageView imageView, @Nullable final List<Transformation> transformations,
            @Nullable final Integer placeHolderDrawable, @Nullable final Integer errorPlaceHolderDrawable,
            @Nullable final Callback callback) {

        try {
            if (context == null || imageView == null) {
                return;
            }
            final Picasso picasso = new Picasso.Builder(context).build();
            picasso.setIndicatorsEnabled(false);
            picasso.setLoggingEnabled(BuildConfig.DEBUG);
            final RequestCreator requestCreator = picasso.load(url);

            requestCreator.placeholder(
                    placeHolderDrawable != null ? placeHolderDrawable : R.drawable.progress_animation);

            if (transformations != null) {
                requestCreator.transform(transformations);
            }

            if (errorPlaceHolderDrawable != null) {
                requestCreator.error(errorPlaceHolderDrawable);
            }

            requestCreator.into(imageView, callback);
        } catch (final Exception e) {
            e.printStackTrace();
            reportException(null, new DownloadImageException(e));
        }
    }

    public static void downloadImageFromUrl(@Nullable Context context, @Nullable final Uri url,
            @Nullable final Target target, @Nullable final List<Transformation> transformations,
            @Nullable final Integer placeHolderDrawable,
            @Nullable final Integer errorPlaceHolderDrawable) {

        try {
            if (context == null || target == null) {
                return;
            }
            final Picasso picasso = new Picasso.Builder(context).build();
            picasso.setIndicatorsEnabled(false);
            picasso.setLoggingEnabled(BuildConfig.DEBUG);
            final RequestCreator requestCreator = picasso.load(url);

            requestCreator.placeholder(
                    placeHolderDrawable != null ? placeHolderDrawable : R.drawable.progress_animation);

            if (transformations != null) {
                requestCreator.transform(transformations);
            }

            if (errorPlaceHolderDrawable != null) {
                requestCreator.error(errorPlaceHolderDrawable);
            }

            requestCreator.into(target);
        } catch (final Exception e) {
            e.printStackTrace();
            reportException(null, new DownloadImageException(e));
        }
    }

    /**
     * get uri to drawable or any other resource type if u wish
     *
     * @param context    - context
     * @param drawableId - drawable res id
     * @return - uri
     */
    @Nullable
    public static Uri drawableToUri(@Nullable Context context,
            @DrawableRes final int drawableId) {
        if (context == null) {
            return null;
        }
        final Resources resources = context.getResources();
        return Uri.parse(
                String.format("%s://%s/%s/%s",
                        ContentResolver.SCHEME_ANDROID_RESOURCE,
                        resources.getResourcePackageName(drawableId),
                        resources.getResourceTypeName(drawableId),
                        resources.getResourceEntryName(drawableId)));
    }

    @Nullable
    public static Bitmap encodeAsBitmap(String contents, BarcodeFormat format, int imgWidth,
            int imgHeight) throws WriterException {
        if (contents == null) {
            return null;
        }
        Map<EncodeHintType, Object> hints = null;
        final String encoding = guessAppropriateEncoding(contents);
        if (encoding != null) {
            hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, encoding);
        }
        final MultiFormatWriter writer = new MultiFormatWriter();
        final BitMatrix result;
        try {
            result = writer.encode(contents, format, imgWidth, imgHeight, hints);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        }
        final int width = result.getWidth();
        final int height = result.getHeight();
        final int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            final int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }

        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

        return bitmap;
    }

    @Nullable
    public static TextDrawable getTextDrawable(@Nullable final String key,
            @NonNull final ColorGenerator generator) {

        if (TextUtils.isEmpty(key)) {
            return null;
        }
        return TEXTDRAWABLE_BUILDER.build(key.substring(0, 1), generator.getColor(key));
    }

    @Nullable
    public static TextDrawable getTextDrawable(@Nullable final String key) {
        return getTextDrawable(key, ColorGenerator.MATERIAL);
    }

    public static void setTextDrawable(@Nullable final ImageView imageView,
            @Nullable final String key, boolean updateVisibilityIfNeeded) {
        setTextDrawable(imageView, key, ColorGenerator.MATERIAL, updateVisibilityIfNeeded);
    }

    public static void setTextDrawable(@Nullable final ImageView imageView,
            @Nullable final String key, @NonNull final ColorGenerator generator,
            boolean updateVisibilityIfNeeded) {
        if (imageView == null) {
            return;
        }
        final TextDrawable textDrawable = getTextDrawable(key, generator);
        if (textDrawable == null) {
            if (updateVisibilityIfNeeded) {
                imageView.setVisibility(View.GONE);
            }
        } else {
            imageView.setImageDrawable(textDrawable);
            if (updateVisibilityIfNeeded) {
                imageView.setVisibility(View.VISIBLE);
            }
        }
    }

    public static void updateNotificationIconWithRouterAvatar(@NonNull final Context mCtx,
            @Nullable Router router, final int notifyID, final Notification notification) {

        try {
            // Get RemoteView and id's needed
            final RemoteViews contentView = notification.contentView;
            final int iconId = android.R.id.icon;

            final Picasso picasso = new Picasso.Builder(mCtx).build();
            picasso.setIndicatorsEnabled(false);
            picasso.setLoggingEnabled(BuildConfig.DEBUG);
            final RequestCreator requestCreator = picasso.load(
                    Router.getRouterAvatarUrl(mCtx, router, DDWRTMainActivity.opts));
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        requestCreator.into(contentView, iconId, notifyID, notification);
                    } catch (final Exception ignored) {
                        ReportingUtils.reportException(mCtx, ignored);
                    }
                }
            };
            if (mCtx instanceof Activity) {
                ((Activity) mCtx).runOnUiThread(runnable);
            } else {
                runnable.run();
            }
        } catch (final Exception e) {
            e.printStackTrace();
            Utils.reportException(mCtx, e);
        }
    }

    private ImageUtils() {
    }

    static void downloadImageForRouter(@Nullable final Context context,
            @NonNull final Router router, @Nullable final ImageView imageView,
            @Nullable final List<Transformation> transformations, @Nullable final Integer placeHolderRes,
            @Nullable final Integer errorPlaceHolderRes, @Nullable final String[] opts) {

        try {
            final String routerModel = Router.getRouterModel(context, router);
            final String routerModelNormalized = Strings.nullToEmpty(routerModel).toLowerCase()
                    .replaceAll("\\s+", "");
            final Uri url = Router.getRouterAvatarUrl(context, router, opts);

            downloadImageFromUrl(context, url, imageView, transformations,
                    placeHolderRes,
                    errorPlaceHolderRes, new Callback() {
                        @Override
                        public void onError(Exception e) {
                            Crashlytics.log(Log.DEBUG, TAG, "onError: " + url);
                            Utils.reportException(context, e);
                            reportException(null, new MissingRouterModelImageException(
                                    routerModel + " (" + routerModelNormalized + ")"));
                            //Report event
                            final Map<String, Object> eventMap = new HashMap<>();
                            eventMap.put("Status", "Error");
                            eventMap.put("Model", routerModel);
                            ReportingUtils.reportEvent(ReportingUtils.EVENT_IMAGE_DOWNLOAD, eventMap);
                        }

                        @Override
                        public void onSuccess() {
                            //Great!
                            Crashlytics.log(Log.DEBUG, TAG, "onSuccess: " + url);

                            //Report event
                            final Map<String, Object> eventMap = new HashMap<>();
                            eventMap.put("Status", "Success");
                            eventMap.put("Model", routerModel);
                            ReportingUtils.reportEvent(ReportingUtils.EVENT_IMAGE_DOWNLOAD, eventMap);
                        }
                    });
        } catch (final Exception e) {
            e.printStackTrace();
            reportException(null, new DownloadImageException(e));
        }
    }
}