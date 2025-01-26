/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
        if (inputMin == inputMax && min == 0) {
            return null;
        }
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
//        double[] input = new double[] {
//                1.5, 1.9, 1.888888889, 2.153846154, 2.0,
//                1.166666667, 1.571428571, 1.333333333, 1.5, 1.571428571
//        };

//        double[] output = FloatUtils.softmax(input);
//        for (double v : output) {
//            System.out.println(v);
//        }
//        System.out.println("----------------------------------------");

        double[] input = new double[] { 10, 14, 60 };
        double[] result = FloatUtils.normalization(input, 0, 1);
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
