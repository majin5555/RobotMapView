//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package lcm.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassDiscoverer {
    public static void findClasses(ClassVisitor var0) {
        String var1 = System.getProperty("path.separator");
        String var2 = System.getenv("CLASSPATH") + var1 + System.getProperty("java.class.path");
        findClasses(var2, var0);
    }

    private static void visitDirectory(ClassVisitor var0, URLClassLoader var1, String var2, File var3, String var4) {
        if (var3.canRead()) {
            for(File var8 : var3.listFiles()) {
                if (var8.canRead()) {
                    String var9 = var8.getName();
                    if (var8.isDirectory()) {
                        if (!var9.contains(".")) {
                            visitDirectory(var0, var1, var2, var8, var9 + ".");
                        }
                    } else if (var8.isFile() && var9.endsWith(".class")) {
                        String var10 = var4 + var9.substring(0, var9.length() - 6);

                        try {
                            Class var11 = var1.loadClass(var10);
                            if (var11 != null) {
                                var0.classFound(var2, var11);
                            }
                        } catch (Throwable var12) {
                        }
                    }
                }
            }

        }
    }

    public static void findClasses(String var0, ClassVisitor var1) {
        if (var0 != null) {
            String var2 = System.getProperty("path.separator");
            String[] var3 = var0.split(var2);

            URLClassLoader var4;
            try {
                URL[] var5 = new URL[var3.length];

                for(int var6 = 0; var6 < var3.length; ++var6) {
                    var5[var6] = (new File(var3[var6])).toURL();
                }

                var4 = new URLClassLoader(var5);
            } catch (IOException var15) {
                System.out.println("ClassDiscoverer ERR: " + var15);
                return;
            }

            for(int var16 = 0; var16 < var3.length; ++var16) {
                String var17 = var3[var16];
                if (var17.endsWith(".jar")) {
                    try {
                        JarFile var18 = new JarFile(var17);
                        Enumeration var8 = var18.entries();

                        while(var8.hasMoreElements()) {
                            JarEntry var9 = (JarEntry)var8.nextElement();
                            String var10 = var9.getName();
                            if (var10.endsWith(".class")) {
                                String var11 = var10.substring(0, var10.length() - 6);
                                var11 = var11.replace('/', '.');
                                var11 = var11.replace('\\', '.');

                                try {
                                    Class var12 = var4.loadClass(var11);
                                    if (var12 != null) {
                                        var1.classFound(var17, var12);
                                    }
                                } catch (Throwable var13) {
                                    System.out.println("ClassDiscoverer: " + var13);
                                    System.out.println("                 jar: " + var17);
                                    System.out.println("                 class: " + var10);
                                }
                            }
                        }
                    } catch (IOException var14) {
                        System.out.println("Error extracting " + var3[var16]);
                    }
                } else {
                    File var7 = new File(var17);
                    if (var7.isDirectory()) {
                        visitDirectory(var1, var4, var17, var7, "");
                    }
                }
            }

        }
    }

    public static void main(String[] var0) {
        ClassVisitor var1 = new ClassVisitor() {
            public void classFound(String var1, Class var2) {
                System.out.printf("%-30s %s\n", var1, var2);
            }
        };
        findClasses(var1);
    }

    public interface ClassVisitor {
        void classFound(String var1, Class var2);
    }
}
