package com.wpf.util.jiagulibrary.utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 反射相关的工具类
 *
 * @author linchaolong
 */
public class Reflect {
    public static Object invokeStaticMethod(String class_name, String method_name, Class[] pareTyple, Object[] pareVaules) {
        try {
            Class obj_class = Class.forName(class_name);
            Method method = obj_class.getMethod(method_name, pareTyple);
            return method.invoke(null, pareVaules);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;

    }

    /**
     * 调用类或对象的方法并返回结果
     *
     * @param clazz          类
     * @param methodName     方法名
     * @param obj            调用该方法的对象，如果是静态方法则传null
     * @param args           参数，如果没有则传null
     * @param parameterTypes 方法参数类型的class，如果没有则传null
     * @return 调用结果
     */
    public static Object invokeMethod(Class<?> clazz, Object obj, String methodName, Object[] args, Class<?>... parameterTypes) {
        try {
            // 反射类指定方法
            Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true); // 暴力反射
            // 调用方法并返回结果
            return method.invoke(obj, args);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 调用类或对象的方法并返回结果
     *
     * @param className      类名
     * @param methodName     方法名
     * @param obj            调用该方法的对象，如果是静态方法则传null
     * @param args           参数，如果没有则传null
     * @param parameterTypes 方法参数类型的class，如果没有则传null
     * @return 调用结果
     */
    public static Object invokeMethod(String className, Object obj, String methodName, Object[] args, Class<?>... parameterTypes) {
        try {
            // 防止空指针错误
            if (parameterTypes == null) {
                parameterTypes = new Class[0];
            }
            if (args == null) {
                args = new Object[0];
            }
            // 加载类的字节码
            Class<?> clazz = Class.forName(className);
            return invokeMethod(clazz, obj, methodName, args, parameterTypes);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object invokeMethod(String class_name, String method_name, Object obj, Class[] pareTyple, Object[] pareVaules) {

        try {
            Class obj_class = Class.forName(class_name);
            Method method = obj_class.getMethod(method_name, pareTyple);
            return method.invoke(obj, pareVaules);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;

    }

    /**
     * 获取对象或类某个字段的值
     *
     * @param clazz     类
     * @param obj       对象，如果是静态字段则传null
     * @param fieldName 字段名称
     * @return 字段的值
     */
    public static Object getFieldValue(Class<?> clazz, Object obj, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 设置类的属性（包括私有和保护）
     *
     * @param classname
     * @param filedName
     * @param obj
     * @param filedVaule
     */
    public static void setFieldOjbect(String classname, String filedName, Object obj, Object filedVaule) {
        try {
            Class obj_class = Class.forName(classname);
            Field field = obj_class.getDeclaredField(filedName);
            field.setAccessible(true);
            field.set(obj, filedVaule);
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * 反射得到类的属性（包括私有和保护）
     *
     * @param class_name
     * @param obj
     * @param filedName
     * @return
     */
    public static Object getFieldOjbect(String class_name, Object obj, String filedName) {
        try {
            Class obj_class = Class.forName(class_name);
            Field field = obj_class.getDeclaredField(filedName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;

    }

    /**
     * 获取对象或类某个字段的值
     *
     * @param className 类名
     * @param obj       对象，如果是静态字段则传null
     * @param fieldName 字段名称
     * @return 字段的值
     */
    public static Object getFieldValue(String className, Object obj, String fieldName) {
        try {
            Class<?> clazz = Class.forName(className);
            return getFieldValue(clazz, obj, fieldName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 设置对象或类某个字段的值
     *
     * @param clazz     类
     * @param obj       对象，如果是静态字段则传null
     * @param fieldName 字段名称
     * @param value     字段值
     * @return 是否设置成功
     */
    public static boolean setFieldValue(Class<?> clazz, Object obj, String fieldName, Object value) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
            return true;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 设置对象或类某个字段的值
     *
     * @param className 类名
     * @param obj       对象，如果是静态字段则传null
     * @param fieldName 字段名称
     * @param value     字段值
     * @return 是否设置成功
     */
    public static boolean setFieldValue(String className, Object obj, String fieldName, Object value) {
        try {
            Class<?> clazz = Class.forName(className);
            setFieldValue(clazz, obj, fieldName, value);
            return true;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 根据类名实例化一个对象
     *
     * @param className 类名
     * @return 对象实例，如果实例化失败返回null
     */
    public static Object newInstance(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return clazz.newInstance();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 通过反射方法获取 instance 类中的 memberName 名称的成员
     * @param instance 成员所在对象
     * @param memberName 成员变量名称
     * @return 返回 Field 类型成员
     * @throws NoSuchFieldException
     */
    public static Field reflexField(Object instance, String memberName) throws NoSuchFieldException {

        // 获取字节码类
        Class clazz = instance.getClass();

        // 循环通过反射获取
        // 可能存在通过反射没有找到成员的情况 , 此时查找其父类是否有该成员
        // 循环次数就是其父类层级个数
        while (clazz != null) {
            try {
                // 获取成员
                Field memberField = clazz.getDeclaredField(memberName);

                // 如果不是 public , 无法访问 , 设置可访问
                if (!memberField.isAccessible()) {
                    memberField.setAccessible(true);
                }
                return memberField;
            } catch (NoSuchFieldException exception){
                // 如果找不到, 就到父类中查找
                clazz = clazz.getSuperclass();
            }
        }

        // 如果没有拿到成员 , 则直接中断程序 , 加载无法进行下去
        throw new NoSuchFieldException("没有在 " + clazz.getName() + " 类中找到 " + memberName +  "成员");
    }

    /**
     * 通过反射方法获取 instance 类中的 参数为 parameterTypes , 名称为 methodName 的成员方法
     * @param instance 成员方法所在对象
     * @param methodName 成员方法名称
     * @param parameterTypes 成员方法参数
     * @return
     * @throws NoSuchMethodException
     */
    public static Method reflexMethod(Object instance, String methodName, Class... parameterTypes)
            throws NoSuchMethodException {

        // 获取字节码类
        Class clazz = instance.getClass();

        // 循环通过反射获取
        // 可能存在通过反射没有找到成员方法的情况 , 此时查找其父类是否有该成员方法
        // 循环次数就是其父类层级个数
        while (clazz != null) {
            try {
                // 获取成员方法
                Method method = clazz.getDeclaredMethod(methodName, parameterTypes);

                // 如果不是 public , 无法访问 , 设置可访问
                if (!method.isAccessible()) {
                    method.setAccessible(true);
                }
                return method;
            } catch (NoSuchMethodException e) {
                // 如果找不到, 就到父类中查找
                clazz = clazz.getSuperclass();
            }
        }

        // 如果没有拿到成员 , 则直接中断程序 , 加载无法进行下去
        throw new NoSuchMethodException("没有在 " + clazz.getName() + " 类中找到 " + methodName +  "成员方法");
    }

}
