package com.com4energy.processor.service.processing;

import com.com4energy.processor.model.FileType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class FileTypeProcessorRegistry {

    private final Map<FileType, FileTypeProcessor> processorsByType;

    public FileTypeProcessorRegistry(List<FileTypeProcessor> processors) {
        EnumMap<FileType, FileTypeProcessor> registry = new EnumMap<>(FileType.class);
        for (FileTypeProcessor processor : processors) {
            for (FileType supportedType : processor.supportedTypes()) {
                FileTypeProcessor previous = registry.putIfAbsent(supportedType, processor);
                if (previous != null) {
                    throw new IllegalStateException("Multiple processors configured for file type: " + supportedType);
                }
            }
        }
        this.processorsByType = Map.copyOf(registry);
    }

    public Optional<FileTypeProcessor> findProcessor(FileType fileType) {
        return Optional.ofNullable(fileType).map(processorsByType::get);
    }

    public FileTypeProcessor getRequiredProcessor(FileType fileType) {
        return findProcessor(fileType)
                .orElseThrow(() -> new IllegalStateException("No processor registered for file type: " + fileType));
    }

    public Set<FileType> supportedTypes() {
        return processorsByType.keySet().stream().collect(Collectors.toUnmodifiableSet());
    }
}

