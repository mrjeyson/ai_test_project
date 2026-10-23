package com.example.test_ai_project.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp

/**
 * Hilt's root. Every `@InstallIn(SingletonComponent::class)` module across all
 * modules is aggregated into the graph generated from this class.
 *
 * Also where Coil's singleton loader is configured. Coil would build a default one lazily,
 * but the disk cache is the half of the offline story Room cannot cover — cached rows with
 * no artwork is a grid of grey rectangles — so it is set up explicitly here rather than
 * left to a default a future Coil version is free to change.
 */
@HiltAndroidApp
class TestAiApplication : Application(), SingletonImageLoader.Factory {

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .diskCache {
                DiskCache.Builder()
                    // `cacheDir`, not the temp directory: this has to survive process
                    // death and reboots to be worth anything offline. Android may still
                    // evict it under storage pressure, which is the correct trade for
                    // artwork that can be re-fetched.
                    .directory(cacheDir.resolve(IMAGE_CACHE_DIRECTORY))
                    .maxSizeBytes(DISK_CACHE_BYTES)
                    .build()
            }
            .crossfade(true)
            .build()

    private companion object {
        const val IMAGE_CACHE_DIRECTORY = "image_cache"
        const val DISK_CACHE_BYTES = 256L * 1024 * 1024
    }
}














