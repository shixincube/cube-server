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

package cube.plugin.test;

import cube.plugin.HookResult;
import cube.plugin.LuaPlugin;
import cube.plugin.PluginContext;
import cube.plugin.PluginSystem;

import java.io.FileNotFoundException;

public class TestLuaPlugin {

    public TestLuaPlugin() {
    }

    public void start() {
        PluginSystem.load();
    }

    public void test() {
        SimplePlugin plugin = new SimplePlugin();

        for (int i = 0; i < 2; ++i) {
            SimpleContext context = new SimpleContext("This is a plugin with lua");

            System.out.println("----------------------------------------");

            plugin.launch(context);

            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println("----------------------------------------");
        }
    }

    public void stop() {
        PluginSystem.unlaod();
    }

    public static void main(String[] args) {
        TestLuaPlugin test = new TestLuaPlugin();

        test.start();

        test.test();

        test.stop();
    }


    public class SimplePlugin extends LuaPlugin {

        public SimplePlugin() {
            super("test/plugin.lua");
        }

        @Override
        public void setup() {
            // Nothing
        }

        @Override
        public void teardown() {
            // Nothing
        }

        @Override
        public HookResult launch(PluginContext context) {
            try {
                this.call(context);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            System.out.println("Data: " + ((SimpleContext)context).text);
            return null;
        }
    }

    public class SimpleContext extends PluginContext {

        protected String text;

        public SimpleContext(String text) {
            super();
            this.text = text;
        }

        @Override
        public Object get(String name) {
            return null;
        }

        @Override
        public void set(String name, Object value) {
        }
    }
}
