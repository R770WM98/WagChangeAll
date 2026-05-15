package com.thisismine.myapplication.core.ui

import android.animation.ValueAnimator
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset

data class MotionScheme(
    val reducedMotion: Boolean,
    val fastMillis: Int,
    val baseMillis: Int,
    val emphasizedMillis: Int,
    val navSlideFraction: Float
)

val LocalMotionScheme = compositionLocalOf { defaultMotionScheme() }

fun defaultMotionScheme(reducedMotion: Boolean = false): MotionScheme = MotionScheme(
    reducedMotion = reducedMotion,
    fastMillis = if (reducedMotion) 70 else 150,
    baseMillis = if (reducedMotion) 100 else 220,
    emphasizedMillis = if (reducedMotion) 140 else 300,
    navSlideFraction = if (reducedMotion) 0.04f else 0.1f
)

@Composable
fun rememberSystemReducedMotion(): Boolean = remember {
    !ValueAnimator.areAnimatorsEnabled()
}

fun MotionScheme.navigationEnter(forward: Boolean): EnterTransition {
    if (reducedMotion) return fadeIn(animationSpec = tween(durationMillis = fastMillis))
    val direction = if (forward) 1 else -1
    return fadeIn(animationSpec = tween(durationMillis = emphasizedMillis)) +
        slideInHorizontally(
            initialOffsetX = { width -> (width * navSlideFraction).toInt() * direction },
            animationSpec = tween(durationMillis = emphasizedMillis, easing = FastOutSlowInEasing)
        )
}

fun MotionScheme.navigationExit(forward: Boolean): ExitTransition {
    if (reducedMotion) return fadeOut(animationSpec = tween(durationMillis = fastMillis))
    val direction = if (forward) -1 else 1
    return fadeOut(animationSpec = tween(durationMillis = baseMillis)) +
        slideOutHorizontally(
            targetOffsetX = { width -> (width * navSlideFraction).toInt() * direction },
            animationSpec = tween(durationMillis = baseMillis, easing = FastOutSlowInEasing)
        )
}

fun MotionScheme.dialogEnter(): EnterTransition {
    if (reducedMotion) return fadeIn(animationSpec = tween(durationMillis = fastMillis))
    return fadeIn(animationSpec = tween(durationMillis = baseMillis)) +
        scaleIn(
            initialScale = 0.95f,
            animationSpec = tween(durationMillis = baseMillis, easing = LinearOutSlowInEasing)
        )
}

fun MotionScheme.dialogExit(): ExitTransition {
    if (reducedMotion) return fadeOut(animationSpec = tween(durationMillis = fastMillis))
    return fadeOut(animationSpec = tween(durationMillis = fastMillis)) +
        scaleOut(
            targetScale = 0.98f,
            animationSpec = tween(durationMillis = fastMillis)
        )
}

fun MotionScheme.itemPlacementSpec(): FiniteAnimationSpec<IntOffset> =
    if (reducedMotion) {
        tween(durationMillis = fastMillis)
    } else {
        spring(stiffness = 450f, dampingRatio = 0.9f)
    }

@Composable
fun animatedProgressValue(target: Float): Float {
    val motion = LocalMotionScheme.current
    val progress by animateFloatAsState(
        targetValue = target.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = if (motion.reducedMotion) motion.fastMillis else 500),
        label = "animated-progress"
    )
    return progress
}

@Composable
fun rememberPressScaleModifier(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true
): Modifier {
    val motion = LocalMotionScheme.current
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (!enabled || motion.reducedMotion || !pressed) 1f else 0.98f,
        animationSpec = tween(durationMillis = motion.fastMillis),
        label = "press-scale"
    )
    return Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
