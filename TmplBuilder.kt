package com.chdown.toolbox.actions.template.engine

import cn.hutool.core.date.DateUtil
import com.chdown.toolbox.actions.template.engine.core.TmplDstUtils
import com.chdown.toolbox.actions.template.engine.core.TmplSrcUtils
import com.chdown.toolbox.actions.template.engine.entity.TmplScriptItem
import com.chdown.toolbox.actions.template.engine.entity.TmplSrcFile
import com.chdown.toolbox.actions.template.engine.entity.ignoreDir
import com.chdown.toolbox.extensions.setting.SettingState
import com.chdown.toolbox.utils.ProjectUtils
import com.chdown.toolbox.utils.ext.*
import com.chdown.toolbox.utils.writePsiFile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.io.FileFilter
import javax.swing.JOptionPane

class TmplBuilder {
    lateinit var mModule: Module
    val mTmplFileList = mutableListOf<TmplSrcFile>() // 处理的文件集合
    val mTmplSrcMap = mutableMapOf<String, MutableSet<String>>()// 原始参数集合
    val mTmplUserMap = mutableMapOf<String, String>() // 用户参数集合
    private val mTmplScriptList = mutableListOf<TmplScriptItem>()// 用户脚本集合
    private var mProjectEnum = ProjectEnum.Other
    private var mModulePath = ""

    companion object {
        fun builder(): TmplBuilder {
            return TmplBuilder()
        }
    }

    fun init(tmplPath: String, virtualFile: VirtualFile, module: Module): TmplBuilder {
        mTmplFileList.clear()
        mTmplSrcMap.clear()
        mTmplScriptList.clear()
        mModule = module
        mProjectEnum = module.project.getProjectEnum()
        mModulePath = ProjectUtils.getModulePath(virtualFile.path.toFile());
        // select
        val tmplSelectFile = "${tmplPath}/select".toFileNull()
        if (tmplSelectFile != null) recursionSelectDir(tmplSelectFile, tmplSelectFile.absolutePath, virtualFile.path)
        // match
        val tmplMatchFileList = "${tmplPath}/match".toFileNull()?.listFiles(FileFilter { !(it.isFile && it.name.startsWith(".")) })
        if (!tmplMatchFileList.isNullOrEmpty()) {
            recursionSearch(tmplMatchFileList.toMutableList(), mModulePath.toFile())
        }
        // script.json
        parseScriptJson(tmplPath)
        return this
    }

    fun parseParams(): TmplBuilder {
        // 解析参数
        mTmplFileList.forEach { tmplSrcFile ->
            mTmplSrcMap.putAllUpdate(TmplSrcUtils.getParamMap(tmplSrcFile.srcFile.nameWithoutExtension))
            mTmplSrcMap.putAllUpdate(TmplSrcUtils.getParamMap(tmplSrcFile.srcFile.readText()))
        }
        // 解析script中的参数
        mTmplScriptList.forEach { textConfig ->
            mTmplSrcMap.putAllUpdate(TmplSrcUtils.getParamMap(textConfig.text))
        }
        return this
    }

    fun parseSystemParam(): TmplBuilder {
        // 移除模板参数中的系统内容参数
        if (mTmplSrcMap.contains("date")) mTmplSrcMap.remove("date")
        if (mTmplSrcMap.contains("author")) mTmplSrcMap.remove("author")
        if (mTmplSrcMap.contains("packageName")) mTmplSrcMap.remove("packageName")
        // 增加用户参数
        mTmplUserMap["date"] = DateUtil.now()
        mTmplUserMap["author"] = SettingState.getInstance().author
        mTmplUserMap["packageName"] = ProjectUtils.getPackageName(mModule.project, mModulePath)
        return this
    }


    /**
     * 处理用户数据
     * @param userMap 用户数据集合
     * */
    fun parseUserParam(userMap: MutableMap<String, String> = mutableMapOf()): TmplBuilder {
        if (userMap.isEmpty()) return this
        /// 处理基础用户数据
        mTmplUserMap.putAll(userMap)
        mTmplSrcMap.forEach { (key, values) ->
            if (mTmplUserMap.contains(key)) mTmplUserMap.putAll(TmplDstUtils.getTextMap(key, values, mTmplUserMap[key]!!))
        }
        // 对所有文件，增加导包参数：无正则的文件名.p
        mTmplFileList.forEach { srcFile ->
            val importPath = srcFile.dstDirPath + if (mProjectEnum == ProjectEnum.Flutter) "\\${srcFile.srcFile.name}" else ""
            val importSrc = TmplDstUtils.parseContent(importPath, mTmplUserMap, mTmplSrcMap)
            mTmplUserMap["${TmplSrcUtils.getFileName(srcFile.srcFile)}.p"] = TmplDstUtils.getTextByImport(importSrc)
        }
        return this
    }

    /** 创建内容，执行自己的方法 */
    fun create(show: TmplBuilder.() -> Unit) {
        show.invoke(this)
    }

    /** 创建用户文件 */
    fun builderFile(): TmplBuilder {
        mTmplFileList.forEach { srcFile ->
            val fileName = TmplDstUtils.parseContent(srcFile.srcFile.nameWithoutExtension, mTmplUserMap, mTmplSrcMap) + "." + srcFile.srcFile.extension
            val destPath = TmplDstUtils.parseContent(srcFile.dstDirPath, mTmplUserMap, mTmplSrcMap)
            val dstFile = (destPath + File.separator + fileName).toFile()
            dstFile.parentFile.mkdirs()
            if (dstFile.exists()) {
                val result = JOptionPane.showConfirmDialog(null, "$fileName 已存在，是否替换", "警告", JOptionPane.OK_CANCEL_OPTION)
                if (result != 0) return@forEach
            }
            dstFile.writePsiFile(mModule.project, TmplDstUtils.parseContent(srcFile.srcFile.readText(), mTmplUserMap, mTmplSrcMap))
        }
        return this
    }

    /** 运行用户脚本 */
    fun runUserScript(): TmplBuilder {
        if (mTmplScriptList.isNotEmpty()) {
            val result = JOptionPane.showConfirmDialog(null, "存在动态处理文本文件，是否进行处理", "警告", JOptionPane.OK_CANCEL_OPTION)
            if (result == 0) {
                val group = mTmplScriptList.groupBy { it.path }
                group.forEach { (_, list) ->
                    val textConfigFile = list[0].path.toFile()
                    if (!textConfigFile.exists()) return@forEach
                    var textConfigContent = textConfigFile.readText()
                    mTmplScriptList.forEach { textConfig ->
                        val content = TmplDstUtils.parseContent(textConfig.text, mTmplUserMap, mTmplSrcMap)
                        textConfigContent = textConfigContent.replace(textConfig.oldText, content)
                    }
                    textConfigFile.writePsiFile(mModule.project, textConfigContent)
                }
            }
        }
        return this
    }

    fun getUserInputMap(): MutableMap<String, MutableSet<String>> {
        val inputMap = mutableMapOf<String, MutableSet<String>>()// 原始参数集合
        mTmplSrcMap.forEach { (key, values) -> inputMap[key] = values }
        // 过滤导包语法带来的参数问题：无正则文件名.p
        mTmplFileList.forEach { tmplSrcFile ->
            if (!TmplSrcUtils.isMatchTmpl(tmplSrcFile.srcFile.nameWithoutExtension)) {
                val parseFileName = TmplSrcUtils.getFileName(tmplSrcFile.srcFile)
                if (inputMap.containsKeyIgnoreCase(parseFileName)) inputMap.removeIgnoreCase(parseFileName)
            }
        }
        return inputMap
    }

    /** 解析script.json */
    private fun parseScriptJson(tmplPath: String) {
        val textFile = (tmplPath + File.separator + "script.json").toFile()
        if (textFile.exists()) {
            mTmplScriptList.addAll(Gson().fromJson(textFile.readText(), object : TypeToken<MutableList<TmplScriptItem>>() {}.type))
            mTmplScriptList.forEach { textConfig ->
                textConfig.path = mModulePath + File.separator + textConfig.path
            }
        }
    }

    /** 递归遍历match文件 */
    private fun recursionSearch(tmplMatchFileList: MutableList<File>, mathDirFile: File) {
        mathDirFile.listFiles(FileFilter { !it.name.startsWith(".") && !ignoreDir.contains(it.name, ignoreCase = false) })?.forEach { mathFile ->
            tmplMatchFileList.forEach { tmplFile ->
                // 文件匹配
                if (mathFile.isFile && tmplFile.isFile && mathFile.name.lowercase() == tmplFile.name.lowercase()) {
                    mTmplFileList.add(TmplSrcFile(tmplFile, mathFile.parentFile.absolutePath))
                }
                // 文件夹匹配
                if (mathFile.isDirectory && tmplFile.isDirectory && mathFile.absolutePath.replace("\\", "-").endsWith(tmplFile.name)) {
                    recursionSelectDir(tmplFile, tmplFile.absolutePath, mathFile.absolutePath)
                }
            }
            if (mathFile.isDirectory) {
                recursionSearch(tmplMatchFileList, mathFile)
            }
        }
    }


    /** 递归遍历select文件 */
    private fun recursionSelectDir(tmplSelectFile: File, tmplPath: String, saveDir: String) {
        tmplSelectFile.listFiles(FileFilter { it.isFile })?.forEach { file ->
            // 获取最终文件的保存目录
            val dstDirPath = (saveDir + file.absolutePath.replace(tmplPath, "")).toFile().parentFile.absolutePath
            mTmplFileList.add(TmplSrcFile(file, dstDirPath))
        }
        tmplSelectFile.listFiles(FileFilter { it.isDirectory })?.forEach { file ->
            recursionSelectDir(file, tmplPath, saveDir)
        }
    }
}