package com.digiwes.osgi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by huangxm on 3/31/2016.
 */
public class BundleManifest extends JarManifest {
    private ImportPackage[] importPackages = null;

    private ExportPackage[] exportPackages = null;

    public BundleManifest(List<KeyValuePair> list, Map<String, String> map) {
        super(list, map);
        init();
    }

    private String[] splitOsgiPackages(String osgiPackageStr) {
        if (null == osgiPackageStr || osgiPackageStr.isEmpty()) {
            return new String[]{};
        }

        List<String> packageList = new ArrayList<String>();
        int indexPkgStart = 0, doubleQuotationCount = 0;
        for (int i = 0; i < osgiPackageStr.length(); i++) {
            String currentChar = osgiPackageStr.substring(i, i + 1);
            if (currentChar.equals(",") && 0 == (doubleQuotationCount % 2)) {
                String singlePackageStr = osgiPackageStr.substring(indexPkgStart, i);
                packageList.add(singlePackageStr);
                indexPkgStart = i + 1;
                doubleQuotationCount = 0;
            }

            if (currentChar.equals("\"")) {
                doubleQuotationCount++;
            }
        }

        String singlePackageStr = osgiPackageStr.substring(indexPkgStart);
        packageList.add(singlePackageStr);
        return packageList.toArray(new String[0]);
    }

    private void initImportPackage() {
        String osgiImportPackageStr = this.getValue("Import-Package");
        if (null == osgiImportPackageStr) {
            importPackages = new ImportPackage[]{};
            return;
        }

        List<ImportPackage> importPackagesList = new ArrayList<ImportPackage>();

        String osgiImportPackages[] = splitOsgiPackages(osgiImportPackageStr);

        for (String osgiImportPackage : osgiImportPackages) {
            if (osgiImportPackage.indexOf(";") >= 0) {
                String[] parts = osgiImportPackage.split(";");
                String packageStr = "";
                String resolution = "";
                String version = "";
                for (String part : parts) {
                    if (part.isEmpty()) {
                        continue;
                    }

                    if (part.indexOf("resolution") >= 0 && part.indexOf("=") >= 0) {
                        resolution = part.substring(part.indexOf("=") + 1).trim();
                    } else if (part.indexOf("version") >= 0 && part.indexOf("=") >= 0) {
                        String tmpStr = part.substring(part.indexOf("=") + 1).trim();
                        if (tmpStr.startsWith("\"")) {
                            tmpStr = tmpStr.substring(1);
                        }
                        if (tmpStr.endsWith("\"")) {
                            tmpStr = tmpStr.substring(0, tmpStr.indexOf("\""));
                        }

                        version = tmpStr.trim();
                    } else if (part.indexOf("=") < 0) {
                        packageStr = part.trim();
                    }
                }

                importPackagesList.add(new ImportPackage(packageStr, resolution, version));
            } else {
                importPackagesList.add(new ImportPackage(osgiImportPackage, "", ""));
            }
        }

        importPackages = importPackagesList.toArray(new ImportPackage[0]);
    }

    private void initExportPackage() {
        String osgiExportPackageStr = this.getValue("Export-Package");
        if (null == osgiExportPackageStr) {
            exportPackages = new ExportPackage[]{};
            return;
        }

        List<ExportPackage> exportPackagesList = new ArrayList<ExportPackage>();

        String osgiExportPackages[] = splitOsgiPackages(osgiExportPackageStr);

        for (String osgiExportPackage : osgiExportPackages) {
            if (osgiExportPackage.indexOf(";") >= 0) {
                String[] parts = osgiExportPackage.split(";");
                String packageStr = "";
                String uses = "";
                String version = "";
                for (String part : parts) {
                    if (part.isEmpty()) {
                        continue;
                    }

                    if (part.indexOf("uses") >= 0 && part.indexOf("=") >= 0) {
                        String tmpStr = part.substring(part.indexOf("=") + 1).trim();
                        if (tmpStr.startsWith("\"")) {
                            tmpStr = tmpStr.substring(1);
                        }
                        if (tmpStr.endsWith("\"")) {
                            tmpStr = tmpStr.substring(0, tmpStr.indexOf("\""));
                        }

                        uses = tmpStr.trim();
                    } else if (part.indexOf("version") >= 0 && part.indexOf("=") >= 0) {
                        String tmpStr = part.substring(part.indexOf("=") + 1).trim();
                        if (tmpStr.startsWith("\"")) {
                            tmpStr = tmpStr.substring(1);
                        }
                        if (tmpStr.endsWith("\"")) {
                            tmpStr = tmpStr.substring(0, tmpStr.indexOf("\""));
                        }

                        version = tmpStr.trim();
                    } else if (part.indexOf("=") < 0) {
                        packageStr = part.trim();
                    }
                }

                exportPackagesList.add(new ExportPackage(packageStr, uses, version));
            } else {
                exportPackagesList.add(new ExportPackage(osgiExportPackage, "", ""));
            }
        }

        exportPackages = exportPackagesList.toArray(new ExportPackage[0]);
    }

    private void init() {
        initImportPackage();
        initExportPackage();
    }

    public ImportPackage[] getImportPackages() {
        return importPackages;
    }

    public ExportPackage[] getExportPackages() {
        return exportPackages;
    }
}
