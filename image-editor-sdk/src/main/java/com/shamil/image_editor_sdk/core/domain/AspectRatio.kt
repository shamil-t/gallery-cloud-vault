package com.shamil.image_editor_sdk.core.domain

/**
 * Common aspect ratios for cropping.
 */
enum class AspectRatio(val title: String, val ratio: Float?) {
    FREE("Free", null),
    SQUARE("1:1", 1f),
    RATIO_4_3("4:3", 4f / 3f),
    RATIO_16_9("16:9", 16f / 9f),
    RATIO_3_2("3:2", 3f / 2f),
    RATIO_2_3("2:3", 2f / 3f),
    RATIO_9_16("9:16", 9f / 16f)
}
