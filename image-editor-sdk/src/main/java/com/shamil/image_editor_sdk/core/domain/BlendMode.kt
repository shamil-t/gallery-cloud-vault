package com.shamil.image_editor_sdk.core.domain

import kotlinx.serialization.Serializable

/**
 * Supported blend modes for layering and effects.
 * Matches standard professional editing software blend modes.
 */
@Serializable
enum class BlendMode {
    NORMAL,
    MULTIPLY,
    SCREEN,
    OVERLAY,
    DARKEN,
    LIGHTEN,
    COLOR_DODGE,
    COLOR_BURN,
    HARD_LIGHT,
    SOFT_LIGHT,
    DIFFERENCE,
    EXCLUSION,
    HUE,
    SATURATION,
    COLOR,
    LUMINOSITY
}
