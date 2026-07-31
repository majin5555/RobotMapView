package lcm.logging;

/**
 * 全局LCM日志工具，提供可动态替换的ILCMLogger实例。
 * 线程安全，允许在任何时候切换Logger实现。
 */
public final class LCMLogger {
    // 默认日志实现，保证非空
    private static volatile ILCMLogger instance = new DefaultLogger();

    // 工具类不允实例化
    private LCMLogger() {}

    /**
     * 获取当前的ILCMLogger实例（永远不会返回null）
     */
    public static ILCMLogger getLogger() {
        return instance;
    }

    /**
     * 替换全局Logger实例（如果参数为null则自动回退到DefaultLogger）
     */
    public static void setLogger(ILCMLogger logger) {
        if (logger != null) {
            instance = logger;
        } else {
            instance = new DefaultLogger();
        }
    }
}