package lcm.logging;

public interface ILCMLogger {
    void v(String tag, String msg);
    void d(String tag, String msg);
    void i(String tag, String msg);

    void w(String tag, String msg, Throwable tr);
    void w(String tag, String msg);
    void e(String tag, String msg);
    void e(String tag, String msg, Throwable tr);
}