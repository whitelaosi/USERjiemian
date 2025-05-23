package com.example.rdfsearch.model.semanticVisual;

/**
 * 语义视觉变量主要影响因素 - 基于PDF表3-1
 */
public class SVVInfluencingFactors {

    public enum SpaceType {
        INFORMATION_SPACE("信息空间"),
        PHYSICAL_SPACE("物理空间"),
        SOCIAL_SPACE("社会空间");

        private String description;

        SpaceType(String description) {
            this.description = description;
        }
    }

    public enum SpaceScale {
        MACRO_SCALE("宏观尺度", "市域级别"),
        MESO_SCALE("中观尺度", "区县级别"),
        MICRO_SCALE("微观尺度", "建筑级别");

        private String name;
        private String description;

        SpaceScale(String name, String description) {
            this.name = name;
            this.description = description;
        }
    }

    public enum TaskType {
        PRESENTATIONAL("展示性可视化"),
        ANALYTICAL("分析性可视化"),
        EXPLORATORY("探索性可视化");

        private String description;

        TaskType(String description) {
            this.description = description;
        }
    }

    public enum LifeCycle {
        BIRTH("诞生"),
        EVOLUTION("演化"),
        EXTINCTION("消亡");

        private String description;

        LifeCycle(String description) {
            this.description = description;
        }
    }

    // 对象特征
    public static class ObjectFeatures {
        private String spatioTemporalReference;
        private String spatioTemporalPosition;
        private String spatioTemporalForm; // 人流、水流、气流等
        private String granularity;
        private String organizationStructure;
        private String relationship;
        private String behavior;
        private String cognitiveAbility;

        // Getters and Setters
    }

    // 平台因素
    public static class PlatformFactors {
        private ViewType viewType;
        private TerminalType terminalType;

        public enum ViewType {
            WINDOWS_2D("2D窗口"),
            IMMERSIVE("沉浸式"),
            AUGMENTED_REALITY("增强现实");

            private String description;

            ViewType(String description) {
                this.description = description;
            }
        }

        public enum TerminalType {
            LARGE_DISPLAY_WALL("大型显示墙"),
            DESKTOP("桌面端"),
            MOBILE("手持移动端"),
            HEAD_MOUNTED("头戴式");

            private String description;

            TerminalType(String description) {
                this.description = description;
            }
        }
    }

    // 用户特征
    public static class UserCharacteristics {
        private String professionalDomain;
        private String taskRelevance;
        private String personalPreference;
        private String mentalState; // 认知负荷

        // Getters and Setters
    }
}
