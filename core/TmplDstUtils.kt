package com.chdown.toolbox.actions.template.engine.core

import com.chdown.toolbox.actions.template.engine.entity.TmplEnum
import com.chdown.toolbox.utils.ext.l
import com.chdown.toolbox.utils.ext.ls
import com.chdown.toolbox.utils.ext.u
import java.util.regex.Matcher

object TmplDstUtils {

    fun fastContent(content: String, userMap: MutableMap<String, String>): String {
        val tmpUserMap = mutableMapOf<String, String>()
        val srcMap = TmplSrcUtils.getParamMap(content)
        srcMap.keys.forEach { key ->
            if (userMap.containsKey(key)) {
                tmpUserMap.putAll(getTextMap(key, srcMap[key]!!, userMap[key]!!))
            }
        }
        tmpUserMap.putAll(userMap)
       return parseContent(content,tmpUserMap,srcMap)
    }


    fun getTextMap(key: String, values: MutableSet<String>, text: String): MutableMap<String, String> {
        val map = mutableMapOf<String, String>()
        map[key] = text
        values.forEach { value ->
            map[value] = getText(value, text)
        }
        return map
    }


    /** 解析获取模板参数语法 */
    private fun getText(srcStr: String, userValue: String): String {
        val srcText = TmplSrcUtils.getText(srcStr)
        val srcTextArray = srcText.split(".")
        if (srcTextArray.size < 2) return userValue
        val special = srcTextArray[1]
        when (special.lowercase()) {
            "l" -> return userValue.l()// xxx.l
            "u" -> return userValue.u()// xxx.u
            "ls" -> return userValue.ls()// xxx.u
        }
        try {
            if (special.lowercase().startsWith("replace")) {
                val array = special.replace("replace(", "").replace(")", "").split(",")
                return userValue.replace(array[0].toRegex(RegexOption.IGNORE_CASE), array[1])
            }
            if (special.lowercase().startsWith("subString")) {
                val array = special.replace("subString(", "").replace(")", "").split(",")
                return if (array.size > 1) userValue.substring(array[0].toInt(), array[1].toInt()) else userValue.substring(array[0].toInt())
            }
        } catch (ex: Exception) {
            return userValue
        }
        return userValue
    }

    /** 获取包名，处理.p */
    fun getTextByImport(srcPath: String): String {
        val java = "src\\main\\java"
        val kt = "src\\main\\kotlin"
        val flutter = "lib"
        var packageName = ""
        if (srcPath.contains(java)) {
            packageName = srcPath.split(java)[1]
        } else if (srcPath.contains(kt)) {
            packageName = srcPath.split(kt)[1]
        } else if (srcPath.contains(flutter)) {
            packageName = srcPath.split(flutter)[1]
        }
        packageName = if (srcPath.contains(flutter)) {
            packageName.replace("\\", "/").replace("\\.{2,}".toRegex(), "/")
        } else {
            packageName.replace("\\", ".").replace("/", ".").replace("\\.{2,}".toRegex(), "")
        }
        if (packageName.startsWith(".")) packageName = packageName.substring(1, packageName.length)
        if (packageName.endsWith(".")) packageName = packageName.substring(0, packageName.length - 1)
        return packageName
    }

    /**
     * 获取新的内容
     * @param tmpContent 内容温拌
     * @param srcMap 原始数据
     * @param userMap 用户数据
     */
    fun parseContent(tmpContent: String, userMap: MutableMap<String, String>, srcMap: MutableMap<String, MutableSet<String>> = mutableMapOf()): String {
        var content = tmpContent
        /// 处理IF、Switch xxx.if(true)、xxx.ifEnd(true) || xxx.switch(xxx) xxx.switchEnd(xxx)
        if (srcMap.isNotEmpty()) {
            userMap.forEach { (key, value) ->
                if (!srcMap.containsKey(key)) return@forEach
                val enum = TmplSrcUtils.getEnum(srcMap[key]!!.toMutableList())
                if (enum == TmplEnum.IF || enum == TmplEnum.SWITCH) {
                    srcMap[key]?.forEach { srcText ->
                        val endName = srcText.replace(enum.name.lowercase(), "${enum.name.lowercase()}End")
                        val startIndex = content.indexOf("#{${srcText}}")
                        if (startIndex > 0) {
                            // 计算end位置
                            val endIndex = startIndex + content.substring(startIndex).indexOf(endName) + endName.length + 1
                            if (endIndex >= 0) {
                                content = if (value != TmplSrcUtils.getMethodValue(srcText)) {
                                    content.replace(content.substring(startIndex, endIndex), "")
                                } else {
                                    content.replace("#{${srcText}}", "").replaceFirst("#{${endName}}", "")
                                }
                            }
                        }
                    }
                }
            }
        }
        /// 参数处理
        for (entry in userMap) {
            content = content.replace("#\\{${parseOldContent(entry.key)}\\}".toRegex(RegexOption.IGNORE_CASE), Matcher.quoteReplacement(entry.value))
        }
        return content
    }

    /** 处理旧的内容，主要用来处理正则的问题 */
    private fun parseOldContent(oldContent: String): String {
        return oldContent.replace("(", "\\(").replace("（", "\\(").replace(")", "\\)").replace("）", "\\)")
    }
}