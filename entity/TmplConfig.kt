package com.chdown.toolbox.actions.template.engine.entity

/**
 * 模板参数
 *
 * 内置
 * 日期：#{data}
 * 做着：#{author}
 *
 * 字符串模板
 * 支持 #{xxx}
 * 支持 #{xxx.p}
 * 支持 #{app}
 *
 *  if条件
 *  #{xxx.if(true)}
 *  #{xxx.ifEnd}
 *
 *  switch条件
 *  #{xxx.switch(yyy)}
 *  #{xxx.switchEnd}
 * */

// replace("\\.[a-zA-Z_0-9.(),|]+".toRegex(),"")
var templateRegex = "#\\{[a-zA-Z_0-9.(),|]+}"

var ignoreDir = "build,gradle,idea"

var srcTextIgnore = "ifEnd,switchEnd"