package suda.liuyj.beidanci.ui

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import suda.liuyj.beidanci.FileUtil
import suda.liuyj.beidanci.checkStorageManagerPermission
import suda.liuyj.beidanci.databinding.FragmentSettingBinding
import suda.liuyj.beidanci.getLang
import suda.liuyj.beidanci.getPronounce
import suda.liuyj.beidanci.importBook
import suda.liuyj.beidanci.openBrowser
import suda.liuyj.beidanci.setClipBoard
import suda.liuyj.beidanci.setLang
import suda.liuyj.beidanci.setPronounce
import suda.liuyj.beidanci.theBook


class SettingFragment : Fragment() {
    val githubUrl="https://github.com/Yeba/BeiDanCi"
    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 15136 && resultCode == RESULT_OK) {
            val uri: Uri? = data?.data
            context?.let {
                val url = uri?.let { it1 -> FileUtil.getPath(it, it1) }
                if (url != null) {
                    if (url!="" && url.endsWith(".txt"))
                        importBook(url,it)
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // 更换词本
        val bookB: Button =binding.setButBok
        bookB.setOnClickListener {
            val mimeTypes = arrayOf("*/*")
//        ACTION_GET_CONTENT：允许用户选择特殊种类的数据，并返回
            //        ACTION_GET_CONTENT：允许用户选择特殊种类的数据，并返回
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            val mimeTypesStr = StringBuilder()
            for (mimeType in mimeTypes) {
                mimeTypesStr.append(mimeType).append("|")
            }
            intent.type = mimeTypesStr.substring(0, mimeTypesStr.length - 1)
            startActivityForResult(Intent.createChooser(intent, "选择文件"), 15136)
        }
        context?.let { it1 -> checkStorageManagerPermission("我需要访问文件权限以导入词本",it1) }

        // 选择语言
        binding.setSelectEnglish.setOnClickListener { selectLang(1) }
        binding.setSelectFa.setOnClickListener { selectLang(2) }
        binding.setSelectHan.setOnClickListener { selectLang(3) }
        binding.setSelectRi.setOnClickListener { selectLang(4) }
        //选择发音
        binding.setSelectYing.setOnClickListener { selectPronounce(1) }
        binding.setSelectMei.setOnClickListener { selectPronounce(2) }
        //单词本仓库1 github
        binding.setTxtBook1.setOnClickListener { context?.let { it1 ->
            openBrowser("$githubUrl/tree/master/books", it1 )
        } }
        //单词本仓库2 蓝奏云  密码:4qf6
        binding.setTxtBook2.setOnClickListener { context?.let { it1 ->
            openBrowser("https://wwph.lanzout.com/b00r8y1vc", it1 )
            setClipBoard("4qf6",it1,"密码:4qf6 已复制到剪切板")
        } }
        //github
        binding.setTxtGithub.setOnClickListener { context?.let { it1 ->
            openBrowser(githubUrl, it1 )
        } }
        // 更新1： github releases页面
        binding.setTxtUpdate1.setOnClickListener { context?.let { it1 ->
            openBrowser("$githubUrl/releases", it1 )
        } }
        // 更新2：蓝奏云 https://wwph.lanzout.com/b00r8y1ta 密码:3iqm
        binding.setTxtUpdate2.setOnClickListener { context?.let { it1 ->
            openBrowser("https://wwph.lanzout.com/b00r8y1ta", it1 )
            setClipBoard("3iqm",it1,"密码:3iqm 已复制到剪切板")
        } }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        context?.let {
            val ctx=it
            //词本
            val bookT:TextView=binding.setTxtBook
            bookT.text="当前词本："+ theBook.name()
            //学习语言
            val lang= getLang(ctx)
            val rd1:RadioButton=binding.setSelectEnglish
            val rd2:RadioButton=binding.setSelectFa
            val rd3:RadioButton=binding.setSelectHan
            val rd4:RadioButton=binding.setSelectRi
            rd1.isChecked= lang=="en"
            rd2.isChecked= lang=="fr"
            rd3.isChecked= lang=="ko"
            rd4.isChecked= lang=="ja"
            //发音
            val pron= getPronounce(ctx)
            val rd5:RadioButton=binding.setSelectYing
            val rd6:RadioButton=binding.setSelectMei
            rd5.isChecked= pron==0
            rd6.isChecked= pron==1
            val pronRoot:View=binding.setPronounceRoot
            pronRoot.visibility=if (lang=="en") View.VISIBLE else View.INVISIBLE
        }
    }


    fun selectLang(id:Int){
        //保存设置状态
        var lang=""
        if (id==1){
            lang="en"
        }else if(id==2){
            lang="fr"
        }else if(id==3){
            lang="ko"
        }else if(id==4){
            lang="ja"
        }else return
        context?.let { setLang(lang, it) }
        // 刷新UI
        onResume()
    }
    fun selectPronounce(id:Int){
        //保存设置状态
        if(!(id==1||id==2))return
        context?.let { setPronounce(id-1, it) }
        // 刷新UI
        onResume()
    }
}