package com.wpf.jiagu.compat;

/** Factory setup and provider dispatch must survive DEX decryption. */
public class FixtureComponentFactory extends android.app.AppComponentFactory {
    static boolean applicationCreated;
    static boolean providerDispatched;

    @Override public android.app.Application instantiateApplication(ClassLoader loader, String name)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        applicationCreated = true;
        return super.instantiateApplication(loader, name);
    }

    @Override public android.content.ContentProvider instantiateProvider(ClassLoader loader, String name)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        if (!applicationCreated) throw new AssertionError("Provider created before host factory Application");
        providerDispatched = true;
        return super.instantiateProvider(loader, name);
    }
}
