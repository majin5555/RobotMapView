//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package lcm.lcm;

import java.util.HashMap;

public class URLParser {
    HashMap<String, String> params = new HashMap();

    public URLParser(String var1) {
        String[] var2 = var1.split("://", 2);
        if (var2.length < 2) {
            throw new IllegalArgumentException("URLParser: Invalid URL: " + var1);
        } else {
            String[] var3 = var2[1].split("[?]");
            this.params.put("protocol", var2[0]);
            if (var3[0].length() > 0) {
                this.params.put("network", var3[0]);
            }

            if (var3.length > 1) {
                String[] var4 = var3[1].split("&");

                for(int var5 = 0; var5 < var4.length; ++var5) {
                    String[] var6 = var4[var5].split("=");
                    if (var6.length != 2) {
                        System.err.println("Invalid key-value pair in URL : " + var4[var5]);
                    } else {
                        this.params.put(var6[0], var6[1]);
                    }
                }
            }

        }
    }

    public String get(String var1) {
        return (String)this.params.get(var1);
    }

    public String get(String var1, String var2) {
        return this.params.get(var1) == null ? var2 : (String)this.params.get(var1);
    }

    public int get(String var1, int var2) {
        String var3 = (String)this.params.get(var1);
        return var3 == null ? var2 : Integer.parseInt(var3);
    }

    public boolean get(String var1, boolean var2) {
        String var3 = (String)this.params.get(var1);
        if (var3 == null) {
            return var2;
        } else {
            return Boolean.parseBoolean(var3) || var3.equals("1");
        }
    }

    public double get(String var1, double var2) {
        String var4 = (String)this.params.get(var1);
        return var4 == null ? var2 : Double.parseDouble(var4);
    }

    public static void main(String[] var0) {
        URLParser var1 = null;
        if (var0.length < 1) {
            String var2 = System.getenv("LCM_DEFAULT_URL");
            if (null != var2) {
                var1 = new URLParser(var2);
            } else {
                System.err.println("Must specify URL");
                System.exit(1);
            }
        } else {
            var1 = new URLParser(var0[0]);
        }

        for(String var3 : var1.params.keySet()) {
            System.err.printf("param %15s: %s\n", var3, var1.params.get(var3));
        }

    }
}
