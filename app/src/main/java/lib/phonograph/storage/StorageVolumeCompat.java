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
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.File;

/**
 * StorageVolume Compat
 * Created by Alex on 2022/3/11.
 */
public class StorageVolumeCompat {

    private final IStorageVolume mVolume;

    StorageVolumeCompat(Object volume) {
        final int api = Build.VERSION.SDK_INT;
        if (api >= Build.VERSION_CODES.R) {
            mVolume = new StorageVolume30((StorageVolume) volume);
        } else if (api >= Build.VERSION_CODES.N) {
            mVolume = new StorageVolume24((StorageVolume) volume);
        } else if (api >= Build.VERSION_CODES.KITKAT) {
            mVolume = new StorageVolume19(volume);
        } else if (api >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mVolume = new StorageVolume17(volume);
        } else if (api >= Build.VERSION_CODES.JELLY_BEAN) {
            mVolume = new StorageVolume16(volume);
        } else {
            mVolume = new StorageVolume14(volume);
        }
    }

    /**
     * Returns the directory where this volume is currently mounted.
     * <p>
     * Direct filesystem access via this path has significant emulation
     * overhead, and apps are instead strongly encouraged to interact with media
     * on storage volumes via the {@link MediaStore} APIs.
     * <p>
     * This directory does not give apps any additional access beyond what they
     * already have via {@link MediaStore}.
     *
     * @return directory where this volume is mounted, or {@code null} if the
     *         volume is not currently mounted.
     */
    @Nullable
    public File getDirectory() {
        return mVolume.getDirectory();
    }

    /**
     * Returns a user-visible description of the volume.
     *
     * @return the volume description
     */
    public String getDescription(Context context) {
        return mVolume.getDescription(context);
    }

    /**
     * Returns true if the volume is the primary shared/external storage, which is the volume
     * backed by {@link Environment#getExternalStorageDirectory()}.
     */
    public boolean isPrimary() {
        return mVolume.isPrimary();
    }

    /**
     * Returns true if the volume is removable.
     *
     * @return is removable
     */
    public boolean isRemovable() {
        return mVolume.isRemovable();
    }

    /**
     * Returns true if the volume is emulated.
     *
     * @return is removable
     */
    public boolean isEmulated() {
        return mVolume.isEmulated();
    }

    /**
     * Gets the volume UUID, if any.
     */
    @RequiresApi(19)
    @Nullable
    public String getUuid() {
        return mVolume.getUuid();
    }

    /**
     * Returns the current state of the volume.
     *
     * @return one of {@link Environment#MEDIA_UNKNOWN}, {@link Environment#MEDIA_REMOVED},
     *         {@link Environment#MEDIA_UNMOUNTED}, {@link Environment#MEDIA_CHECKING},
     *         {@link Environment#MEDIA_NOFS}, {@link Environment#MEDIA_MOUNTED},
     *         {@link Environment#MEDIA_MOUNTED_READ_ONLY}, {@link Environment#MEDIA_SHARED},
     *         {@link Environment#MEDIA_BAD_REMOVAL}, or {@link Environment#MEDIA_UNMOUNTABLE}.
     */
    @RequiresApi(19)
    public String getState() {
        return mVolume.getState();
    }

    private interface IStorageVolume {

        @Nullable
        File getDirectory();

        String getDescription(Context context);

        boolean isPrimary();

        boolean isRemovable();

        boolean isEmulated();

        @Nullable
        String getUuid();

        String getState();
    }

    private static class StorageVolume14 implements IStorageVolume {

        private final Object mVolume;

        public StorageVolume14(Object volume) {
            mVolume = volume;
        }

        protected Object getWrapped() {
            return mVolume;
        }

        @NonNull
        private String getPath() {
            final Object volume = getWrapped();
            String path;
            try {
                path = (String) volume.getClass().getMethod("getPath").invoke(volume);
            } catch (Exception e) {
                path = null;
            }
            if (path == null) {
                throw new IllegalStateException("Cannot get path.");
            }
            return path;
        }

        @Override
        public File getDirectory() {
            return new File(getPath());
        }

        @Override
        public String getDescription(Context context) {
            final Object volume = getWrapped();
            try {
                return (String) volume.getClass().getMethod("getDescription")
                        .invoke(volume);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public boolean isPrimary() {
            return TextUtils.equals(getPath(), Environment.getExternalStorageDirectory().getPath());
        }

        @Override
        public boolean isRemovable() {
            final Object volume = getWrapped();
            Boolean result;
            try {
                result = (Boolean) volume.getClass().getMethod("isRemovable").invoke(volume);
            } catch (Exception e) {
                result = null;
            }
            return result != null && result;
        }

        @Override
        public boolean isEmulated() {
            final Object volume = getWrapped();
            Boolean result;
            try {
                result = (Boolean) volume.getClass().getMethod("isEmulated").invoke(volume);
            } catch (Exception e) {
                result = null;
            }
            return result != null && result;
        }

        @Override
        @Nullable
        public String getUuid() {
            throw new IllegalStateException("Cannot get uuid.");
        }

        @Override
        public String getState() {
            throw new IllegalStateException("Cannot get state.");
        }
    }

    @RequiresApi(16)
    private static class StorageVolume16 extends StorageVolume14 {

        public StorageVolume16(Object volume) {
            super(volume);
        }

        @Override
        public String getDescription(Context context) {
            final Object volume = getWrapped();
            try {
                return (String) volume.getClass().getMethod("getDescription", Context.class)
                        .invoke(volume, context);
            } catch (Exception e) {
                return super.getDescription(context);
            }
        }
    }

    @RequiresApi(17)
    private static class StorageVolume17 extends StorageVolume16 {

        public StorageVolume17(Object volume) {
            super(volume);
        }

        @Override
        public File getDirectory() {
            final Object volume = getWrapped();
            try {
                return (File) volume.getClass().getMethod("getPathFile").invoke(volume);
            } catch (Exception e) {
                return super.getDirectory();
            }
        }

        @Override
        public boolean isPrimary() {
            final Object volume = getWrapped();
            Boolean result;
            try {
                result = (Boolean) volume.getClass().getMethod("isPrimary").invoke(volume);
            } catch (Exception e) {
                result = null;
            }
            return result != null && result;
        }
    }

    @RequiresApi(19)
    private static class StorageVolume19 extends StorageVolume17 {

        public StorageVolume19(Object volume) {
            super(volume);
        }

        @Override
        @Nullable
        public String getUuid() {
            final Object volume = getWrapped();
            try {
                return (String) volume.getClass().getMethod("getUuid").invoke(volume);
            } catch (Exception e) {
                return super.getState();
            }
        }

        @Override
        public String getState() {
            final Object volume = getWrapped();
            try {
                return (String) volume.getClass().getMethod("getState").invoke(volume);
            } catch (Exception e) {
                return super.getState();
            }
        }
    }

    @RequiresApi(24)
    private static class StorageVolume24 extends StorageVolume19 {

        public StorageVolume24(StorageVolume volume) {
            super(volume);
        }

        @Override
        protected StorageVolume getWrapped() {
            return (StorageVolume) super.getWrapped();
        }

        @Override
        public String getDescription(Context context) {
            return getWrapped().getDescription(context);
        }

        @Override
        public boolean isPrimary() {
            return getWrapped().isPrimary();
        }

        @Override
        public boolean isRemovable() {
            return getWrapped().isRemovable();
        }

        @Override
        public boolean isEmulated() {
            return getWrapped().isEmulated();
        }

        @Override
        @Nullable
        public String getUuid() {
            return getWrapped().getUuid();
        }

        @Override
        public String getState() {
            return getWrapped().getState();
        }
    }

    @RequiresApi(30)
    private static class StorageVolume30 extends StorageVolume24 {

        public StorageVolume30(StorageVolume volume) {
            super(volume);
        }

        @Override
        public File getDirectory() {
            return getWrapped().getDirectory();
        }
    }
}