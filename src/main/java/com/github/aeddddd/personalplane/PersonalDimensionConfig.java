package com.github.aeddddd.personalplane;

import com.github.aeddddd.personalplane.dimension.DimensionMode;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * 模组配置中心。
 *
 * <p>配置键使用 {@code personal_plane.configuration.<key>} 形式的本地化键，
 * 与语言文件中的 {@code personal_plane.configuration.*} 对应。</p>
 */
public final class PersonalDimensionConfig {

    private static final String TRANSLATION_PREFIX = "personal_plane.configuration.";

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        COMMON = new Common(builder);
        COMMON_SPEC = builder.build();
    }

    private PersonalDimensionConfig() {
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
    }

    private static ForgeConfigSpec.Builder translation(ForgeConfigSpec.Builder builder, String key) {
        return builder.translation(TRANSLATION_PREFIX + key);
    }

    public static class Common {
        public final ForgeConfigSpec.IntValue personalDimensionFloorY;
        public final ForgeConfigSpec.IntValue personalDimensionEntryY;
        public final ForgeConfigSpec.ConfigValue<String> personalDimensionPresetPath;
        public final ForgeConfigSpec.EnumValue<DimensionMode> defaultDimensionMode;
        public final ForgeConfigSpec.BooleanValue allowAllyEnter;
        public final ForgeConfigSpec.BooleanValue allyCanBuild;
        public final ForgeConfigSpec.BooleanValue officerCanManage;
        public final ForgeConfigSpec.BooleanValue useFtbRanks;
        public final ForgeConfigSpec.IntValue opOverrideLevel;
        public final ForgeConfigSpec.IntValue denyMessageCooldownTicks;

        Common(ForgeConfigSpec.Builder builder) {
            builder.push("personalDimension");
            personalDimensionFloorY = translation(builder, "floorY")
                    .comment("个人维度地板高度")
                    .defineInRange("floorY", 64, 1, 250);
            personalDimensionEntryY = translation(builder, "entryY")
                    .comment("个人维度默认进入高度")
                    .defineInRange("entryY", 65, 2, 255);
            personalDimensionPresetPath = translation(builder, "presetPath")
                    .comment("个人维度地板样式：API 注册的命名样式 id 或预设 JSON 路径")
                    .define("presetPath", "personal_plane:default");
            defaultDimensionMode = translation(builder, "defaultDimensionMode")
                    .comment("新玩家的默认维度模式：TEAM = 跟随 FTB 队伍共用维度，PERSONAL = 始终使用个人维度")
                    .defineEnum("defaultDimensionMode", DimensionMode.TEAM);
            allowAllyEnter = translation(builder, "allowAllyEnter")
                    .comment("FTB 盟友（同队盟友队伍）是否允许进入队伍维度；盟友默认不可建造/交互")
                    .define("allowAllyEnter", true);
            allyCanBuild = translation(builder, "allyCanBuild")
                    .comment("FTB 盟友是否允许在队伍维度内建造与交互")
                    .define("allyCanBuild", false);
            officerCanManage = translation(builder, "officerCanManage")
                    .comment("队伍 OFFICER 是否可管理队伍维度（规则/配色/布局/删除）")
                    .define("officerCanManage", true);
            useFtbRanks = translation(builder, "useFtbRanks")
                    .comment("是否查询 FTB Ranks 权限节点（FTB Ranks 缺席时自动忽略）")
                    .define("useFtbRanks", true);
            opOverrideLevel = translation(builder, "opOverrideLevel")
                    .comment("可绕过维度权限的操作员等级，默认 2")
                    .defineInRange("opOverrideLevel", 2, 0, 4);
            denyMessageCooldownTicks = translation(builder, "denyMessageCooldownTicks")
                    .comment("权限拒绝提示的冷却 tick，防止聊天栏刷屏")
                    .defineInRange("denyMessageCooldownTicks", 20, 0, 200);
            builder.pop();
        }
    }
}
