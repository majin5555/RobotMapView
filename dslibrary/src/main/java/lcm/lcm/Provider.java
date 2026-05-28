//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package lcm.lcm;

public interface Provider {
    void publish(String var1, byte[] var2, int var3, int var4);

    void subscribe(String var1);

    void unsubscribe(String var1);

    void close();
}
