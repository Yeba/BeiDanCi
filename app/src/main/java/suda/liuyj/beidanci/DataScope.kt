package suda.liuyj.beidanci

import android.content.Context
import android.util.Log
import java.io.File
import java.lang.Integer.min
import java.lang.Math.max
import java.lang.Math.round
import java.util.Calendar

private val debug=false
var dir_inner=""
var dir_outter=""

fun loadDataSP(file: String, k: String, ctx: Context): String? {
    val sp = ctx.getSharedPreferences(file, Context.MODE_PRIVATE)
    return sp.getString(k, null)
}
fun saveDataSP(file: String, k: String, v: String, ctx: Context) {
    val sp = ctx.getSharedPreferences(file, Context.MODE_PRIVATE)
    val edit = sp.edit()
    edit.putString(k, v)
    edit.apply()
}

fun timeMillis_Day(last: Long): Long {
    return (System.currentTimeMillis() - last) / 1000 / 60 / 60 / 24
}
fun timeMillis_Now(): Long { return System.currentTimeMillis()}

enum class DataFiles {
    Sys,
    Book, // [name]=name, [word]="a\nb\nc..."
    WordState // lang, k=word
}
enum class LearnMode{New,Old,Mix}
enum class LearnStateOnce{Bad,Ok,Good,None}
data class MyWord(val word:String,var date:Long,var state:Int)
private val fib = listOf(0,1, 2, 3, 5, 8, 13, 21, 34, 55, 89,144,)
private const val levelLearned=5
var theBook:MyBook= MyBook()
const val ALPHA="QWERTYUIOPASDFGHJKLZXCVBNM"
const val TheEndOfBook="!@#$!~D:>:235给会54wsdg👍☀FWfb.;984*@#B"

fun getPronounce(ctx: Context):Int{
    var s = loadDataSP(DataFiles.Sys.name, "Pronounce", ctx) ?: "0"
    if (s=="") s="0"
    return s.toInt()
}
fun setPronounce(i:Int,ctx: Context){
    saveDataSP(DataFiles.Sys.name, "Pronounce", i.toString(), ctx)
}

fun getLang(ctx: Context):String{
    var s = loadDataSP(DataFiles.Sys.name, "lang", ctx) ?: ""
    if (s=="") s="en"
    return s
}
fun setLang(s:String,ctx: Context){
    saveDataSP(DataFiles.Sys.name, "lang", s, ctx)
}

fun getTodayLearn(ctx: Context):Int{
    val calendar = Calendar.getInstance()
    val year=calendar.get(Calendar.YEAR)
    val month=calendar.get(Calendar.MONDAY)
    val dayOfMonth=calendar.get(Calendar.DAY_OF_MONTH)
    val weekOfYear=calendar.get(Calendar.WEEK_OF_YEAR)
    val dayOfWeek=calendar.get(Calendar.DAY_OF_WEEK)
    val dayS="${year},${month},${dayOfMonth},${dayOfWeek},${weekOfYear}"
    val data=loadDataSP(DataFiles.Sys.name, "leanNum_$dayS", ctx) ?: "0"
    return data.toInt()
}
fun incTodayLearn(ctx:Context){
    val calendar = Calendar.getInstance()
    val year=calendar.get(Calendar.YEAR)
    val month=calendar.get(Calendar.MONDAY)
    val dayOfMonth=calendar.get(Calendar.DAY_OF_MONTH)
    val weekOfYear=calendar.get(Calendar.WEEK_OF_YEAR)
    val dayOfWeek=calendar.get(Calendar.DAY_OF_WEEK)
    val dayS="${year},${month},${dayOfMonth},${dayOfWeek},${weekOfYear}"
    val data=loadDataSP(DataFiles.Sys.name, "leanNum_$dayS", ctx) ?: "0"
    val num= data.toInt()+1
    saveDataSP(DataFiles.Sys.name, "leanNum_$dayS", num.toString(), ctx)
}

fun _importBook(name:String, txt:String, ctx:Context){
    saveDataSP( DataFiles.Book.name,"name",name,ctx)
    saveDataSP( DataFiles.Book.name,"word",txt,ctx)
}
fun importBook(path: String, ctx: Context) {
    val name = path.split('/').last().split('.')[0]
    if(debug) Log.e("dataScope","import:$path, name:$name")
    try {
        val s = File(path).readText()
        theBook=MyBook()
        _importBook(name,s,ctx)
        theBook.load(ctx)
        toast("导入成功",1,ctx)
    }catch (e:Exception){
        var err="\n$e"
        for (s in e.stackTrace) err+="\n$s"
        toast("发生错误，请检查单词本"+err,7,ctx)
        Log.e("dataScope","import error$err")
    }
}

var learnMode:LearnMode=LearnMode.Mix
private val batch_size = 8 // 每批次单词数
private val times_pre_learn = 3 //每个单词学习几遍以升级

class MyBook() {
    private var name:String=""
    private var words = mutableListOf<MyWord>()
    private var idx = mutableListOf<Int>() // 乱序扫描
    private var batch = mutableListOf<Int>() // 一个批次的单词
    private var batch_count = mutableListOf<Int>() // 一个批次的学习次数
    private var batch_i: Int = 0
    private var lang="en"

    fun nextBatch() {
        batch_i = 0
        batch.clear()
        batch_count.clear()
        idx=idx.shuffled().toMutableList()
        idx=idx.shuffled().toMutableList()

        var zero=0.01
        for (w in words)if (w.state==0)zero+=1

        var new_pre_batch = round((zero/words.size)* batch_size).toInt() // 每批新学数量
        if (learnMode== LearnMode.New) new_pre_batch= batch_size
        if (learnMode==LearnMode.Old) new_pre_batch=0

        if(debug) Log.e("dataScope", "mode ${learnMode.name}, new_pre_batch:$new_pre_batch")
        if(debug) for (i in idx)Log.e("dataScope","word ${words[i].word}, state ${words[i].state}, day ${timeMillis_Day(words[i].date)}")
        for (i in idx) {//复习
            if (batch.size >= batch_size - new_pre_batch) break
            if (words[i].state <= 0) continue
            if (words[i].state > 0 && timeMillis_Day(words[i].date) >= fib[words[i].state]) batch.add(i)
        }
        var new = min( batch_size - batch.size,new_pre_batch)
        if(debug) Log.e("dataScope","nextNatch size ${batch.size} (strong old), new $new")
        for (i in idx) {//若 绝对超出时间的 都复习完了，学新词
            if (new <= 0) break
            if (words[i].state == 0) {//new
                batch.add(i)
                new--
            }
        }
        if(debug) Log.e("dataScope","nextNatch size ${batch.size} (strong old + new),  soft old ${batch_size-batch.size}")
        var level=1 // 如果新词也学完了，复习软超过时间的
        var bk=false
        while (true){
            for (i in idx){
                if (batch.size >= batch_size || level>=fib.size) {
                    bk=true
                    break
                }
                if (words[i].state==level)batch.add(i)
            }
            if (bk)break
            else level++
        }

        for (i in batch)batch_count.add(0)
        //如果没新词也没旧词，batch应为空，next中提升结束
    }

    //ok:  -1 忘了  0 删除（等级+1） 1 过
    fun next(ok: LearnStateOnce, ctx: Context): String {
        // 修改state
        if (batch.size > 0) {
            if (ok == LearnStateOnce.Bad) {
                words[batch[batch_i]].state = 0
                batch_count[batch_i] = 0
                saveWord(words[batch[batch_i]], ctx)
            } else if (ok == LearnStateOnce.Good) {
                batch_count[batch_i] = times_pre_learn
                words[batch[batch_i]].state =max(words[batch[batch_i]].state+1, levelLearned)
                words[batch[batch_i]].date = timeMillis_Now()
                saveWord(words[batch[batch_i]], ctx)
            } else if (ok == LearnStateOnce.Ok) {
                batch_count[batch_i] += 1
                if (batch_count[batch_i] == times_pre_learn) {
                    words[batch[batch_i]].state =min(words[batch[batch_i]].state+1, fib.size-1)
                    words[batch[batch_i]].date = timeMillis_Now()
                    incTodayLearn(ctx)
                    saveWord(words[batch[batch_i]], ctx)
                }
            }
            //从batch取一个
            var j = batch_i
            do {
                j = (j + 1) % batch.size
                if (j == batch_i) break
                if (batch_count[j] < times_pre_learn) {
                    batch_i = j
                    if(debug) Log.e("dataScope","next, select word ${words[batch[batch_i]].word}")
                    return words[batch[batch_i]].word
                }
            } while (true)
        }
        nextBatch()
        if(debug) Log.e("dataScope","next, new batch size ${batch.size}, ${batch_count.size}, ${batch_i}")
        if(debug) for (e in batch)Log.e("dataScope","next, new batch item ${words[e].word}")
        if (batch.size==0){
            toast("恭喜你已经学完了\n休息一下或者修改学习计划吧",5,ctx)
            return TheEndOfBook
        }
        return words[batch[batch_i]].word
    }
    fun name():String{return name}
    fun info(): String {
        var unknown = 0
        var learing = 0
        var know = 0
        for (wd in words) {
            if (wd.state == 0) unknown++
            else if (wd.state >= levelLearned) know++
            else learing++
        }
        return "熟悉：${know}，在学：${learing}，未学：${unknown}，共：${words.size}"
    }
    fun len():Int{return words.size}
    fun _load(txt: String, ctx: Context){
        words.clear()
        idx.clear()
        batch.clear()
        for (line in txt.split('\n')) {
            if (line =="" || line.isNotEmpty() && line[0]==' ') continue
            val wordStateIn = loadDataSP(DataFiles.WordState.name+lang, line, ctx) ?: "0\t0"
            val stateIn = wordStateIn.split('\t')
            var level=stateIn[0].toInt()
            var tim=stateIn[1].toLong()
            var name=line
            if ('\t' in line){
                val ss=line.split('\t')
                name=ss[0]
                level=ss[1].toInt()
                tim=ss[2].toLong()
            }
            val myWd=MyWord(name,tim,level)
            idx.add(this.words.size)
            this.words.add(myWd)
        }
    }
    fun load(ctx: Context) {
        lang= getLang(ctx)
        name = loadDataSP(DataFiles.Book.name, "name", ctx) ?: return
        val wordS=loadDataSP(DataFiles.Book.name, "word", ctx) ?: return
        _load(wordS,ctx)
    }
    fun saveWord(w:MyWord,ctx: Context){
        saveDataSP(DataFiles.WordState.name+lang, w.word, "${w.state}\t${w.date}", ctx)
    }
    fun backup():String{
        val path=dir_outter+File.separator+ this.name+".beidanci"
        File(path).printWriter().use { f->
            for (wd in words){
                f.println("${wd.word}\t${wd.state}\t${wd.date}")
            }
        }
        return path
    }

}

