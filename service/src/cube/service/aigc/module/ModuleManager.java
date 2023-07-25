/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.service.aigc.module;

import java.util.ArrayList;
import java.util.List;

/**
 * 模组管理器。
 */
public class ModuleManager {

    private final static ModuleManager instance = new ModuleManager();

    private List<Module> moduleList;

    public final static ModuleManager getInstance() {
        return ModuleManager.instance;
    }

    private ModuleManager() {
        this.moduleList = new ArrayList<>();
    }

    public void start() {
        for (Module module : this.moduleList) {
            module.start();
        }
    }

    public void stop() {
        for (Module module : this.moduleList) {
            module.stop();
        }
    }

    public void addModule(Module module) {
        this.moduleList.add(module);
    }

    public void removeModule(Module module) {
        this.moduleList.remove(module);
    }

    public Module matchModule(List<String> words) {
        Module module = null;
        for (Module mod : this.moduleList) {
            List<String> matchingWords = mod.getMatchingWords();
            for (String word : words) {
                if (matchingWords.contains(word)) {
                    module = mod;
                    break;
                }
            }

            if (null != module) {
                break;
            }
        }
        return module;
    }
}
