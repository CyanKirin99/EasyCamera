package com.example.easycamera.data.model

/**
 * 非理想图像提示类型，用于非理想姊妹版。
 * 6种非理想原因各拍一张，取代原先的ABCD四个角度。
 */
enum class NonIdealPromptType(
    val code: String,
    val label: String
) {
    MOTION_BLUR("MB", "运动模糊"),
    TOO_FAR("TF", "距离太远"),
    TOO_CLOSE("TC", "距离太近"),
    BAD_ANGLE("BA", "角度不佳"),
    OCCLUSION("OC", "遮挡或主体不居中"),
    BAD_EXPOSURE("BE", "拍摄参数错误");

    companion object {
        fun fromCode(code: String): NonIdealPromptType? {
            return entries.firstOrNull { it.code == code }
        }
    }
}