package com.siasun.dianshi.bean.RC;

import java.io.Serializable;

/**
 * Created by changjian.song on 2022/5/7 at 9:36
 *
 * @description ：从RC接收数据结构体，现只用于建图实时显示和建图优化
 */
public class RCData implements Serializable {
    //表示RC发送给UI的通道
    public  static final String CHANNEL_RC2UI = "RC2UI";
    //表示机器人控制通道
    public  static final String CHANNEL_ROBOT_CTRL = "ROBOT_CTRL";
    //表示与视觉交互通道
    public  static final String CHANNEL_VISION = "VISION_INTERACTION";

    //表示位姿通道
    public  static final String CHANNEL_POSE = "POSE";

    public  static final String CHANNEL_SYSTEST = "SYSTEST";

    public static final String CHANNEL_CREATE_MAP = "CREATE_MAP";
    public static final String CHANNEL_CORRECT_MAP = "CORRECT_MAP";
    public static final String CHANNEL_UPDATE_MAP = "UPDATE_MAP";



    //表示RC状态返回
    public static final int RC_BACK_STATUS = 1;
    //表示RC反馈错误日志
    public static final int RC_BACK_ERROR = 2;
    //表示RC反馈可以标定
    public static final int RC_BACK_AXLE_MARK = 3;
    //表示发送给遥控器人脸识别摄像头开关状态
    public static final int RC_BACK_FACE_DETECTION_BTN_STATUS = 4;
    //表示作业重载状态 1成功
    public static final int RC_BACK_TASK_RELOAD_STATUS = 5;
    //表示本体UI反馈连续识别状态
    public static final int UI_BACK_CONTINUOUS_ARS_STATUS = 6;
    //表示定位结果
    public static final int RC_BACK_LOCATION_RESULT = 7;
    //表示反馈导航状态
    public static final int RC_BACK_NAVIGATION_STATUS = 8;
    //表示反馈电池电量
    public static final int RC_BACK_BETTERY_DATA = 9;
    //表示RC是否启动状态
    public static final int RC_START_STATUS = 10;
    //表示RC反馈的作业XML
    public static final int RC_BACK_TASK_XML = 11;
    //表示RC反馈的各轴数值
    public static final int RC_BACK_AXLE_VALUE = 12;
    //表示RC反馈的参数
    public static final int RC_BACK_PARAM = 13;
    //表示RC反馈的底层控制板详细子错误
    public static final int RC_BACK_SUBQUESTION = 14;
    //表示RC反馈的底层板卡版本号
    public static final int RC_BACK_BOARD_VERSION = 15;
    //表示RC反馈的TCPARM手臂值
    public static final int RC_BACK_TCP_ARM_VALUE = 16;


    //表示机器人系统退出指令
    public static final int RC_BACK_SYSTEM_QUITE = 17;
    //表示反馈导航地图列表
    public static final int NAVI_MAP_LIST = 18;


    //表示接收视觉图片 Mecanum
    public static final int VISION_PIC_MECANUM = 19;


    public static final int SYSTEST_Opensystem=20;//系统开关
    public static final int SYSTEST_Functiontest=21;//功能测试
    public static final int SYSTEST_stability=22;//稳定性测试

    //清扫任务2017-8-8 14：35
    public static final int RC_BACK_CLEANJOB_INFO = 23;
    public static final int RC_BACK_CLEANJOB_STATUS = 24;
    public static final int RC_BACK_CLEANJOB_TIMER = 25;
    public static final int RC_BACK_LIBRARYJOB_INFO = 26;
    public static final int RC_BACK_MODIFY_TASK_NAME_INFO = 27;
    public static final int RC_BACK_LIBRARY_TASK_STATUS = 28;

    //更新地图需求2021-5-17
    public static final int RC_BACK_UPDATE_MAP = 29;


    //协议类型
    private int type;
    //协议通道
    private String chanel;


    //RC反馈状态对象
    private RCstatusback rcstatus;
    //RC反馈错误
    private int i_error_level;
    //RC 反馈错误内容
    private byte[] b_error_content;
    //RC反馈人脸识别摄像头开关状态  0关 1开
    private int i_face_detection_btn_status;
    //RC反馈单轴手臂标定状态
    private int i_axle_mark_status;
    //RC反馈标定结果
    private RCAxleMark rcaxlemark;
    //RC反馈作业重载状态
    private int i_task_reload;
    //本体UI 反馈连续识别状态
    private int i_continuous_asr_status;
    //RC反馈定位结果
    private int i_location_result;
    //反馈导航状态
    private int i_navigation_status;
    //反馈电池电量
    private int i_bettery_data;
    //反馈RC启动状态
    private int i_rcstart_status;
    //RC反馈的作业XML
    private String s_task_xml;
    //RC反馈各轴数值
    private double[] d_axle_value;

    //RC反馈参数类型
    private int i_rc_param_type;
    //RC反馈参数 double
    private double[] d_rc_param;
    //RC反馈参数 string
    private String[] s_rc_param;
    //RC反馈参数 byte
    private byte[] b_rc_param;

    //RC反馈底层控制板详细子错误
    private double[] d_rc_sub_question;
    //RC反馈底层板卡版本号
    private double[] d_rc_board_version;

    //RC反馈tcparm手臂值
    private double[] d_rc_tcp_arm_value;
    //RC反馈tcparm手臂类型
    private int i_rc_tcp_arm_type;
    //RC反馈tcparm手臂特征参数
    private byte[] b_rc_tcp_arm_value;


    //返回导航地图列表
    private String[] s_navi_map_list;



    //返回视觉图片Mecanum
    private byte[] b_vision_pic_mecanum;
    //返回视觉图片是否被采集到
    private int i_vision_pic_mecanum_status;


    //接收位姿数据
    private double[] d_pose_value;

    private double[] d_systest_status;
    private int i_systest_status;

    //清扫任务信息
    private double[] d_cleanjob_info;
    private byte[] i_cleanjob_info;
    private byte[] b_cleanjob_name;

    //清扫状态
    private byte[] b_cleanjob_status;
    //清扫定时
    private byte[] b_cleanjob_timer;
    private double[] d_cleanjob_timer;
    private byte[] i_libraryjob_info;
    private byte[] i_libraryjob_status;
    public float f_scan_id;
    private float[] f_create_map_data;
    private float[] f_map_info;
    private float[] f_correct_map_data;

    //更新地图
    private byte[] i_updatemap_info;





    public double[] getD_cleanjob_info() {
        return d_cleanjob_info;
    }
    public void setD_cleanjob_info(double[] d_cleanjob_info) {
        this.d_cleanjob_info = d_cleanjob_info;
    }
    public byte[] getI_cleanjob_info() {
        return i_cleanjob_info;
    }
    public void setI_cleanjob_info(byte[] I_cleanjob_info) {
        this.i_cleanjob_info = I_cleanjob_info;
    }
    public byte[] getI_libraryjob_info() {
        return this.i_libraryjob_info;
    }
    public void setI_libraryjob_info(byte[] i_libraryjob_info) {
        this.i_libraryjob_info = i_libraryjob_info;
    }
    public byte[] getI_libraryjob_status() {
        return this.i_libraryjob_status;
    }
    public void setI_libraryjob_status(byte[] i_libraryjob_status) {
        this.i_libraryjob_status = i_libraryjob_status;
    }
    public byte[] getB_cleanjob_name() {
        return b_cleanjob_name;
    }
    public void setB_cleanjob_name(byte[] b_cleanjob_name) {
        this.b_cleanjob_name = b_cleanjob_name;
    }
    public byte[] getB_cleanjob_status() {
        return b_cleanjob_status;
    }
    public void setB_cleanjob_status(byte[] b_cleanjob_status) {
        this.b_cleanjob_status = b_cleanjob_status;
    }
    public byte[] getB_cleanjob_timer() {
        return b_cleanjob_timer;
    }
    public void setB_cleanjob_timer(byte[] b_cleanjob_timer) {
        this.b_cleanjob_timer = b_cleanjob_timer;
    }
    public double[] getD_cleanjob_timer() {
        return d_cleanjob_timer;
    }
    public void setD_cleanjob_timer(double[] d_cleanjob_timer) {
        this.d_cleanjob_timer = d_cleanjob_timer;
    }

    public double[] getD_systest_status() {
        return d_systest_status;
    }
    public void setD_systest_status(double[] d_systest_status) {
        this.d_systest_status = d_systest_status;
    }
    public int getI_systest_status() {
        return i_systest_status;
    }
    public void setI_systest_status(int i_systest_status) {
        this.i_systest_status = i_systest_status;
    }


    public double[] getD_pose_value() {
        return d_pose_value;
    }
    public void setD_pose_value(double[] d_pose_value) {
        this.d_pose_value = d_pose_value;
    }


    public int getI_vision_pic_mecanum_status() {
        return i_vision_pic_mecanum_status;
    }
    public void setI_vision_pic_mecanum_status(int i_vision_pic_mecanum_status) {
        this.i_vision_pic_mecanum_status = i_vision_pic_mecanum_status;
    }
    public byte[] getB_vision_pic_mecanum() {
        return b_vision_pic_mecanum;
    }
    public void setB_vision_pic_mecanum(byte[] b_vision_pic_mecanum) {
        this.b_vision_pic_mecanum = b_vision_pic_mecanum;
    }


    public String[] getS_navi_map_list() {
        return s_navi_map_list;
    }
    public void setS_navi_map_list(String[] s_navi_map_list) {
        this.s_navi_map_list = s_navi_map_list;
    }


    public double[] getD_rc_tcp_arm_value() {
        return d_rc_tcp_arm_value;
    }
    public void setD_rc_tcp_arm_value(double[] d_rc_tcp_arm_value) {
        this.d_rc_tcp_arm_value = d_rc_tcp_arm_value;
    }
    public int getI_rc_tcp_arm_type() {
        return i_rc_tcp_arm_type;
    }
    public void setI_rc_tcp_arm_type(int i_rc_tcp_arm_type) {
        this.i_rc_tcp_arm_type = i_rc_tcp_arm_type;
    }
    public byte[] getB_rc_tcp_arm_value() {
        return b_rc_tcp_arm_value;
    }
    public void setB_rc_tcp_arm_value(byte[] b_rc_tcp_arm_value) {
        this.b_rc_tcp_arm_value = b_rc_tcp_arm_value;
    }



    public double[] getD_rc_board_version() {
        return d_rc_board_version;
    }
    public void setD_rc_board_version(double[] d_rc_board_version) {
        this.d_rc_board_version = d_rc_board_version;
    }


    public double[] getD_rc_sub_question() {
        return d_rc_sub_question;
    }
    public void setD_rc_sub_question(double[] d_rc_sub_question) {
        this.d_rc_sub_question = d_rc_sub_question;
    }


    public int getI_rc_param_type() {
        return i_rc_param_type;
    }
    public void setI_rc_param_type(int i_rc_param_type) {
        this.i_rc_param_type = i_rc_param_type;
    }
    public double[] getD_rc_param() {
        return d_rc_param;
    }
    public void setD_rc_param(double[] d_rc_param) {
        this.d_rc_param = d_rc_param;
    }
    public String[] getS_rc_param() {
        return s_rc_param;
    }
    public void setS_rc_param(String[] s_rc_param) {
        this.s_rc_param = s_rc_param;
    }
    public byte[] getB_rc_param() {
        return b_rc_param;
    }
    public void setB_rc_param(byte[] b_rc_param) {
        this.b_rc_param = b_rc_param;
    }



    public double[] getD_axle_value() {
        return d_axle_value;
    }
    public void setD_axle_value(double[] d_axle_value) {
        this.d_axle_value = d_axle_value;
    }


    public String getS_task_xml() {
        return s_task_xml;
    }
    public void setS_task_xml(String s_task_xml) {
        this.s_task_xml = s_task_xml;
    }

    public int getI_rcstart_status() {
        return i_rcstart_status;
    }
    public void setI_rcstart_status(int i_rcstart_status) {
        this.i_rcstart_status = i_rcstart_status;
    }


    public int getI_bettery_data() {
        return i_bettery_data;
    }
    public void setI_bettery_data(int i_bettery_data) {
        this.i_bettery_data = i_bettery_data;
    }


    public int getI_navigation_status() {
        return i_navigation_status;
    }
    public void setI_navigation_status(int i_navigation_status) {
        this.i_navigation_status = i_navigation_status;
    }


    public int getI_location_result() {
        return i_location_result;
    }
    public void setI_location_result(int i_location_result) {
        this.i_location_result = i_location_result;
    }


    public int getI_continuous_asr_status() {
        return i_continuous_asr_status;
    }
    public void setI_continuous_asr_status(int i_continuous_asr_status) {
        this.i_continuous_asr_status = i_continuous_asr_status;
    }


    public int getI_task_reload() {
        return i_task_reload;
    }
    public void setI_task_reload(int i_task_reload) {
        this.i_task_reload = i_task_reload;
    }


    public int getI_face_detection_btn_status() {
        return i_face_detection_btn_status;
    }
    public void setI_face_detection_btn_status(int i_face_detection_btn_status) {
        this.i_face_detection_btn_status = i_face_detection_btn_status;
    }



    public int getI_axle_mark_status() {
        return i_axle_mark_status;
    }
    public void setI_axle_mark_status(int i_axle_mark_status) {
        this.i_axle_mark_status = i_axle_mark_status;
    }

    public RCAxleMark getRcaxlemark() {
        return rcaxlemark;
    }
    public void setRcaxlemark(RCAxleMark rcaxlemark) {
        this.rcaxlemark = rcaxlemark;
    }



    public byte[] getB_error_content() {
        return b_error_content;
    }
    public int getI_error_level() {
        return i_error_level;
    }

    public void setI_error_level(int i_error_level) {
        this.i_error_level = i_error_level;
    }
    public void setB_error_content(byte[] b_error_content) {
        this.b_error_content = b_error_content;
    }


    public RCstatusback getRcstatus() {
        return rcstatus;
    }
    public void setRcstatus(RCstatusback rcstatus) {
        this.rcstatus = rcstatus;
    }
    public int getType() {
        return type;
    }
    public void setType(int type) {
        this.type = type;
    }
    public String getChanel() {
        return chanel;
    }
    public void setChanel(String chanel) {
        this.chanel = chanel;
    }


    public float getF_scan_id() {
        return this.f_scan_id;
    }

    public void setF_scan_id(float f_scan_id) {
        this.f_scan_id = f_scan_id;
    }

    public float[] getF_create_map_data() {
        return this.f_create_map_data;
    }

    public void setF_create_map_data(float[] f_create_map_data) {
        this.f_create_map_data = f_create_map_data;
    }

    public float[] getF_map_info() {
        return this.f_map_info;
    }

    public void setF_map_info(float[] f_map_info) {
        this.f_map_info = f_map_info;
    }

    public float[] getF_correct_map_data() {
        return this.f_correct_map_data;
    }

    public void setF_correct_map_data(float[] f_correct_map_data) {
        this.f_correct_map_data = f_correct_map_data;
    }

    public void setI_updatemap_info(byte[] I_updatemap_info) {
        this.i_updatemap_info = I_updatemap_info;
    }

    public byte[] getI_updatemap_info() {
        return this.i_updatemap_info;
    }
} 