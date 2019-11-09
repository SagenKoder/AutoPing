package app.sagen.autoping;

import lc.kra.system.keyboard.GlobalKeyboardHook;
import lc.kra.system.keyboard.event.GlobalKeyEvent;
import lc.kra.system.keyboard.event.GlobalKeyListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AutoPingMain {

    private static final Duration DURATION = Duration.of(5, ChronoUnit.MINUTES);
    private static final int KEY_TO_PRESS = KeyEvent.VK_SHIFT;

    private static Instant lastUsed = Instant.now();

    private static ScheduledExecutorService exec;

    public static void main(String[] args) {
        System.out.println("Registering global keyboard hook...");

        try {
            GlobalKeyboardHook globalKeyboardHook = new GlobalKeyboardHook(true);
            globalKeyboardHook.addKeyListener(new GlobalKeyListener() {
                @Override
                public void keyPressed(GlobalKeyEvent globalKeyEvent) {
                    if (globalKeyEvent.getVirtualKeyCode() != KEY_TO_PRESS) {
                        lastUsed = Instant.now();
                    }
                }

                @Override
                public void keyReleased(GlobalKeyEvent globalKeyEvent) {
                } // ignored
            });
        } catch (Exception e) {
            System.err.println("Critical error occurred while registering global keyboard hook!");
            e.printStackTrace();
            showCriticalAlertAndShutdown(e);
        }

        System.out.println("Starting scheduler to run");
        exec = Executors.newScheduledThreadPool(1);
        exec.scheduleAtFixedRate(new Runnable() {
            public void run() {
                System.out.println("Starting scheduled task");
                if(Instant.now().isBefore(lastUsed.plus(DURATION))) {
                    System.out.println("Too little time since last use... waiting for next task");
                    return; // used less than 5 minutes ago
                }
                System.out.println("Trying to press a key....");
                try {
                    Robot robot = new Robot();
                    robot.keyPress(16);
                    Thread.sleep(30L);
                    robot.keyRelease(16);
                    throw new RuntimeException("asd");
                } catch (Exception e) {
                    System.err.println("Critical error occurred while trying to press keys!");
                    e.printStackTrace();
                    exec.shutdown();
                    exec.shutdownNow(); // shutdown if error
                    showCriticalAlertAndShutdown(e);
                }
                System.out.println("Done.");
            }
        }, 5, DURATION.get(ChronoUnit.SECONDS), TimeUnit.SECONDS);
    }

    public static void showCriticalAlertAndShutdown(Exception e) {

        StringWriter errors = new StringWriter();
        e.printStackTrace(new PrintWriter(errors));
        String stackTract = errors.toString();


        JTextArea jta = new JTextArea(stackTract);
        JScrollPane jsp = new JScrollPane(jta){
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(480, 320);
            }
        };
        JOptionPane.showMessageDialog(null, jsp, "AutoPing - Kritisk feil", JOptionPane.ERROR_MESSAGE);
    }

}
