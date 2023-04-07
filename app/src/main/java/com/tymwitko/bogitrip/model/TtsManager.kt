package com.tymwitko.bogitrip.model

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import org.koin.core.component.KoinComponent
import java.util.*


class TtsManager(context: Context): KoinComponent, TextToSpeech.OnInitListener {

    private val tts = TextToSpeech(context, this)

    override fun onInit(p0: Int) {
        // TTS
        if (p0 == TextToSpeech.SUCCESS) {
            // set US English as language for tts
            var lang = Locale.ENGLISH
            //supported langs: ENG, FR, DE, CHN, JAP, KOR, IT
//            if (Locale.getDefault() in Locale.getAvailableLocales()){
//                lang = Locale.getDefault()
//            }
//            val result = tts.setLanguage(lang)
//
//            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
//                Log.e("TTS","The Language specified is not supported!")
//                if (firstCreate) {
//                    firstCreate = false
//                }
//            }
        } else {
            Log.e("TTS", "Initialization Failed!")
        }
    }

    private fun setLang(lang: Locale) {
        if (tts.isLanguageAvailable(lang) == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE ||
            tts.isLanguageAvailable(lang) == TextToSpeech.LANG_AVAILABLE ||
            tts.isLanguageAvailable(lang) == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
            tts.language = lang
        } else {
            Log.e("TTS", "Language is not directly available, code ${tts.isLanguageAvailable(lang)}, whole list has ${tts.availableLanguages?.size} elements")
        }
    }

    fun speak(phrase: String) {
//        forceTtsInit()
        setLang(Locale.ENGLISH)
        tts.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "")
    }
}