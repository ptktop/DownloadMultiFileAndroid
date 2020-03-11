package com.ptktop.downloadmultifileandroid.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.widget.ContentLoadingProgressBar
import com.ptktop.downloadmultifileandroid.R
import com.ptktop.downloadmultifileandroid.dao.DownloadDao
import com.ptktop.downloadmultifileandroid.download.DownloadFile
import kotlin.math.pow

class ItemDownloadViewGroup : FrameLayout {

    private lateinit var tvFileName: AppCompatTextView
    private lateinit var tvPercent: AppCompatTextView
    private lateinit var tvFileSize: AppCompatTextView
    private lateinit var progressDownload: ContentLoadingProgressBar
    private lateinit var imgStop: AppCompatImageView
    private lateinit var imgDownload: AppCompatImageView
    private lateinit var imgPause: AppCompatImageView

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    private fun init(context: Context) {
        View.inflate(context, R.layout.item_download, this)
        tvFileName = findViewById(R.id.tvFileName)
        tvPercent = findViewById(R.id.tvPercent)
        tvFileSize = findViewById(R.id.tvFileSize)
        progressDownload = findViewById(R.id.progressDownload)
        imgStop = findViewById(R.id.imgStop)
        imgDownload = findViewById(R.id.imgDownload)
        imgPause = findViewById(R.id.imgPause)
    }

    @SuppressLint("SetTextI18n")
    fun setupView(dao: DownloadDao, position: Int) {
        val url = dao.url
        dao.fileName = url.substring(url.lastIndexOf("/") + 1)
        tvFileName.text = "${position + 1}. ${dao.fileName}"
        imgStop.setOnClickListener { DownloadFile.getInstance().cancel(dao) }
        imgDownload.setOnClickListener { DownloadFile.getInstance().download(dao) }
        imgPause.setOnClickListener { DownloadFile.getInstance().pause(dao) }
        downloadProcess(dao.bytesRead, dao.contentLength, dao.done)
    }

    private var startTime = System.currentTimeMillis()
    private var timeCount = 1

    @SuppressLint("SetTextI18n")
    private fun downloadProcess(bytesRead: Long, contentLength: Long, done: Boolean) {
        val totalFileSize = "%.2f".format(contentLength / 1024.0.pow(2.0))
        val current = "%.2f".format(bytesRead / 1024.0.pow(2.0))
        val progress = if (contentLength == 0L) 0 else (bytesRead * 100 / contentLength).toInt()
        val currentTime: Long = System.currentTimeMillis() - startTime
        tvPercent.text = "$progress%"
        progressDownload.progress = progress
        tvFileSize.text = "$current/$totalFileSize MB"
        if (currentTime > 1000 * timeCount) {
            timeCount++
        }
        if (done) onDownloadComplete()
    }

    private fun onDownloadComplete() {
        // do something
    }
}