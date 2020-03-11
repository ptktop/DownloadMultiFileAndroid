package com.ptktop.downloadmultifileandroid.download

import android.annotation.SuppressLint
import android.os.Environment
import com.ptktop.downloadmultifileandroid.dao.DownloadDao
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import okio.*
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.io.IOException
import java.util.HashMap
import java.util.concurrent.TimeUnit

@Suppress("DEPRECATION")
class DownloadFile {

    private var downCalls: HashMap<String, Call>? = null

    /* applicationContext.filesDir.absolutePath */
    private val filePathApp: String = Environment.getExternalStorageDirectory().absolutePath +
            "/DownloadMultiFileAndroid/"
    private val filePathDownload: String = Environment.getExternalStorageDirectory().absolutePath +
            "/DownloadMultiFileAndroid/Download/"

    val download: String = "download"
    val pause: String = "pause"
    val cancel: String = "cancel"
    val success: String = "success"
    val error: String = "error"

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: DownloadFile? = null

        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: DownloadFile().also { instance = it }
            }
    }

    init {
        downCalls = HashMap()
        checkFolderInsideStorage()
    }

    private fun checkFolderInsideStorage() {
        val pathApp = File(filePathApp)
        val pathDownload = File(filePathDownload)
        if (!pathApp.exists()) { // not found folder app
            pathApp.mkdir()
            if (!pathDownload.exists()) { // not found folder download
                pathDownload.mkdir()
            }
        } else { // have folder main
            if (!pathDownload.exists()) { // have folder app , but not found folder download
                pathDownload.mkdir()
            }
        }
    }

    private fun setUpOkHttp(dao: DownloadDao): OkHttpClient {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        return OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.MINUTES)
            .connectTimeout(30, TimeUnit.MINUTES)
            .addInterceptor(interceptor)
            .addNetworkInterceptor { chain ->
                val originalResponse = chain.proceed(chain.request())
                originalResponse.newBuilder()
                    .body(DownloadResponseBody(originalResponse.body, dao))
                    .build()
            }
            .build()
    }

    fun download(daoParam: DownloadDao) {
        val url = daoParam.url
        if (downCalls!!.containsKey(url)) return // protect if duplicate url download
        val dao = getRealFileFolder(daoParam) // check file if have continue download ag ain
        dao.downloadStatus = download
        Observable.fromCallable {
                val request = Request.Builder()
                    .url(url)
                    .build()
                val call: Call = setUpOkHttp(dao).newCall(request)
                downCalls!![url] = call
                call.execute()
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe(object : Observer<Response?> {
                override fun onSubscribe(d: Disposable) {}

                override fun onNext(response: Response) {
                    if (response.isSuccessful) {
                        storageFile(response.body, dao)
                    } else {
                        downloadFileComplete(false, "On Next >>> " + response.body)
                    }
                }

                override fun onComplete() {
                    dao.downloadStatus = success
                    EventBus.getDefault().post(dao)
                    downloadFileComplete(true, "Complete")
                }

                override fun onError(e: Throwable) {
                    if (downCalls!!.containsKey(url)) {
                        pause(dao)
                        dao.downloadStatus = error
                        EventBus.getDefault().post(dao)
                    } else {
                        dao.downloadStatus = pause
                        EventBus.getDefault().post(dao)
                    }
                    downloadFileComplete(false, "On Error >>> " + e.message)
                }
            })
    }

    fun pause(dao: DownloadDao) {
        downCalls?.let {
            val call = downCalls!![dao.url]
            call?.cancel()
            downCalls!!.remove(dao.url)
        }
    }

    fun cancel(dao: DownloadDao) {
        pause(dao)
        dao.bytesRead = 0
        dao.contentLength = 0
        dao.downloadStatus = cancel
        EventBus.getDefault().post(dao)
        deleteFile(dao.fileName)
    }

    private fun storageFile(responseBody: ResponseBody?, dao: DownloadDao) {
        responseBody?.let {
            val file = File(filePathDownload, dao.fileName)
            val sink = file.sink().buffer()
            sink.writeAll(responseBody.source())
            sink.close()
            downCalls!!.remove(dao.url)
            storageFileComplete()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun downloadFileComplete(isComplete: Boolean, msg: String) {
        // Up to you handle
    }

    private fun storageFileComplete() {
        // Do something
    }

    private fun getRealFileFolder(dao: DownloadDao): DownloadDao {
        val fileName: String = dao.fileName
        val contentLength: Long = dao.contentLength
        var file = File(filePathDownload, fileName)
        if (file.exists()) {
            var downloadLength = file.length() // get length file if have ever download
            if (downloadLength < contentLength) {
                val dotIndex = fileName.lastIndexOf(".")
                val fileNameOther =
                    if (dotIndex == -1) { // as image_1
                        "${fileName}_copy"
                    } else { // as image_1.jpg
                        "${fileName.substring(0, dotIndex)}_copy${fileName.substring(dotIndex)}"
                    }
                val newFile = File(filePathDownload, fileNameOther)
                file = newFile
                downloadLength = newFile.length()
            }
            dao.bytesRead = downloadLength
            dao.fileName = file.name
        }
        return dao
    }

    private fun deleteFile(fileName: String): Boolean {
        val status: Boolean
        val checker = SecurityManager()
        val file = File(filePathDownload + fileName)
        status = if (file.exists()) {
            checker.checkDelete(file.toString())
            if (file.isFile) {
                try {
                    file.delete()
                    true
                } catch (se: SecurityException) {
                    se.printStackTrace()
                    false
                }
            } else false
        } else false
        return status
    }

    private class DownloadResponseBody(
        private val responseBody: ResponseBody?,
        private val dao: DownloadDao
    ) : ResponseBody() {

        private var bufferedSource: BufferedSource? = null

        override fun contentType(): MediaType? {
            return responseBody!!.contentType()
        }

        override fun contentLength(): Long {
            return responseBody!!.contentLength()
        }

        override fun source(): BufferedSource {
            if (bufferedSource == null) {
                bufferedSource = source(responseBody!!.source()).buffer()
            }
            return bufferedSource!!
        }

        private fun source(source: Source): Source {
            return object : ForwardingSource(source) {
                var sumBytesRead = 0L

                @Throws(IOException::class)
                override fun read(sink: Buffer, byteCount: Long): Long {
                    val bytesRead = super.read(sink, byteCount)
                    // read() returns the number of bytes read, or -1 if this source is exhausted.
                    sumBytesRead += if (bytesRead != -1L) bytesRead else 0 // not support pause
                    val totalBytesRead = if (bytesRead != -1L) bytesRead else 0
                    dao.bytesRead =
                        if (sumBytesRead > dao.bytesRead) totalBytesRead + dao.bytesRead else dao.bytesRead
                    dao.contentLength =
                        if (dao.contentLength == 0L) responseBody!!.contentLength() else dao.contentLength
                    dao.done = bytesRead == -1L
                    EventBus.getDefault().post(dao)
                    return bytesRead
                }
            }
        }
    }
}