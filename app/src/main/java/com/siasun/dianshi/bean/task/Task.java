package com.siasun.dianshi.bean.task;

import com.jeremyliao.liveeventbus.core.LiveEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by changjian.song on 2021/12/9 at 16:43
 *
 * @description ：任务
 */

//"task_id",
public class Task implements LiveEvent {

    public String task_name;            //任务名称，只用于UI显示
    public int cycle;                   //任务循环次数
    public int if_clean_back;           //是否回扫，是：1；否：0；默认为是：1
    public int allClean;           //0：按区域清扫，1：全部清扫
    public List<Sub_Task> sub_task;
    //任务模式 1普通任务 2定时任务
    public int mTaskMode = 1;
    //任务唯一标识
    public int mTaskFlag = 0;
    public int return_home;//是否回站
    public List<ExecutionTimeBean> executionTimeBeans = new ArrayList<>();
    public List<ExecutionTimeBean> finishTimeBeans = new ArrayList<>();

    public Task() {
    }

    @Override
    public String toString() {
        return "Task{" +
                "task_name='" + task_name + '\'' +
                ", cycle=" + cycle +
                ", if_clean_back=" + if_clean_back +
                ", allClean=" + allClean +
                ", sub_task=" + sub_task +
                ", mTaskMode=" + mTaskMode +
                ", mTaskFlag=" + mTaskFlag +
                ", executionTimeBeans=" + executionTimeBeans +
                ", finishTimeBeans=" + finishTimeBeans +
                '}';
    }
}