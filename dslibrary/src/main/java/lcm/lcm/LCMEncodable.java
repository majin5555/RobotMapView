//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package lcm.lcm;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public interface LCMEncodable {
    void encode(DataOutput var1) throws IOException;

    void _encodeRecursive(DataOutput var1) throws IOException;

    void _decodeRecursive(DataInput var1) throws IOException;
}
