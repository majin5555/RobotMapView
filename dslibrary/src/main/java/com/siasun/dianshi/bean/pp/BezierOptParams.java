package com.siasun.dianshi.bean.pp;

/**
 * Created by changjian.song on 2022/4/10 at 9:43
 *
 * @description ：bezier曲线优化参数
 */
public class BezierOptParams {
    private static BezierOptParams instance;
    private float max_steer_angle;
    private float steer_x_install;
    private float steer_y_install;
    private float max_line_vel;
    private float max_angle_vel;
    private float center_max_angle_vel;
    private float center_max_angle_acc;
    private float center_max_line_acc;
    private float reserving_steer_dis;
    private float endpoint_max_angle_vel;

    public static BezierOptParams getInstance() {
        if (instance == null) {
            instance = new BezierOptParams();
        }

        return instance;
    }

    private BezierOptParams() {}

    public float getMax_steer_angle() {
        return max_steer_angle;
    }

    public void setMax_steer_angle(float max_steer_angle) {
        this.max_steer_angle = max_steer_angle;
    }

    @Override
    public String toString() {
        return "AgvParams{" +
                "max_steer_angle=" + max_steer_angle +
                ", steer_x_install=" + steer_x_install +
                ", steer_y_install=" + steer_y_install +
                ", max_line_vel=" + max_line_vel +
                ", max_angle_vel=" + max_angle_vel +
                ", center_max_angle_vel=" + center_max_angle_vel +
                ", center_max_angle_acc=" + center_max_angle_acc +
                ", center_max_line_acc=" + center_max_line_acc +
                ", reserving_steer_dis=" + reserving_steer_dis +
                ", endpoint_max_angle_vel=" + endpoint_max_angle_vel +
                '}';
    }

    public float getSteer_x_install() {
        return steer_x_install;
    }

    public void setSteer_x_install(float steer_x_install) {
        this.steer_x_install = steer_x_install;
    }

    public float getSteer_y_install() {
        return steer_y_install;
    }

    public void setSteer_y_install(float steer_y_install) {
        this.steer_y_install = steer_y_install;
    }

    public float getMax_line_vel() {
        return max_line_vel;
    }

    public void setMax_line_vel(float max_line_vel) {
        this.max_line_vel = max_line_vel;
    }

    public float getMax_angle_vel() {
        return max_angle_vel;
    }

    public void setMax_angle_vel(float max_angle_vel) {
        this.max_angle_vel = max_angle_vel;
    }

    public float getCenter_max_angle_vel() {
        return center_max_angle_vel;
    }

    public void setCenter_max_angle_vel(float center_max_angle_vel) {
        this.center_max_angle_vel = center_max_angle_vel;
    }

    public float getCenter_max_angle_acc() {
        return center_max_angle_acc;
    }

    public void setCenter_max_angle_acc(float center_max_angle_acc) {
        this.center_max_angle_acc = center_max_angle_acc;
    }

    public float getCenter_max_line_acc() {
        return center_max_line_acc;
    }

    public void setCenter_max_line_acc(float center_max_line_acc) {
        this.center_max_line_acc = center_max_line_acc;
    }

    public float getReserving_steer_dis() {
        return reserving_steer_dis;
    }

    public void setReserving_steer_dis(float reserving_steer_dis) {
        this.reserving_steer_dis = reserving_steer_dis;
    }

    public float getEndpoint_max_angle_vel() {
        return endpoint_max_angle_vel;
    }

    public void setEndpoint_max_angle_vel(float endpoint_max_angle_vel) {
        this.endpoint_max_angle_vel = endpoint_max_angle_vel;
    }

    public float[] GetAllParam()
    {
        // 顺序不能变化
        float[] param = new float[10];
        param[0] = getMax_steer_angle();
        param[1] = getSteer_x_install();
        param[2] = getSteer_y_install();
        param[3] = getMax_line_vel();
        param[4] = getMax_angle_vel();
        param[5] = getCenter_max_angle_vel();
        param[6] = getCenter_max_angle_acc();
        param[7] = getCenter_max_line_acc();
        param[8] = getReserving_steer_dis();
        param[9] = getEndpoint_max_angle_vel();
        return param;
    }

}