/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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

package cube.plugin;

import org.keplerproject.luajava.LuaState;
import org.keplerproject.luajava.LuaStateFactory;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * 基于 Lua 实现的插件。
 */
public abstract class LuaPlugin implements Plugin {

    /**
     * Lua 脚本文件。
     */
    private File luaFile;

    private LuaState luaState;

    public LuaPlugin(String luaFilename) {
        this.luaFile = new File(luaFilename);
    }

    public File getLuaFile() {
        return this.luaFile;
    }

    public void call(PluginContext context) throws FileNotFoundException {
        if (!this.luaFile.exists()) {
            throw new FileNotFoundException("Not found file " + this.luaFile.getAbsolutePath());
        }

        this.prepare();

        // 查找参数
        this.luaState.getField(LuaState.LUA_GLOBALSINDEX, "onAction");

        // 参数压栈
        this.luaState.pushJavaObject(context);

        // 调用 onAction
        this.luaState.call(1, 0);
    }

    public void close() {
        if (null != this.luaState) {
            this.luaState.close();
        }
    }

    private void prepare() {
        if (null != this.luaState) {
            return;
        }

        this.luaState = LuaStateFactory.newLuaState();
        this.luaState.openLibs();

        this.luaState.LdoFile(this.luaFile.getAbsolutePath());
    }
}
