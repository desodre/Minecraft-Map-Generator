package org.learn.minecraftmap.generator;

import com.sun.jna.Pointer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.learn.minecraftmap.jna.CubiomesNative;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class CustomDatapackManager {

    private static final Logger logger = LoggerFactory.getLogger(CustomDatapackManager.class);

    @Value("${cubiomes.datapack-path:}")
    private String datapackPath;

    private Pointer customTreePointer;

    @PostConstruct
    public void init() {
        if (datapackPath == null || datapackPath.trim().isEmpty()) {
            logger.info("No custom datapack path configured. Operating in standard vanilla mode.");
            return;
        }

        File file = new File(datapackPath);
        if (!file.exists()) {
            logger.error("Configured custom datapack file does not exist: {}", datapackPath);
            return;
        }

        logger.info("Loading custom biome datapack from: {}", datapackPath);
        try {
            customTreePointer = CubiomesNative.loadCustomTreeFromDatapack(datapackPath);
            if (customTreePointer == null) {
                logger.error("Failed to parse datapack or build KdTree from: {}", datapackPath);
            } else {
                logger.info("Successfully loaded custom datapack and constructed native 6D Kd-Tree.");
            }
        } catch (Exception e) {
            logger.error("Error loading custom datapack", e);
        }
    }

    public void setDatapackPath(String datapackPath) {
        this.datapackPath = datapackPath;
    }

    public Pointer getCustomTree() {
        return customTreePointer;
    }

    @PreDestroy
    public void cleanup() {
        if (customTreePointer != null) {
            logger.info("Releasing native memory allocated for the custom datapack Kd-Tree...");
            try {
                CubiomesNative.freeCustomTree(customTreePointer);
                customTreePointer = null;
                logger.info("Successfully released native custom datapack memory.");
            } catch (Exception e) {
                logger.error("Error freeing custom datapack Kd-Tree memory", e);
            }
        }
    }
}
