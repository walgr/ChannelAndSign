//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package android.support.v4.app;

import android.app.Activity;
import android.app.AppComponentFactory;
import android.app.Application;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.Intent;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;

@RequiresApi(
    api = 28
)
@RestrictTo({Scope.LIBRARY_GROUP})
public class CoreComponentFactory extends AppComponentFactory {
    private static final String TAG = "CoreComponentFactory";

    public CoreComponentFactory() {
    }

    public Activity instantiateActivity(ClassLoader cl, String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return (Activity)checkCompatWrapper(super.instantiateActivity(cl, className, intent));
    }

    public Application instantiateApplication(ClassLoader cl, String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return (Application)checkCompatWrapper(super.instantiateApplication(cl, className));
    }

    public BroadcastReceiver instantiateReceiver(ClassLoader cl, String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return (BroadcastReceiver)checkCompatWrapper(super.instantiateReceiver(cl, className, intent));
    }

    public ContentProvider instantiateProvider(ClassLoader cl, String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return (ContentProvider)checkCompatWrapper(super.instantiateProvider(cl, className));
    }

    public Service instantiateService(ClassLoader cl, String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return (Service)checkCompatWrapper(super.instantiateService(cl, className, intent));
    }

    static <T> T checkCompatWrapper(T obj) {
        if (obj instanceof CompatWrapped) {
            T wrapper = (T)((CompatWrapped)obj).getWrapper();
            if (wrapper != null) {
                return wrapper;
            }
        }

        return obj;
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public interface CompatWrapped {
        Object getWrapper();
    }
}
