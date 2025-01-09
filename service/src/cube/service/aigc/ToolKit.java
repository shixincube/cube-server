/*
 * This source file is part of Cube.
 * <p>
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2020-2025 Ambrose Xu.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.service.aigc;

import cell.util.log.Logger;
import cube.auth.AuthConsts;
import cube.common.entity.FileLabel;
import cube.core.AbstractModule;
import cube.util.CodeUtils;
import cube.util.FileUtils;

import java.awt.image.BufferedImage;

/**
 *
 */
public class ToolKit {

    private final static ToolKit instance = new ToolKit();

    private AIGCService service;

    private AbstractModule fileStorageService;

    private ToolKit() {
    }

    public final static ToolKit getInstance() {
        return ToolKit.instance;
    }

    public void setService(AIGCService service, AbstractModule fileStorageService) {
        this.service = service;
        this.fileStorageService = fileStorageService;
    }

    public FileLabel generateBarCode(String data, int width, int height, String header, String footer) {
        BufferedImage image = CodeUtils.generateBarCode(data, width, height, header, footer);
        if (null == image) {
            Logger.e(this.getClass(), "#generateBarCode - Generate bar code image failed");
            return null;
        }

        String fileCode = FileUtils.makeFileCode(data, AuthConsts.DEFAULT_DOMAIN,
                String.format("%dx%d_%s_%s", width, height, header, footer));

        return null;
    }
}
