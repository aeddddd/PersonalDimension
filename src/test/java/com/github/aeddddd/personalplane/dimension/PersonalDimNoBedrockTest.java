package com.github.aeddddd.personalplane.dimension;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 「个人维度不生成基岩」哨兵：生成、重铺、预设采样与样式切片路径都不得再引用基岩。
 */
class PersonalDimNoBedrockTest {

    private static final Path DIMENSION_PKG = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath()
            .resolve("src/main/java/com/github/aeddddd/personalplane/dimension");

    @Test
    void dimensionSourcesShouldNotReferenceBedrock() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (String file : List.of("PersonalDimChunkGenerator.java", "PersonalDimensionManager.java",
                "PresetLoader.java", "ModFloorStyles.java", "FloorPreset.java")) {
            String source = Files.readString(DIMENSION_PKG.resolve(file), StandardCharsets.UTF_8);
            if (source.toLowerCase(Locale.ROOT).contains("bedrock")) {
                offenders.add(file);
            }
        }
        assertThat(offenders).as("仍引用基岩的个人维度源文件").isEmpty();
    }
}
