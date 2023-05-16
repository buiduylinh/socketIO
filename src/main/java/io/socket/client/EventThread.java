//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.socket.client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EventThread extends Thread {
    private static final Logger logger = Logger.getLogger(EventThread.class.getName());
    private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {
        public Thread newThread(Runnable runnable) {
            EventThread.thread = new EventThread(runnable);
            EventThread.thread.setName("EventThread");
            EventThread.thread.setPriority(MAX_PRIORITY);
            EventThread.thread.setDaemon(Thread.currentThread().isDaemon());
            return EventThread.thread;
        }
    };
    private static EventThread thread;
    private static ExecutorService service;
    private static int counter = 0;

    private EventThread(Runnable runnable) {
        super(runnable);
    }

    public static boolean isCurrent() {
        return currentThread() == thread;
    }

    public static void exec(Runnable task) {
        if (isCurrent()) {
            task.run();
        } else {
            nextTick(task);
        }

    }

    public static void nextTick(final Runnable task) {
        Class var2 = EventThread.class;
        ExecutorService executor;
        synchronized(EventThread.class) {
            ++counter;
            if (service == null) {
                service = Executors.newSingleThreadExecutor(THREAD_FACTORY);
            }

            executor = service;
        }

        executor.execute(new Runnable() {
            public void run() {
                boolean var10 = false;

                try {
                    var10 = true;
                    task.run();
                    var10 = false;
                } catch (Throwable var13) {
                    EventThread.logger.log(Level.SEVERE, "Task threw exception", var13);
                    throw var13;
                } finally {
                    if (var10) {
                        Class var4 = EventThread.class;
                        synchronized(EventThread.class) {
                            EventThread.counter--;
                            if (EventThread.counter == 0) {
                                EventThread.service.shutdown();
                                EventThread.service = null;
                                EventThread.thread = null;
                            }

                        }
                    }
                }

                Class var1 = EventThread.class;
                synchronized(EventThread.class) {
                    EventThread.counter--;
                    if (EventThread.counter == 0) {
                        EventThread.service.shutdown();
                        EventThread.service = null;
                        EventThread.thread = null;
                    }

                }
            }
        });
    }
}
