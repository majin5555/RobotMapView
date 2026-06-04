package lcm.logging;

import android.util.Log;

public class DefaultLogger implements ILCMLogger {

    @Override
    public void v(String tag, String msg) {
        Log.v(tag, msg);
    }

    @Override
    public void d(String tag, String msg) {
        Log.d(tag, msg);
    }

    @Override
    public  void i(String tag, String msg) {
        Log.i(tag, msg);
    }
    @Override
    public  void w(String tag, String msg) {
        Log.w(tag, msg);
    }
    @Override
    public  void w(String tag, String msg, Throwable tr) {
        Log.w(tag, msg, tr);
    }
    @Override
    public  void e(String tag, String msg) {
        Log.e(tag, msg);
    }
    @Override
    public  void e(String tag, String msg, Throwable tr) {
        Log.e(tag, msg, tr);
    }

}
