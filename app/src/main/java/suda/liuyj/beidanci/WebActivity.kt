package suda.liuyj.beidanci

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import suda.liuyj.beidanci.databinding.ActivityWebBinding
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL

fun __word_file(word: String): String {
    var w = word
    while (' ' in w) w = w.replace(' ', '_')
    val sp = File.separator
    return "${dir_inner}${sp}cache${sp}$w"
}
//网页缓存路径
fun word_file_mht(word: String): String {
    return "${__word_file(word)}.mht"
}
//翻译缓存路径
fun word_file_tran(word: String): String {
    return "${__word_file(word)}.tran"
}
//发音缓存路径
fun word_file_mp3(word: String, fix: String): String {
    val t = fix[5]
    return "${__word_file(word)}_${t}.mp3"
}
//协程异步下载音频
class Mp3Download {
    // kotlin没有static方法，而是要使用伴生对象来替代
    companion object {
        suspend fun asyncDownload(url: String, path: String, ctx: Context) {
            return GlobalScope.async(Dispatchers.Default) {
                download(url, path, ctx)
            }.await()
        }

        fun download(_url: String, path: String, ctx: Context) {
            val url = URL(_url)
            val connection = url.openConnection()
            connection.connect()
            val inputStream = BufferedInputStream(url.openStream())
//            val outputStream = ctx.openFileOutput(path, Context.MODE_PRIVATE)
            val outputStream = FileOutputStream(File(path))
            val data = ByteArray(1024)
            var count = inputStream.read(data)
            var total = count
            while (count != -1) {
                outputStream.write(data, 0, count)
                count = inputStream.read(data)
                total += count
            }
            outputStream.flush()
            outputStream.close()
            inputStream.close()
        }
    }
}
//异步播放音频
fun __playAudioAsync(path: String) {
    val mediaPlayer = MediaPlayer()
    mediaPlayer.setDataSource(path)
    mediaPlayer.prepareAsync()
    mediaPlayer.setOnPreparedListener {
        it.start()
    }
    mediaPlayer.setOnCompletionListener {
        it.stop()
        it.release()
    }
}

private var urlSuffixAudio = ""
//对于word，异步下载并播放
fun playMp3(word: String, ctx: Context) {
    val f3 = word_file_mp3(word, urlSuffixAudio)
    if (!FileUtil.exist(f3)) {
        val url = "https://dict.youdao.com/dictvoice?audio=$word&$urlSuffixAudio"
        GlobalScope.launch(Dispatchers.Default) {
            Mp3Download.asyncDownload(url, f3, ctx)
            __playAudioAsync(f3)
        }
    } else {
        __playAudioAsync(f3)
    }
}

class WebActivity : AppCompatActivity() {
    private var _coverView: View? = null
    private var webView: WebView? = null
    private var urlSuffixWeb = ""
    private var dark = false
    private var word = ""

    //加载网页缓存或下载
    fun _load_word_web(){
        webView?.let {
            val f1 = word_file_mht(word)
            if (FileUtil.exist(f1)) {
                it.loadUrl(f1)
            } else {
                it.loadUrl("https://dict.youdao.com/m/result?word=$word&$urlSuffixWeb")
            }
        }
    }
    //刷新缓存
    fun clickRef(view:View?){
        // clear bad cache
        val f1 = word_file_mht(word)
        val f2 = word_file_tran(word)
        val f3 = word_file_mp3(word, urlSuffixAudio)
        if (FileUtil.exist(f1))File(f1).delete()
        if (FileUtil.exist(f2))File(f2).delete()
        if (FileUtil.exist(f3))File(f3).delete()
        // reload
        webView?.loadUrl("https://dict.youdao.com/m/result?word=$word&$urlSuffixWeb")
        playMp3(word, this)
    }
    //发音
    fun clickSound(view:View?){
        playMp3(word, this)
    }
    // 下一个单词
    fun next(ok: LearnStateOnce = LearnStateOnce.None) {
        word = theBook.next(ok, applicationContext)
        if (word == TheEndOfBook) {
            toast("已完成可学习任务，请更换任务或更换单词本", 3, applicationContext)
            this.finish()
        }
        this.title = word
        if (_coverView != null) _coverView!!.visibility = View.VISIBLE
        _load_word_web()
        playMp3(word, this)
    }

    fun clickCover(view: View) {
        view.visibility = View.INVISIBLE
        if (_coverView == null) _coverView = view
    }

    fun clickOk(view: View) {
        next(LearnStateOnce.Ok)
    }

    fun clickBad(view: View) {
        next(LearnStateOnce.Bad)
    }

    fun clickGood(view: View) {
        next(LearnStateOnce.Good)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)
        val _binding = ActivityWebBinding.inflate(layoutInflater)
        webView = _binding.webWeb
        webInit()
    }

    override fun onResume() {
        super.onResume()
        dark = isDark(applicationContext)
        if (theBook.name() == "" || theBook.len() == 0) {
            toast("单词本为空，请导入", 5, applicationContext)
            Log.e("web", "id<0 or size =0")
            finish()
            return
        }
        urlSuffixAudio = "type=${getPronounce(application) + 1}" // type 1 英式 2 美式
        urlSuffixWeb = "lang=${getLang(application)}"
        next()
    }

    fun webInit() {
        webView = findViewById(R.id.web_web)
//        webView.loadUrl()

        val webClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                view?.let {
                    val f1 = word_file_mht(word)
                    val f3 = word_file_tran(word)
                    if (!FileUtil.exist(f1)) {
                        it.saveWebArchive(f1)
                    }
                    if(dark){
                    it.evaluateJavascript(
                        "javascript:document.querySelectorAll('*').forEach(function(node) {node.style.backgroundColor = '#000';node.style.color = '#fff';});",
                        null
                    )}
                    if (!FileUtil.exist(f3)) {
                        it.evaluateJavascript(
                            "javascript:s=\"\";x=document.getElementsByClassName(\"trans\");for(i=0;i<x.length;i++)s+=x[i].innerText+\"\\n\";s",
                            ValueCallback { s ->
                                File(f3).printWriter().use {
                                    it.print(
                                        s.replace('"', ' ').replace('\'', ' ').replace("\\n", "\n")
                                            .trim()
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        //下面这些直接复制就好
        webView?.webViewClient = webClient

        val webSettings = webView!!.settings
        webSettings.javaScriptEnabled = true  // 开启 JavaScript 交互
//        webSettings.setAppCacheEnabled(true) // 启用或禁用缓存
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT // 只要缓存可用就加载缓存, 哪怕已经过期失效 如果缓存不可用就从网络上加载数据
//        webSettings.setAppCachePath(cacheDir.path) // 设置应用缓存路径

        // 缩放操作
        webSettings.setSupportZoom(false) // 支持缩放 默认为true 是下面那个的前提
        webSettings.builtInZoomControls = false // 设置内置的缩放控件 若为false 则该WebView不可缩放
        webSettings.displayZoomControls = false // 隐藏原生的缩放控件

        webSettings.blockNetworkImage = false // 阻止图片
        webSettings.loadsImagesAutomatically = true // 自动加载图片

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webSettings.safeBrowsingEnabled = true // 是否开启安全模式
        }

        webSettings.javaScriptCanOpenWindowsAutomatically = false // 支持通过JS打开新窗口
        webSettings.domStorageEnabled = true // 启用或禁用DOM缓存
        webSettings.setSupportMultipleWindows(false) // 设置WebView是否支持多窗口

        // 设置自适应屏幕, 两者合用
        webSettings.useWideViewPort = true  // 将图片调整到适合webview的大小
        webSettings.loadWithOverviewMode = true  // 缩放至屏幕的大小
        webSettings.allowFileAccess = true // 设置可以访问文件

        webSettings.setGeolocationEnabled(false) // 是否使用地理位置

        webView?.fitsSystemWindows = true
        webView?.setLayerType(View.LAYER_TYPE_HARDWARE, null)
//        webView?.loadUrl(WEB_URL)

    }

}