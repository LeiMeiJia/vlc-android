/*****************************************************************************
 * TvUtil.java
 *****************************************************************************
 * Copyright © 2014-2017 VLC authors, VideoLAN and VideoLabs
 * Author: Geoffrey Métais
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *****************************************************************************/
package org.videolan.vlc.gui.tv;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.widget.DiffCallback;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.Row;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.LruCache;
import android.text.TextUtils;

import org.videolan.medialibrary.media.DummyItem;
import org.videolan.medialibrary.media.MediaLibraryItem;
import org.videolan.medialibrary.media.MediaWrapper;
import org.videolan.vlc.R;
import org.videolan.vlc.VLCApplication;
import org.videolan.vlc.gui.DialogActivity;
import org.videolan.vlc.gui.helpers.AudioUtil;
import org.videolan.vlc.gui.helpers.BitmapUtil;
import org.videolan.vlc.gui.helpers.UiTools;
import org.videolan.vlc.gui.tv.audioplayer.AudioPlayerActivity;
import org.videolan.vlc.gui.tv.browser.VerticalGridActivity;
import org.videolan.vlc.media.MediaUtils;
import org.videolan.vlc.util.AndroidDevices;
import org.videolan.vlc.util.Constants;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import static org.videolan.vlc.util.Constants.AUDIO_CATEGORY;
import static org.videolan.vlc.util.Constants.AUDIO_ITEM;
import static org.videolan.vlc.util.Constants.CATEGORY_ALBUMS;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class TvUtil {

    public static void applyOverscanMargin(Activity activity) {
        final int hm = activity.getResources().getDimensionPixelSize(R.dimen.tv_overscan_horizontal);
        final int vm = activity.getResources().getDimensionPixelSize(R.dimen.tv_overscan_vertical);
        activity.findViewById(android.R.id.content).setPadding(hm, vm, hm, vm);
    }

    static void playMedia(Activity activity, MediaWrapper media){
        if (media.getType() == MediaWrapper.TYPE_AUDIO) {
            ArrayList<MediaWrapper> tracks = new ArrayList<>();
            tracks.add(media);
            Intent intent = new Intent(activity, AudioPlayerActivity.class);
            intent.putExtra(AudioPlayerActivity.MEDIA_LIST, tracks);
            activity.startActivity(intent);
        } else
            MediaUtils.openMedia(activity, media);
    }

    public static void openMedia(Activity activity, Object item , Row row){
        if (item instanceof MediaWrapper) {
            MediaWrapper mw = (MediaWrapper) item;
            if (mw.getType() == MediaWrapper.TYPE_AUDIO) {
                showMediaDetail(activity, mw);
            } else if (mw.getType() == MediaWrapper.TYPE_DIR) {
                final Intent intent = new Intent(activity, VerticalGridActivity.class);
                intent.putExtra(MainTvActivity.BROWSER_TYPE, Constants.HEADER_NETWORK);
                intent.setData(mw.getUri());
                activity.startActivity(intent);
            } else if (mw.getType() == MediaWrapper.TYPE_GROUP) {
                final Intent intent = new Intent(activity, VerticalGridActivity.class);
                intent.putExtra(MainTvActivity.BROWSER_TYPE, Constants.HEADER_VIDEO);
                final String title = mw.getTitle().substring(mw.getTitle().toLowerCase().startsWith("the") ? 4 : 0);
                intent.putExtra(Constants.KEY_GROUP, title);
                activity.startActivity(intent);
            } else {
                MediaUtils.openMedia(activity, mw);
            }
        } else if (item instanceof DummyItem){
            if (((DummyItem) item).getId() == Constants.HEADER_STREAM) {
                activity.startActivity(new Intent(activity, DialogActivity.class).setAction(DialogActivity.KEY_STREAM)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } else {
                Intent intent = new Intent(activity, VerticalGridActivity.class);
                intent.putExtra(MainTvActivity.BROWSER_TYPE, ((DummyItem) item).getId());
                activity.startActivity(intent);
            }
        }
    }

    static void showMediaDetail(Context activity, MediaWrapper mediaWrapper) {
        Intent intent = new Intent(activity, DetailsActivity.class);
        intent.putExtra("media", mediaWrapper);
        intent.putExtra("item", new MediaItemDetails(mediaWrapper.getTitle(), mediaWrapper.getArtist(), mediaWrapper.getAlbum(), mediaWrapper.getLocation(), mediaWrapper.getArtworkURL()));
        activity.startActivity(intent);
    }

    public static void browseFolder(Activity activity, long type, Uri uri) {
        Intent intent = new Intent(activity, VerticalGridActivity.class);
        intent.putExtra(MainTvActivity.BROWSER_TYPE, type);
        intent.setData(uri);
        activity.startActivity(intent);
    }

    public static void playAudioList(Activity activity, MediaWrapper[] array, int position) {
        playAudioList(activity, new ArrayList<>(Arrays.asList(array)), position);
    }

    private static void playAudioList(Activity activity, ArrayList<MediaWrapper> list, int position) {
        Intent intent = new Intent(activity, AudioPlayerActivity.class);
        intent.putExtra(AudioPlayerActivity.MEDIA_LIST, list);
        intent.putExtra(AudioPlayerActivity.MEDIA_POSITION, position);
        activity.startActivity(intent);
    }

    public static void openAudioCategory(Activity context, MediaLibraryItem mediaLibraryItem) {
        if (mediaLibraryItem.getItemType() == MediaLibraryItem.TYPE_ALBUM) {
            TvUtil.playAudioList(context, mediaLibraryItem.getTracks(), 0);
        } else {
            Intent intent = new Intent(context, VerticalGridActivity.class);
            intent.putExtra(AUDIO_ITEM, mediaLibraryItem);
            intent.putExtra(AUDIO_CATEGORY, CATEGORY_ALBUMS);
            intent.putExtra(MainTvActivity.BROWSER_TYPE, Constants.HEADER_CATEGORIES);
            context.startActivity(intent);
        }
    }

    private static final String TAG = "VLC/TvUtil";
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static void updateBackground(final BackgroundManager bm, Object item) {
        if (bm == null) return;
        if (item instanceof MediaLibraryItem) {
            final boolean crop = ((MediaLibraryItem) item).getItemType() != MediaLibraryItem.TYPE_MEDIA
                    || ((MediaWrapper)item).getType() == MediaWrapper.TYPE_AUDIO;
            final String artworkMrl = ((MediaLibraryItem) item).getArtworkMrl();
            if (!TextUtils.isEmpty(artworkMrl)) {
                VLCApplication.runBackground(new Runnable() {
                    @Override
                    public void run() {
                        if (bm == null) return;
                        Bitmap cover = AudioUtil.readCoverBitmap(Uri.decode(artworkMrl), 512);
                        if (cover == null) return;
                        if (crop)
                            cover = BitmapUtil.centerCrop(cover, cover.getWidth(), cover.getWidth()*10/16);
                        final Bitmap blurred = UiTools.blurBitmap(cover, 10f);
                        VLCApplication.runOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                if (bm == null) return;
                                bm.setColor(0);
                                bm.setDrawable(new BitmapDrawable(VLCApplication.getAppResources(), blurred));
                            }
                        });
                    }
                });
                return;
            }
        }
        clearBackground(bm);
    }

    private static void clearBackground(BackgroundManager bm) {
        bm.setColor(ContextCompat.getColor(VLCApplication.getAppContext(), R.color.tv_bg));
        bm.setDrawable(null);
    }

    private static LruCache<BackgroundManager, WeakReference<ValueAnimator>> refCache = new LruCache<>(5);
    //See https://issuetracker.google.com/issues/37135111
    public static void releaseBackgroundManager(BackgroundManager backgroundManager) {
        Field field;
        final WeakReference<ValueAnimator> ref = refCache.get(backgroundManager);
        ValueAnimator valueAnimator = null;
        if (ref != null) {
            valueAnimator = ref.get();
            if (valueAnimator == null) refCache.remove(backgroundManager);
        }
        if (valueAnimator == null) {
            try {
                field = backgroundManager.getClass().getDeclaredField("mAnimator");
                field.setAccessible(true);
                valueAnimator = (ValueAnimator) field.get(backgroundManager);
                refCache.put(backgroundManager, new WeakReference<>(valueAnimator));
            } catch (Exception ignored) {}
        }
        if (valueAnimator != null && valueAnimator.isStarted()) valueAnimator.cancel();
        backgroundManager.release();
    }
    
    static int getIconRes(MediaLibraryItem mediaLibraryItem) {
        switch (mediaLibraryItem.getItemType()) {
            case MediaLibraryItem.TYPE_ALBUM:
                return R.drawable.ic_album_big;
            case MediaLibraryItem.TYPE_ARTIST:
                return R.drawable.ic_artist_big;
            case MediaLibraryItem.TYPE_GENRE:
                return R.drawable.ic_genre_big;
            case MediaLibraryItem.TYPE_MEDIA:
                final MediaWrapper mw = (MediaWrapper) mediaLibraryItem;
                if (mw.getType() == MediaWrapper.TYPE_VIDEO)
                    return R.drawable.ic_browser_video_big_normal;
                if (mw.getType() == MediaWrapper.TYPE_DIR
                        && TextUtils.equals(AndroidDevices.EXTERNAL_PUBLIC_DIRECTORY, mw.getLocation()))
                    return R.drawable.ic_menu_folder_big;
                else
                    return R.drawable.ic_song_big;
            case MediaLibraryItem.TYPE_DUMMY:
                final long id = mediaLibraryItem.getId();
                if (id == Constants.HEADER_VIDEO) {
                    return R.drawable.ic_video_collection_big;
                } else if (id == Constants.HEADER_DIRECTORIES) {
                    return R.drawable.ic_menu_folder_big;
                } else if (id == Constants.HEADER_NETWORK) {
                    return R.drawable.ic_menu_network_big;
                } else if (id == Constants.HEADER_STREAM) {
                    return R.drawable.ic_menu_stream_big;
                } else if (id == Constants.ID_SETTINGS) {
                    return R.drawable.ic_menu_preferences_big;
                } else if (id == Constants.ID_ABOUT_TV
                        || id == Constants.ID_LICENCE) {
                    return R.drawable.ic_default_cone;
                } else if (id == Constants.CATEGORY_ARTISTS) {
                    return R.drawable.ic_artist_big;
                } else if (id == Constants.CATEGORY_ALBUMS) {
                    return R.drawable.ic_album_big;
                } else if (id == Constants.CATEGORY_GENRES) {
                    return R.drawable.ic_genre_big;
                } else if (id == Constants.CATEGORY_SONGS) {
                    return R.drawable.ic_song_big;
                }
            default:
                return R.drawable.ic_browser_unknown_big_normal;
        }
    }

    public static DiffCallback<MediaLibraryItem> diffCallback = new DiffCallback<MediaLibraryItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull MediaLibraryItem oldItem, @NonNull MediaLibraryItem newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areContentsTheSame(@NonNull MediaLibraryItem oldItem, @NonNull MediaLibraryItem newItem) {
            if (oldItem.getItemType() == MediaLibraryItem.TYPE_DUMMY) return TextUtils.equals(oldItem.getDescription(), newItem.getDescription());
            if (oldItem.getItemType() != MediaLibraryItem.TYPE_MEDIA) return true;
            final MediaWrapper oldMedia = (MediaWrapper) oldItem;
            final MediaWrapper newMedia = (MediaWrapper) newItem;
            return oldMedia == newMedia || (oldMedia.getTime() == newMedia.getTime()
                    && TextUtils.equals(oldMedia.getArtworkMrl(), newMedia.getArtworkMrl())
                    && oldMedia.getSeen() == newMedia.getSeen());
        }

        public Object getChangePayload(@NonNull MediaLibraryItem oldItem, @NonNull MediaLibraryItem newItem) {
            if (oldItem.getItemType() == MediaLibraryItem.TYPE_DUMMY) return Constants.UPDATE_DESCRIPTION;
            final MediaWrapper oldMedia = (MediaWrapper) oldItem;
            final MediaWrapper newMedia = (MediaWrapper) newItem;
            if (oldMedia.getTime() != newMedia.getTime()) return Constants.UPDATE_TIME;
            if (!TextUtils.equals(oldMedia.getArtworkMrl(), newMedia.getArtworkMrl())) return Constants.UPDATE_THUMB;
            else return Constants.UPDATE_SEEN;
        }
    };

    public static DiffCallback<ListRow> listDiffCallback = new DiffCallback<ListRow>() {
        @Override
        public boolean areItemsTheSame(@NonNull ListRow oldItem, @NonNull ListRow newItem) {
            return Objects.equals(oldItem.getContentDescription(), newItem.getContentDescription());
        }

        @Override
        public boolean areContentsTheSame(@NonNull ListRow oldItem, @NonNull ListRow newItem) {
            return true;
        }
    };
}
