package com.digiwes.osgi;

/**
 * Created by huangxm on 4/11/2016.
 */
public class ImportPackage {
    private final String RESOLUTION_OPTIONAL = "optional";
    private String javaPackage = "";
    private boolean resolution = true;
    private String version = "";

    public ImportPackage(String javaPackage, String resolution, String version) {
        if (null != javaPackage) {
            this.javaPackage = javaPackage;
        }

        if (null != resolution && resolution.equals(RESOLUTION_OPTIONAL)) {
            this.resolution = false;
        }

        if (null == version) {
            this.version = "";
        } else {
            this.version = version;
        }
    }

    public String getJavaPackage() {
        return javaPackage;
    }

    public boolean getResolution() {
        return resolution;
    }

    public String getVersion() {
        return version;
    }
}
