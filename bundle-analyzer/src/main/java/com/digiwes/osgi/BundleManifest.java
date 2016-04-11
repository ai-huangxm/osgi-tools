package com.digiwes.osgi;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by huangxm on 3/31/2016.
 */
public class BundleManifest extends JarManifest {
    private String[] importPackages = null;

    private String[] exportPackages = null;

    public BundleManifest(List<KeyValuePair> list, Map<String, String> map) {
        super(list, map);
        init();
    }

    private void initImportPackage() {
        String importPackage = this.getValue("Import-Package");
        if (null == importPackage) {
            importPackages = new String[]{};
            return;
        }

        Pattern pattern = Pattern.compile(";\\s*version\\s*=\\s*\\\".*?\\\"");
        Matcher matcher = pattern.matcher(importPackage);
        if (matcher.find()) {
            importPackage = matcher.replaceAll("");
        }

        pattern = Pattern.compile(";\\s*resolution\\s*:\\s*=\\s*\\w*");
        matcher = pattern.matcher(importPackage);
        if (matcher.find()) {
            importPackage = matcher.replaceAll("");
        }

        importPackages = importPackage.split(",");
    }

    private void initExportPackage() {
        String exportPackage = this.getValue("Export-Package");
        if (null == exportPackage) {
            exportPackages = new String[]{};
            return;
        }

        Pattern pattern = Pattern.compile(";\\s*uses\\s*:\\s*=\\s*\\\".*?\\\"");
        Matcher matcher = pattern.matcher(exportPackage);
        if (matcher.find()) {
            exportPackage = matcher.replaceAll("");
        }

        pattern = Pattern.compile(";\\s*version\\s*=\\s*\\\".*?\\\"");
        matcher = pattern.matcher(exportPackage);
        if (matcher.find()) {
            exportPackage = matcher.replaceAll("");
        }

        exportPackages = exportPackage.split(",");
    }

    private void init() {
        initImportPackage();
        initExportPackage();
    }

    public String[] getImportPackages() {
        return importPackages;
    }

    public String[] getExportPackages() {
        return exportPackages;
    }
}
