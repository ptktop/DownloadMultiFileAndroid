package com.ptktop.downloadmultifileandroid.dao

class DownloadDao(var url: String) {

    var fileName: String = ""
    var downloadStatus: String = ""
    var bytesRead: Long = 0
    var contentLength: Long = 0
    var done: Boolean = false

}