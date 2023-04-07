package com.tymwitko.bogitrip.model

import android.util.Log
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.util.GeoPoint
import kotlin.math.abs

class TurnByTurnNavigator {

    fun getCurrentInstruction(road: Road, step: Int): String? =
        try {
            try {
                Log.d(
                "TAG",
                "mInstructions: ${road.mNodes[step - 1].mInstructions}," +
                        "${road.mNodes[step].mInstructions}, ${road.mNodes[step + 1].mInstructions}"
                )
                road.mNodes[step + 1].mInstructions
            } catch (e: java.lang.IndexOutOfBoundsException){
                road.mNodes[step].mInstructions
            }
        } catch (e: Exception) {
            Log.d("TAG", "ERROR while instructing $e")
            null
        }
}