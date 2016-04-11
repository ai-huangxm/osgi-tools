package com.digiwes.osgi;

import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by huangxm on 3/30/2016.
 */
public class BundleAnalyzer {
    private String path = null;

    public BundleAnalyzer(String path) {
        this.path = (null == path) ? "." : path;

        if (this.path.endsWith("/")) {
            this.path = this.path.substring(0, this.path.length() - 1);
        }
    }

    private List<BundleManifest> readBundleManifest() throws Exception {
        File[] bundleFiles = new File(path).listFiles();
        List<BundleManifest> bundleManifestList = new ArrayList<BundleManifest>();
        for (int i = 0; i < bundleFiles.length; i++) {
            String fileName = bundleFiles[i].getName();
            if (fileName.endsWith(".jar")) {
                BundleManifest bundleManifest = readBundleManifest(fileName);
                bundleManifestList.add(bundleManifest);
            }
        }
        return bundleManifestList;
    }

    private BundleManifest readBundleManifest(String bundleName) throws Exception {
        if (null == bundleName || bundleName.isEmpty()) {
            throw new Exception("the argument \"bundleName\" cannot be null");
        }

        List<KeyValuePair> manifestList = new ArrayList<KeyValuePair>();
        Map<String, String> manifestMap = new HashMap<String, String>();

        String bundleFullName = path + "/" + bundleName;
        JarFile jarFile = new JarFile(bundleFullName);
        JarEntry entry = jarFile.getJarEntry("META-INF/MANIFEST.MF");
        BufferedReader reader = new BufferedReader(new InputStreamReader(jarFile.getInputStream(entry)));

        String strLine = reader.readLine();
        while (strLine != null) {
            Pattern pattern = Pattern.compile("\\w+:\\w*");
            Matcher matcher = pattern.matcher(strLine);
            if (matcher.find()) {
                String[] strArrary = strLine.split(":");
                String key = strArrary[0];
                StringBuffer stringBuffer = new StringBuffer();
                stringBuffer.append(strLine.substring(key.length() + 1));
                strLine = reader.readLine();
                while ((strLine != null) && 0 == (strLine.indexOf(" "))) {
                    stringBuffer.append(strLine);
                    strLine = reader.readLine();
                }

                String value = stringBuffer.toString().replace(" ", "");
                KeyValuePair keyValuePair = new KeyValuePair(key, value);
                manifestList.add(keyValuePair);
                manifestMap.put(key, value);
            } else {
                strLine = reader.readLine();
            }
        }

        reader.close();
        jarFile.close();

        return new BundleManifest(manifestList, manifestMap);
    }

    public void analyzeUnsatisfiedBundles() throws Exception {
        List<BundleManifest> bundleManifestList = readBundleManifest();
        Map<String, Set<BundleManifest>> exportPackageMap = indexExportPackage(bundleManifestList);
        Iterator<BundleManifest> iterator = bundleManifestList.iterator();
        while (iterator.hasNext()) {
            BundleManifest bundleManifest = iterator.next();
            String[] importPackages = bundleManifest.getImportPackages();
            List<String> unsatisfiedPackages = new ArrayList<String>();

            for (int i = 0; i < importPackages.length; i++) {
                String importPackage = importPackages[i].trim();
                if (importPackage.isEmpty()) {
                    continue;
                }

                if (exportPackageMap.containsKey(importPackage)) {
                    continue;
                }

                if (importPackage.indexOf(".") >= 0) {
                    String[] strArr = importPackage.split("\\.");
                    if (null != strArr && strArr.length > 0) {
                        boolean isMatched = false;
                        for (int j = strArr.length - 2; j >= 1; j--) {
                            StringBuffer stringBuffer = new StringBuffer(strArr[0].trim());
                            for (int k = 1; k <= j; k++) {
                                stringBuffer.append("." + strArr[k].trim());
                            }
                            if (exportPackageMap.containsKey(stringBuffer.toString())) {
                                isMatched = true;
                                break;
                            }
                        }

                        if (!isMatched) {
                            unsatisfiedPackages.add(importPackage);
                        }
                    }
                } else {
                    if (!exportPackageMap.containsKey(importPackage)) {
                        unsatisfiedPackages.add(importPackage);
                    }
                }
            }

            if (unsatisfiedPackages.size() > 0) {
                System.out.println("Unsatisfied bundle: "
                        + bundleManifest.getValue("Bundle-SymbolicName")
                        + "_" + bundleManifest.getValue("Bundle-Version"));
                System.out.println("    Unsatisfied Import-Package:");
                for (String unsatisfiedPackage : unsatisfiedPackages) {
                    System.out.println("        " + unsatisfiedPackage);
                }
            }
        }
    }

    private Map<String, Set<BundleManifest>> indexExportPackage(List<BundleManifest> bundleManifests) {
        Map<String, Set<BundleManifest>> exportPackageMap = new HashMap<String, Set<BundleManifest>>();
        if (null != bundleManifests) {
            Iterator<BundleManifest> iterator = bundleManifests.iterator();
            while (iterator.hasNext()) {
                BundleManifest bundleManifest = iterator.next();
                String[] exportPackages = bundleManifest.getExportPackages();
                for (int i = 0; i < exportPackages.length; i++) {
                    String exportPackage = exportPackages[i];
                    Set<BundleManifest> bundleManifestSet = null;
                    if (exportPackageMap.containsKey(exportPackage)) {
                        bundleManifestSet = exportPackageMap.get(exportPackage);
                    } else {
                        bundleManifestSet = new HashSet<BundleManifest>();
                        exportPackageMap.put(exportPackage, bundleManifestSet);
                    }
                    bundleManifestSet.add(bundleManifest);
                }
            }
        }
        return exportPackageMap;
    }

    public static void main(String[] args) throws Exception {
        String path = "E:/Work/temp/wso2am-1.10.0/repository/components/plugins";
//        String bundleName = "org.wso2.carbon.ui_4.4.3.jar";
//        String path = "E:\\Work\\temp\\tmp";
        BundleAnalyzer bundleAnalyzer = new BundleAnalyzer(path);
        bundleAnalyzer.analyzeUnsatisfiedBundles();
    }
}
