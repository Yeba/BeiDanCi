# BeiDanCi
一个基于网易有道词典的背单词app  

## 支持语言
基于[网易有道词典](https://dict.youdao.com/)，因此支持英语、法语、韩语、日语。

## 单词本
使用前请先导入单词本

格式为：每行一个单词的文本文件

部分单词本来自于https://github.com/mahavivo/english-wordlists

## 逻辑和记忆曲线

每个单词重复3次，都认识则升级

若有一次不认识则该单词的等级清零

记忆曲线偷懒采用斐波那契数列，if(今天-上次背的时间 >= fib[等级])则需复习

## 其他

侵删

欢迎PR

email: yjliu045@stu.suda.edu.cn
