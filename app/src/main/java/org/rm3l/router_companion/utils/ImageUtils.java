package org.rm3l.router_companion.utils;

import android.app.Activity;
import android.app.Notification;
import android.content.Context;
import android.graphics.Bitmap;
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
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
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

import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.exceptions.DDWRTCompanionException;
import org.rm3l.router_companion.main.DDWRTMainActivity;
import org.rm3l.router_companion.resources.conn.Router;

import java.net.URLEncoder;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.rm3l.router_companion.utils.Utils.guessAppropriateEncoding;
import static org.rm3l.router_companion.utils.Utils.reportException;

/**
 * Created by rm3l on 07/11/15.
 */
public final class ImageUtils {

    private static final String TAG = ImageUtils.class.getSimpleName();

    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;

    private static TextDrawable.IBuilder TEXTDRAWABLE_BUILDER = TextDrawable.builder()
            .beginConfig()
            .withBorder(4)
            .toUpperCase()
            .endConfig()
            .roundRect(15);

    private ImageUtils() {}

    public static void downloadImageForRouter(@Nullable Context context,
                                              @NonNull final String routerModel,
                                              @Nullable final ImageView imageView) {
        downloadImageForRouter(context,
                routerModel,
                imageView,
                null,
                R.drawable.router_picker_background);
    }

    public static void downloadImageForRouter(@Nullable Context context,
                                              @NonNull final String routerModel,
                                              @Nullable final ImageView imageView,
                                              @Nullable final Integer placeHolderRes,
                                              @Nullable final Integer errorPlaceHolderRes) {
        downloadImageForRouter(context,
                routerModel,
                imageView,
                placeHolderRes,
                errorPlaceHolderRes,
                null);
    }

    public static void downloadImageForRouter(@Nullable Context context,
                                              @NonNull final String routerModel,
                                              @Nullable final ImageView imageView,
                                              @Nullable final Integer placeHolderRes,
                                              @Nullable final Integer errorPlaceHolderRes,
                                              @Nullable final String[] opts) {
        downloadImageForRouter(context,
                routerModel,
                imageView,
                null,
                placeHolderRes,
                errorPlaceHolderRes,
                opts);
    }

    public static void downloadImageForRouter(@Nullable Context context,
                                              @NonNull final String routerModel,
                                              @Nullable final ImageView imageView,
                                              @Nullable final List<Transformation> transformations,
                                              @Nullable final Integer placeHolderRes,
                                              @Nullable final Integer errorPlaceHolderRes,
                                              @Nullable final String[] opts) {

        try {
            final String routerModelNormalized = routerModel.toLowerCase().replaceAll("\\s+", "");
            final String url = Router.getRouterAvatarUrl(routerModel, opts);

            downloadImageFromUrl(context,
                    url,
                    imageView,
                    transformations,
                    placeHolderRes != null ? placeHolderRes : null,
                    errorPlaceHolderRes != null ? errorPlaceHolderRes : null,
                    new Callback() {
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

                        @Override
                        public void onError() {
                            Crashlytics.log(Log.DEBUG, TAG, "onError: " + url);
                            reportException(null, new MissingRouterModelImageException(routerModel + " (" +
                                    routerModelNormalized + ")"));
                            //Report event
                            final Map<String, Object> eventMap = new HashMap<>();
                            eventMap.put("Status", "Error");
                            eventMap.put("Model", routerModel);
                            ReportingUtils.reportEvent(ReportingUtils.EVENT_IMAGE_DOWNLOAD, eventMap);
                        }
                    });

        } catch (final Exception e) {
            e.printStackTrace();
            reportException(null, new DownloadImageException(e));
        }
    }

    public static void downloadImageForRouter(@Nullable Context context,
                                              @NonNull final String routerModel,
                                              @Nullable final ImageView imageView,
                                              @Nullable final Integer placeHolderRes,
                                              @Nullable final Integer errorPlaceHolderRes,
                                              @Nullable final String[] opts,
                                              @Nullable final Callback callback) {
        try {
            final String routerModelNormalized = routerModel.toLowerCase().replaceAll("\\s+", "");
            final String url = String.format("%s/%s/%s.jpg", RouterCompanionAppConstants.IMAGE_CDN_URL_PREFIX,
                    Joiner
                            .on(",")
                            .skipNulls().join(opts != null ?
                            opts : RouterCompanionAppConstants.CLOUDINARY_OPTS),
                    URLEncoder.encode(routerModelNormalized, Charsets.UTF_8.name()));

            downloadImageFromUrl(context,
                    url,
                    imageView,
                    placeHolderRes != null ? placeHolderRes : null,
                    errorPlaceHolderRes != null ? errorPlaceHolderRes : null,
                    new Callback() {
                        @Override
                        public void onSuccess() {
                            //Great!
                            reportException(null, new SuccessfulRouterModelImageDownloadNotice(
                                    routerModel + " (" + routerModelNormalized + ")"
                            ));
                            if (callback != null) {
                                callback.onSuccess();
                            }
                        }

                        @Override
                        public void onError() {
                            Crashlytics.log(Log.DEBUG, TAG, "onError: " + url);
                            reportException(null, new MissingRouterModelImageException(routerModel + " (" +
                                    routerModelNormalized + ")"));
                            if (callback != null) {
                                callback.onError();
                            }
                        }
                    });

        } catch (final Exception e) {
            e.printStackTrace();
            reportException(null, new DownloadImageException(e));
        }
    }

    public static void downloadImageFromUrl(@Nullable Context context, @NonNull final String url,
                                            @Nullable final ImageView imageView,
                                            @Nullable final Integer placeHolderDrawable,
                                            @Nullable final Integer errorPlaceHolderDrawable,
                                            @Nullable final Callback callback) {
        downloadImageFromUrl(context,
                url,
                imageView,
                null,
                placeHolderDrawable,
                errorPlaceHolderDrawable,
                callback);
    }

    public static void downloadImageFromUrl(@Nullable Context context, @NonNull final String url,
                                            @Nullable final ImageView imageView,
                                            @Nullable final List<Transformation> transformations,
                                            @Nullable final Integer placeHolderDrawable,
                                            @Nullable final Integer errorPlaceHolderDrawable,
                                            @Nullable final Callback callback) {

        try {
            if (context == null || imageView == null) {
                return;
            }
            final Picasso picasso = Picasso.with(context);
            picasso.setIndicatorsEnabled(false);
            picasso.setLoggingEnabled(BuildConfig.DEBUG);
            final RequestCreator requestCreator = picasso.load(url);

            requestCreator.placeholder(placeHolderDrawable != null ?
                    placeHolderDrawable : R.drawable.progress_animation);

            if (transformations !=null) {
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
    
    public static void downloadImageFromUrl(@Nullable Context context, @NonNull final String url,
                                            @Nullable final Target target,
                                            @Nullable final List<Transformation> transformations,
                                            @Nullable final Integer placeHolderDrawable,
                                            @Nullable final Integer errorPlaceHolderDrawable) {

        try {
            if (context == null || target == null) {
                return;
            }
            final Picasso picasso = Picasso.with(context);
            picasso.setIndicatorsEnabled(false);
            picasso.setLoggingEnabled(BuildConfig.DEBUG);
            final RequestCreator requestCreator = picasso.load(url);

            requestCreator.placeholder(placeHolderDrawable != null ?
                    placeHolderDrawable : R.drawable.progress_animation);

            if (transformations !=null) {
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

    public static void updateNotificationIconWithRouterAvatar(@NonNull Context mCtx,
                                                              @Nullable Router router,
                                                              final int notifyID,
                                                              final Notification notification) {

        try {
            // Get RemoteView and id's needed
            final RemoteViews contentView = notification.contentView;
            final int iconId = android.R.id.icon;

            final Picasso picasso = Picasso.with(mCtx);
            picasso.setIndicatorsEnabled(false);
            picasso.setLoggingEnabled(BuildConfig.DEBUG);
            final RequestCreator requestCreator = picasso.load(
                    Router.getRouterAvatarUrl(
                            Router.getRouterModel(mCtx, router),
                            DDWRTMainActivity.opts)
            );
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        requestCreator
                                .into(contentView,
                                        iconId,
                                        notifyID,
                                        notification);
                    } catch (final Exception ignored) {
                        ignored.printStackTrace();
                        Crashlytics.logException(ignored);
                    }
                }
            };
            if (mCtx instanceof Activity) {
                ((Activity) mCtx)
                        .runOnUiThread(runnable);
            } else {
                runnable.run();
            }
        } catch (final Exception e) {
            e.printStackTrace();
            Utils.reportException(mCtx, e);
        }
    }

    @Nullable
    public static TextDrawable getTextDrawable(@Nullable final String key,
                                               @NonNull final ColorGenerator generator) {

        if (TextUtils.isEmpty(key)) {
            return null;
        }
        return TEXTDRAWABLE_BUILDER.build(key.substring(0,1), generator.getColor(key));
    }

    @Nullable
    public static TextDrawable getTextDrawable(@Nullable final String key) {
        return getTextDrawable(key, ColorGenerator.MATERIAL);
    }

    public static void setTextDrawable(@Nullable final ImageView imageView,
                                       @Nullable final String key,
                                       boolean updateVisibilityIfNeeded) {
        setTextDrawable(imageView, key, ColorGenerator.MATERIAL, updateVisibilityIfNeeded);
    }

    public static void setTextDrawable(@Nullable final ImageView imageView,
                                       @Nullable final String key,
                                       @NonNull final ColorGenerator generator,
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


    @Nullable
    public static Bitmap encodeAsBitmap(String contents, BarcodeFormat format, int imgWidth, int imgHeight) throws WriterException {
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

        final Bitmap bitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

        return bitmap;
    }


}