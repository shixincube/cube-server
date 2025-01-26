/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
