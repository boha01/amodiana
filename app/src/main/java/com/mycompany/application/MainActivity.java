package com.mycompany.application;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Compatibility bridge for ChillbaseActivity.onCreate -> MainActivity.Start(Context)
 * and early GuardInitProvider initialization.
 */
public class MainActivity {
    private static final String TAG = "SilkBridge";
    public static volatile boolean silkInstalled = false;
    public static volatile String silkStatus = "Pending";
    private static volatile boolean diagnosticShown = false;

    public static synchronized boolean ensureSilkGuard(Context ctx) {
        if (silkInstalled) return true;
        if (ctx == null) {
            silkStatus = "SilkGuard FAIL: null context";
            return false;
        }
        Context appCtx = ctx.getApplicationContext() != null ? ctx.getApplicationContext() : ctx;

        Class<?> sg = null;
        Throwable lastErr = null;
        ClassLoader[] loaders = new ClassLoader[] {
                ctx.getClassLoader(),
                appCtx.getClassLoader(),
                MainActivity.class.getClassLoader(),
                ClassLoader.getSystemClassLoader()
        };
        for (ClassLoader cl : loaders) {
            if (cl == null) continue;
            try {
                sg = Class.forName("com.silk.guard.SilkGuard", true, cl);
                if (sg != null) break;
            } catch (Throwable t) {
                lastErr = t;
            }
        }
        if (sg == null) {
            try {
                sg = Class.forName("com.silk.guard.SilkGuard");
            } catch (Throwable t) {
                lastErr = t;
            }
        }

        if (sg == null) {
            silkStatus = "SilkGuard FAIL: classes6.dex missing (" + describeErr(lastErr) + ")";
            Log.e(TAG, silkStatus, lastErr);
            return false;
        }

        try {
            Method m = sg.getMethod("install", Context.class);
            Object res = m.invoke(null, ctx);
            boolean ok = Boolean.TRUE.equals(res);
            if (!ok && appCtx != ctx) {
                res = m.invoke(null, appCtx);
                ok = Boolean.TRUE.equals(res);
            }
            if (ok) {
                silkInstalled = true;
                silkStatus = "SilkGuard: OK";
                Log.i(TAG, silkStatus);
                return true;
            }
            silkStatus = "SilkGuard FAIL: install()=false (check libsilkcore.so STORED/zipalign & assets/sys/q.dat)";
            Log.e(TAG, silkStatus);
            return false;
        } catch (Throwable t) {
            Throwable cause = (t instanceof InvocationTargetException && t.getCause() != null) ? t.getCause() : t;
            silkStatus = "SilkGuard ERR: " + describeErr(cause);
            Log.e(TAG, silkStatus, cause);
            return false;
        }
    }

    private static String describeErr(Throwable t) {
        if (t == null) return "unknown";
        String msg = t.getMessage();
        return (msg != null && !msg.isEmpty())
                ? (t.getClass().getSimpleName() + ": " + msg)
                : t.getClass().getSimpleName();
    }

    public static void Start(final Context ctx) {
        if (ctx == null) return;
        final boolean ok = ensureSilkGuard(ctx);

        final Context appCtx = ctx.getApplicationContext() != null ? ctx.getApplicationContext() : ctx;
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override public void run() {
                if (!ok && !diagnosticShown) {
                    diagnosticShown = true;
                    try {
                        Toast.makeText(appCtx, silkStatus, Toast.LENGTH_LONG).show();
                    } catch (Throwable ignored) {}
                }
                com.bonika.modmenu.MainActivity.startMenu(ctx);
                // Ensure NativeBridge class is initialized even if overlay permission is pending
                try {
                    com.bonika.modmenu.NativeBridge.isLibraryLoaded();
                } catch (Throwable ignored) {
                    try { com.bonika.modmenu.MainActivity.startMenu(appCtx); } catch (Throwable ignored2) {}
                }
            }
        }, 600);
    }
}
