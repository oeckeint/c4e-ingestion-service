package com.com4energy.processor.service.impl;

import com.com4energy.processor.service.FileProcessingService;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class FileProcessingServiceImpl implements FileProcessingService {

    @Override
    public void processFile(File file) {
        // Simular procesamiento en un nuevo hilo
        new Thread(() -> {
            try {
                System.out.println("Procesando archivo: " + file.getName());
                Thread.sleep(5000); // Simular carga
                System.out.println("Archivo procesado: " + file.getName());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
