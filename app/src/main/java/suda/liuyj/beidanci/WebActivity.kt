package suda.liuyj.beidanci

import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import suda.liuyj.beidanci.databinding.ActivityWebBinding


class WebActivity : AppCompatActivity() {
    private var _coverView:View?=null
    private var webView: WebView? = null
    private var urlSuffixAudio=""
    private var urlSuffixWeb=""

//    private val _words= listOf("hello","world","fun")
//    private var _wdidx=0
    fun next(ok:LearnStateOnce=LearnStateOnce.None){
//        _wdidx=(_wdidx+1)%_words.size
//        val word=_words[_wdidx]
        val word= theBook.next(ok,applicationContext)
        if (word== TheEndOfBook) {
            toast("已完成可学习任务，请更换任务或更换单词本",3,applicationContext)
            this.finish()
        }
        this.title=word
        if(_coverView!=null) _coverView!!.visibility= View.VISIBLE
        webView?.loadUrl("https://dict.youdao.com/m/result?word=$word&$urlSuffixWeb")
        playAudioAsync("https://dict.youdao.com/dictvoice?audio=$word&$urlSuffixAudio")
    }

    fun clickCover(view: View){
        view.visibility=View.INVISIBLE
        if(_coverView == null)_coverView=view
    }
    fun clickOk(view: View){ next(LearnStateOnce.Ok) }
    fun clickBad(view:View){ next(LearnStateOnce.Bad) }
    fun clickGood(view: View){ next(LearnStateOnce.Good) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)
        val _binding = ActivityWebBinding.inflate(layoutInflater)
        webView= _binding.webWeb
        webInit()
    }

    override fun onResume() {
        super.onResume()
        if (theBook.name()==""||theBook.len()==0){
            toast("单词本为空，请导入",5,applicationContext)
            Log.e("web","id<0 or size =0")
            finish()
            return
        }
        urlSuffixAudio="type=${getPronounce(application)+1}" // type 1 英式 2 美式
        urlSuffixWeb="lang=${getLang(application)}"
        next()
    }

    fun playAudioAsync(url:String){
        val mediaPlayer=MediaPlayer()
        mediaPlayer.setDataSource(url)
        mediaPlayer.prepareAsync()
        mediaPlayer.setOnPreparedListener {
            it.start()
        }
        mediaPlayer.setOnCompletionListener {
            it.stop()
            it.release()
        }
    }

    fun webInit(){
        webView = findViewById(R.id.web_web)
//        webView.loadUrl()

        val webClient = object : WebViewClient(){
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }
        }

        //下面这些直接复制就好
        webView?.webViewClient=webClient

        val webSettings = webView!!.settings
        webSettings.javaScriptEnabled = true  // 开启 JavaScript 交互
//        webSettings.setAppCacheEnabled(true) // 启用或禁用缓存
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT // 只要缓存可用就加载缓存, 哪怕已经过期失效 如果缓存不可用就从网络上加载数据
//        webSettings.setAppCachePath(cacheDir.path) // 设置应用缓存路径

        // 缩放操作
        webSettings.setSupportZoom(false) // 支持缩放 默认为true 是下面那个的前提
        webSettings.builtInZoomControls = false // 设置内置的缩放控件 若为false 则该WebView不可缩放
        webSettings.displayZoomControls = false // 隐藏原生的缩放控件

        webSettings.blockNetworkImage = false // 禁止或允许WebView从网络上加载图片
        webSettings.loadsImagesAutomatically = true // 支持自动加载图片

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
        webView?.setLayerType(View.LAYER_TYPE_HARDWARE,null)
//        webView?.loadUrl(WEB_URL)

    }

}