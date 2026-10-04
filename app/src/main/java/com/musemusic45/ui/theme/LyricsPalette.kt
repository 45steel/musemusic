package com.musemusic45.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * 歌词配色。
 *
 * 需求是：文字颜色跟随莫奈主题色 —— **当前行用高对比度的主题色，
 * 非当前行用同色系的低饱和颜色**；背景固定为纯色，不跟随歌曲。
 *
 * 抽成纯函数是因为"低饱和同色系"很容易不小心写成灰色（那样色相就丢了，
 * 莫奈的味道也没了）。单测把"结果仍在主题色与背景色之间"这个性质钉住。
 *
 * 所有函数只做颜色运算，不读 Compose 运行时，因而可以直接单测。
 */
object LyricsPalette {

    /** 非当前行往背景色混的比例。越大越淡、越接近背景。 */
    const val IDLE_MIX = 0.72f

    /** 非当前行再叠一层透明度，形成"半透明弱化"。 */
    const val IDLE_ALPHA = 0.75f

    /** 逐字高亮里"还没唱到"的部分，比非当前行更靠近主题色一点。 */
    const val UNSUNG_MIX = 0.45f

    /** 当前行下方译文的比例：比当前行淡，但仍明显比非当前行醒目。 */
    const val CURRENT_TRANSLATION_MIX = 0.35f

    /** 当前行：直接用高对比度的主题色。 */
    fun current(primary: Color): Color = primary

    /**
     * 把主题色往背景色混，得到**同色系**的低饱和颜色（不是灰）。
     *
     * @param mix 0 = 完全是主题色，1 = 完全是背景色
     */
    fun idle(primary: Color, background: Color, mix: Float = IDLE_MIX): Color =
        lerp(primary, background, mix.coerceIn(0f, 1f))

    /** 非当前行的最终颜色：同色系低饱和 + 半透明。 */
    fun idleFaded(
        primary: Color,
        background: Color,
        mix: Float = IDLE_MIX,
        alpha: Float = IDLE_ALPHA,
    ): Color = idle(primary, background, mix).copy(alpha = alpha.coerceIn(0f, 1f))

    /** 逐字高亮里尚未唱到的部分：介于当前行与非当前行之间。 */
    fun unsung(primary: Color, background: Color, mix: Float = UNSUNG_MIX): Color =
        idle(primary, background, mix)

    /** 当前行译文的颜色：同色系，比当前行淡、比非当前行醒目。 */
    fun currentTranslation(
        primary: Color,
        background: Color,
        mix: Float = CURRENT_TRANSLATION_MIX,
    ): Color = idle(primary, background, mix)
}
