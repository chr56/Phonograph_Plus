package com.kabouzeid.appthemehelper.color;

import android.graphics.Color;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;

import com.kabouzeid.appthemehelper.R;

/**
 * Created by mikepenz on 07.07.15.
 */
public class MaterialColor {

    public static class Elements {

        /**
         * for light backgrounds
         */
        public enum Light implements IColor {
            ICON("#8A000000", R.color.md_light_primary_icon),
            TEXT("#DE000000", R.color.md_light_primary_text),
            SECONDARY_TEXT("#8A000000", R.color.md_light_secondary),
            SECONDARY_ICON("#8A000000", R.color.md_light_secondary),
            DISABLED_TEXT("#42000000", R.color.md_light_disabled),
            HINT_TEXT("#42000000", R.color.md_light_disabled),
            DIVIDER("#1F000000", R.color.md_light_dividers);

            String color;
            int resource;

            Light(String color, int resource) {
                this.color = color;
                this.resource = resource;
            }

            @Override
            public String getAsString() {
                return color;
            }

            @Override
            @ColorInt
            public int getAsColor() {
                return Color.parseColor(color);
            }

            @Override
            @ColorRes
            public int getAsResource() {
                return resource;
            }
        }

        /**
         * for dark backgrounds
         */
        public enum Dark implements IColor {
            ICON("#8AFFFFFF", R.color.md_dark_primary_icon),
            TEXT("#DEFFFFFF", R.color.md_dark_primary_text),
            SECONDARY_TEXT("#8AFFFFFF", R.color.md_dark_secondary),
            SECONDARY_ICON("#8AFFFFFF", R.color.md_dark_secondary),
            DISABLED_TEXT("#42FFFFFF", R.color.md_dark_disabled),
            HINT_TEXT("#42FFFFFF", R.color.md_dark_disabled),
            DIVIDER("#1FFFFFFF", R.color.md_dark_dividers);

            String color;
            int resource;

            Dark(String color, int resource) {
                this.color = color;
                this.resource = resource;
            }

            @Override
            public String getAsString() {
                return color;
            }

            @Override
            @ColorInt
            public int getAsColor() {
                return Color.parseColor(color);
            }

            @Override
            @ColorRes
            public int getAsResource() {
                return resource;
            }
        }
    }


    public enum Red implements IColor {
        _50("#FFEBEE", R.color.md_red_50),
        _100("#FFCDD2", R.color.md_red_100),
        _200("#EF9A9A", R.color.md_red_200),
        _300("#E57373", R.color.md_red_300),
        _400("#EF5350", R.color.md_red_400),
        _500("#F44336", R.color.md_red_500),
        _600("#E53935", R.color.md_red_600),
        _700("#D32F2F", R.color.md_red_700),
        _800("#C62828", R.color.md_red_800),
        _900("#B71C1C", R.color.md_red_900),
        _A100("#FF8A80", R.color.md_red_A100),
        _A200("#FF5252", R.color.md_red_A200),
        _A400("#FF1744", R.color.md_red_A400),
        _A700("#D50000", R.color.md_red_A700);

        String color;
        int resource;

        Red(String color, int resource) {
            this.color = color;
            this.resource = resource;
        }

        @Override
        public String getAsString() {
            return color;
        }

        @Override
        @ColorInt
        public int getAsColor() {
            return Color.parseColor(color);
        }

        @Override
        @ColorRes
        public int getAsResource() {
            return resource;
        }
    }

    public enum Pink implements IColor {

        _50("#E91E63", R.color.md_pink_50),
        _100("#F8BBD0", R.color.md_pink_100),
        _200("#F48FB1", R.color.md_pink_200),
        _300("#F06292", R.color.md_pink_300),
        _400("#EC407A", R.color.md_pink_400),
        _500("#E91E63", R.color.md_pink_500),
        _600("#D81B60", R.color.md_pink_600),
        _700("#C2185B", R.color.md_pink_700),
        _800("#AD1457", R.color.md_pink_800),
        _900("#880E4F", R.color.md_pink_900),
        _A100("#FF80AB", R.color.md_pink_A100),
        _A200("#FF4081", R.color.md_pink_A200),
        _A400("#F50057", R.color.md_pink_A400),
        _A700("#C51162", R.color.md_pink_A700);

        String color;
        int resource;

        Pink(String color, int resource) {
            this.color = color;
            this.resource = resource;
        }

        @Override
        public String getAsString() {
            return color;
        }

        @Override
        @ColorInt
        public int getAsColor() {
            return Color.parseColor(color);
        }

        @Override
        @ColorRes
        public int getAsResource() {
            return resource;
        }
    }

    public enum Purple implements IColor {

        _50("#F3E5F5", R.color.md_purple_50),
        _100("#E1BEE7", R.color.md_purple_100),
        _200("#CE93D8", R.color.md_purple_200),
        _300("#BA68C8", R.color.md_purple_300),
        _400("#AB47BC", R.color.md_purple_400),
        _500("#9C27B0", R.color.md_purple_500),
        _600("#8E24AA", R.color.md_purple_600),
        _700("#7B1FA2", R.color.md_purple_700),
        _800("#6A1B9A", R.color.md_purple_800),
        _900("#4A148C", R.color.md_purple_900),
        _A100("#EA80FC", R.color.md_purple_A100),
        _A200("#E040FB", R.color.md_purple_A200),
        _A400("#D500F9", R.color.md_purple_A400),
        _A700("#AA00FF", R.color.md_purple_A700);

        String color;
        int resource;

        Purple(String color, int resource) {
            this.color = color;
            this.resource = resource;
        }

        @Override
        public String getAsString() {
            return color;
        }

        @Override
        @ColorInt
        public int getAsColor() {
            return Color.parseColor(color);
        }

        @Override
        @ColorRes
        public int getAsResource() {
            return resource;
        }
    }

    public enum DeepPurple implements IColor {

        _50("#EDE7F6", R.color.md_purple_50),
        _100("#D1C4E9", R.color.md_purple_100),
        _200("#B39DDB", R.color.md_purple_200),
        _300("#9575CD", R.color.md_purple_300),
        _400("#7E57C2", R.color.md_purple_400),
        _500("#673AB7", R.color.md_purple_500),
        _600("#5E35B1", R.color.md_purple_600),
        _700("#512DA8", R.color.md_purple_700),
        _800("#4527A0", R.color.md_purple_800),
        _900("#311B92", R.color.md_purple_900),
        _A100("#B388FF", R.color.md_purple_A100),
        _A200("#7C4DFF", R.color.md_purple_A200),
        _A400("#651FFF", R.color.md_purple_A400),
        _A700("#6200EA", R.color.md_purple_A700);

        String color;
        int resource;

        DeepPurple(String color, int resource) {
            this.color = color;
            this.resource = resource;
        }

        @Override
        public String getAsString() {
            return color;
        }

        @Override
        @ColorInt
        public int getAsColor() {
            return Color.parseColor(color);
        }

        @Override
        @ColorRes
        public int getAsResource() {
            return resource;
        }
    }

    public enum Indigo implements IColor {

        _50("#E8EAF6", R.color.md_indigo_50),
        _100("#C5CAE9", R.color.md_indigo_100),
        _200("#9FA8DA", R.color.md_indigo_200),
        _300("#7986CB", R.color.md_indigo_300),
        _400("#5C6BC0", R.color.md_indigo_400),
        _500("#3F51B5", R.color.md_indigo_500),
        _600("#3949AB", R.color.md_indigo_600),
        _700("#303F9F", R.color.md_indigo_700),
        _800("#283593", R.color.md_indigo_800),
        _900("#1A237E", R.color.md_indigo_900),
        _A100("#8C9EFF", R.color.md_indigo_A100),
        _A200("#536DFE", R.color.md_indigo_A200),
        _A400("#3D5AFE", R.color.md_indigo_A400),
        _A700("#304FFE", R.color.md_indigo_A700);

        String color;
        int resource;

        Indigo(String color, int resource) {
            this.color = color;
            this.resource = resource;
        }

        @Override
        public String getAsString() {
            return color;
        }

        @Override
        @ColorInt
        public int getAsColor() {
            return Color.parseColor(color);
        }

        @Override
        @ColorRes
        public int getAsResource() {
            return resource;
        }
    }

    public enum Blue implements IColor {

        _50("#E3F2FD", R.color.md_blue_50),
        _100("#BBDEFB", R.color.md_blue_100),
        _200("#90CAF9", R.color.md_blue_200),
        _300("#64B5F6", R.color.md_blue_300),
        _400("#42A5F5", R.color.md_blue_400),
        _500("#2196F3", R.color.md_blue_500),
        _600("#1E88E5", R.color.md_blue_600),
        _700("#1976D2", R.color.md_blue_700),
        _800("#1565C0", R.color.md_blue_800),
        _900("#0D47A1", R.color.md_blue_900),
        _A100("#82B1FF", R.color.md_blue_A100),
        _A200("#448AFF", R.color.md_blue_A200),
        _A400("#2979FF", R.color.md_blue_A400),
        _A700("#2962FF", R.color.md_blue_A700);

        String color;
        int resource;

        Blue(String color, int resource) {
            this.color = color;
            this.resource = resource;
        }

        @Override
        public String getAsString() {
            return color;
        }

        @Override
        @ColorInt
        public int getAsColor() {
            return Color.parseColor(color);
        }

        @Override
        @ColorRes
        public int getAsResource() {
            return resource;
        }
    }

    public enum LightBlue implements IColor {

        _50("#E1F5FE", R.color.md_light_blue_50),
        _100("#B3E5FC", R.color.md_light_blue_100),
        _200("#81D4FA", R.color.md_light_blue_200),
        _300("#4FC3F7", R.color.md_light_blue_300),
        _400("#29B6F6", R.color.md_light_blue_400),
        _500("#03A9F4", R.color.md_light_blue_500),
        _600("#039BE5", R.color.md_light_blue_600),
        _700("#0288D1", R.color.md_light_blue_700),
        _800("#0277BD", R.color.md_light_blue_800),
        _900("#01579B", R.color.md_light_blue_900),
        _A100("#80D8FF", R.color.md_light_blue_A100),
        _A200("#40C4FF", R.color.md_light_blue_A200),
        _A400("#00B0FF", R.color.md_light_blue_A400),
        _A700("#0091EA", R.color.md_light_blue_A700);

        String color;
        int resource;

        LightBlue(String color, int resource) {
            this.color = color;
            this.resource = resource;
        }

        @Override
        public String getAsString() {
            return color;
        }

        @Override
        @ColorInt
        public int getAsColor() {
            return Color.parseColor(color);
        }

        @Override
        @ColorRes
        public int getAsResource() {
            return resource;
        }
    }

    public enum Cyan implements IColor {

        _50("#E0F7FA", R.color.md_cyan_50),
        _100("#B2EBF2", R.color.md_cyan_100),
        _200("#80DEEA", R.color.md_cyan_200),
        _300("#4DD0E1", R.color.md_cyan_300),
        _400("#26C6DA", R.color.md_cyan_400),
        _500("#00BCD4", R.color.md_cyan_500),
        _600("#00ACC1", R.color.md_cyan_600),
        _700("#0097A7", R.color.md_cyan_700),
        _800("#00838F", R.color.md_cyan_800),
        _900("#006064", R.color.md_cyan_900),
        _A100("#84FFFF", R.color.md_cyan_A100),
        _A200("#18FFFF", R.color.md_cyan_A200),
        _A400("#00E5FF", R.color.md_cyan_A400),
        _A700("#00B8D4", R.color.md_cyan_A700);

        String color;
        int resource;

        Cyan(String color, int resource) {
            this.color = color;
            this.resource = resource;
        }

        @Override
        public String getAsString() {
            return color;
        }

        @Override
        @ColorInt
        public int getAsColor() {
            return Color.parseColor(color);
        }

        @Override
        @ColorRes
        public int getAsResource() {
            return resource;
        }
    }

    public enum Teal implements IColor {

        _50("#E0F2F1", R.color.md_teal_50),
        _100("#B2DFDB", R.color.md_teal_100),
        _200("#80CBC4", R.color.md_teal_200),
        _300("#4DB6AC", R.color.md_teal_300),
        _400("#26A69A", R.color.md_teal_400),
        _500("#009688", R.color.md_teal_500),
        _600("#00897B", R.color.md_teal_600),
        _700("#00796B", R.color.md_teal_700),
        _800("#00695C", R.color.md_teal_800),
        _900("#004D40", R.color.md_teal_900),
        _A100("#A7FFEB", R.color.md_teal_A100),
        _A200("#64FFDA", R.color.md_teal_A200),
        _A400("#1DE9B6", R.color.md_teal_A400),
        _A700("#00BFA5", R.color.md_teal_A700);

        String color;
        int resource;

        Teal(String color, int resource) {
            this.color = color;
            this.resource = resource;
        }

        @Override
        public String getAsString() {
            return color;
        }

        @Override
        @ColorInt
        public int getAsColor() {
            return Color.parseColor(color);
        }

        @Override
        @ColorRes
        public int getAsResource() {
            return resource;
        }
    }

    public enum Green implements IColor {

        _50("#E8F5E9", R.color.md_green_50),
        _100("#C8E6C9", R.color.md_green_100),
        _200("#A5D6A7", R.color.md_green_200),
        _300("#81C784", R.color.md_green_300),
        _400("#66BB6A", R.color.md_green_400),
        _500("#4CAF50", R.color.md_green_500),
        _600("#43A047", R.color.md_green_600),
        _700("#388E3C", R.color.md_green_700),
        _800("#2E7D32", R.color.md_green_800),
        _900("#1B5E20", R.color.md_green_900),
        _A100("#B9F6CA", R.color.md_green_A100),
        _A200("#69F0AE", R.color.md_green_A200),
        _A400("#00E676", R.color.md_green_A400),
        _A700("#00C853", R.color.md_green_A700);

        String color;
        int resource;

        Green(String color, int resource) {
            this.color = color;
            this.resource = resource;
        }

        @Override
        public String getAsString() {
            return color;
        }

        @Override
        @ColorInt
        public int getAsColor() {
            return Color.parseColor(color);
        }

        @Override
        @ColorRes
        public int getAsResource() {
            return resource;
        }
    }

    public enum LightGreen implements IColor {

        _50("#F1F8E9", R.color.md_light_green_50),
        _100("#DCEDC8", R.color.md_light_green_100),
        _200("#C5E1A5", R.color.md_light_green_200),
        _300("#AED581", R.color.md_light_green_300),
        _400("#9CCC65", R.color.md_light_green_400),
        _500("#8BC34A", R.color.md_light_green_500),
        _600("#7CB342", R.color.md_light_green_600),
        _700("#689F38", R.color.md_light_green_700),
        _800("#558B2F", R.color.md_light_green_800),
        _900("#33691E", R.color.md_light_green_900),
        _A100("#CCFF90", R.color.md_light_green_A100),
        _A200("#B2FF59", R.color.md_light_green_A200),
        _A400("#76FF03", R.color.md_light_green_A400),
        _A700("#64DD17", R.color.md_light_green_A700);

        String color;
        int resource;

        LightGreen(String color, int resource) {
            this.color = color;
            this.resource = resource;
        }

        @Override
        public String getAsString() {
            return color;
        }

        @Override
        @ColorInt
        public int getAsColor() {
            return Color.parseColor(color);
        }

        @Override
        @ColorRes
        public int getAsResource() {
            return resource;
        }
    }

    public enum Lime implements IColor {

        _50("#F9FBE7", R.color.md_lime_50),
        _100("#F0F4C3", R.color.md_lime_100),
        _200("#E6EE9C", R.color.md_lime_200),
        _300("#DCE775", R.color.md_lime_300),
        _400("#D4E157", R.color.md_lime_400),
        _500("#CDDC39", R.color.md_lime_500),
        _600("#C0CA33", R.color.md_lime_600),
        _700("#AFB42B", R.color.md_lime_700),
        _800("#9E9D24", R.color.md_lime_800),
        _900("#827717", R.color.md_lime_900),
        _A100("#F4FF81", R.color.md_lime_A100),
        _A200("#EEFF41", R.color.md_lime_A200),
        _A400("#C6FF00", R.color.md_lime_A400),
        _A700("#AEEA00", R.color.md_lime_A700);

        String color;
        int resource;

        Lime(String color, int resource) {
            this.color = color;
            this.resource = resource;
        }

        @Override
        public String getAsString() {
            return color;
        }

        @Override
        @ColorInt
        public int getAsColor() {
            return Color.parseColor(color);
        }

        @Override
        @ColorRes
        public int getAsResource() {
            return resource;
        }
    }

    public enum Yellow implements IColor {

        _50("#FFFDE7", R.color.md_yellow_50),
        _100("#FFF9C4", R.color.md_yellow_100),
        _200("#FFF59D", R.color.md_yellow_200),
        _300("#FFF176", R.color.md_yellow_300),
        _400("#FFEE58", R.color.md_yellow_400),
        _500("#FFEB3B", R.color.md_yellow_500),
        _600("#FDD835", R.color.md_yellow_600),
        _700("#FBC02D", R.color.md_yellow_700),
        _800("#F9A825", R.color.md_yellow_800),
        _900("#F57F17", R.color.md_yellow_900),
        _A100("#FFFF8D", R.color.md_yellow_A100),
        _A200("#FFFF00", R.color.md_yellow_A200),
        _A400("#FFEA00", R.color.md_yellow_A400),
        _A700("#FFD600", R.color.md_yellow_A700);

        String color;
        int resource;

        Yellow(String color, int resource) {
            this.color = color;
            this.resource = resource;
        }

        @Override
        public String getAsString() {
            return color;
        }

        @Override
        @ColorInt
        public int getAsColor() {
            return Color.parseColor(color);
        }

        @Override
        @ColorRes
        public int getAsResource() {
            return resource;
        }
    }

    public enum Amber implements IColor {

        _50("#FFF8E1", R.color.md_amber_50),
        _100("#FFECB3", R.color.md_amber_100),
        _200("#FFE082", R.color.md_amber_200),
        _300("#FFD54F", R.color.md_amber_300),
        _400("#FFCA28", R.color.md_amber_400),
        _500("#FFCA28", R.color.md_amber_500),
        _600("#FFB300", R.color.md_amber_600),
        _700("#FFA000", R.color.md_amber_700),
        _800("#FF8F00", R.color.md_amber_800),
        _900("#FF6F00", R.color.md_amber_900),
        _A100("#FFE57F", R.color.md_amber_A100),
        _A200("#FFD740", R.color.md_amber_A200),
        _A400("#FFC400", R.color.md_amber_A400),
        _A700("#FFAB00", R.color.md_amber_A700);

        String color;
        int resource;

        Amber(String color, int resource) {
            this.color = color;
            this.resource = resource;
        }

        @Override
        public String getAsString() {
            return color;
        }

        @Override
        @ColorInt
        public int getAsColor() {
            return Color.parseColor(color);
        }

        @Override
        @ColorRes
        public int getAsResource() {
            return resource;
        }
    }

    public enum Orange implements IColor {

        _50("#FFF3E0", R.color.md_orange_50),
        _100("#FFE0B2", R.color.md_orange_100),
        _200("#FFCC80", R.color.md_orange_200),
        _300("#FFB74D", R.color.md_orange_300),
        _400("#FFA726", R.color.md_orange_400),
        _500("#FF9800", R.color.md_orange_500),
        _600("#FB8C00", R.color.md_orange_600),
        _700("#F57C00", R.color.md_orange_700),
        _800("#EF6C00", R.color.md_orange_800),
        _900("#E65100", R.color.md_orange_900),
        _A100("#FFD180", R.color.md_orange_A100),
        _A200("#FFAB40", R.color.md_orange_A200),
        _A400("#FF9100", R.color.md_orange_A400),
        _A700("#FF6D00", R.color.md_orange_A700);

        String color;
        int resource;

        Orange(String color, int resource) {
            this.color = color;
            this.resource = resource;
        }

        @Override
        public String getAsString() {
            return color;
        }

        @Override
        @ColorInt
        public int getAsColor() {
            return Color.parseColor(color);
        }

        @Override
        @ColorRes
        public int getAsResource() {
            return resource;
        }
    }

    public enum DeepOrange implements IColor {

        _50("#FBE9E7", R.color.md_deep_orange_50),
        _100("#FFCCBC", R.color.md_deep_orange_100),
        _200("#FFAB91", R.color.md_deep_orange_200),
        _300("#FF8A65", R.color.md_deep_orange_300),
        _400("#FF7043", R.color.md_deep_orange_400),
        _500("#FF5722", R.color.md_deep_orange_500),
        _600("#F4511E", R.color.md_deep_orange_600),
        _700("#E64A19", R.color.md_deep_orange_700),
        _800("#D84315", R.color.md_deep_orange_800),
        _900("#BF360C", R.color.md_deep_orange_900),
        _A100("#FF6E40", R.color.md_deep_orange_A100),
        _A200("#FFAB40", R.color.md_deep_orange_A200),
        _A400("#FF3D00", R.color.md_deep_orange_A400),
        _A700("#DD2C00", R.color.md_deep_orange_A700);

        String color;
        int resource;

        DeepOrange(String color, int resource) {
            this.color = color;
            this.resource = resource;
        }

        @Override
        public String getAsString() {
            return color;
        }

        @Override
        @ColorInt
        public int getAsColor() {
            return Color.parseColor(color);
        }

        @Override
        @ColorRes
        public int getAsResource() {
            return resource;
        }
    }

    public enum Brown implements IColor {

        _50("#EFEBE9", R.color.md_brown_50),
        _100("#D7CCC8", R.color.md_brown_100),
        _200("#BCAAA4", R.color.md_brown_200),
        _300("#A1887F", R.color.md_brown_300),
        _400("#8D6E63", R.color.md_brown_400),
        _500("#795548", R.color.md_brown_500),
        _600("#6D4C41", R.color.md_brown_600),
        _700("#5D4037", R.color.md_brown_700),
        _800("#4E342E", R.color.md_brown_800),
        _900("#3E2723", R.color.md_brown_900);

        String color;
        int resource;

        Brown(String color, int resource) {
            this.color = color;
            this.resource = resource;
        }

        @Override
        public String getAsString() {
            return color;
        }

        @Override
        @ColorInt
        public int getAsColor() {
            return Color.parseColor(color);
        }

        @Override
        @ColorRes
        public int getAsResource() {
            return resource;
        }
    }

    public enum Grey implements IColor {

        _50("#FAFAFA", R.color.md_grey_50),
        _100("#F5F5F5", R.color.md_grey_100),
        _200("#EEEEEE", R.color.md_grey_200),
        _300("#E0E0E0", R.color.md_grey_300),
        _400("#BDBDBD", R.color.md_grey_400),
        _500("#9E9E9E", R.color.md_grey_500),
        _600("#757575", R.color.md_grey_600),
        _700("#616161", R.color.md_grey_700),
        _800("#424242", R.color.md_grey_800),
        _900("#212121", R.color.md_grey_900);

        String color;
        int resource;

        Grey(String color, int resource) {
            this.color = color;
            this.resource = resource;
        }

        @Override
        public String getAsString() {
            return color;
        }

        @Override
        @ColorInt
        public int getAsColor() {
            return Color.parseColor(color);
        }

        @Override
        @ColorRes
        public int getAsResource() {
            return resource;
        }
    }

    public enum BlueGrey implements IColor {

        _50("#ECEFF1", R.color.md_blue_grey_50),
        _100("#CFD8DC", R.color.md_blue_grey_100),
        _200("#B0BEC5", R.color.md_blue_grey_200),
        _300("#90A4AE", R.color.md_blue_grey_300),
        _400("#78909C", R.color.md_blue_grey_400),
        _500("#607D8B", R.color.md_blue_grey_500),
        _600("#546E7A", R.color.md_blue_grey_600),
        _700("#455A64", R.color.md_blue_grey_700),
        _800("#37474F", R.color.md_blue_grey_800),
        _900("#263238", R.color.md_blue_grey_900);

        String color;
        int resource;

        BlueGrey(String color, int resource) {
            this.color = color;
            this.resource = resource;
        }

        @Override
        public String getAsString() {
            return color;
        }

        @Override
        @ColorInt
        public int getAsColor() {
            return Color.parseColor(color);
        }

        @Override
        @ColorRes
        public int getAsResource() {
            return resource;
        }
    }

    public enum Black implements IColor {

        _1000("#000000", R.color.md_black_1000);

        String color;
        int resource;

        Black(String color, int resource) {
            this.color = color;
            this.resource = resource;
        }

        @Override
        public String getAsString() {
            return color;
        }

        @Override
        @ColorInt
        public int getAsColor() {
            return Color.parseColor(color);
        }

        @Override
        @ColorRes
        public int getAsResource() {
            return resource;
        }
    }


    public enum White implements IColor {

        _1000("#FFFFFF", R.color.md_white_1000);

        String color;
        int resource;

        White(String color, int resource) {
            this.color = color;
            this.resource = resource;
        }

        @Override
        public String getAsString() {
            return color;
        }

        @Override
        @ColorInt
        public int getAsColor() {
            return Color.parseColor(color);
        }

        @Override
        @ColorRes
        public int getAsResource() {
            return resource;
        }
    }
}
