package com.aazhou.mtforum

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.aazhou.mtforum.data.network.HttpClient
import com.aazhou.mtforum.image.MtfImageLoaderFactory

class MtfApp : Application(), ImageLoaderFactory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        installCrashHandler()
    }

    





    private fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            container.logger.logCrash(thread, throwable)
            if (previous != null) {
                previous.uncaughtException(thread, throwable)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
                kotlin.system.exitProcess(10)
            }
        }
    }

    




    override fun newImageLoader(): ImageLoader =
        MtfImageLoaderFactory(
            context = this,
            userAgent = HttpClient.MOBILE_UA,
            cookieHeader = { container.httpClient.cookieHeader() },
        ).newImageLoader()
}
