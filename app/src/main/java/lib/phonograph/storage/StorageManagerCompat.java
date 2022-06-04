/*
 * Copyright (C) 2022 AlexMofer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package lib.phonograph.storage;
//package com.am.appcompat.storage;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * StorageManager Compat
 * API 28 引入灰名单，所有hide方法均可访问
 * API 29 强化灰名单，少量不重要hide方法已禁止访问
 * API 30 进一步强化灰名单，部分重要hide方法已禁止访问
 * Created by Alex on 2022/3/11.
 */
public class StorageManagerCompat {

    private StorageManagerCompat() {
        //no instance
    }

    /**
     * Return the primary shared/external storage volume available to the
     * current user. This volume is the same storage device returned by
     * {@link Environment#getExternalStorageDirectory()} and
     * {@link Context#getExternalFilesDir(String)}.
     */
    @NonNull
    public static StorageVolumeCompat getPrimaryStorageVolume(StorageManager manager) {
        final Object volume;
        final int api = Build.VERSION.SDK_INT;
        if (api >= Build.VERSION_CODES.N) {
            volume = Api24Impl.getPrimaryStorageVolume(manager);
        } else if (api >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            volume = Api17Impl.getPrimaryStorageVolume(manager);
        } else {
            volume = Api14Impl.getPrimaryStorageVolume(manager);
        }
        return new StorageVolumeCompat(volume);
    }

    /**
     * Return the {@link StorageVolume} that contains the given file, or
     * {@code null} if none.
     */
    @Nullable
    public static StorageVolumeCompat getStorageVolume(StorageManager manager, @NonNull File file) {
        final Object volume;
        final int api = Build.VERSION.SDK_INT;
        if (api >= Build.VERSION_CODES.N) {
            volume = Api24Impl.getStorageVolume(manager, file);
        } else if (api >= Build.VERSION_CODES.M) {
            volume = Api23Impl.getStorageVolume(manager, file);
        } else {
            volume = Api14Impl.getStorageVolume(manager, file);
        }
        return volume == null ? null : new StorageVolumeCompat(volume);
    }

    /**
     * Return the {@link StorageVolume} that contains the given
     * {@link MediaStore} item.
     */
    @RequiresApi(29)
    @NonNull
    public static StorageVolumeCompat getStorageVolume(StorageManager manager, @NonNull Uri uri) {
        return new StorageVolumeCompat(Api29Impl.getStorageVolume(manager, uri));
    }

    /**
     * Return the list of shared/external storage volumes currently available to
     * the calling user.
     * <p>
     * These storage volumes are actively attached to the device, but may be in
     * any mount state, as returned by {@link StorageVolume#getState()}. Returns
     * both the primary shared storage device and any attached external volumes,
     * including SD cards and USB drives.
     */
    @NonNull
    public static List<StorageVolumeCompat> getStorageVolumes(StorageManager manager) {
        final List<?> volumes = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ?
                Api24Impl.getStorageVolumes(manager) : Api14Impl.getStorageVolumes(manager);
        if (volumes.isEmpty()) {
            return new ArrayList<>();
        }
        final List<StorageVolumeCompat> result = new ArrayList<>();
        for (Object volume : volumes) {
            result.add(new StorageVolumeCompat(volume));
        }
        return result;
    }

    private static class Api14Impl {

        private Api14Impl() {
            //no instance
        }

        @NonNull
        public static Object getPrimaryStorageVolume(StorageManager manager) {
            return getStorageVolumes(manager).get(0);
        }

        @Nullable
        public static Object getStorageVolume(StorageManager manager, @NonNull File file) {
            final String path = file.getPath();
            final List<Object> volumes = getStorageVolumes(manager);
            Method method = null;
            for (Object volume : volumes) {
                if (method == null) {
                    try {
                        method = volume.getClass().getMethod("getPath");
                    } catch (NoSuchMethodException e) {
                        // ignore
                    }
                }
                String p;
                try {
                    p = method == null ? null : (String) method.invoke(volume);
                } catch (Exception e) {
                    p = null;
                }
                if (p != null && path.startsWith(p)) {
                    return volume;
                }
            }
            return null;
        }

        @NonNull
        public static List<Object> getStorageVolumes(StorageManager manager) {
            // Method removed at api 24
            // Method created at api 14
            Object[] volumes;
            try {
                volumes = (Object[]) manager.getClass().getMethod("getVolumeList").invoke(manager);
            } catch (Exception e) {
                volumes = null;
            }
            return volumes == null ? new ArrayList<>() : Arrays.asList(volumes);
        }
    }

    @RequiresApi(17)
    private static class Api17Impl {

        private Api17Impl() {
            //no instance
        }

        @NonNull
        public static Object getPrimaryStorageVolume(StorageManager manager) {
            // Method removed at api 24
            // Method created at api 17
            Object volume;
            try {
                volume = manager.getClass().getMethod("getPrimaryVolume").invoke(manager);
            } catch (Exception e) {
                volume = null;
            }
            if (volume == null) {
                throw new IllegalStateException("Cannot get primary volume.");
            }
            return volume;
        }
    }

    @RequiresApi(23)
    private static class Api23Impl {

        private Api23Impl() {
            //no instance
        }

        @Nullable
        public static Object getStorageVolume(StorageManager manager, @NonNull File file) {
            // Method created at api 23
            try {
                return manager.getClass().getMethod("getStorageVolume", File.class)
                        .invoke(manager, file);
            } catch (Exception e) {
                return null;
            }
        }
    }

    @RequiresApi(24)
    private static class Api24Impl {

        private Api24Impl() {
            //no instance
        }

        @NonNull
        public static StorageVolume getPrimaryStorageVolume(StorageManager manager) {
            return manager.getPrimaryStorageVolume();
        }

        @Nullable
        public static StorageVolume getStorageVolume(StorageManager manager, @NonNull File file) {
            return manager.getStorageVolume(file);
        }

        @NonNull
        public static List<StorageVolume> getStorageVolumes(StorageManager manager) {
            return manager.getStorageVolumes();
        }
    }

    @RequiresApi(29)
    private static class Api29Impl {

        private Api29Impl() {
            //no instance
        }

        @NonNull
        public static StorageVolume getStorageVolume(StorageManager manager, @NonNull Uri uri) {
            return manager.getStorageVolume(uri);
        }
    }
}