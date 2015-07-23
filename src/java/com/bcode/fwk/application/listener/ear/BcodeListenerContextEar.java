/**
 * 
 */
package com.bcode.fwk.application.listener.ear;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.jar.JarFile;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import weblogic.application.AppClassLoaderManager;
import weblogic.application.ApplicationException;
import weblogic.application.ApplicationLifecycleEvent;
import weblogic.application.ApplicationLifecycleListener;
import weblogic.application.internal.AppClassLoaderManagerImpl;

import com.bcode.fwk.application.listener.utils.ClassLoaderUtils;
import com.bcode.fwk.application.listener.utils.thread.ClearingThreadLocalProcessor;
import com.bcode.fwk.application.listener.utils.thread.ThreadLocalProcessor;

/**
 * @author X116604
 *
 */
public class BcodeListenerContextEar extends ApplicationLifecycleListener {
    /** Default no of milliseconds to wait for threads to finish execution */
    public static final int THREAD_WAIT_MS_DEFAULT = 5 * 1000; // 5 seconds

    /** Default no of milliseconds to wait for shutdown hook to finish execution */
    public static final int SHUTDOWN_HOOK_WAIT_MS_DEFAULT = 10 * 1000; // 10 seconds

    /** Should shutdown hooks registered from the application be executed at application shutdown? */
    protected boolean executeShutdownHooks = true;

    protected final Field java_lang_Thread_threadLocals;

    protected final Field java_lang_Thread_inheritableThreadLocals;

    protected final Field java_lang_ThreadLocal$ThreadLocalMap_table;

    protected Field java_lang_ThreadLocal$ThreadLocalMap$Entry_value;

    /** Should threads tied to the web app classloader be forced to stop at application shutdown? */
    protected boolean stopThreads = true;

    /** Should Timer threads tied to the web app classloader be forced to stop at application shutdown? */
    protected boolean stopTimerThreads = true;

    /**
     * No of milliseconds to wait for threads to finish execution, before stopping them.
     */
    protected int threadWaitMs = SHUTDOWN_HOOK_WAIT_MS_DEFAULT;

    /**
     * No of milliseconds to wait for shutdown hooks to finish execution, before stopping them.
     * If set to -1 there will be no waiting at all, but Thread is allowed to run until finished.
     */
    protected int shutdownHookWaitMs = SHUTDOWN_HOOK_WAIT_MS_DEFAULT;

    /**
     * liste des jar
     */
    private static final HashSet<String> setJarFileNames2Close = new HashSet<String>();

    /**
     * 
     */
    public BcodeListenerContextEar() {
        // Initialize some reflection variables
        java_lang_Thread_threadLocals = ClassLoaderUtils.findField(Thread.class, "threadLocals");
        java_lang_Thread_inheritableThreadLocals = ClassLoaderUtils.findField(Thread.class, "inheritableThreadLocals");
        java_lang_ThreadLocal$ThreadLocalMap_table = ClassLoaderUtils.findFieldOfClass("java.lang.ThreadLocal$ThreadLocalMap", "table");

//        if (java_lang_Thread_threadLocals == null)
//            System.err.println("java.lang.Thread.threadLocals not found; something is seriously wrong!");
//
//        if (java_lang_Thread_inheritableThreadLocals == null)
//            System.err.println("java.lang.Thread.inheritableThreadLocals not found; something is seriously wrong!");
//
//        if (java_lang_ThreadLocal$ThreadLocalMap_table == null)
//            System.err.println("java.lang.ThreadLocal$ThreadLocalMap.table not found; something is seriously wrong!");
    }

    @Override
    public void postStart(ApplicationLifecycleEvent arg0) throws ApplicationException {
        // TODO Auto-generated method stub

        super.postStart(arg0);
        ClassLoaderUtils.printClassLoaderHierarchy("ear", getClass());
    }

    /*
     * (non-Javadoc)
     * @see weblogic.application.ApplicationLifecycleListener#postStop(weblogic.application.ApplicationLifecycleEvent)
     */
    public void postStop(ApplicationLifecycleEvent arg0) throws ApplicationException {
        cleanClassLoaderEar();
        super.postStop(arg0);
    }

    @Override
    public void applicationDestroyed(ApplicationLifecycleEvent arg0) {
        super.applicationDestroyed(arg0);
    }

    @Override
    public void applicationInitialized(ApplicationLifecycleEvent arg0) {
        super.applicationInitialized(arg0);
    }

    @Override
    public void initializingApplication(ApplicationLifecycleEvent arg0) {
        super.initializingApplication(arg0);
    }

    @Override
    public void prepared(ApplicationLifecycleEvent arg0) {
        super.prepared(arg0);
    }

    @Override
    public void preparing(ApplicationLifecycleEvent arg0) {
        super.preparing(arg0);
    }

    @Override
    public void preStart(ApplicationLifecycleEvent arg0) throws ApplicationException {
        super.preStart(arg0);
    }

    @Override
    public void preStop(ApplicationLifecycleEvent arg0) throws ApplicationException {
        super.preStop(arg0);
    }

    @Override
    public void unprepared(ApplicationLifecycleEvent arg0) {
        super.unprepared(arg0);
    }

    @Override
    public void unpreparing(ApplicationLifecycleEvent arg0) {
        super.unpreparing(arg0);
    }  
    
    /**
     * Méthode qui lance tous les job de clean.
     */
    private void cleanClassLoaderEar(){
        // TODO Auto-generated method stub
        close();
        cleanAllClassInClassLoader();
        //////////////////
        // Fix known leaks
        //////////////////        
        java.beans.Introspector.flushCaches(); // Clear cache of strong references
        // 
        clearReferences();

        // Deregister JDBC drivers contained in web application
        deregisterJdbcDrivers();
        // idem pour le MBEAN
        unregisterMBeans();
        // idem pour le MBEAN
        deregisterRmiTargets();   
        // Unregister MBeans loaded by the web application class loader
        unregisterMBeans();        
        // Deregister shutdown hooks - execute them immediately
        deregisterShutdownHooks();        
        clearThreadLocalsOfAllThreads();       
        stopThreads();

        try {
              ResourceBundle.clearCache();
        } catch (Exception ex) {
            //System.err.println(ex);
            //ex.printStackTrace();
        }

        // Release this classloader from Apache Commons Logging (ACL) by calling
        // LogFactory.release(getCurrentClassLoader());
        // Use reflection in case ACL is not present.
        // Do this last, in case other shutdown procedures want to log something.
        final Class logFactory = ClassLoaderUtils.findClass("org.apache.commons.logging.LogFactory");
        if (logFactory != null) { // Apache Commons Logging present
            //System.out.println("Releasing web app classloader from Apache Commons Logging");
            try {
                logFactory.getMethod("release", java.lang.ClassLoader.class).invoke(null, getEarApplicationClassLoader());
            } catch (Exception ex) {
                //ex.printStackTrace();
            }
        }
    }
    
    /**
     * enchainement des méthodes pour forcer le vidage d URLCLassLoader
     */
    public void close() {
        setJarFileNames2Close.clear();
        closeClassLoader(URLClassLoader.class.getClassLoader());
        finalizeNativeLibs(URLClassLoader.class.getClassLoader());
    }



    /**
     * Force le clean des jar loadé.
     * 
     * @param cl
     * @return
     */
    @SuppressWarnings( { "nls", "unchecked" })
    private boolean closeClassLoader(ClassLoader cl) {
        boolean res = false;
        if (cl == null) {
            return res;
        }
        Class classURLClassLoader = URLClassLoader.class;
        Field f = null;
        try {
            f = classURLClassLoader.getDeclaredField("ucp"); // récupération du champ
        } catch (NoSuchFieldException e1) {
           //e1.printStackTrace();
        }
        if (f != null) {
            f.setAccessible(true);
            Object obj = null;
            try {
                obj = f.get(cl);
            } catch (IllegalAccessException e1) {
                // ignore
            }
            if (obj != null) {
                final Object ucp = obj;
                f = null;
                try {
                    f = ucp.getClass().getDeclaredField("loaders"); // récupération du champ
                } catch (NoSuchFieldException e1) {
                    // ignore
                }
                if (f != null) {
                    f.setAccessible(true);
                    ArrayList loaders = null;
                    try {
                        loaders = (ArrayList) f.get(ucp);
                        res = true;
                    } catch (IllegalAccessException e1) {
                        // ignore
                    }
                    for (int i = 0; loaders != null && i < loaders.size(); i++) {
                        obj = loaders.get(i);
                        f = null;
                        try {
                            f = obj.getClass().getDeclaredField("jar");
                        } catch (NoSuchFieldException e) {
                            // ignore
                        }
                        if (f != null) {
                            f.setAccessible(true);
                            try {
                                obj = f.get(obj);
                            } catch (IllegalAccessException e1) {
                                // ignore
                            }
                            if (obj instanceof JarFile) {
                                final JarFile jarFile = (JarFile) obj;
                                setJarFileNames2Close.add(jarFile.getName());
                                // try {
                                // jarFile.getManifest().clear();
                                // } catch (IOException e) {
                                // // ignore
                                // }
                                try {
                                    jarFile.close();
                                } catch (IOException e) {
                                    // ignore
                                }
                            }
                        }
                    }
                }
            }
        }
        return res;
    }

    /**
     * finalize native libraries
     * 
     * @param cl
     * @return
     */
    @SuppressWarnings( { "nls", "unchecked" })
    public boolean finalizeNativeLibs(ClassLoader cl) {
        boolean res = false;
        if(cl != null){
            Class classClassLoader = ClassLoader.class;
            java.lang.reflect.Field nativeLibraries = null;
            try {
                nativeLibraries = classClassLoader.getDeclaredField("nativeLibraries");
            } catch (NoSuchFieldException e1) {
                // ignore
            }
            if (nativeLibraries == null) {
                return res;
            }
            nativeLibraries.setAccessible(true);
            Object obj = null;
            try {
                obj = nativeLibraries.get(cl);
            } catch (IllegalAccessException e1) {
                // ignore
            }
            if (!(obj instanceof Vector)) {
                return res;
            }
            res = true;
            Vector java_lang_ClassLoader_NativeLibrary = (Vector) obj;
            for (Object lib : java_lang_ClassLoader_NativeLibrary) {
                java.lang.reflect.Method finalize = null;
                try {
                    finalize = lib.getClass().getDeclaredMethod("finalize", new Class[0]);
                } catch (NoSuchMethodException e) {
                    // ignore
                }
                if (finalize != null) {
                    finalize.setAccessible(true);
                    try {
                        finalize.invoke(lib, new Object[0]);
                    } catch (IllegalAccessException e) {
                    } catch (InvocationTargetException e) {
                        // ignore
                    }
                }
            }  
        }

        return res;
    }

    /**
     * Force le clean des classes chargé par l'EAR.
     */
    private void clearReferences() {
        try {
            Field declaredField = AppClassLoaderManagerImpl.class.getDeclaredField("interAppCLMap");
            declaredField.setAccessible(true);
            Map map = (Map) declaredField.get(AppClassLoaderManager.getAppClassLoaderManager());
            map.clear();
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    /**
     * Force le clean des classes chargé par la grappe des ClassLoader.
     */
    private void cleanAllClassInClassLoader() {
        List<ClassLoader> listAllClassLoader = ClassLoaderUtils.getAllClassLoaderEAR();

        for (ClassLoader classLoader : listAllClassLoader) {
            if (classLoader != null) {
                if (classLoader instanceof weblogic.utils.classloaders.ChangeAwareClassLoader) {
                    ClassLoaderUtils.clearClass(classLoader, "cachedClasses");
                    ClassLoaderUtils.clearClass(classLoader, "modTimes");
                } else if (classLoader instanceof weblogic.utils.classloaders.FilteringClassLoader) {
                    ClassLoaderUtils.clearClass(classLoader, "classPatterns");
                    ClassLoaderUtils.clearClass(classLoader, "resourcePatterns");
                } else if (classLoader instanceof weblogic.utils.classloaders.GenericClassLoader) {
                    ClassLoaderUtils.clearClass(classLoader, "exclude");
                }
            }
        }
        listAllClassLoader.clear();
        ClassLoaderUtils.removeAllClassLoaderEAR();
    }

    /** 
     * Deregister JDBC drivers loaded by web app classloader 
     * 
     * */
    private void deregisterJdbcDrivers() {
        final List<Driver> driversToDeregister = new ArrayList<Driver>();
        final Enumeration<Driver> allDrivers = DriverManager.getDrivers();
        while (allDrivers.hasMoreElements()) {
            final Driver driver = allDrivers.nextElement();
            if (ClassLoaderUtils.isLoadedInEarApplication(driver,this.getClass())) // Should be true for all returned by DriverManager.getDrivers()
                driversToDeregister.add(driver);
        }

        for (Driver driver : driversToDeregister) {
            try {
                DriverManager.deregisterDriver(driver);
            } catch (SQLException e) {
//                System.err.println(e);
                //e.printStackTrace();
            }
        }
    }

    /**
     * provient de l'ear 
     * 
     * @return
     */
    private ClassLoader getEarApplicationClassLoader() {
        return BcodeListenerContextEar.class.getClassLoader();
    }

    /** Unregister MBeans loaded by the web application class loader */
    private void unregisterMBeans() {
        try {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            final Set<ObjectName> allMBeanNames = mBeanServer.queryNames(new ObjectName("*:*"), null);
            for (ObjectName objectName : allMBeanNames) {
                try {
                    final ClassLoader mBeanClassLoader = mBeanServer.getClassLoaderFor(objectName);
                    if (ClassLoaderUtils.getValueIsEarAppClassLoaderOrChild(mBeanClassLoader,this.getClass())) { // MBean loaded in web application
                        mBeanServer.unregisterMBean(objectName);
                    }
                } catch (Exception e) { // MBeanRegistrationException / InstanceNotFoundException
//                    System.err.println(e);
                    //e.printStackTrace();
                }
            }
        } catch (Exception e) { // MalformedObjectNameException
//            System.err.println(e);
            //e.printStackTrace();
        }
    }

    /** Find and deregister shutdown hooks. Will by default execute the hooks after removing them. */
    private void deregisterShutdownHooks() {
        // We will not remove known shutdown hooks, since loading the owning class of the hook,
        // may register the hook if previously unregistered
        Map<Thread, Thread> shutdownHooks = (Map<Thread, Thread>) ClassLoaderUtils.getStaticFieldValue("java.lang.ApplicationShutdownHooks", "hooks");
        if (shutdownHooks != null) { // Could be null during JVM shutdown, which we already avoid, but be extra precautious
            // Iterate copy to avoid ConcurrentModificationException
            for (Thread shutdownHook : new ArrayList<Thread>(shutdownHooks.keySet())) {
                if (isThreadInEarApplication(shutdownHook)) { // Planned to run in web app
                    removeShutdownHook(shutdownHook);
                }
            }
        }
    }

    private boolean isThreadInEarApplication(Thread thread) {
        return ClassLoaderUtils.isLoadedInEarApplication(thread,this.getClass()) || // Custom Thread class in web app
        ClassLoaderUtils.getValueIsEarAppClassLoaderOrChild(thread.getContextClassLoader(),this.getClass()); // Running in web application
    }

    /** Deregister shutdown hook and execute it immediately */
    @SuppressWarnings("deprecation")
    private void removeShutdownHook(Thread shutdownHook) {
        final String displayString = "'" + shutdownHook + "' of type " + shutdownHook.getClass().getName();
        Runtime.getRuntime().removeShutdownHook(shutdownHook);

        if (executeShutdownHooks) { // Shutdown hooks should be executed
            // Make sure it's from this web app instance
            shutdownHook.start(); // Run cleanup immediately

            if (shutdownHookWaitMs > 0) { // Wait for shutdown hook to finish
                try {
                    shutdownHook.join(shutdownHookWaitMs); // Wait for thread to run
                } catch (InterruptedException e) {
                    // Do nothing
                }
                if (shutdownHook.isAlive()) {
                     shutdownHook.stop();
                }
            }
        }
    }

    /** This method is heavily inspired by org.apache.catalina.loader.WebappClassLoader.clearReferencesRmiTargets() */
    private void deregisterRmiTargets() {
        try {
            final Class objectTableClass = ClassLoaderUtils.findClass("sun.rmi.transport.ObjectTable");
            if (objectTableClass != null) {
                clearRmiTargetsMap((Map<?, ?>) ClassLoaderUtils.getStaticFieldValue(objectTableClass, "objTable"));
                clearRmiTargetsMap((Map<?, ?>) ClassLoaderUtils.getStaticFieldValue(objectTableClass, "implTable"));
            }
        } catch (Exception ex) {
//            System.err.println(ex);
//            ex.printStackTrace();
        }
    }

    /** Iterate RMI Targets Map and remove entries loaded by web app classloader */
    private void clearRmiTargetsMap(Map<?, ?> rmiTargetsMap) {
        try {
            final Field cclField = ClassLoaderUtils.findFieldOfClass("sun.rmi.transport.Target", "ccl");
           for (Iterator<?> iter = rmiTargetsMap.values().iterator(); iter.hasNext();) {
                Object target = iter.next(); // sun.rmi.transport.Target
                ClassLoader ccl = (ClassLoader) cclField.get(target);
                if (ClassLoaderUtils.getValueIsEarAppClassLoaderOrChild(ccl,this.getClass())) {
                    iter.remove();
                }
            }
        } catch (Exception ex) {
//            System.err.println(ex);
//            ex.printStackTrace();
        }
    }

    private void clearThreadLocalsOfAllThreads() {
        final ThreadLocalProcessor clearingThreadLocalProcessor = new ClearingThreadLocalProcessor("ear");
        for (Thread thread : ClassLoaderUtils.getAllThreads()) {
            forEachThreadLocalInThread(thread, clearingThreadLocalProcessor);
        }
    }

    /**
     * Partially inspired by org.apache.catalina.loader.WebappClassLoader.clearReferencesThreads()
     */
    @SuppressWarnings("deprecation")
    private void stopThreads() {
        final Class<?> workerClass = ClassLoaderUtils.findClass("java.util.concurrent.ThreadPoolExecutor$Worker");
        final Field targetField = ClassLoaderUtils.findField(Thread.class, "target");

        for (Thread thread : ClassLoaderUtils.getAllThreads()) {
            final Runnable target = ClassLoaderUtils.getFieldValue(targetField, thread);
            if (thread != Thread.currentThread() && // Ignore current thread
                    (isThreadInEarApplication(thread) || ClassLoaderUtils.isLoadedInEarApplication(target,this.getClass()))) {

                if (thread.getThreadGroup() != null && ("system".equals(thread.getThreadGroup().getName()) || // System thread
                        "RMI Runtime".equals(thread.getThreadGroup().getName()))) { // RMI thread (honestly, just copied from Tomcat)

                    if ("Keep-Alive-Timer".equals(thread.getName())) {
                        thread.setContextClassLoader(getEarApplicationClassLoader().getParent());
                    }
                } else if (thread.isAlive()) { // Non-system, running in web app

                    if ("java.util.TimerThread".equals(thread.getClass().getName())) {
                        if (stopTimerThreads) {
                            stopTimerThread(thread);
                        } else {
                            //System.out.println("Timer thread is running in classloader, but will not be stopped");
                        }
                    } else {

                        // If threads is running an java.util.concurrent.ThreadPoolExecutor.Worker try shutting down the executor
                        if (workerClass != null && workerClass.isInstance(target)) {
                            if (stopThreads) {
                                try {
                                    // java.util.concurrent.ThreadPoolExecutor, introduced in Java 1.5
                                    final Field workerExecutor = ClassLoaderUtils.findField(workerClass, "this$0");
                                    final ThreadPoolExecutor executor = ClassLoaderUtils.getFieldValue(workerExecutor, target);
                                    executor.shutdownNow();
                                } catch (Exception ex) {
//                                    System.err.println(ex);
//                                    ex.printStackTrace();
                                }
                            } else{
                              //System.out.println(ThreadPoolExecutor.class.getName() + " running within the classloader will not be shut down.");
                            }
                                
                        }

                        final String displayString = "'" + thread + "' of type " + thread.getClass().getName();

                        if (stopThreads) {
                            final String waitString = (threadWaitMs > 0) ? "after " + threadWaitMs + " ms " : "";

                            if (threadWaitMs > 0) {
                                try {
                                    thread.join(threadWaitMs); // Wait for thread to run
                                } catch (InterruptedException e) {
                                    // Do nothing
                                }
                            }

                            // Normally threads should not be stopped (method is deprecated), since it may cause an inconsistent state.
                            // In this case however, the alternative is a classloader leak, which may or may not be considered worse.
                            if (thread.isAlive())
                                thread.stop();
                        } else {
                            //System.out.println("Thread " + displayString + " is still running in web app");
                        }

                    }
                }
            }
        }
    }

    private void stopTimerThread(Thread thread) {
        // Seems it is not possible to access Timer of TimerThread, so we need to mimic Timer.cancel()
        /**
         * try {
         * Timer timer = (Timer) findField(thread.getClass(), "this$0").get(thread); // This does not work!
         * warn("Cancelling Timer " + timer + " / TimeThread '" + thread + "'");
         * timer.cancel();
         * }
         * catch (IllegalAccessException iaex) {
         * error(iaex);
         * }
         */

        try {
            final Field newTasksMayBeScheduled = ClassLoaderUtils.findField(thread.getClass(), "newTasksMayBeScheduled");
            final Object queue = ClassLoaderUtils.findField(thread.getClass(), "queue").get(thread); // java.lang.TaskQueue
            final Method clear = queue.getClass().getDeclaredMethod("clear");
            clear.setAccessible(true);

            // Do what java.util.Timer.cancel() does
            // noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (queue) {
                newTasksMayBeScheduled.set(thread, false);
                clear.invoke(queue);
                queue.notify(); // "In case queue was already empty."
            }

            // We shouldn't need to join() here, thread will finish soon enough
        } catch (Exception ex) {
//            System.err.println(ex);
//            ex.printStackTrace();
        }
    }

    /**
     * Loop ThreadLocals and inheritable ThreadLocals in current Thread
     * and for each found, invoke the callback interface
     */
    private void forEachThreadLocalInCurrentThread(ThreadLocalProcessor threadLocalProcessor) {
        final Thread thread = Thread.currentThread();

        forEachThreadLocalInThread(thread, threadLocalProcessor);
    }

    private void forEachThreadLocalInThread(Thread thread, ThreadLocalProcessor threadLocalProcessor) {
        try {
            if (java_lang_Thread_threadLocals != null) {
                processThreadLocalMap(thread, threadLocalProcessor, java_lang_Thread_threadLocals.get(thread));
            }

            if (java_lang_Thread_inheritableThreadLocals != null) {
                processThreadLocalMap(thread, threadLocalProcessor, java_lang_Thread_inheritableThreadLocals.get(thread));
            }
        } catch (/* IllegalAccess */Exception ex) {
//            System.err.println(ex);
//            ex.printStackTrace();
        }
    }

    private void processThreadLocalMap(Thread thread, ThreadLocalProcessor threadLocalProcessor, Object threadLocalMap) throws IllegalAccessException {
        if (threadLocalMap != null && java_lang_ThreadLocal$ThreadLocalMap_table != null) {
            final Object[] threadLocalMapTable = (Object[]) java_lang_ThreadLocal$ThreadLocalMap_table.get(threadLocalMap); // java.lang.ThreadLocal.ThreadLocalMap.Entry[]
            for (Object entry : threadLocalMapTable) {
                if (entry != null) {
                    // Key is kept in WeakReference
                    Reference reference = (Reference) entry;
                    final ThreadLocal<?> threadLocal = (ThreadLocal<?>) reference.get();

                    if (java_lang_ThreadLocal$ThreadLocalMap$Entry_value == null) {
                        java_lang_ThreadLocal$ThreadLocalMap$Entry_value = ClassLoaderUtils.findField(entry.getClass(), "value");
                    }

                    final Object value = java_lang_ThreadLocal$ThreadLocalMap$Entry_value.get(entry);

                    threadLocalProcessor.process(thread, reference, threadLocal, value);
                }
            }
        }
    }

}
