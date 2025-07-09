package com.chdown.toolbox.actions.template.engine.entity

import com.intellij.ui.components.JBTextField
import java.io.File

class TmplSrcFile(
    // 源文件路径
    var srcFile: File,
    // 目标保存路径
    var dstDirPath: String
) {
    var input: JBTextField? = null
}