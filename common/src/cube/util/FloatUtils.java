/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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

package cube.util;

import cell.util.Utils;

import java.util.Arrays;

public final class FloatUtils {

    private FloatUtils() {
    }

    public static double random(double floor, double ceil) {
        double scale = 10000.0f;
        int num = Utils.randomInt((int)(floor * scale), (int)(ceil * scale));
        return ((double) num) / scale;
    }

    public static double[] softmax(double[] input) {
        double[] output = new double[input.length];
        double total = Arrays.stream(input).map(Math::exp).sum();
        for (int i = 0; i < input.length; ++i) {
            output[i] = Math.exp(input[i]) / total;
        }
        return output;
    }

    public static double[] normalization(double[] input) {
        double inputMax = Arrays.stream(input).max().getAsDouble();
        double inputMin = Arrays.stream(input).min().getAsDouble();
        double[] output = new double[input.length];
        for (int i = 0; i < input.length; ++i) {
            output[i] = (input[i] - inputMin) / (inputMax - inputMin);
        }
        return output;
    }

    public static double[] normalization(double[] input, double min, double max) {
        double inputMax = Arrays.stream(input).max().getAsDouble();
        double inputMin = Arrays.stream(input).min().getAsDouble();
        double[] output = new double[input.length];
        for (int i = 0; i < input.length; ++i) {
            output[i] = (max - min) * (input[i] - inputMin) / (inputMax - inputMin) + min;
        }
        return output;
    }

    public static double[] scale(double[] input, double max) {
        double inputMax = Arrays.stream(input).max().getAsDouble();
        double scale = (max / inputMax);
        double[] output = new double[input.length];
        for (int i = 0; i < input.length; ++i) {
            output[i] = input[i] * scale;
        }
        return output;
    }

    public static void main(String[] args) {
        double[] input = new double[] {
                34, 20, 36, 26, 11, 22, 0.2, 192
        };

//        double[] output = FloatUtils.softmax(input);
//        for (double v : output) {
//            System.out.println(v);
//        }
//        System.out.println("----------------------------------------");

//        double inputMin = Arrays.stream(input).min().getAsDouble();
        double[] result = FloatUtils.normalization(input, 0, 100);
        for (double v : result) {
            System.out.println(v);
        }
        System.out.println("----------------------------------------");

//        result = FloatUtils.scale(input, 100);
//        for (double v : result) {
//            System.out.println(v);
//        }
//        System.out.println("----------------------------------------");
    }
}
