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

    public List<BundleManifest> readBundleManifest() throws Exception {
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

    public BundleManifest readBundleManifest(String bundleName) throws Exception {
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
        Map<String, Set<ExportPackage>> exportPackageMap = indexExportPackage(bundleManifestList);
        Iterator<BundleManifest> iterator = bundleManifestList.iterator();
        while (iterator.hasNext()) {
            BundleManifest bundleManifest = iterator.next();
            ImportPackage[] importPackages = bundleManifest.getImportPackages();
            List<ImportPackage> unsatisfiedPackages = new ArrayList<ImportPackage>();
            List<ImportPackage> duplicatedPackages = new ArrayList<ImportPackage>();

            for (int i = 0; i < importPackages.length; i++) {
                String javaPackage = importPackages[i].getJavaPackage().trim();
                boolean resolution = importPackages[i].getResolution();
                if (javaPackage.isEmpty()) {
                    continue;
                }

                if (!resolution) {
                    continue;
                }

                if (exportPackageMap.containsKey(javaPackage)) {
                    Set<ExportPackage> exportPackageSet = exportPackageMap.get(javaPackage);
                    ExportPackage[] matchedExportPackages = matchPackageVersion(importPackages[i].getVersion(), exportPackageSet);
                    if (null != matchedExportPackages && matchedExportPackages.length > 0) {
                        if (matchedExportPackages.length > 1) {
                            duplicatedPackages.add(importPackages[i]);
                        }
                        continue;
                    }
                }

                if (javaPackage.indexOf(".") >= 0) {
                    String[] strArr = javaPackage.split("\\.");
                    if (null != strArr && strArr.length > 0) {
                        boolean isMatched = false;
                        for (int j = strArr.length - 2; j >= 1; j--) {
                            StringBuffer stringBuffer = new StringBuffer(strArr[0].trim());
                            for (int k = 1; k <= j; k++) {
                                stringBuffer.append("." + strArr[k].trim());
                            }
                            if (exportPackageMap.containsKey(stringBuffer.toString())) {
                                Set<ExportPackage> exportPackageSet = exportPackageMap.get(stringBuffer.toString());
                                ExportPackage[] matchedExportPackages = matchPackageVersion(importPackages[i].getVersion(), exportPackageSet);
                                if (null != matchedExportPackages && matchedExportPackages.length > 0) {
                                    if (matchedExportPackages.length > 1) {
                                        duplicatedPackages.add(importPackages[i]);
                                    }
                                    isMatched = true;
                                    break;
                                }
                            }
                        }

                        if (!isMatched) {
                            unsatisfiedPackages.add(importPackages[i]);
                        }
                    }
                } else {
                    if (!exportPackageMap.containsKey(javaPackage)) {
                        unsatisfiedPackages.add(importPackages[i]);
                    } else {
                        Set<ExportPackage> exportPackageSet = exportPackageMap.get(javaPackage);
                        ExportPackage[] matchedExportPackages = matchPackageVersion(importPackages[i].getVersion(), exportPackageSet);
                        if (null == matchedExportPackages || matchedExportPackages.length <= 0) {
                            unsatisfiedPackages.add(importPackages[i]);
                        } else {
                            if (matchedExportPackages.length > 1) {
                                duplicatedPackages.add(importPackages[i]);
                            }
                        }
                    }
                }
            }

            if (unsatisfiedPackages.size() > 0 || duplicatedPackages.size() > 0) {
                System.out.println("Unsatisfied bundle: "
                        + bundleManifest.getValue("Bundle-SymbolicName")
                        + "_" + bundleManifest.getValue("Bundle-Version"));
                if (unsatisfiedPackages.size() > 0) {
                    System.out.println("    Unsatisfied Import-Package:");
                    for (ImportPackage unsatisfiedPackage : unsatisfiedPackages) {
                        System.out.println("        " + unsatisfiedPackage.getJavaPackage()
                                + ";version=\"" + unsatisfiedPackage.getVersion() + "\"" );
                    }
                }

                if (duplicatedPackages.size() > 0) {
                    System.out.println("    Duplicated Import-Package:");
                    for (ImportPackage duplicatedPackage : duplicatedPackages) {
                        System.out.println("        " + duplicatedPackage.getJavaPackage()
                                + ";version=\"" + duplicatedPackage.getVersion() + "\"" );
                    }
                }
            }
        }
    }

    private ExportPackage[] matchPackageVersion(String importVersion, Set<ExportPackage> exportPackageSet) {
        if (null == importVersion && importVersion.isEmpty()) {
            return exportPackageSet.toArray(new ExportPackage[0]);
        }

        List<ExportPackage> matchedExportPackages = new ArrayList<ExportPackage>();
        for (ExportPackage exportPackage : exportPackageSet) {
            String exportVersion = exportPackage.getVersion();
            if (importVersion.indexOf(",") < 0) {
                if (exportVersion.compareTo(importVersion) >= 0) {
                    matchedExportPackages.add(exportPackage);
                }
            } else {
                String versionValues[] = importVersion.split(",");
                String importVersionBegin = versionValues[0].substring(1).trim();
                String importVersionEnd = versionValues[1].substring(0, versionValues[1].length() - 1).trim();
                if (importVersion.startsWith("[") && importVersion.endsWith("]")) {
                    if (exportVersion.compareTo(importVersionBegin) >= 0 && exportVersion.compareTo(importVersionEnd) <= 0) {
                        matchedExportPackages.add(exportPackage);
                    }
                } else if (importVersion.startsWith("(") && importVersion.endsWith(")")) {
                    if (exportVersion.compareTo(importVersionBegin) > 0 && exportVersion.compareTo(importVersionEnd) < 0) {
                        matchedExportPackages.add(exportPackage);
                    }
                } else if (importVersion.startsWith("[") && importVersion.endsWith(")")) {
                    if (exportVersion.compareTo(importVersionBegin) >= 0 && exportVersion.compareTo(importVersionEnd) < 0) {
                        matchedExportPackages.add(exportPackage);
                    }
                } else if (importVersion.startsWith("(") && importVersion.endsWith("]")) {
                    if (exportVersion.compareTo(importVersionBegin) > 0 && exportVersion.compareTo(importVersionEnd) <= 0) {
                        matchedExportPackages.add(exportPackage);
                    }
                }
            }
        }

        return matchedExportPackages.toArray(new ExportPackage[0]);
    }

    private Map<String, Set<ExportPackage>> indexExportPackage(List<BundleManifest> bundleManifests) {
        Map<String, Set<ExportPackage>> exportPackageMap = new HashMap<String, Set<ExportPackage>>();
        if (null != bundleManifests) {
            for (BundleManifest bundleManifest : bundleManifests) {
                ExportPackage[] exportPackages = bundleManifest.getExportPackages();
                for (ExportPackage exportPackage : exportPackages) {
                    Set<ExportPackage> exportPackageSet = null;
                    String javaPackage = exportPackage.getJavaPackage();
                    if (exportPackageMap.containsKey(javaPackage)) {
                        exportPackageSet = exportPackageMap.get(javaPackage);
                    } else {
                        exportPackageSet = new HashSet<ExportPackage>();
                        exportPackageMap.put(javaPackage, exportPackageSet);
                    }
                    exportPackageSet.add(exportPackage);
                }
            }
        }
        return exportPackageMap;
    }

    public static void main(String[] args) throws Exception {
        String path = "E:/Work/temp/wso2am-1.10.0/repository/components/plugins";
        BundleAnalyzer bundleAnalyzer = new BundleAnalyzer(path);
        bundleAnalyzer.analyzeUnsatisfiedBundles();
//        List<BundleManifest> bundleManifestList = bundleAnalyzer.readBundleManifest();
//        for (BundleManifest bundleManifest : bundleManifestList) {
//            for (ExportPackage exportPackage : bundleManifest.getExportPackages()) {
//                if (exportPackage.getJavaPackage().equals("org.wso2.carbon.registry.core.config")) {
//                    System.out.println("==========================");
//                }
//            }
//        }
    }
}
