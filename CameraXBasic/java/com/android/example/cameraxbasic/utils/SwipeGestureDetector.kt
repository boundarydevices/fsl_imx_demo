/*
* Copyright 2021 NXP
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.android.example.cameraxbasic.utils

import android.view.GestureDetector
import android.view.MotionEvent
import kotlin.math.abs

class SwipeGestureDetector : GestureDetector.SimpleOnGestureListener() {
    companion object {
        private const val MIN_SWIPE_DISTANCE_X = 100
    }

    var swipeCallback: SwipeCallback? = null

    override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
        if (e1 == null || e2 == null) return super.onFling(e1, e2, velocityX, velocityY)
        val deltaX = e1.x - e2.x
        val deltaXAbs = abs(deltaX)

        if (deltaXAbs >= MIN_SWIPE_DISTANCE_X) {
            if (deltaX > 0) {
                swipeCallback?.onLeftSwipe()
            } else {
                swipeCallback?.onRightSwipe()
            }
        }

        return true
    }

    interface SwipeCallback {
        fun onLeftSwipe()

        fun onRightSwipe()
    }

    fun setSwipeCallback(left: ()-> Unit = {}, right: ()-> Unit = {}) {
        swipeCallback = object : SwipeCallback {
            override fun onLeftSwipe() {
                left()
            }

            override fun onRightSwipe() {
                right()
            }
        }
    }
}
