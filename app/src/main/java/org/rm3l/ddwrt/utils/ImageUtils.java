package org.rm3l.ddwrt.utils;

import android.app.Activity;
import android.app.Notification;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RemoteViews;

import com.crashlytics.android.Crashlytics;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Transformation;
import com.squareup.picasso.Target;

import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.exceptions.DDWRTCompanionException;
import org.rm3l.ddwrt.main.DDWRTMainActivity;
import org.rm3l.ddwrt.resources.conn.Router;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.rm3l.ddwrt.utils.Utils.reportException;

/**
 * Created by rm3l on 07/11/15.
 */
public final class ImageUtils {

    private static final String TAG = ImageUtils.class.getSimpleName();

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
            final String url = String.format("%s/%s/%s.jpg", DDWRTCompanionConstants.IMAGE_CDN_URL_PREFIX,
                    Joiner
                            .on(",")
                            .skipNulls().join(opts != null ?
                            opts : DDWRTCompanionConstants.CLOUDINARY_OPTS),
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
                    requestCreator
                            .into(contentView,
                                    iconId,
                                    notifyID,
                                    notification);
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

}