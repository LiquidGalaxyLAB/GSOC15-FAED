package is.erle.lg;

import interactivespaces.activity.impl.BaseActivity;

/**
 * A simple Interactive Spaces Java-based activity.
 */
public class IsErleLgActivity extends BaseActivity {

    @Override
    public void onActivitySetup() {
        getLog().info("Activity is.erle.lg setup");
    }

    @Override
    public void onActivityStartup() {
        getLog().info("Activity is.erle.lg startup");
    }

    @Override
    public void onActivityPostStartup() {
        getLog().info("Activity is.erle.lg post startup");
    }

    @Override
    public void onActivityActivate() {
        getLog().info("Activity is.erle.lg activate");
    }

    @Override
    public void onActivityDeactivate() {
        getLog().info("Activity is.erle.lg deactivate");
    }

    @Override
    public void onActivityPreShutdown() {
        getLog().info("Activity is.erle.lg pre shutdown");
    }

    @Override
    public void onActivityShutdown() {
        getLog().info("Activity is.erle.lg shutdown");
    }

    @Override
    public void onActivityCleanup() {
        getLog().info("Activity is.erle.lg cleanup");
    }
}
