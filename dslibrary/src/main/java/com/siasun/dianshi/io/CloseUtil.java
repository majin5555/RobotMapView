package com.siasun.dianshi.io;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by 健 on 2017/10/24.
 */

public class CloseUtil {
    private CloseUtil() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    /**
     * 关闭IO
     *
     * @param closeables closeables
     */
    public static void closeIO(final Closeable... closeables) {
        if (closeables == null) return;
        for (Closeable closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
