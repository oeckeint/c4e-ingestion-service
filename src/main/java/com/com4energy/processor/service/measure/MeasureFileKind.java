package com.com4energy.processor.service.measure;

import com.com4energy.processor.model.FileType;

public enum MeasureFileKind {
    F5(FileType.MEDIDA_QH_F5),
    P5(FileType.MEDIDA_QH_P5),
    P1(FileType.MEDIDA_QH_P1),
    P2(FileType.MEDIDA_QH_P2);

    private final FileType fileType;

    MeasureFileKind(FileType fileType) {
        this.fileType = fileType;
    }

    public FileType toFileType() {
        return fileType;
    }

    public static MeasureFileKind fromPrefix(String token) {
        if (token == null || token.length() < 2) {
            throw new InvalidMeasureFilenameException("El nombre del archivo no contiene un prefijo de medida válido");
        }

        return switch (token.substring(0, 2).toLowerCase()) {
            case "f5" -> F5;
            case "p5" -> P5;
            case "p1" -> P1;
            case "p2" -> P2;
            default -> throw new InvalidMeasureFilenameException("El tipo de medida del archivo no es reconocido");
        };
    }
}

