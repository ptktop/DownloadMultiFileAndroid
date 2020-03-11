package com.ptktop.downloadmultifileandroid

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ptktop.downloadmultifileandroid.adapter.DownloadAdapter
import com.ptktop.downloadmultifileandroid.dao.DownloadDao
import com.ptktop.downloadmultifileandroid.download.DownloadFile
import com.tbruyelle.rxpermissions2.RxPermissions
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MainActivity : AppCompatActivity() {

    private lateinit var rcView: RecyclerView

    private var adapter: DownloadAdapter? = null
    private var linearLayoutManager: LinearLayoutManager? = null

    private val requestSetting: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        EventBus.getDefault().register(this)
        linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager!!.isSmoothScrollbarEnabled = true
        initView()
        checkPermission()
    }

    private fun initView() {
        rcView = findViewById(R.id.rcView)
        setupView()
    }

    private fun setupView() {
        val url1 =
            "https://3.bp.blogspot.com/-wNWvZodhy_s/XNCCDxlgjJI/AAAAAAAACgk/hWhZl-Yl568rnJSKTxubgzA7-e51_s_LQCLcBGAs/s1600/the-beatles-early.jpg"
        val url2 =
            "https://static.independent.co.uk/s3fs-public/thumbnails/image/2016/03/11/11/10-Beatles.jpg"
        val url3 = "https://thestandard.co/wp-content/uploads/2019/10/The-Beatles.jpg"
        val listDao = ArrayList<DownloadDao>()
        listDao.clear()
        listDao.add(DownloadDao(url1))
        listDao.add(DownloadDao(url2))
        listDao.add(DownloadDao(url3))
        adapter = DownloadAdapter(listDao)
        rcView.layoutManager = linearLayoutManager
        rcView.adapter = adapter
        rcView.setHasFixedSize(true)
        adapter!!.setOnItemClickListener(object : DownloadAdapter.OnItemClickListener {
            override fun onItemClick(itemView: View, position: Int) {

            }
        })
    }

    //------------------------------------- Permission ---------------------------------------------
    @SuppressLint("CheckResult")
    private fun checkPermission() {
        val rxPermission = RxPermissions(this)
        rxPermission.requestEach(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .subscribe { permission ->  // will emit 2 Permission objects
                when {
                    permission.granted -> {
                        // `permission.name` is granted
                        setupView()
                    }
                    permission.shouldShowRequestPermissionRationale -> {
                        // Denied permission not check ask never again
                        clearRecycleView()
                        showDialogSetting()
                    }
                    else -> {
                        // Denied permission with check ask never again & go to setting
                        showDialogSetting()
                    }
                }
            }
    }

    private fun clearRecycleView() {
        if (adapter != null) {
            rcView.adapter = null
            adapter = null
        }
    }

    private fun showDialogSetting() {
        val builder =
            AlertDialog.Builder(this, R.style.Theme_AppCompat_Dialog_Alert)
                .setCancelable(false)
                .setIcon(ContextCompat.getDrawable(this, R.mipmap.ic_launcher))
                .setTitle("Write storage permission request")
                .setMessage("File will not be available until you accept the permission request.")
                .setPositiveButton(
                    "OK"
                ) { _: DialogInterface?, _: Int ->
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    intent.data = Uri.fromParts("package", packageName, null)
                    startActivityForResult(intent, requestSetting)
                }
        val dialog: AppCompatDialog = builder.create()
        dialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestSetting) {
            if (resultCode == Activity.RESULT_CANCELED) {
                checkPermission()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun followDownload(dao: DownloadDao) {
        when (dao.downloadStatus) {
            DownloadFile.getInstance().download -> {
                adapter?.let { adapter!!.updateProgress(dao) }
            }
            DownloadFile.getInstance().pause -> {
            }
            DownloadFile.getInstance().cancel -> {
                adapter?.let { adapter!!.updateProgress(dao) }
            }
            DownloadFile.getInstance().success -> {
                adapter?.let { adapter!!.updateProgress(dao) }
            }
            DownloadFile.getInstance().error -> {
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }
}
