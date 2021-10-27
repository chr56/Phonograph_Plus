package player.phonograph.ui.activities.intro

import android.os.Bundle
import com.heinrichreimersoftware.materialintro.app.IntroActivity
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide
import player.phonograph.R

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class AppIntroActivity : IntroActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isButtonCtaVisible = true
        isButtonNextVisible = false
        isButtonBackVisible = false
        buttonCtaTintMode = BUTTON_CTA_TINT_MODE_TEXT
        addSlide(
            SimpleSlide.Builder()
                .title(R.string.app_name)
                .description(R.string.welcome_to_phonograph)
                .image(R.drawable.icon_web)
                .background(R.color.md_blue_grey_100)
                .backgroundDark(R.color.md_blue_grey_200)
                .layout(R.layout.fragment_simple_slide_large_image)
                .build()
        )
        addSlide(
            SimpleSlide.Builder()
                .title(R.string.label_playing_queue)
                .description(R.string.open_playing_queue_instruction)
                .image(R.drawable.tutorial_queue_swipe_up)
                .background(R.color.md_deep_purple_500)
                .backgroundDark(R.color.md_deep_purple_600)
                .layout(R.layout.fragment_simple_slide_large_image)
                .build()
        )
        addSlide(
            SimpleSlide.Builder()
                .title(R.string.label_playing_queue)
                .description(R.string.rearrange_playing_queue_instruction)
                .image(R.drawable.tutorial_rearrange_queue)
                .background(R.color.md_indigo_500)
                .backgroundDark(R.color.md_indigo_600)
                .layout(R.layout.fragment_simple_slide_large_image)
                .build()
        )
    }
}