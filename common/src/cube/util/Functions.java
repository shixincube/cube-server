/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.util;

public final class Functions {

    private Functions() {
    }

    public static double sum(double[] data) {
        double sum = 0;
        for (double v : data) {
            sum += v;
        }
        return sum;
    }

    /**
     * 均值。
     *
     * @param data
     * @return
     */
    public static double mean(double[] data) {
        return sum(data) / (double) data.length;
    }

    /**
     * 样本方差。
     *
     * @param data
     * @return
     */
    public static double sampleVariance(double[] data) {
        double variance = 0;
        double mean = mean(data);
        for (double v : data) {
            variance = variance + (Math.pow((v - mean), 2));
        }
        variance = variance / (data.length - 1);
        return variance;
    }

    /**
     * 样本标准差。
     *
     * @param data
     * @return
     */
    public static double sampleStandardDeviation(double[] data) {
        return Math.sqrt(sampleVariance(data));
    }

    public static void main(String[] args) {
        double[] data = new double[]{ 1000, 1200, 1300, 2000 };
        System.out.println(mean(data));
        System.out.println(sampleStandardDeviation(data));
        System.out.println(sampleStandardDeviation(data) / mean(data));

        data = new double[]{ 0, 10, 23, 1000 };
        System.out.println(mean(data));
        System.out.println(sampleStandardDeviation(data));
        System.out.println(sampleStandardDeviation(data) / mean(data));
    }
}
