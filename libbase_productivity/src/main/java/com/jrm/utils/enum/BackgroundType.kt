package com.jrm.utils.enum
/**
 * Enum class defining types of backgrounds that can be set
 */
enum class BackgroundType {
    /** Background from drawable resource */
    DRAWABLE,

    /** Solid color background */
    COLOR,

    /** Gradient background with two colors */
    GRADIENT;

    companion object {
        /**
         * Parse string to BackgroundType
         * @param value String value ("drawable", "color", "gradient")
         * @return BackgroundType or null if invalid
         */
        fun fromString(value: String): BackgroundType? {
            return when (value.lowercase()) {
                "drawable" -> DRAWABLE
                "color" -> COLOR
                "gradient" -> GRADIENT
                else -> null
            }
        }
    }
}
