package suda.liuyj.beidanci.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import suda.liuyj.beidanci.LearnMode
import suda.liuyj.beidanci.WebActivity
import suda.liuyj.beidanci.databinding.FragmentHomeBinding
import suda.liuyj.beidanci.getTodayLearn
import suda.liuyj.beidanci.learnMode
import suda.liuyj.beidanci.theBook

fun startWeb(ctx: Context) {
    val intent = Intent(ctx, WebActivity::class.java)
    theBook.nextBatch()
    ctx.startActivity(intent)
}

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // action of buttons
        binding.mainButStartNew.setOnClickListener {
            learnMode = LearnMode.New
            context?.let { it1 -> startWeb(it1) }
        }
        binding.mainButStartOld.setOnClickListener {
            learnMode = LearnMode.Old
            context?.let { it1 -> startWeb(it1) }
        }
        binding.mainButStartMix.setOnClickListener {
            learnMode = LearnMode.Mix
            context?.let { it1 -> startWeb(it1) }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        // 刷新主页数据
        val t1: TextView = binding.mainTxt1
        val t2: TextView = binding.mainTxt2
        val t3: TextView = binding.mainTxt3
        context?.let {
            t1.text = theBook.name()
            t2.text = "今日学习 ${getTodayLearn(it)} 词"
            t3.text = theBook.info()
        }
    }
}
