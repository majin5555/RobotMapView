//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package lcm.logging;

import java.io.IOException;

public class LogDiagnostic {
    public static void main(String[] var0) {
        try {
            main_ex(var0);
        } catch (IOException var2) {
            System.out.println("ex: " + var2);
        }

    }

    public static void main_ex(String[] var0) throws IOException {
        Log var1 = new Log(var0[0], "r");
        long var2 = 0L;

        while(true) {
            Log.Event var4 = var1.readNext();
            long var5 = var4.utime - var2;
            if (var5 < 0L && var2 != 0L) {
                System.out.printf("%15d Negative utime (%10d)\n", var4.utime, var5);
            }

            if (var5 > 1000000L) {
                System.out.printf("%15d Large utime    (%10d)\n", var4.utime, var5);
            }

            var2 = var4.utime;
        }
    }
}
