//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package lcm.spy;

import java.lang.reflect.Field;
import java.util.HashMap;

import lcm.util.ClassDiscoverer;

public class LCMTypeDatabase {
    HashMap<Long, Class> classes = new HashMap();

    public LCMTypeDatabase() {
        ClassDiscoverer.findClasses(new MyClassVisitor());
        System.out.println("Found " + this.classes.size() + " LCM types");
    }

    public Class getClassByFingerprint(long var1) {
        return (Class)this.classes.get(var1);
    }

    class MyClassVisitor implements ClassDiscoverer.ClassVisitor {
        public void classFound(String var1, Class var2) {
            try {
                Field[] var3 = var2.getFields();

                for(Field var7 : var3) {
                    if (var7.getName().equals("LCM_FINGERPRINT")) {
                        long var8 = var7.getLong((Object)null);
                        LCMTypeDatabase.this.classes.put(var8, var2);
                        break;
                    }
                }
            } catch (IllegalAccessException var10) {
                System.out.println("Bad LCM Type? " + var10);
            }

        }
    }
}
