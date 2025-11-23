/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.module;

import cube.aigc.Module;

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

    public List<Module> getModuleList() {
        return this.moduleList;
    }

    public void addModule(Module module) {
        this.moduleList.add(module);
    }

    public void removeModule(Module module) {
        this.moduleList.remove(module);
    }

    public Module getModule(String moduleName) {
        for (Module module : this.moduleList) {
            if (module.getName().equals(moduleName)) {
                return module;
            }
        }
        return null;
    }

//    public Module matchModule(List<String> words) {
//        Module module = null;
//        for (Module mod : this.moduleList) {
//            List<String> matchingWords = mod.getMatchingWords();
//            for (String word : words) {
//                if (matchingWords.contains(word)) {
//                    module = mod;
//                    break;
//                }
//            }
//
//            if (null != module) {
//                break;
//            }
//        }
//        return module;
//    }
}
