package com.bcode.fwk.application.listener.utils;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.bcode.fwk.application.listener.ear.ListenerContextEar;
import com.bcode.fwk.application.listener.web.ListenerContextWeb;

public class ClassLoaderUtils {

    private static final List<ClassLoader> listEAR = new ArrayList<ClassLoader>();
    private static final List<ClassLoader> listWeb = new ArrayList<ClassLoader>();

    /**
     * Méthode permettant de purger la liste des classes maintenues dans le ClassLoader.
     * 
     * @param cl
     * @param fieldName
     */
    public static void clearClass(ClassLoader cl, String fieldName) {
        Field declaredField;
        // //System.out.println("Nom des class loader " + cl.getClass().getCanonicalName());
        try {
            declaredField = cl.getClass().getDeclaredField(fieldName);
            declaredField.setAccessible(true);
            if (declaredField.get(cl) instanceof weblogic.utils.collections.ArraySet) {
                weblogic.utils.collections.ArraySet list = (weblogic.utils.collections.ArraySet) declaredField.get(cl);
                list.clear();
            } else if (declaredField.get(cl) instanceof Map) {
                Map map = (Map) declaredField.get(cl);
                map.clear();
            }
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
//            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            // TODO Auto-generated catch block
//            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
//            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
//            e.printStackTrace();
        }
    }

    public static void printClassLoaderHierarchy(String typeModule, Class c) {
        ClassLoader cl = c.getClassLoader();
        printClassLoaderTree(typeModule, cl);
        ClassLoader l = Thread.currentThread().getContextClassLoader();
        printClassLoaderTree(typeModule, l);
    }

    public static void auditClassLoaderHierarchy(String typeModule) {
        List<ClassLoader> allClassLoader;
        if (typeModule.equals("ear")) {
            allClassLoader = getAllClassLoaderEAR();
        } else {
            allClassLoader = getAllClassLoaderWeb();
        }

        for (ClassLoader classLoader : allClassLoader) {
            if (classLoader != null) {
                if (classLoader instanceof weblogic.utils.classloaders.ChangeAwareClassLoader) {
                    getSizeCollectionInClassLoader(classLoader, "cachedClasses");
                    getSizeCollectionInClassLoader(classLoader, "modTimes");
                } else if (classLoader instanceof weblogic.utils.classloaders.FilteringClassLoader) {
                    getSizeCollectionInClassLoader(classLoader, "classPatterns");
                    getSizeCollectionInClassLoader(classLoader, "resourcePatterns");
                } else if (classLoader instanceof weblogic.utils.classloaders.GenericClassLoader) {
                    getSizeCollectionInClassLoader(classLoader, "exclude");
                }
            }
        }
    }

    public static void getSizeCollectionInClassLoader(ClassLoader cl, String fieldName) {
        Field declaredField;
        // //System.out.println("Nom des class loader " + cl.getClass().getCanonicalName());
        try {
            declaredField = cl.getClass().getDeclaredField(fieldName);
            declaredField.setAccessible(true);
            if (declaredField.get(cl) instanceof weblogic.utils.collections.ArraySet) {
                weblogic.utils.collections.ArraySet list = (weblogic.utils.collections.ArraySet) declaredField.get(cl);
            } else if (declaredField.get(cl) instanceof Map) {
                Map map = (Map) declaredField.get(cl);
            }
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
//            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            // TODO Auto-generated catch block
//            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
//            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
//            e.printStackTrace();
        }
    }

    public static void printClassLoaderTree(String typeModule, ClassLoader l) {
        incrementeListClassLoader(typeModule, l);
        ClassLoader p = l.getParent();
        if (p != null) {
            printClassLoaderTree(typeModule, p);
        } else {
            if (sun.misc.Launcher.getLauncher().getClassLoader().getClass().getCanonicalName().contains("ExtClassLoader")
                    || sun.misc.Launcher.getLauncher().getClassLoader().getClass().getCanonicalName().contains("AppClassLoader")) {
                ClassLoader pLauncer = sun.misc.Launcher.getLauncher().getClassLoader().getParent();
                if (p != null) {
                    printClassLoaderTree(typeModule, pLauncer);
                }

            }
        }

        String u = "";
        if (l instanceof URLClassLoader) {
            u = getURLs(((URLClassLoader) l).getURLs());
        }

        // ////System.out.println((new StringBuilder("\t|\n")).append(l).append(" ").append(u).toString());
    }

    public static String getURLs(URL urls[]) {
        if (urls == null)
            return "{}";
        StringBuffer b = new StringBuffer("{");
        for (int i = 0; i < urls.length; i++)
            b.append(urls[i]).append(":");

        b.append("}");
        return b.toString();
    }

    public static void incrementeListClassLoader(String typeModule, ClassLoader cl) {
        if (typeModule.equals("ear")) {
            listEAR.add(cl);
        } else {
            listWeb.add(cl);
        }

        // if(typeModule.equals("ear")){
        // System.err.println("ClassLoader charge par le contexte " + listEAR.size());
        // }else{
        // System.err.println("ClassLoader charge par le contexte " + listWeb.size());
        // }
    }

    public static void removeAllClassLoaderEAR() {
        listEAR.clear();
    }

    public static List<ClassLoader> getAllClassLoaderEAR() {
        return listEAR;
    }

    public static void removeAllClassLoaderWeb() {
        listWeb.clear();
    }

    public static List<ClassLoader> getAllClassLoaderWeb() {
        return listWeb;
    }

    public static <E> E getStaticFieldValue(Class clazz, String fieldName) {
        Field staticField = findField(clazz, fieldName);
        return (staticField != null) ? (E) getStaticFieldValue(staticField) : null;
    }

    public static <E> E getStaticFieldValue(String className, String fieldName) {
        Field staticField = findFieldOfClass(className, fieldName);
        return (staticField != null) ? (E) getStaticFieldValue(staticField) : null;
    }

    public static Field findField(Class clazz, String fieldName) {
        if (clazz == null)
            return null;

        try {
            final Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true); // (Field is probably private)
            return field;
        } catch (NoSuchFieldException ex) {
            // Silently ignore
            return null;
        } catch (Exception ex) { // Example SecurityException
            //System.out.println(ex);
            return null;
        }
    }

    public static <T> T getStaticFieldValue(Field field) {
        try {
            return (T) field.get(null);
        } catch (Exception ex) {
//            System.err.println(ex);
            
            // Silently ignore
            return null;
        }
    }

    public static <T> T getFieldValue(Field field, Object obj) {
        try {
            return (T) field.get(obj);
        } catch (Exception ex) {
            //System.out.println(ex);
            // Silently ignore
            return null;
        }
    }

    public static Field findFieldOfClass(String className, String fieldName) {
        Class clazz = findClass(className);
        if (clazz != null) {
            return findField(clazz, fieldName);
        } else
            return null;
    }

    public static Class findClass(String className) {
        try {
            return Class.forName(className);
        }
        // catch (NoClassDefFoundError e) {
        // // Silently ignore
        // return null;
        // }
        catch (ClassNotFoundException e) {
            // Silently ignore
            return null;
        } catch (Exception ex) { // Example SecurityException
            //System.out.println(ex);
            return null;
        }
    }

    /**
     * Get a Collection with all Threads.
     * This method is heavily inspired by org.apache.catalina.loader.WebappClassLoader.getThreads()
     */
    public static Collection<Thread> getAllThreads() {
        // This is some orders of magnitude slower...
        // return Thread.getAllStackTraces().keySet();

        // Find root ThreadGroup
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        while (tg.getParent() != null)
            tg = tg.getParent();

        // Note that ThreadGroup.enumerate() silently ignores all threads that does not fit into array
        int guessThreadCount = tg.activeCount() + 50;
        Thread[] threads = new Thread[guessThreadCount];
        int actualThreadCount = tg.enumerate(threads);
        while (actualThreadCount == guessThreadCount) { // Map was filled, there may be more
            guessThreadCount *= 2;
            threads = new Thread[guessThreadCount];
            actualThreadCount = tg.enumerate(threads);
        }

        // Filter out nulls
        final List<Thread> output = new ArrayList<Thread>();
        for (Thread t : threads) {
            if (t != null) {
                output.add(t);
            }
        }
        return output;
    }

    /** teste si l'objet est dans le class loader courant ou parent de l'ear */
    public static boolean isLoadedInEarApplication(Object o) {
        return o != null && isLoadedByEarApplication(o.getClass());
    }

    /** teste si l'objet est dans le class loader courant ou parent de l'ear */
    public static boolean isLoadedInEarApplication(Object o, Class applicationClass) {
        return o != null && isLoadedByEarApplication(o.getClass(), applicationClass);
    }
    
    /** teste si l'objet est dans le class loader courant ou parent de l'ear */
    private static boolean isLoadedByEarApplication(Class clazz) {
        return clazz != null && isEarAppClassLoaderOrChild(clazz.getClassLoader());
    }
    
    /** teste si l'objet est dans le class loader courant ou parent de l'ear */
    private static boolean isLoadedByEarApplication(Class clazz, Class applicationClass) {
        return clazz != null && isEarAppClassLoaderOrChild(clazz.getClassLoader(), applicationClass);
    }
    
    
    public static boolean getValueIsEarAppClassLoaderOrChild(ClassLoader cl){
        return isEarAppClassLoaderOrChild(cl);
    }
    
    public static boolean getValueIsEarAppClassLoaderOrChild(ClassLoader cl, Class applicationClass){
        return isEarAppClassLoaderOrChild(cl, applicationClass);
    }
    
    /** teste si l'objet est dans le class loader courant ou parent de l'ear */
    private static boolean isEarAppClassLoaderOrChild(ClassLoader cl) {
        final ClassLoader webAppCL = getEarApplicationClassLoader();
        // final ClassLoader webAppCL = Thread.currentThread().getContextClassLoader();

        while (cl != null) {
            if (cl == webAppCL)
                return true;

            cl = cl.getParent();
        }

        return false;
    }

    
    /** teste si l'objet est dans le class loader courant ou parent de l'ear */
    private static boolean isEarAppClassLoaderOrChild(ClassLoader cl, Class applicationClass) {
        final ClassLoader webAppCL = getEarApplicationClassLoader(applicationClass);
        // final ClassLoader webAppCL = Thread.currentThread().getContextClassLoader();

        while (cl != null) {
            if (cl == webAppCL)
                return true;

            cl = cl.getParent();
        }

        return false;
    }
    
    
    /**
     * provient de l'ear
     * 
     * @return
     */
    public static ClassLoader getEarApplicationClassLoader( Class applicationClass) {
        return applicationClass.getClassLoader();
    }
    
    
    /**
     * provient de l'ear
     * 
     * @return
     */
    public static ClassLoader getEarApplicationClassLoader() {
        return ListenerContextEar.class.getClassLoader();
    }
    
    
    /** Test si l'objet provient du cl web */
    public static boolean isLoadedInWebApplication(Object o) {
        return o != null && isLoadedByWebApplication(o.getClass());
    }

    /** Test si l'objet provient du cl web */
    public static boolean isLoadedInWebApplication(Object o, Class classApplication) {
        return o != null && isLoadedByWebApplication(o.getClass(), classApplication);
    }
    
    /** Test si l'objet provient du cl web */
    private static  boolean isLoadedByWebApplication(Class clazz) {
        return clazz != null && isWebAppClassLoaderOrChild(clazz.getClassLoader());
    }

    
    /** Test si l'objet provient du cl web */
    private static  boolean isLoadedByWebApplication(Class clazz, Class classApplication) {
        return clazz != null && isWebAppClassLoaderOrChild(clazz.getClassLoader(), classApplication);
    }
    
    
    /** Test si l'objet provient du cl web */
    public static boolean getValueIsWebAppClassLoaderOrChild(ClassLoader cl){
        return isWebAppClassLoaderOrChild( cl);
    }
    
    /** Test si l'objet provient du cl web */
    public static boolean getValueIsWebAppClassLoaderOrChild(ClassLoader cl, Class classAppli){
        return isWebAppClassLoaderOrChild( cl, classAppli);
    }
    
    
    /** Test si l'objet provient du cl web */
    private static boolean isWebAppClassLoaderOrChild(ClassLoader cl) {
        final ClassLoader webAppCL = getWebApplicationClassLoader();
        // final ClassLoader webAppCL = Thread.currentThread().getContextClassLoader();

        while (cl != null) {
            if (cl == webAppCL)
                return true;

            cl = cl.getParent();
        }

        return false;
    }

    /** Test si l'objet provient du cl web */
    private static boolean isWebAppClassLoaderOrChild(ClassLoader cl, Class classApplication) {
        final ClassLoader webAppCL = getWebApplicationClassLoader(classApplication);
        // final ClassLoader webAppCL = Thread.currentThread().getContextClassLoader();

        while (cl != null) {
            if (cl == webAppCL)
                return true;

            cl = cl.getParent();
        }

        return false;
    }
    
    /** Test si l'objet provient du cl web */
    public static ClassLoader getWebApplicationClassLoader() {
        return ListenerContextWeb.class.getClassLoader();
        // Alternative return Thread.currentThread().getContextClassLoader();
    }    
    
    /** Test si l'objet provient du cl web */
    public static ClassLoader getWebApplicationClassLoader(Class classApplication) {
        return classApplication.getClassLoader();
        // Alternative return Thread.currentThread().getContextClassLoader();
    }    
}
