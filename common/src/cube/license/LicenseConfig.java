/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.license;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Base64;
import java.util.Properties;

public class LicenseConfig {

    public String vendor;

    public String expiration;

    public String sn;

    public String signature;

    public LicenseConfig(File file) {
        FileInputStream fis = null;

        try {
            fis = new FileInputStream(file);
            Properties properties = new Properties();
            properties.load(fis);

            this.vendor = properties.getProperty("vendor");
            this.expiration = properties.getProperty("expiration");
            this.sn = properties.getProperty("sn");
            this.signature = properties.getProperty("signature");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException e) {
                    // Nothing
                }
            }
        }
    }

    public String extractSignContent() {
        StringBuilder buf = new StringBuilder();
        buf.append(this.vendor);
        buf.append(this.expiration);
        buf.append(this.sn);
        return buf.toString();
    }
}
