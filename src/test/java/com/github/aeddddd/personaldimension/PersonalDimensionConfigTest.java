package com.github.aeddddd.personaldimension;

import com.github.aeddddd.personaldimension.dimension.DimensionMode;
import com.github.aeddddd.personaldimension.testutil.ForgeConfigTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PersonalDimensionConfigTest {

    @BeforeAll
    static void bootstrap() {
        ForgeConfigTestBootstrap.bootstrap();
    }

    @Test
    void defaultsMatchDocumentedValues() {
        assertThat(PersonalDimensionConfig.COMMON.personalDimensionFloorY.get()).isEqualTo(64);
        assertThat(PersonalDimensionConfig.COMMON.personalDimensionEntryY.get()).isEqualTo(65);
        assertThat(PersonalDimensionConfig.COMMON.personalDimensionPresetPath.get())
                .isEqualTo("personal_dimension:default");
        assertThat(PersonalDimensionConfig.COMMON.defaultDimensionMode.get()).isEqualTo(DimensionMode.TEAM);
        assertThat(PersonalDimensionConfig.COMMON.allowAllyEnter.get()).isTrue();
        assertThat(PersonalDimensionConfig.COMMON.allyCanBuild.get()).isFalse();
        assertThat(PersonalDimensionConfig.COMMON.officerCanManage.get()).isTrue();
        assertThat(PersonalDimensionConfig.COMMON.useFtbRanks.get()).isTrue();
        assertThat(PersonalDimensionConfig.COMMON.opOverrideLevel.get()).isEqualTo(2);
        assertThat(PersonalDimensionConfig.COMMON.denyMessageCooldownTicks.get()).isEqualTo(20);
    }
}
