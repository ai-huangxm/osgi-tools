package com.digiwes.osgi;

/**
 * Created by huangxm on 4/12/2016.
 */
public class ExportPackage {
    private String javaPackage = "";
    private String[] uses = null;
    private String version = "";

    public ExportPackage(String javaPackage, String uses, String version) {
        if (null != javaPackage) {
            this.javaPackage = javaPackage;
        }

        if (null != uses) {
            if (uses.indexOf(",") >= 0) {
                this.uses = uses.split(",");
            } else {
                this.uses = new String[]{uses};
            }
        } else {
            this.uses = new String[]{};
        }

        if (null == version || version.isEmpty()) {
            this.version = "0.0";
        } else {
            this.version = version;
        }
    }

    public String getJavaPackage() {
        return javaPackage;
    }

    public String[] getUses() {
        return uses;
    }

    public String getVersion() {
        return version;
    }
}
