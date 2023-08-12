package suda.liuyj.beidanci

import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import suda.liuyj.beidanci.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_setting
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        global_first_run(applicationContext)
        theBook.load(applicationContext)
        // 新建缓存文件夹
        val sp = File.separator
        dir_inner= this.getExternalFilesDir(null)?.path ?: ""
        dir_outter=Environment.getExternalStorageDirectory().absolutePath+sp+"BeiDanCi"
        Log.e("main", "dir_base: $dir_inner, $dir_outter")
        val f1= File("${dir_inner}${sp}cache${sp}")
        if (!f1.exists())f1.mkdirs()
        val f2= File("${dir_outter}${sp}")
        if (!f2.exists())f2.mkdirs()
    }

    fun global_first_run(ctx:Context){
        val first= loadDataSP("Sys","init",ctx)
        if (first==null){
            var s=""
            for (c in ALPHA)s+="$c\n"
            _importBook("测试词本",s,ctx)
        }
        saveDataSP("Sys","init","1",ctx)
    }

}