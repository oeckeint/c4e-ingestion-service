package com.com4energy.processor.config.properties.features;

public class Test {

    public static int getOptimalStorage (int [] databaseStorage) {

        int n = databaseStorage.length;

        int countZero = 0;
        for (int bit : databaseStorage) {
            if (bit == 0) countZero++;
        }

        int countOne = n - countZero;

        int wrongZeros = 0;
        for (int i = countZero; i < n; i++) {
            if (databaseStorage[i] == 0) wrongZeros++;
        }

        int swapCaseA = wrongZeros;

        int wrongOnes = 0;
        for (int i = 0; i < countOne; i++) {
            if (databaseStorage[i] == 0) wrongOnes++;
        }

        int swapCaseB = wrongOnes;

        return Math.min(swapCaseA, swapCaseB);
    }


    public static void main(String[] args) {
        //int [] databaseStorage = {1,0,1,1,0,0,1}; // 1
        //int [] databaseStorage = {1,0,1,0,1}; // 1
        int [] databaseStorage = {1, 1, 0, 1, 1, 0, 0, 0, 0}; // 2
        System.out.println(getOptimalStorage(databaseStorage));
    }

}
