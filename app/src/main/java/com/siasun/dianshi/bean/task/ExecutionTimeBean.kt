package com.siasun.dianshi.bean.task

import com.jeremyliao.liveeventbus.core.LiveEvent

/**
 * @Author: CheFuX1n9
 * @Date: 2024/4/15 11:10
 * @Description: 任务执行时间Bean
 */
data class ExecutionTimeBean(
    var time: String = "",
    val week: MutableList<String> = mutableListOf()
) : LiveEvent