package net.sklcc.util;

import java.io.File;
import java.io.UnsupportedEncodingException;

/**
 * Created by Hazza on 2016/8/4.
 */
public class ClassUtil {
    private ClassUtil() {}

    /**
     *
     * @param c
     * @return String
     * @author hazzacheng
     * @discription  returns the path of the class file
     */
    public static String getClassPath(Class<?> c) {
        return c.getResource("").getPath().replaceAll("%20", " ");
    }

    /**
     *
     * @param c
     * @return String
     * @author hazzacheng
     * @discription returns the root path of the class file
     */
    public static String getClassRootPath(Class<?> c) {
        return c.getResource("/").getPath().replaceAll("%20", " ");
}



    /**
     *
     * @param c
     * @param hasName whether to include class name
     * @return String
     * @author hazzacheng
     * @discription returns the path of the class file
     */
    public static String getClassPath(Class<?> c, boolean hasName) {
        String name = c.getSimpleName() + ".class";
        String path = c.getResource(name).getPath().replace("%20", " ");
        if (hasName) {
            return path;
        } else {
            return path.substring(0,path.length() - name.length());
        }
    }

    public static void main(String[] args) {

    }
}
