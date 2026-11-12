package com.example;

import android.R;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import androidx.compose.runtime.internal.ComposableLambda;
import androidx.compose.runtime.internal.ComposableLambdaKt;
import androidx.media3.exoplayer.Renderer;
import androidx.media3.session.MediaController;
import com.example.data.repository.GbPlayerApi;
import defpackage.ai0;
import defpackage.ak1;
import defpackage.bc3;
import defpackage.c6;
import defpackage.cb0;
import defpackage.cw1;
import defpackage.e44;
import defpackage.et;
import defpackage.fb0;
import defpackage.fd;
import defpackage.gb0;
import defpackage.ge0;
import defpackage.gf3;
import defpackage.i33;
import defpackage.j03;
import defpackage.jg4;
import defpackage.jy0;
import defpackage.ky0;
import defpackage.la;
import defpackage.ly0;
import defpackage.m61;
import defpackage.me1;
import defpackage.my0;
import defpackage.no0;
import defpackage.ny0;
import defpackage.oe1;
import defpackage.oy0;
import defpackage.p5;
import defpackage.pb3;
import defpackage.pk2;
import defpackage.ra3;
import defpackage.rg2;
import defpackage.rh1;
import defpackage.rj4;
import defpackage.rn1;
import defpackage.tb3;
import defpackage.u5;
import defpackage.vf4;
import defpackage.vg2;
import defpackage.vg3;
import defpackage.w5;
import defpackage.yb3;
import defpackage.ym2;
import defpackage.zg2;
import defpackage.zh0;
import java.util.LinkedHashMap;

/* JADX INFO: loaded from: classes.dex */
public final class MainActivity extends fb0 {
    public static final /* synthetic */ int C = 0;
    public final w5 A;
    public volatile boolean B;

    public MainActivity() {
        final p5 p5Var = new p5(1);
        final pk2 pk2Var = new pk2(2);
        final cb0 cb0Var = this.o;
        cb0Var.getClass();
        final String str = "activity_rq#" + this.n.getAndIncrement();
        LinkedHashMap linkedHashMap = cb0Var.c;
        zg2 zg2Var = this.g;
        if (zg2Var.c.compareTo(rg2.j) >= 0) {
            StringBuilder sb = new StringBuilder("LifecycleOwner ");
            sb.append(this);
            rg2 rg2Var = zg2Var.c;
            sb.append(" is attempting to register while current state is ");
            sb.append(rg2Var);
            sb.append(". LifecycleOwners must call register before they are STARTED.");
            throw new IllegalStateException(sb.toString().toString());
        }
        cb0Var.c(str);
        u5 u5Var = (u5) linkedHashMap.get(str);
        u5Var = u5Var == null ? new u5(zg2Var) : u5Var;
        vg2 vg2Var = new vg2() { // from class: s5
            @Override // defpackage.vg2
            public final void a(xg2 xg2Var, qg2 qg2Var) {
                qg2 qg2Var2 = qg2.ON_START;
                cb0 cb0Var2 = cb0Var;
                String str2 = str;
                if (qg2Var2 != qg2Var) {
                    if (qg2.ON_STOP == qg2Var) {
                        cb0Var2.e.remove(str2);
                        return;
                    } else {
                        if (qg2.ON_DESTROY == qg2Var) {
                            cb0Var2.d(str2);
                            return;
                        }
                        return;
                    }
                }
                LinkedHashMap linkedHashMap2 = cb0Var2.e;
                Bundle bundle = cb0Var2.g;
                LinkedHashMap linkedHashMap3 = cb0Var2.f;
                pk2 pk2Var2 = pk2Var;
                p5 p5Var2 = p5Var;
                linkedHashMap2.put(str2, new t5(pk2Var2, p5Var2));
                if (linkedHashMap3.containsKey(str2)) {
                    Object obj = linkedHashMap3.get(str2);
                    linkedHashMap3.remove(str2);
                    pk2Var2.a(obj);
                }
                n5 n5Var = (n5) rt4.Z(bundle, str2, n5.class);
                if (n5Var != null) {
                    bundle.remove(str2);
                    pk2Var2.a(p5Var2.S(n5Var.g, n5Var.h));
                }
            }
        };
        u5Var.a.addObserver(vg2Var);
        u5Var.b.add(vg2Var);
        linkedHashMap.put(str, u5Var);
        this.A = new w5(cb0Var, str, p5Var, 0);
    }

    public static final boolean f(String str) {
        return (jg4.d0(str) || str.equals("none")) ? false : true;
    }

    /* JADX WARN: Code duplicated, block: B:48:0x0086  */
    @Override // defpackage.eb0, android.app.Activity, android.view.Window.Callback
    public final boolean dispatchKeyEvent(KeyEvent keyEvent) {
        me1 me1Var;
        oe1 oe1Var;
        oe1 oe1Var2;
        Long lValueOf = Long.valueOf(Renderer.DEFAULT_DURATION_TO_PROGRESS_US);
        keyEvent.getClass();
        if (keyEvent.getAction() == 0) {
            int keyCode = keyEvent.getKeyCode();
            if (keyCode == 66) {
                if (((Boolean) cw1.c.invoke()).booleanValue() && (me1Var = cw1.a) != null) {
                    me1Var.invoke();
                    return true;
                }
            } else if (keyCode == 79 || keyCode == 85) {
                me1 me1Var2 = cw1.a;
                if (me1Var2 != null) {
                    me1Var2.invoke();
                    return true;
                }
            } else if (keyCode == 89) {
                oe1 oe1Var3 = cw1.b;
                if (oe1Var3 != null) {
                    oe1Var3.invoke(-10000L);
                    return true;
                }
            } else if (keyCode == 90) {
                oe1 oe1Var4 = cw1.b;
                if (oe1Var4 != null) {
                    oe1Var4.invoke(lValueOf);
                    return true;
                }
            } else if (keyCode != 126 && keyCode != 127) {
                switch (keyCode) {
                    case 21:
                        if (((Boolean) cw1.c.invoke()).booleanValue() && (oe1Var = cw1.b) != null) {
                            oe1Var.invoke(-10000L);
                            return true;
                        }
                        break;
                    case 22:
                        if (((Boolean) cw1.c.invoke()).booleanValue() && (oe1Var2 = cw1.b) != null) {
                            oe1Var2.invoke(lValueOf);
                            return true;
                        }
                        break;
                    case 23:
                        if (((Boolean) cw1.c.invoke()).booleanValue()) {
                            me1Var.invoke();
                            return true;
                        }
                        break;
                }
            } else {
                me1 me1Var3 = cw1.a;
                if (me1Var3 != null) {
                    me1Var3.invoke();
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(keyEvent);
    }

    @Override // defpackage.fb0, defpackage.eb0, android.app.Activity
    public final void onCreate(Bundle bundle) throws Exception {
        fd ly0Var;
        super.onCreate(bundle);
        Intent intent = getIntent();
        int i = 1;
        if (intent != null && intent.getBooleanExtra("open_music", false)) {
            MediaController mediaController = j03.a;
            if (j03.d()) {
                j03.e.setValue(Boolean.TRUE);
            }
        }
        System.setProperty("java.net.preferIPv4Stack", "true");
        zh0 zh0Var = la.a;
        Context applicationContext = getApplicationContext();
        applicationContext.getClass();
        zh0 zh0Var2 = la.a;
        ai0 ai0Var = null;
        c6 c6Var = new c6(applicationContext, ai0Var, i);
        int i2 = 3;
        bc3.T(zh0Var2, null, c6Var, 3);
        vf4 vf4Var = vf4.a;
        Context applicationContext2 = getApplicationContext();
        applicationContext2.getClass();
        SharedPreferences sharedPreferences = applicationContext2.getSharedPreferences("stream_proxy_settings", 0);
        byte[] bArr = gf3.g;
        String string = sharedPreferences.getString("proxy_base_url", gf3.y(bArr));
        if (string == null) {
            string = gf3.y(bArr);
        }
        vf4.b = string;
        byte[] bArr2 = gf3.h;
        String string2 = sharedPreferences.getString("stream_secret", gf3.y(bArr2));
        if (string2 == null) {
            string2 = gf3.y(bArr2);
        }
        vf4.c = string2;
        String string3 = sharedPreferences.getString("toffee_cookie_endpoint", vf4.c());
        if (string3 == null) {
            string3 = vf4.c();
        }
        vf4.d = string3;
        vf4.e = sharedPreferences.getString("toffee_cookie_cached", null);
        vf4.f = sharedPreferences.getLong("toffee_cookie_cached_at", 0L);
        Context applicationContext3 = getApplicationContext();
        applicationContext3.getClass();
        int i3 = Build.VERSION.SDK_INT;
        if (i3 >= 26) {
            Object systemService = applicationContext3.getSystemService("notification");
            systemService.getClass();
            NotificationManager notificationManager = (NotificationManager) systemService;
            if (notificationManager.getNotificationChannel("match_reminders") == null) {
                i33.o();
                NotificationChannel notificationChannelA = ak1.a();
                notificationChannelA.setDescription("Alerts you 10 minutes before kickoff for matches you saved");
                notificationChannelA.enableLights(true);
                notificationChannelA.enableVibration(true);
                notificationManager.createNotificationChannel(notificationChannelA);
            }
        }
        rh1 rh1Var = rh1.g;
        no0 no0Var = no0.h;
        int i4 = 2;
        bc3.T(rh1Var, no0Var, new rn1(this, ai0Var, i2), 2);
        if (i3 >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != 0) {
            this.A.a("android.permission.POST_NOTIFICATIONS");
        }
        try {
            String strV = gf3.v();
            if (f(strV)) {
                Context applicationContext4 = getApplicationContext();
                applicationContext4.getClass();
                vf4.d = jg4.y0(strV).toString();
                applicationContext4.getSharedPreferences("stream_proxy_settings", 0).edit().putString("toffee_cookie_endpoint", vf4.d).apply();
            }
            String strY = gf3.y(bArr);
            String strY2 = gf3.y(bArr2);
            if (f(strY) && f(strY2)) {
                Context applicationContext5 = getApplicationContext();
                applicationContext5.getClass();
                vf4.e(applicationContext5, strY, strY2);
            }
            if (f("none")) {
                String str = m61.a;
                Context applicationContext6 = getApplicationContext();
                applicationContext6.getClass();
                m61.a = jg4.d0("none") ? null : "none";
                e44.a.a(applicationContext6).edit().putString("firebase_refresh_token", "none").apply();
            }
            if (f("none")) {
                GbPlayerApi.Companion.setCloudLookupUrl("none");
                bc3.T(rh1Var, no0Var, new et(i4, i2, ai0Var), 2);
            }
        } catch (Throwable unused) {
        }
        rj4 rj4Var = new rj4();
        rj4 rj4Var2 = new rj4();
        int i5 = jy0.a;
        View decorView = getWindow().getDecorView();
        decorView.getClass();
        decorView.getResources().getClass();
        decorView.getResources().getClass();
        int i6 = Build.VERSION.SDK_INT;
        if (i6 >= 30) {
            ly0Var = new oy0();
        } else if (i6 >= 29) {
            ly0Var = new ny0();
        } else if (i6 >= 28) {
            ly0Var = new my0();
        } else {
            ly0Var = i6 >= 26 ? new ly0() : new ky0();
        }
        fd fdVar = ly0Var;
        Window window = getWindow();
        window.getClass();
        fdVar.Z(rj4Var, rj4Var2, window, decorView, true, true);
        Window window2 = getWindow();
        window2.getClass();
        fdVar.l(window2);
        ComposableLambda composableLambdaComposableLambdaInstance = ComposableLambdaKt.composableLambdaInstance(-601144069, true, new ym2(this, i));
        ViewGroup.LayoutParams layoutParams = gb0.a;
        View childAt = ((ViewGroup) getWindow().getDecorView().findViewById(R.id.content)).getChildAt(0);
        ge0 ge0Var = childAt instanceof ge0 ? (ge0) childAt : null;
        if (ge0Var != null) {
            ge0Var.setParentCompositionContext(null);
            ge0Var.setContent(composableLambdaComposableLambdaInstance);
            return;
        }
        ge0 ge0Var2 = new ge0(this);
        ge0Var2.setParentCompositionContext(null);
        ge0Var2.setContent(composableLambdaComposableLambdaInstance);
        View decorView2 = getWindow().getDecorView();
        if (pb3.p(decorView2) == null) {
            decorView2.setTag(com.aistudio.bdstream.vfxrtq.R.id.view_tree_lifecycle_owner, this);
        }
        if (yb3.n(decorView2) == null) {
            decorView2.setTag(com.aistudio.bdstream.vfxrtq.R.id.view_tree_view_model_store_owner, this);
        }
        if (tb3.m(decorView2) == null) {
            decorView2.setTag(com.aistudio.bdstream.vfxrtq.R.id.view_tree_saved_state_registry_owner, this);
        }
        setContentView(ge0Var2, gb0.a);
    }

    @Override // defpackage.fb0, android.app.Activity
    public final void onNewIntent(Intent intent) {
        intent.getClass();
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.getBooleanExtra("open_music", false)) {
            MediaController mediaController = j03.a;
            if (j03.d()) {
                j03.e.setValue(Boolean.TRUE);
            }
        }
    }

    @Override // defpackage.fb0, android.app.Activity
    public final void onPictureInPictureModeChanged(boolean z, Configuration configuration) {
        configuration.getClass();
        super.onPictureInPictureModeChanged(z, configuration);
        vg3.b.setValue(Boolean.valueOf(z));
    }

    @Override // defpackage.fb0, android.app.Activity
    public final void onUserLeaveHint() {
        if (!this.B && Build.VERSION.SDK_INT >= 31) {
            try {
                ak1.n();
                setPictureInPictureParams(ak1.d().setAutoEnterEnabled(false).build());
            } catch (Throwable unused) {
            }
        }
        super.onUserLeaveHint();
        if (this.B) {
            boolean z = vg3.a;
            if (vg3.a) {
                vg3.c(this);
            }
        }
    }

    @Override // android.app.Activity
    public final void setRequestedOrientation(int i) {
        if (ra3.v(this)) {
            super.setRequestedOrientation(-1);
        } else {
            super.setRequestedOrientation(i);
        }
    }
}
