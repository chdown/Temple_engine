package com.chdown.toolbox.actions.template.engine.core

import cn.hutool.core.util.ReUtil
import com.chdown.toolbox.actions.template.engine.entity.TmplEnum
import com.chdown.toolbox.actions.template.engine.entity.srcTextIgnore
import com.chdown.toolbox.actions.template.engine.entity.templateRegex
import java.io.File

object TmplSrcUtils {

    /** 字符串是否为模板语法 */
    fun isMatchTmpl(srcStr: String) = templateRegex.toRegex().matches(srcStr)

    /** 获取语法的Key */
    fun getText(srcStr: String) = srcStr.replace("#{", "").replace("}", "")

    /** 获取语法的Key */
    private fun getKey(srcStr: String) = getText(srcStr).split(".")[0]

    fun getMethodValue(srcStr: String) = getText(srcStr).split(".")[1]
            .replace("switch(", "")
            .replace("switchEnd(", "")
            .replace("if(", "")
            .replace("ifEnd(", "")
            .replace(")", "")

    /** 获取枚举类型 */
    fun getEnum(values: MutableList<String>): TmplEnum {
        if (values.isEmpty()) return TmplEnum.PARAM
        if (values[0].contains(".")) {
            val valueStr = values[0].split(".")[1]
            if (valueStr.lowercase().startsWith("if")) return TmplEnum.IF
            if (valueStr.lowercase().startsWith("switch")) return TmplEnum.SWITCH
        }
        return TmplEnum.PARAM
    }

    /** 获取参数 */
    fun getParamMap(content: String): MutableMap<String, MutableSet<String>> {
        val map = mutableMapOf<String, MutableSet<String>>()
        val textList = ReUtil.findAll(templateRegex, content, 0)
        textList.forEach { srcStr ->
            val key = getKey(srcStr)
            if (!map.containsKey(key)) map[key] = mutableSetOf()
            val srcText = getText(srcStr)
            if (!srcTextIgnore.contains(srcText)) map[key]?.add(srcText)
        }
        return map
    }

    /** 获取没有参数的文件名 */
    fun getFileName(file: File) = file.nameWithoutExtension.replace("#{", "").replace("\\.[a-z]+}".toRegex(), "")
}